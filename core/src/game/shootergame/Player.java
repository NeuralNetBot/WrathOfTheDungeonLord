package game.shootergame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;

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
    float rotation;

    final float moveSpeed = 2.0f;

    boolean isDodging;

    int lastMouse = 0;

    public Player() {
        ranged = null;

        health = 100.0f;
        stamina = 100.0f;

        damageMultiplier = 1.0f;
        resistanceMultiplier = 1.0f;
        attackSpeed = 1.0f;
    }

    public float x() { return x; }
    public float y() { return y; }
    public float rotation() { return rotation; }

    void update(float delta) {
        rotation += Gdx.input.getDeltaX() * 0.1f;

        float rotationR = (float)Math.toRadians(rotation);

        float speed = moveSpeed * delta;
        if(Gdx.input.isKeyPressed(Keys.W)) {
            x += Math.cos(rotationR) * speed;
            y += Math.sin(rotationR) * speed;
        }
        if(Gdx.input.isKeyPressed(Keys.S)) {
            x -= Math.cos(rotationR) * speed;
            y -= Math.sin(rotationR) * speed;
        }
        if(Gdx.input.isKeyPressed(Keys.A)) {
            x -= Math.cos(rotationR + Math.PI / 2.0f) * speed;
            y -= Math.sin(rotationR + Math.PI / 2.0f) * speed;
        }
        if(Gdx.input.isKeyPressed(Keys.D)) {
            x += Math.cos(rotationR + Math.PI / 2.0f) * speed;
            y += Math.sin(rotationR + Math.PI / 2.0f) * speed;
        }
    }

    void applyDamage(float damage) {
        if(!isDodging)
            health -= damage * resistanceMultiplier;
    }
}
