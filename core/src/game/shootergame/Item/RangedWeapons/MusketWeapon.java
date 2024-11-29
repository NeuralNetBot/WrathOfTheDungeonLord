package game.shootergame.Item.RangedWeapons;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import game.shootergame.Item.RangedWeapon;
import game.shootergame.Physics.Collider;
import game.shootergame.ShooterGame;
import game.shootergame.World;

public class MusketWeapon implements RangedWeapon {
    Texture spriteSheetFire;
    Texture spriteSheetReload;
    Animation<TextureRegion> animationFire;
    Animation<TextureRegion> animationReload;
    float animTime = 0.0f;
    float pauseTime = 0.0f;
    final float maxPauseTime = 0.0f;
    Sprite sprite;

    boolean firing = false;
    boolean reloading = false;
    boolean held = false;

    int ammo = 10;

    final float damage = 50.0f;

    public MusketWeapon() {
        ShooterGame.getInstance().am.load("musket_fire.png", Texture.class);
        ShooterGame.getInstance().am.load("musket_reload.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        spriteSheetFire = ShooterGame.getInstance().am.get("musket_fire.png", Texture.class);
        spriteSheetReload = ShooterGame.getInstance().am.get("musket_reload.png", Texture.class);
        {
            TextureRegion[][] tempFrames = TextureRegion.split(spriteSheetFire, spriteSheetFire.getWidth() / 6, spriteSheetFire.getHeight() / 6);
            TextureRegion[] animFrames = new TextureRegion[6 * 6];
            int index = 0;
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < 6; j++) {
                    animFrames[index++] = tempFrames[i][j];
                }
            }
            animationFire = new Animation<TextureRegion>(0.04167f, animFrames);
        }
        {
            TextureRegion[][] tempFrames = TextureRegion.split(spriteSheetReload, spriteSheetReload.getWidth() / 8, spriteSheetReload.getHeight() / 6);
            TextureRegion[] animFrames = new TextureRegion[8 * 6];
            int index = 0;
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < 8; j++) {
                    animFrames[index++] = tempFrames[i][j];
                }
            }
            animationReload = new Animation<TextureRegion>(0.04167f, animFrames);
        }

        sprite = new Sprite(animationFire.getKeyFrame(0.0f));
        sprite.setSize(2.0f * 16.0f / 9.0f, 2.0f);
        sprite.setOriginCenter();
        sprite.setOriginBasedPosition(0.0f, 0.0f);
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
                reloading = true;
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
}
