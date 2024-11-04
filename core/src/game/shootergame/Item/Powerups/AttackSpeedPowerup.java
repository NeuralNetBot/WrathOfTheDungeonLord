package game.shootergame.Item.Powerups;

import game.shootergame.Item.Powerup;
import game.shootergame.Player;

public class AttackSpeedPowerup implements Powerup {

    private float remainingTime;
    private boolean isActive;

    public AttackSpeedPowerup() {
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
        player.attackSpeed = 2.0f;
        System.out.println("Attack Speed Boost Initiated: Multiplier is " + player.attackSpeed);
    }

    @Override
    public void onTimeout(Player player) {
        player.attackSpeed = 1.0f;
        System.out.println("Attack Speed Boost Ended: Multiplier is " + player.attackSpeed);
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
        return "Attack Speed";
    }
}
