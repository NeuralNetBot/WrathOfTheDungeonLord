package game.shootergame.Item.MeleeWeapons;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import game.shootergame.Item.MeleeWeapon;
import game.shootergame.ShooterGame;
import game.shootergame.World;

public class HalberdWeapon implements MeleeWeapon {
    Texture spriteSheet;
    Animation<TextureRegion> animation;
    float animTime = 0.0f;
    Sprite sprite;

    boolean attackingLight = false;

    final float lightDamage = 15.0f;
    final float heavyDamage = 40.0f;

    final float reach = 1.3f;
    final float angleReach = (float)Math.toRadians(30);

    public HalberdWeapon() {
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
        if(!attackingLight) {
            World.getPhysicsWorld().runAngleSweep(World.getPlayerCollider(),
              World.getPlayer().x(), World.getPlayer().y(),
               (float) Math.toRadians(World.getPlayer().rotation()), angleReach, reach, lightDamage * World.getPlayer().damageMultiplier);
        }
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
