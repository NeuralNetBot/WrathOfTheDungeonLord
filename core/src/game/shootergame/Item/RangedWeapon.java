package game.shootergame.Item;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public interface RangedWeapon {
    void update(float delta);
    void renderFirstPerson();
    void renderThirdPerson();
    void fire();
    boolean isReloading();
    int getAmmo();
    boolean isHeld();
    void setAmmo(int ammo);
    TextureRegion getItemTexture();
    String getName();
}
