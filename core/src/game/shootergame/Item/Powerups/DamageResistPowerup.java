package game.shootergame.Item.Powerups;

import game.shootergame.Item.Powerup;
import game.shootergame.Player;

public class DamageResistPowerup implements Powerup {

    private float remainingTime;
    private boolean isActive;

    public DamageResistPowerup() {
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
        player.resistanceMultiplier = 2.0f;
        System.out.println("Damage Resist Initiated: Multiplier is " + player.resistanceMultiplier);
    }

    @Override
    public void onTimeout(Player player) {
        player.resistanceMultiplier = 1.0f;
        System.out.println("Damage Resist Ended: Multiplier is " + player.resistanceMultiplier);
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
        return "Damage Resist";
    }
}
