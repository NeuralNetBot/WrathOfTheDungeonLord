package game.shootergame.Item.MeleeWeapons;

import java.util.ArrayList;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import game.shootergame.ShooterGame;
import game.shootergame.Item.MeleeWeapon;
import game.shootergame.Physics.Collider;
import game.shootergame.World;

public class SwordWeapon implements MeleeWeapon{

    Texture spriteSheet;
    Animation<TextureRegion> animation;
    float animTime = 0.0f;
    Sprite sprite;

    boolean attackingLight = false;

    final float lightDamage = 10.0f;
    final float heavyDamage = 25.0f;

    final float reach = 1.0f;
    final float angleReach = (float)Math.toRadians(30);

    Sound wooshSound;
    Sound hitSound;

    float animFrameSpeed = 0.04167f;

    public SwordWeapon() {
        ShooterGame.getInstance().am.load("sword_woosh.mp3", Sound.class);
        ShooterGame.getInstance().am.load("sword_hit.mp3", Sound.class);
        ShooterGame.getInstance().am.load("sword_light.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        wooshSound = ShooterGame.getInstance().am.get("sword_woosh.mp3", Sound.class);
        hitSound = ShooterGame.getInstance().am.get("sword_hit.mp3", Sound.class);
        spriteSheet = ShooterGame.getInstance().am.get("sword_light.png", Texture.class);
        TextureRegion[][] tempFrames = TextureRegion.split(spriteSheet, spriteSheet.getWidth() / 4, spriteSheet.getHeight() / 4);
        TextureRegion[] animFrames = new TextureRegion[4 * 4];
        int index = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                animFrames[index++] = tempFrames[i][j];
            }
        }
        animation = new Animation<TextureRegion>(animFrameSpeed, animFrames);
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
            animation.setFrameDuration(animFrameSpeed / World.getPlayer().attackSpeed);
            wooshSound.play();
            ArrayList<Collider> hits = World.getPhysicsWorld().runAngleSweep(World.getPlayerCollider(),
              World.getPlayer().x(), World.getPlayer().y(),
               (float) Math.toRadians(World.getPlayer().rotation()), angleReach, reach, lightDamage * World.getPlayer().damageMultiplier);

            for (Collider collider : hits) {
                if(!collider.isStatic) {
                    hitSound.play(1.5f);
                    break;
                }
            }
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
