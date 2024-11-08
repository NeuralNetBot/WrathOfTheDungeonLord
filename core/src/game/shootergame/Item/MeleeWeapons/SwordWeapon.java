package game.shootergame.Item.MeleeWeapons;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import game.shootergame.Physics.Collider;
import game.shootergame.ShooterGame;
import game.shootergame.Item.MeleeWeapon;
import game.shootergame.World;

public class SwordWeapon implements MeleeWeapon{

    Texture spriteSheet;
    Animation<TextureRegion> animation;
    float animTime = 0.0f;
    Sprite sprite;

    boolean attackingLight = false;

    private Collider collider;
    private float swingAngle;
    private float swingRange;
    private float weaponLength;

    public SwordWeapon() {
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

        this.weaponLength = 1.0f;
        this.swingRange = 0.7f;
        createCollider();

        World.getPhysicsWorld().addCollider(collider);
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

            swingAngle += delta;
            if (swingAngle > swingRange) {
                swingAngle = 0;
            }

            float angle = (float) Math.toRadians(World.getPlayer().rotation() + swingAngle);
            float weaponEndX = World.getPlayer().x() + weaponLength * (float) Math.cos(angle);
            float weaponEndY = World.getPlayer().y() + weaponLength * (float) Math.sin(angle);

            //collider.setPosition(weaponEndX, weaponEndY);
           // collider.setRotation(angle);

            System.out.println("X: " + weaponEndX + "  Y: " + weaponEndY);
        }
    }

    private void createCollider() {
        Collider tempCollider = new Collider(5.0f, 0.0f, 0.1f, null);
        World.getPhysicsWorld().addCollider(tempCollider);

        collider = new Collider(0, 0, weaponLength, (Collider collider, float newDx, float newDy)->{
            if (collider == tempCollider) {
                System.out.println("Damage");
            }
        });
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
