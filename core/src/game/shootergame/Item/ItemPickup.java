package game.shootergame.Item;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import game.shootergame.World;
import game.shootergame.Physics.Collider;
import game.shootergame.Renderer.Renderer;
import game.shootergame.Renderer.Sprite2_5D;

public class ItemPickup {
    public static enum Payload {
        POWERUP,
        RANGED_WEAPON,
        NONE
    }

    Payload payload;
    RangedWeapon weapon;
    Powerup powerup;
    public Collider collider;
    String name;
    boolean isActive;

    float x, y;

    Sprite2_5D sprite;

    public ItemPickup(float x, float y, float itemScale, RangedWeapon weapon) {
        payload = Payload.RANGED_WEAPON;
        this.weapon = weapon;
        powerup = null;
        this.x = x; this.y = y;
        createPickupSprite(itemScale);
        createCollider();
        this.name = weapon.getName();
        isActive = true;
    }

    public ItemPickup(float x, float y, float itemScale, Powerup powerup) {
        payload = Payload.POWERUP;
        weapon = null;
        this.powerup = powerup;
        this.x = x; this.y = y;
        createPickupSprite(itemScale);
        createCollider();
        this.name = powerup.getName();
        isActive = true;
    }

    public ItemPickup(float x, float y) {
        payload = Payload.NONE;
        weapon = null;
        powerup = null;
        this.x = x; this.y = y;
        createPickupSprite(1.0f);
        createCollider();
    }

    private void createPickupSprite(float itemScale) {
        TextureRegion reg;
        switch (payload) {
            case RANGED_WEAPON:
                reg = weapon.getItemTexture(); //TODO: MAKE SPRITE FOR RANGED WEAPONS
                break;
            case POWERUP:
                reg = powerup.getItemTexture();
                break;
            default:
                return;
        }
        sprite = new Sprite2_5D(reg, x, y, -0.7f, 0.3f * itemScale, 0.3f * itemScale);
        Renderer.inst().addSprite(sprite);
    }

    private void createCollider() {
        collider = new Collider(x, y, 1.0f, (Collider collider, float newDx, float newDy, float damage)->{
            if (collider == World.getPlayerCollider()) {
                if (isActive) {
                    World.showPickupPrompt(this);
                }
            }
        });
        World.getPhysicsWorld().addCollider(collider);
    }

    public Payload getPayloadType() {
        return payload;
    }

    public RangedWeapon getRangedWeapon() {
        return weapon;
    }

    public Powerup getPowerup() {
        return powerup;
    }

    public String getName() {
        return this.name;
    }

    public void removeFromWorld() {
        isActive = false;
        Renderer.inst().removeSprite(sprite);
        if (payload == ItemPickup.Payload.POWERUP) {
            World.getPlayer().addPowerup(powerup);
        } else if(payload == ItemPickup.Payload.RANGED_WEAPON) {
            World.getPlayer().addRangedWeapon(weapon);
        }
    }

}
