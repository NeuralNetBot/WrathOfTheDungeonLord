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

    Texture spriteSheetLight;
    Texture spriteSheetHeavy;
    Animation<TextureRegion> animationLight;
    Animation<TextureRegion> animationHeavy;
    float animTime = 0.0f;
    Sprite sprite;

    boolean attackingLight = false;
    boolean attackingHeavy = false;

    final float lightDamage = 10.0f;
    final float heavyDamage = 25.0f;

    final float reachLight = 1.5f;
    final float angleReachLight = (float)Math.toRadians(30);

    final float reachHeavy = 2.0f;
    final float angleReachHeavy = (float)Math.toRadians(15);
    final float heavyAttackDamageDelay = 0.5f;
    boolean heavyDidDamage = false;

    final float heavyAttackStaminaUsage = 50.0f;

    Sound wooshSound;
    Sound hitSound;

    float animFrameSpeed = 0.04167f;

    public SwordWeapon() {
        ShooterGame.getInstance().am.load("sword_woosh.mp3", Sound.class);
        ShooterGame.getInstance().am.load("sword_hit.mp3", Sound.class);
        ShooterGame.getInstance().am.load("sword_light.png", Texture.class);
        ShooterGame.getInstance().am.load("sword_heavy.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        wooshSound = ShooterGame.getInstance().am.get("sword_woosh.mp3", Sound.class);
        hitSound = ShooterGame.getInstance().am.get("sword_hit.mp3", Sound.class);
        spriteSheetLight = ShooterGame.getInstance().am.get("sword_light.png", Texture.class);
        spriteSheetHeavy = ShooterGame.getInstance().am.get("sword_heavy.png", Texture.class);
        {
            TextureRegion[][] tempFrames = TextureRegion.split(spriteSheetLight, spriteSheetLight.getWidth() / 4, spriteSheetLight.getHeight() / 4);
            TextureRegion[] animFrames = new TextureRegion[4 * 4];
            int index = 0;
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    animFrames[index++] = tempFrames[i][j];
                }
            }
            animationLight = new Animation<TextureRegion>(animFrameSpeed, animFrames);
        }
        {
            TextureRegion[][] tempFrames = TextureRegion.split(spriteSheetHeavy, spriteSheetHeavy.getWidth() / 8, spriteSheetHeavy.getHeight() / 5);
            TextureRegion[] animFrames = new TextureRegion[8 * 5];
            int index = 0;
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 8; j++) {
                    animFrames[index++] = tempFrames[i][j];
                }
            }
            animationHeavy = new Animation<TextureRegion>(animFrameSpeed, animFrames);
        }
        sprite = new Sprite(animationLight.getKeyFrame(0.0f));
        sprite.setSize(2.0f * 16.0f / 9.0f, 2.0f);
        sprite.setOriginCenter();
        sprite.setOriginBasedPosition(0.0f, 0.0f);
    }

    @Override
    public void update(float delta) {
        if(attackingLight) {
            animTime += delta;
            sprite.setRegion(animationLight.getKeyFrame(animTime));
            if(animationLight.isAnimationFinished(animTime)) {
                animTime = 0.0f;
                attackingLight = false;
            }
        }
        if(attackingHeavy) {
            if(animTime >= heavyAttackDamageDelay && !heavyDidDamage) {
                heavyDidDamage = true;
                wooshSound.play();
                World.getPlayer().removeStamina(heavyAttackStaminaUsage);
                ArrayList<Collider> hits = World.getPhysicsWorld().runAngleSweep(World.getPlayerCollider(),
                World.getPlayer().x(), World.getPlayer().y(),
                 (float) Math.toRadians(World.getPlayer().rotation()), angleReachHeavy, reachHeavy, heavyDamage * World.getPlayer().damageMultiplier);
  
                for (Collider collider : hits) {
                    if(!collider.isStatic) {
                        hitSound.play(1.5f);
                        break;
                    }
                }
            }
            animTime += delta;
            sprite.setRegion(animationHeavy.getKeyFrame(animTime));
            if(animationHeavy.isAnimationFinished(animTime)) {
                animTime = 0.0f;
                attackingHeavy = false;
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
        if(attackingHeavy) return;
        if(!attackingLight) {
            animationLight.setFrameDuration(animFrameSpeed / World.getPlayer().attackSpeed);
            wooshSound.play();
            ArrayList<Collider> hits = World.getPhysicsWorld().runAngleSweep(World.getPlayerCollider(),
              World.getPlayer().x(), World.getPlayer().y(),
               (float) Math.toRadians(World.getPlayer().rotation()), angleReachLight, reachLight, lightDamage * World.getPlayer().damageMultiplier);

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
        if(World.getPlayer().getStamina() < heavyAttackStaminaUsage) return;
        if(attackingLight) return;
        if(!attackingHeavy) {
            heavyDidDamage = false;
            animationHeavy.setFrameDuration(animFrameSpeed / World.getPlayer().attackSpeed);
        }
        attackingHeavy = true;
    }

    @Override
    public void beginBlock() {
    }

    @Override
    public void endBlock() {
    }
    
}
