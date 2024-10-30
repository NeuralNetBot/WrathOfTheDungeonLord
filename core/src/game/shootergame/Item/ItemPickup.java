package game.shootergame.Item;

public class ItemPickup {
    public enum Payload {
        POWERUP,
        RANGED_WEAPON
    }

    Payload payload;
    RangedWeapon weapon;
    Powerup powerup;

    ItemPickup(float x, float y, RangedWeapon weapon) {
        payload = Payload.RANGED_WEAPON;
        this.weapon = weapon;
        powerup = null;
    }

    ItemPickup(float x, float y, Powerup powerup) {
        payload = Payload.POWERUP;
        weapon = null;
        this.powerup = powerup;
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

}
