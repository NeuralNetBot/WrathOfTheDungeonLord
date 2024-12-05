package game.shootergame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import game.shootergame.Item.MeleeWeapon;
import game.shootergame.Item.Powerup;
import game.shootergame.Item.RangedWeapon;
import game.shootergame.Network.RemotePlayer;
import game.shootergame.Physics.Collider;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

public class Player {
    MeleeWeapon melee;
    RangedWeapon ranged;

    int selectedWeapon = 1;//1 melee, 2 ranged if equiped

    final float maxHealth = 100.0f;
    final float maxStamina = 100.0f;
    float health;
    final float staminaRegenPerSecond = 45.0f;
    float stamina;
    
    public float damageMultiplier;
    public float resistanceMultiplier;
    public float attackSpeed;

    float x, y;
    float dx = 0.0f, dy = 0.0f;
    float distanceMoved = 0.0f;

    float rotation;

    float moveDirX;
    float moveDirY;

    final float moveSpeed = 4.0f;

    boolean isDodging;
    final float maxDodgeTime = 0.3f;
    final float dodgeSpeedMultiplier = 3.0f;
    final float dodgeStaminaCost = 20.0f;
    final float staminaRegenDelayMax = 0.85f;
    boolean isDelaying;
    float dodgeTime;
    float staminaRegenDelay;

    final float regenCheckpointPercentage = 25.0f;
    final float damageRegenDelayTime = 5.0f;
    float regenDelayTimer = 0.0f;
    final float regenRate = 10.0f;

    int lastMouse = 0;

    Collider collider;

    LinkedList<Powerup> activePowerups;

    Sprite barSprite;
    Sprite powerupSprite;
    Sprite crosshairSprite;
    Texture tex;
    TextureRegion reg;

    Sound footstepSound;

    Sprite fullscreenYouDied;
    float respawnTimer = 0.0f;
    final float respawnTime = 5.0f;
    float deathPosX = 0.0f;
    float deathPosY = 0.0f;
    boolean justDied = false;

