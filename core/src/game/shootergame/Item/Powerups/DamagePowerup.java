package game.shootergame.Item.Powerups;

import game.shootergame.Item.Powerup;
import game.shootergame.Player;

public class DamagePowerup implements Powerup {

    private float remainingTime;
    private boolean isActive;

    public DamagePowerup() {
        this.remainingTime = 10.0f;
        this.isActive = true;
    }

    @Override
    public void update(float delta) {
        if (isActive) {
            remainingTime -= delta;
            if (remainingTime <= 0) {
                isActive = false;
            }
        }
    }

    @Override
    public void onActivate(Player player) {
        player.damageMultiplier = 2.0f;
        System.out.println("Damage Boost Initiated: Multiplier is " + player.damageMultiplier);
    }

    @Override
    public void onTimeout(Player player) {
        player.damageMultiplier = 1.0f;
        System.out.println("Damage Boost Ended: Multiplier is " + player.damageMultiplier);
    }

    @Override
    public float getRemainingTime() {
        return remainingTime;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public String getName() {
        return "Strength";
    }
}
