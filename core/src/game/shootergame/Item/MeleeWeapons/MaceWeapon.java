package game.shootergame.Item.MeleeWeapons;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import game.shootergame.Item.MeleeWeapon;
import game.shootergame.ShooterGame;
import game.shootergame.World;

public class MaceWeapon implements MeleeWeapon {
    Texture spriteSheet;
    Animation<TextureRegion> animation;
    float animTime = 0.0f;
    Sprite sprite;

    boolean attackingLight = false;

    final float damage = 1.0f;
    final float reach = 0.7f;
    final float angleReach = (float)Math.toRadians(30);

    public MaceWeapon() {
        ShooterGame.getInstance().am.load("sword_light.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        spriteSheet = ShooterGame.getInstance().am.get("sword_light.png", Texture.class);
        TextureRegion[][] tempFrames = TextureRegion.split(spriteSheet, spriteSheet.getWidth() / 4, spriteSheet.getHeight() / 4);
        TextureRegion[] animFrames = new TextureRegion[4 * 4];
        int index = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                animFrames[index++] = tempFrames[i][j];
            }
        }
        animation = new Animation<TextureRegion>(0.04167f, animFrames);
        sprite = new Sprite(animation.getKeyFrame(0.0f));
        sprite.setSize(2.0f * 16.0f / 9.0f, 2.0f);
        sprite.setOriginCenter();
        sprite.setOriginBasedPosition(0.0f, 0.0f);
    }

    @Override
    public void update(float delta) {
        if(attackingLight) {

            animTime += delta;
            sprite.setRegion(animation.getKeyFrame(animTime));
            if(animation.isAnimationFinished(animTime)) {
                animTime = 0.0f;
                attackingLight = false;
            }
            //TODO: make so it doesnt attack every frame while the attack is playing
            World.getPhysicsWorld().runAngleSweep(World.getPlayerCollider(),
                    World.getPlayer().x(), World.getPlayer().y(),
                    (float) Math.toRadians(World.getPlayer().rotation()), angleReach, reach, damage * World.getPlayer().damageMultiplier);
        }
    }

    @Override
    public void renderFirstPerson() {
        sprite.draw(ShooterGame.getInstance().coreBatch);
    }

    @Override
    public void renderThirdPerson() {
    }

    @Override
    public void attackLight() {
        attackingLight = true;
    }

    @Override
    public void attackHeavy() {
    }

    @Override
    public void beginBlock() {
    }

    @Override
    public void endBlock() {
    }
}
