package game.shootergame.Item;

public interface RangedWeapon {
    void update(float delta);
    void renderFirstPerson();
    void renderThirdPerson();
    void fire();
    boolean isReloading();
    int getAmmo();
    boolean isHeld();
    void setAmmo(int ammo);
}
