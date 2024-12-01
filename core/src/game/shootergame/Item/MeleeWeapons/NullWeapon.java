package game.shootergame.Item.MeleeWeapons;

import game.shootergame.Item.MeleeWeapon;

public class NullWeapon implements MeleeWeapon {

    @Override
    public void update(float delta) {
    }

    @Override
    public void renderFirstPerson() {
    }

    @Override
    public void renderThirdPerson() {
    }

    @Override
    public void attackLight() {
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

    @Override
    public float getBlockMultiplier() {
        return 1.0f;
    }
    
}
