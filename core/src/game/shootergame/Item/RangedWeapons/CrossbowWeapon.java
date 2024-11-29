package game.shootergame.Item.RangedWeapons;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import game.shootergame.Item.RangedWeapon;
import game.shootergame.Physics.Collider;
import game.shootergame.ShooterGame;
import game.shootergame.World;

public class CrossbowWeapon implements RangedWeapon {
    Texture spriteSheetFire;
    Texture spriteSheetReload;
    Animation<TextureRegion> animationFire;
    Animation<TextureRegion> animationReload;
    float animTime = 0.0f;
    float pauseTime = 0.0f;
    final float maxPauseTime = 0.3f;
    Sprite sprite;

    TextureRegion reg;
    Texture tex;

    boolean firing = false;
    boolean reloading = false;
    boolean held = false;

    int ammo = 10;

    final float damage = 30.0f;

    Sound soundFire;
    Sound soundReload;

    public CrossbowWeapon() {
        ShooterGame.getInstance().am.load("crossbow_fire.png", Texture.class);
        ShooterGame.getInstance().am.load("crossbow_reload.png", Texture.class);
        ShooterGame.getInstance().am.load("crossbow_fire.mp3", Sound.class);
        ShooterGame.getInstance().am.load("crossbow_reload.mp3", Sound.class);
        ShooterGame.getInstance().am.finishLoading();
        spriteSheetFire = ShooterGame.getInstance().am.get("crossbow_fire.png", Texture.class);
        spriteSheetReload = ShooterGame.getInstance().am.get("crossbow_reload.png", Texture.class);
        soundFire = ShooterGame.getInstance().am.get("crossbow_fire.mp3", Sound.class);
        soundReload = ShooterGame.getInstance().am.get("crossbow_reload.mp3", Sound.class);
        {
        TextureRegion[][] tempFrames = TextureRegion.split(spriteSheetFire, spriteSheetFire.getWidth() / 2, spriteSheetFire.getHeight());
        animationFire = new Animation<TextureRegion>(0.04167f, tempFrames[0]);
        }
        {
            TextureRegion[][] tempFrames = TextureRegion.split(spriteSheetReload, spriteSheetReload.getWidth() / 4, spriteSheetReload.getHeight() / 3);
            TextureRegion[] animFrames = new TextureRegion[4 * 3];
            int index = 0;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 4; j++) {
                    animFrames[index++] = tempFrames[i][j];
                }
            }
            animationReload = new Animation<TextureRegion>(0.04167f, animFrames);
        }

        sprite = new Sprite(animationFire.getKeyFrame(0.0f));
        sprite.setSize(2.0f * 16.0f / 9.0f, 2.0f);
        sprite.setOriginCenter();
        sprite.setOriginBasedPosition(0.0f, 0.0f);

        ShooterGame.getInstance().am.load("ranged_ammo_pickups.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        tex = ShooterGame.getInstance().am.get("ranged_ammo_pickups.png", Texture.class);
        reg = new TextureRegion(tex, 0, 256, 256, 256);
    }



    @Override
    public void update(float delta) {
        if(reloading) {
            pauseTime += delta;
            if(pauseTime >= maxPauseTime) {
                animTime += delta;
                sprite.setRegion(animationReload.getKeyFrame(animTime));
                if(animationReload.isAnimationFinished(animTime)) {
                    animTime = 0.0f;
                    reloading = false;
                    pauseTime = 0.0f;
                }
            }
        }
        if (firing) {
            animTime += delta;
            sprite.setRegion(animationFire.getKeyFrame(animTime));
            if(animationFire.isAnimationFinished(animTime)) {
                animTime = 0.0f;
                firing = false;
                if(ammo > 0) {
                    reloading = true;
                    soundReload.play();
                }
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
    public void fire() {
        if(reloading) return;
        if(ammo > 0) {
            soundFire.play();
            firing = true;
            ammo--;
            Vector2 d = new Vector2(World.getPlayer().dx(), World.getPlayer().dy()).nor();
            Collider hitCollider = World.getPhysicsWorld().rayCast(World.getPlayerCollider(), World.getPlayer().x(), World.getPlayer().y(), d.x, d.y);
            if(hitCollider != null) {
                hitCollider.Callback(World.getPlayerCollider(), 0, 0, damage * World.getPlayer().damageMultiplier);
            }
        }
    }

    @Override
    public boolean isReloading() {
        return reloading;
    }

    @Override
    public int getAmmo() {
        return ammo;
    }

    @Override
    public boolean isHeld() {
        return held;
    }

    @Override
    public void setAmmo(int ammo) {
        this.ammo += ammo;
    }

    @Override
    public TextureRegion getItemTexture() {
        return reg;
    }

    @Override
    public String getName() {
        return "Crossbow";
    }
}
