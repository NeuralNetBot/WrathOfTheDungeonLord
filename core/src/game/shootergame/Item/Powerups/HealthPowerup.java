package game.shootergame.Item.Powerups;

import game.shootergame.Item.Powerup;
import game.shootergame.Player;

public class HealthPowerup implements Powerup {

    boolean isActive;

    public HealthPowerup() {
        isActive = true;
    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void onActivate(Player player) {
        player.addHealth(25.0f);
        System.out.println("Player Health: " + player.getHealth());
        isActive = false;
    }

    @Override
    public void onTimeout(Player player) {

    }

    @Override
    public float getRemainingTime() {
        return 0;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    public String getName() {
        return "Health Pack";
    }
}
