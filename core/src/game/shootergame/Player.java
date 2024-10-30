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

    float moveDirX;
    float moveDirY;

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

    void processInput() {

        rotation += Gdx.input.getDeltaX() * 0.1f;

        moveDirX = 0.0f;
        moveDirY = 0.0f;
        if(Gdx.input.isKeyPressed(Keys.W)) {
            moveDirX += 1;
        }
        if(Gdx.input.isKeyPressed(Keys.S)) {
            moveDirX -= 1;
        }
        if(Gdx.input.isKeyPressed(Keys.A)) {
            moveDirY -= 1;
        }
        if(Gdx.input.isKeyPressed(Keys.D)) {
            moveDirY += 1;
        }
    }

    void update(float delta) {
        float rotationR = (float)Math.toRadians(rotation);

        float speed = moveSpeed * delta;

        float moveX = moveDirX * (float)Math.cos(rotationR) - moveDirY * (float)Math.sin(rotationR);
        float moveY = moveDirX * (float)Math.sin(rotationR) + moveDirY * (float)Math.cos(rotationR);

        x += moveX * speed;
        y += moveY * speed;
    }

    void applyDamage(float damage) {
        if(!isDodging)
            health -= damage * resistanceMultiplier;
    }
}
