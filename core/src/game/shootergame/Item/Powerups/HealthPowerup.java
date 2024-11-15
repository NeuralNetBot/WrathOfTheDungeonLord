package game.shootergame.Item.Powerups;

import game.shootergame.Item.Powerup;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import game.shootergame.Player;
import game.shootergame.ShooterGame;

public class HealthPowerup implements Powerup {

    boolean isActive;
    Texture tex;
    TextureRegion reg;

    public HealthPowerup() {
        isActive = true;
        ShooterGame.getInstance().am.load("powerups.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        tex = ShooterGame.getInstance().am.get("powerups.png", Texture.class);
        reg = new TextureRegion(tex, 0, 256, 256, 256);
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

    @Override
    public TextureRegion getItemTexture() {
        return reg;
    }

}
