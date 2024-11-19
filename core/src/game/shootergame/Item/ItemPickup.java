package game.shootergame.Item;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import game.shootergame.World;
import game.shootergame.Physics.Collider;
import game.shootergame.Renderer.Renderer;
import game.shootergame.Renderer.Sprite2_5D;

public class ItemPickup {
    public enum Payload {
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

    public ItemPickup(float x, float y, RangedWeapon weapon) {
        payload = Payload.RANGED_WEAPON;
        this.weapon = weapon;
        powerup = null;
        this.x = x; this.y = y;
        createPickupSprite();
        createCollider();
        isActive = true;
    }

    public ItemPickup(float x, float y, Powerup powerup) {
        payload = Payload.POWERUP;
        weapon = null;
        this.powerup = powerup;
        this.x = x; this.y = y;
        createPickupSprite();
        createCollider();
        this.name = powerup.getName();
        isActive = true;
    }

    public ItemPickup(float x, float y) {
        payload = Payload.NONE;
        weapon = null;
        powerup = null;
        this.x = x; this.y = y;
        createPickupSprite();
        createCollider();
    }

    private void createPickupSprite() {
        TextureRegion reg;
        switch (payload) {
            case RANGED_WEAPON:
                return;//TODO:
                //break;
            case POWERUP:
                reg = powerup.getItemTexture();
                break;
            default:
                return;
        }
        sprite = new Sprite2_5D(reg, x, y, -0.7f, 0.3f, 0.3f);
        Renderer.inst().addSprite(sprite);
    }

    private void createCollider() {
        collider = new Collider(x, y, 1.0f, (Collider collider, float newDx, float newDy, float damage)->{
            if (collider == World.getPlayerCollider()) {
                if (isActive) {
                    World.showPickupPrompt(this);
                    if (payload == Payload.POWERUP && powerup.isActive()) {
                        if (Gdx.input.isKeyPressed(Input.Keys.E)) {
                            Renderer.inst().removeSprite(sprite);
                            World.getPlayer().addPowerup(powerup);
                            isActive = false;
                        }
                    }
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

}
