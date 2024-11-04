package game.shootergame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;

import game.shootergame.Item.MeleeWeapon;
import game.shootergame.Item.Powerup;
import game.shootergame.Item.RangedWeapon;
import game.shootergame.Physics.Collider;

import java.util.Iterator;
import java.util.LinkedList;

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
    float dx = 0.0f, dy = 0.0f;

    float rotation;

    float moveDirX;
    float moveDirY;

    final float moveSpeed = 4.0f;

    boolean isDodging;

    int lastMouse = 0;

    Collider collider;

    LinkedList<Powerup> activePowerups;

    public Player(MeleeWeapon melee) {
        this.melee = melee;
        ranged = null;

        health = 50.0f;
        stamina = 100.0f;

        damageMultiplier = 1.0f;
        resistanceMultiplier = 1.0f;
        attackSpeed = 1.0f;

        collider = new Collider(x, y, 0.5f,  (Collider collider, float newDX, float newDY)->{
            if(collider == null) { //wall coll
                dx = newDX; dy = newDY;
            }
        }, false, 1.3f);
        World.getPhysicsWorld().addCollider(collider);

        activePowerups = new LinkedList<>();
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

        x += dx;
        y += dy;
        collider.x = x;
        collider.y = y;

        float rotationR = (float)Math.toRadians(rotation);

        float speed = moveSpeed * delta;

        dx = speed * (moveDirX * (float)Math.cos(rotationR) - moveDirY * (float)Math.sin(rotationR));
        dy = speed * (moveDirX * (float)Math.sin(rotationR) + moveDirY * (float)Math.cos(rotationR));
        collider.dx = dx;
        collider.dy = dy;



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

        collider.x = x;
        collider.y = y;

        Iterator<Powerup> iterator = activePowerups.iterator();
        while (iterator.hasNext()) {
            Powerup powerup = iterator.next();
            powerup.update(delta);
            if (!powerup.isActive()) {
                powerup.onTimeout(this);
                iterator.remove();
            }
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

    public Collider getCollider() {
        return collider;
    }

    public void addPowerup(Powerup powerup) {
        activePowerups.add(powerup);
        powerup.onActivate(this);
    }

    public float getHealth() { return health; }

    public void addHealth(float health) {
        this.health = Math.min(this.health + health, 100.0f);
    }

    public LinkedList<Powerup> getActivePowerups() { return activePowerups; }
}
