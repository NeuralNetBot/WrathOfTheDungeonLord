package game.shootergame.Item.MeleeWeapons;

import java.util.ArrayList;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import game.shootergame.Item.MeleeWeapon;
import game.shootergame.Physics.Collider;
import game.shootergame.ShooterGame;
import game.shootergame.World;

public class HalberdWeapon implements MeleeWeapon {
    Texture spriteSheetLight;
    Texture spriteSheetHeavy;
    Texture spriteSheetBlock;
    Animation<TextureRegion> animationLight;
    Animation<TextureRegion> animationHeavy;
    Animation<TextureRegion> animationBlock;
    float animTime = 0.0f;
    Sprite sprite;

    boolean attackingLight = false;
    boolean attackingHeavy = false;
    boolean blocking = false;
    boolean playingBlockAnimation = false;

    final float lightDamage = 15.0f;
    final float heavyDamage = 40.0f;

    final float reachLight = 2.5f;
    final float angleReachLight = (float)Math.toRadians(5);

    final float reachHeavy = 2.0f;
    final float angleReachHeavy = (float)Math.toRadians(45);
    final float heavyAttackDamageDelay = 0.5f;
    boolean heavyDidDamage = false;

    final float heavyAttackStaminaUsage = 50.0f;

    final float blockingMultiplier = 0.1f;

    Sound wooshSound;
    Sound hitSound;

    float animFrameSpeed = 0.04167f;

    public HalberdWeapon() {
        ShooterGame.getInstance().am.load("sword_woosh.mp3", Sound.class);
        ShooterGame.getInstance().am.load("sword_hit.mp3", Sound.class);
        ShooterGame.getInstance().am.load("halberd_light.png", Texture.class);
        ShooterGame.getInstance().am.load("halberd_heavy.png", Texture.class);
        ShooterGame.getInstance().am.load("halberd_block.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        wooshSound = ShooterGame.getInstance().am.get("sword_woosh.mp3", Sound.class);
        hitSound = ShooterGame.getInstance().am.get("sword_hit.mp3", Sound.class);
        spriteSheetLight = ShooterGame.getInstance().am.get("halberd_light.png", Texture.class);
        spriteSheetHeavy = ShooterGame.getInstance().am.get("halberd_heavy.png", Texture.class);
        spriteSheetBlock = ShooterGame.getInstance().am.get("halberd_block.png", Texture.class);
        {
            TextureRegion[][] tempFrames = TextureRegion.split(spriteSheetLight, spriteSheetLight.getWidth() / 5, spriteSheetLight.getHeight() / 3);
            TextureRegion[] animFrames = new TextureRegion[5 * 3];
            int index = 0;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 5; j++) {
                    animFrames[index++] = tempFrames[i][j];
                }
            }
            animationLight = new Animation<TextureRegion>(animFrameSpeed, animFrames);
        }
        {
            TextureRegion[][] tempFrames = TextureRegion.split(spriteSheetHeavy, spriteSheetHeavy.getWidth() / 9, spriteSheetHeavy.getHeight() / 5);
            TextureRegion[] animFrames = new TextureRegion[9 * 5];
            int index = 0;
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 9; j++) {
                    animFrames[index++] = tempFrames[i][j];
                }
            }
            animationHeavy = new Animation<TextureRegion>(animFrameSpeed, animFrames);
        }
        {
            TextureRegion[][] tempFrames = TextureRegion.split(spriteSheetBlock, spriteSheetBlock.getWidth() / 8, spriteSheetBlock.getHeight());
            animationBlock = new Animation<TextureRegion>(animFrameSpeed, tempFrames[0]);
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

        if(playingBlockAnimation) {
            animTime += delta;
            sprite.setRegion(animationBlock.getKeyFrame(animTime));
            if(animationBlock.isAnimationFinished(animTime)) {
                animTime = 0.0f;
                playingBlockAnimation = false;
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
        if(blocking) return;
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
        if(blocking) return;
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
        if(attackingHeavy || attackingLight) return;
        if(!blocking) {
            animationBlock.setPlayMode(PlayMode.NORMAL);
            playingBlockAnimation = true;
        }
        blocking = true;
    }

    @Override
    public void endBlock() {
        if(blocking) {
            animationBlock.setPlayMode(PlayMode.REVERSED);
            playingBlockAnimation = true;
        }
        blocking = false;
    }

    @Override
    public float getBlockMultiplier() {
        return blocking ? blockingMultiplier : 1.0f;
    }
}
