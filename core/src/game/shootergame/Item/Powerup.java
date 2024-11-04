package game.shootergame.Item;

import game.shootergame.Player;

public interface Powerup {
    void update(float delta);
    void onActivate(Player player);
    void onTimeout(Player player);
    float getRemainingTime();
    boolean isActive();
    String getName();
}