    public Player(MeleeWeapon melee) {
        this.melee = melee;
        ranged = null;

        health = maxHealth;
        stamina = maxStamina;

        damageMultiplier = 1.0f;
        resistanceMultiplier = 1.0f;
        attackSpeed = 1.0f;

        ShooterGame.getInstance().am.load("footstep.mp3", Sound.class);
        ShooterGame.getInstance().am.load("bar.png", Texture.class);
        ShooterGame.getInstance().am.load("powerups.png", Texture.class);
        ShooterGame.getInstance().am.load("dead.png", Texture.class);
        ShooterGame.getInstance().am.load("crosshair.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        footstepSound = ShooterGame.getInstance().am.get("footstep.mp3", Sound.class);   
        tex = ShooterGame.getInstance().am.get("powerups.png", Texture.class);
        barSprite = new Sprite(ShooterGame.getInstance().am.get("bar.png", Texture.class));
        barSprite.setOrigin(0, 0);
        fullscreenYouDied = new Sprite(ShooterGame.getInstance().am.get("dead.png", Texture.class));
        fullscreenYouDied.setSize(4.0f, 2.0f);
        fullscreenYouDied.setOriginCenter();
        fullscreenYouDied.setOriginBasedPosition(0.0f, 0.0f);
        crosshairSprite = new Sprite(ShooterGame.getInstance().am.get("crosshair.png", Texture.class));
        crosshairSprite.setSize(0.11f, 0.11f);
        crosshairSprite.setOriginCenter();
        crosshairSprite.setOriginBasedPosition(0.0f, -0.01f);

        collider = new Collider(x, y, 0.5f,  (Collider collider, float newDX, float newDY, float damage)->{
            if(collider == null) { //wall coll
                dx = newDX; dy = newDY;
            }
            if(damage != 0.0f) {
                doDamage(damage);
            }
        }, false, 1.3f);
        collider.isPlayer = true;
        World.getPhysicsWorld().addCollider(collider);

        activePowerups = new LinkedList<>();
    }

    public void doDamage(float damage) {
        float damageDone = isDodging ? 0.0f : damage / resistanceMultiplier;
        float blockingMultiplier = melee.getBlockMultiplier();
        if(blockingMultiplier != 1.0f) {
            removeStamina(damage * 2.0f);
        }
        damageDone *= blockingMultiplier;
        health -= damageDone;
        regenDelayTimer = 0.0f;//reset the timer when taken damage
    }

    public void removeStamina(float stamina) {
        this.stamina -= stamina;
        if(this.stamina <= 0.0f) this.stamina = 0.0f;
        isDelaying = true;
        staminaRegenDelay = 0.0f;
    }

    public float getStamina() {
        return stamina;
    }

    void processInput() {

        rotation += Gdx.input.getDeltaX() * 0.2f;

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

        if(Gdx.input.isButtonJustPressed(Buttons.RIGHT)) {
            switch (selectedWeapon) {
            case 1:
                melee.attackHeavy();
                break;
            default:
                break;
            }
        }

        if(Gdx.input.isKeyPressed(Keys.SHIFT_LEFT)) {
            melee.beginBlock();
        } else {
            melee.endBlock();
        }

        if(Gdx.input.isKeyJustPressed(Keys.SPACE)) {
            if(!isDodging && (moveDirX != 0.0f || moveDirY != 0.0f)) {
                if(stamina >= dodgeStaminaCost) {
                    isDodging = true;
                    dodgeTime = 0.0f;
                    removeStamina(dodgeStaminaCost);
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

        if(health <= 0.0f) {
            if(!justDied) {
                deathPosX = x;
                deathPosY = y;
                justDied = true;
            }
            respawnTimer += delta;
            x = Float.MAX_VALUE;
            y = Float.MAX_VALUE;
            dx = 0.0f;
            dy = 0.0f;

            if(respawnTimer >= respawnTime) {
                justDied = false;
                health = maxHealth;
                
                x = 0.0f;//if no other players respawn at origin
                y = 0.0f;
                float dst = Float.MAX_VALUE;
                for (Entry<Integer, RemotePlayer> player : World.getRemotePlayers().entrySet()) {
                    Collider rCollider = player.getValue().getCollider();
                    float rDst = Vector2.dst(rCollider.x, rCollider.y, deathPosX, deathPosY);
                    if(rDst < dst) {
                        dst = rDst;
                        x = rCollider.x;
                        y = rCollider.y;
                    }
                }
                respawnTimer = 0.0f;
            }
        }

        if(isDodging) {
            dodgeTime += delta;
            if(dodgeTime >= maxDodgeTime) {
                isDodging = false;
                isDelaying = true;
            }
        }
        if(isDelaying) {
            staminaRegenDelay += delta;
            if(staminaRegenDelay > staminaRegenDelayMax) {
                isDelaying = false;
            }
        } else {
            if(!isDodging) {
                stamina += delta * staminaRegenPerSecond;
                if(stamina > maxStamina) stamina = maxStamina;
            }
        }

        if(regenDelayTimer >= damageRegenDelayTime) {
            float stepSize = (maxHealth * regenCheckpointPercentage) / 100.0f;
            float checkPointIDX = (float)Math.ceil(health / stepSize);
            float checkPointValue = checkPointIDX * stepSize;
            health += regenRate * delta;
            health = Math.min(checkPointValue, health);
        } else {
            regenDelayTimer += delta;
        }

        float rotationR = (float)Math.toRadians(rotation);

        x += dx;
        y += dy;

        float dst = Vector2.dst(dx, dy, 0, 0);
        distanceMoved += dst;
        if(distanceMoved > 1.75f) {
            footstepSound.play(0.05f, 0.75f, 0.0f);
            distanceMoved = 0.0f;
        }

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




        if(health > 0.0f) {
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
        if(health <= 0.0f) {
            fullscreenYouDied.draw(ShooterGame.getInstance().coreBatch);
        } else {
            switch (selectedWeapon) {
            case 1:
            melee.renderFirstPerson();
            break;
            case 2:
            if(ranged != null) {
                ranged.renderFirstPerson();
                crosshairSprite.draw(ShooterGame.getInstance().coreBatch);
            }
            default:
                break;
            }
        }

        Color healthColor = new Color().fromHsv((health / maxHealth) * 120.0f, 1.0f, 1.0f);
        healthColor.a = 1.0f;
        float aspect = (float)Gdx.graphics.getWidth() / (float)Gdx.graphics.getHeight();

        barSprite.setPosition(-aspect + 0.1f, 0.9f);

        barSprite.setSize(1.0f, 0.03f);
        barSprite.setColor(Color.BLACK);
        barSprite.draw(ShooterGame.getInstance().coreBatch);
        barSprite.setColor(healthColor);
        int maxI = (int)(100.0f/ regenCheckpointPercentage);
        for (int i = 0; i < maxI; i++) {
            barSprite.setPosition(-aspect + 0.1f + (regenCheckpointPercentage / 100.0f * i), 0.9f);

            float healthPerMaker = maxHealth * (regenCheckpointPercentage / 100.0f);
            float healthMarkerMax = healthPerMaker * (i + 1);
            float healthMarkerMin = healthPerMaker * (i);
            if(health >= healthMarkerMin) {
                float l = 0.0f;
                if(health >= healthMarkerMax)
                    l = (regenCheckpointPercentage / 100.0f);
                else
                    l = ((health - healthMarkerMin) / healthPerMaker) * (regenCheckpointPercentage / 100.0f);
                
                barSprite.setSize(l * (i == maxI-1 ? 1.0f : 0.95f), 0.03f);
                barSprite.draw(ShooterGame.getInstance().coreBatch);
            }
        }

        barSprite.setPosition(-aspect + 0.1f, 0.8f);

        barSprite.setSize(1.0f, 0.03f);
        barSprite.setColor(Color.BLACK);
        barSprite.draw(ShooterGame.getInstance().coreBatch);
        barSprite.setSize(stamina / maxStamina, 0.03f);
        barSprite.setColor(Color.GOLDENROD);
        barSprite.draw(ShooterGame.getInstance().coreBatch);

        int visibleIndex = 0;

        for (int i = 0; i < activePowerups.size(); i++) {
            Powerup powerup = activePowerups.get(i);

            if (powerup.getName().equals("Health Pack")) {
                reg = new TextureRegion(tex, 0, 256, 256, 256);
                powerupSprite = new Sprite (reg);
                powerupSprite.setPosition(-aspect + 1.11f, 0.88f);
                powerupSprite.setSize(0.07f, 0.07f);
                powerupSprite.draw(ShooterGame.getInstance().coreBatch);
                continue;
            }

            barSprite.setPosition(-aspect + 0.1f, 0.7f - (0.1f * visibleIndex));
            barSprite.setSize(0.5f, 0.03f);
            barSprite.setColor(Color.BLACK);
            barSprite.draw(ShooterGame.getInstance().coreBatch);
            barSprite.setSize((powerup.getRemainingTime() / powerup.getMaxTime()) * 0.5f, 0.03f);

            switch (powerup.getName()) {
                case "Attack Speed":
                    reg = new TextureRegion(tex, 0, 0, 256, 256);
                    barSprite.setColor(Color.FIREBRICK);
                    break;
                case "Damage Boost":
                    reg = new TextureRegion(tex, 256, 256, 256, 256);
                    barSprite.setColor(Color.MAGENTA);
                    break;
                case "Damage Resist":
                    reg = new TextureRegion(tex, 256, 0, 256, 256);
                    barSprite.setColor(Color.BLUE);
                    break;
            }

            barSprite.draw(ShooterGame.getInstance().coreBatch);

            powerupSprite = new Sprite(reg);
            powerupSprite.setPosition(-aspect + 0.015f, 0.68f - (0.1f * visibleIndex));
            powerupSprite.setSize(0.07f, 0.07f);
            powerupSprite.draw(ShooterGame.getInstance().coreBatch);
            visibleIndex++;
        }
    }

    public Collider getCollider() {
        return collider;
    }

    public void addPowerup(Powerup powerup) {
        activePowerups.add(powerup);
        powerup.onActivate(this);
    }

    public void addRangedWeapon(RangedWeapon weapon) {
        ranged = weapon;
    }

    public float getHealth() { return health; }

    public void addHealth(float health) {
        this.health = Math.min(this.health + health, maxHealth);
    }

    public LinkedList<Powerup> getActivePowerups() { return activePowerups; }

    public RangedWeapon getRangedWeapon() {
        return ranged;
    }

    public void setMeleeWeapon(MeleeWeapon weapon) {
        this.melee = weapon;
    }

    public void setRangedWeapon(RangedWeapon weapon) {
        this.ranged = weapon;
    }
}
