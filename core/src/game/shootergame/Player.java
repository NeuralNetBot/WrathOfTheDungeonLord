package game.shootergame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;

import game.shootergame.Item.MeleeWeapon;
import game.shootergame.Item.RangedWeapon;

public class Player {
    MeleeWeapon melee;
    RangedWeapon ranged;

    int selectedWeapon = 1;//1 melee, 2 ranged if equiped

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

    public Player(MeleeWeapon melee) {
        this.melee = melee;
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

        if(Gdx.input.isKeyPressed(Keys.NUM_1)) {
            selectedWeapon = 1;
        }
        if(ranged != null && Gdx.input.isKeyPressed(Keys.NUM_2)) {
            selectedWeapon = 2;
        }

        if(Gdx.input.isButtonJustPressed(Buttons.LEFT)) {
            switch (selectedWeapon) {
            case 1:
                melee.attackLight();
                break;
            case 2:
                ranged.fire();
                break;
            default:
                break;
            }
        }

        if(Gdx.input.isButtonJustPressed(Buttons.LEFT)) {
            switch (selectedWeapon) {
            case 1:
                melee.attackHeavy();
                break;
            default:
                break;
            }
        }
    }

    void update(float delta) {
        float rotationR = (float)Math.toRadians(rotation);

        float speed = moveSpeed * delta;

        float moveX = moveDirX * (float)Math.cos(rotationR) - moveDirY * (float)Math.sin(rotationR);
        float moveY = moveDirX * (float)Math.sin(rotationR) + moveDirY * (float)Math.cos(rotationR);

        x += moveX * speed;
        y += moveY * speed;

        switch (selectedWeapon) {
        case 1:
        melee.update(delta);
            break;
        case 2:
        if(ranged != null) {
            ranged.update(delta);
        }
        default:
            break;
        }
    }

    void render() {
        switch (selectedWeapon) {
        case 1:
        melee.renderFirstPerson();
            break;
        case 2:
        if(ranged != null) {
            ranged.renderFirstPerson();
        }
        default:
            break;
        }
    }

    void applyDamage(float damage) {
        if(!isDodging)
            health -= damage * resistanceMultiplier;
    }
}
