package game.shootergame.Item;

import game.shootergame.World;
import game.shootergame.Physics.Collider;

public class ItemPickup {
    public enum Payload {
        POWERUP,
        RANGED_WEAPON,
        NONE
    }

    Payload payload;
    RangedWeapon weapon;
    Powerup powerup;
    Collider collider;

    float x, y;

    public ItemPickup(float x, float y, RangedWeapon weapon) {
        payload = Payload.RANGED_WEAPON;
        this.weapon = weapon;
        powerup = null;
        this.x = x; this.y = y;
        createCollider();
    }

    public ItemPickup(float x, float y, Powerup powerup) {
        payload = Payload.POWERUP;
        weapon = null;
        this.powerup = powerup;
        this.x = x; this.y = y;
        createCollider();
    }

    public ItemPickup(float x, float y) {
        payload = Payload.NONE;
        weapon = null;
        powerup = null;
        this.x = x; this.y = y;
        createCollider();
    }

    private void createCollider() {
        collider = new Collider(x, y, 1.0f, (Collider collider)->{
            if(collider == World.getPlayerCollider()) {
                System.out.println("Collide!");
                World.showPickupPrompt(this);
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
        return "";
    }

}
