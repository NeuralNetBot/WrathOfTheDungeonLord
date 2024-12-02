package game.shootergame.Item;

public interface MeleeWeapon {
    void update(float delta);
    void renderFirstPerson();
    void renderThirdPerson();
    void attackLight();
    void attackHeavy();
    void beginBlock();
    void endBlock();
    float getBlockMultiplier();
}
