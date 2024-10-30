package game.shootergame;

import game.shootergame.Item.MeleeWeapon;
import game.shootergame.Item.RangedWeapon;

public class Player {
    MeleeWeapon melee;
    RangedWeapon ranged;

    float health;
    float stamina;
    
    public float damageMultiplier;
    public float resistanceMultiplier;
    public float attackSpeed;

    float x, y;

    boolean isDodging;

    public Player() {
        ranged = null;

        health = 100.0f;
        stamina = 100.0f;

        damageMultiplier = 1.0f;
        resistanceMultiplier = 1.0f;
        attackSpeed = 1.0f;
    }

    void update(float delta) {

    }

    void applyDamage(float damage) {
        if(!isDodging)
            health -= damage * resistanceMultiplier;
    }
}
