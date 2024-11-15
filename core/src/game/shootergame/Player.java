package game.shootergame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;

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

    final float maxHealth = 100.0f;
    final float maxStamina = 100.0f;
    float health;
    final float staminaRegenPerSecond = 20.0f;
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
    final float maxDodgeTime = 0.3f;
    final float dodgeSpeedMultiplier = 3.0f;
    final float dodgeStaminaCost = 20.0f;
    float dodgeTime;

    int lastMouse = 0;

    Collider collider;

    LinkedList<Powerup> activePowerups;

    Sprite barSprite;

    public Player(MeleeWeapon melee) {
        this.melee = melee;
        ranged = null;

        health = maxHealth;
        stamina = maxStamina;

        damageMultiplier = 1.0f;
        resistanceMultiplier = 1.0f;
        attackSpeed = 1.0f;

        ShooterGame.getInstance().am.load("bar.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        barSprite = new Sprite(ShooterGame.getInstance().am.get("bar.png", Texture.class));
        barSprite.setOrigin(0, 0);

        collider = new Collider(x, y, 0.5f,  (Collider collider, float newDX, float newDY, float damage)->{
            if(collider == null) { //wall coll
                dx = newDX; dy = newDY;
            }
            if(damage != 0.0f) {
                float damageDone = isDodging ? 0.0f : damage * resistanceMultiplier;
                health -= damageDone;
            }
        }, false, 1.3f);
        World.getPhysicsWorld().addCollider(collider);

        activePowerups = new LinkedList<>();
    }

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

        if(Gdx.input.isKeyJustPressed(Keys.SPACE)) {
            if(!isDodging && (moveDirX != 0.0f || moveDirY != 0.0f)) {
                if(stamina >= dodgeStaminaCost) {
                    isDodging = true;
                    dodgeTime = 0.0f;
                    stamina -= dodgeStaminaCost;
                }
            }
        }
    }

    public float x() { return x; }
    public float y() { return y; }
    public float dx() { return dx; }
    public float dy() { return dy; }
    public float rotation() { return rotation; }

    void update(float delta) {
        if(isDodging) {
            dodgeTime += delta;
            if(dodgeTime >= maxDodgeTime) {
                isDodging = false;
            }
        }
        
        stamina += delta * staminaRegenPerSecond;
        if(stamina > maxStamina) stamina = maxStamina;

        rotation += Gdx.input.getDeltaX() * 0.1f;
        float rotationR = (float)Math.toRadians(rotation);

        x += dx;
        y += dy;
        collider.x = x;
        collider.y = y;

        float finalDodgeSpeed = 1.0f;
        if(isDodging) {
            finalDodgeSpeed = 1.0f + 2.0f * (float)Math.sin((dodgeTime / maxDodgeTime) * Math.PI);
        }
        float speed = moveSpeed * delta * finalDodgeSpeed;

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

        Color healthColor = new Color().fromHsv((health / maxHealth) * 120.0f, 1.0f, 1.0f);
        healthColor.a = 1.0f;
        float aspect = (float)Gdx.graphics.getWidth() / (float)Gdx.graphics.getHeight();

        barSprite.setPosition(-aspect + 0.1f, 0.9f);

        barSprite.setSize(1.0f, 0.03f);
        barSprite.setColor(Color.BLACK);
        barSprite.draw(ShooterGame.getInstance().coreBatch);
        barSprite.setSize(health / maxHealth, 0.03f);
        barSprite.setColor(healthColor);
        barSprite.draw(ShooterGame.getInstance().coreBatch);

        barSprite.setPosition(-aspect + 0.1f, 0.8f);

        barSprite.setSize(1.0f, 0.03f);
        barSprite.setColor(Color.BLACK);
        barSprite.draw(ShooterGame.getInstance().coreBatch);
        barSprite.setSize(stamina / maxStamina, 0.03f);
        barSprite.setColor(Color.GOLDENROD);
        barSprite.draw(ShooterGame.getInstance().coreBatch);
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
