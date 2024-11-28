package game.shootergame.Item.Powerups;

import game.shootergame.Item.Powerup;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import game.shootergame.Player;
import game.shootergame.ShooterGame;

public class DamagePowerup implements Powerup {

    private float remainingTime;
    private boolean isActive;
    Texture tex;
    TextureRegion reg;
    float maxTime;

    public DamagePowerup() {
        this.maxTime = 15.f;
        this.remainingTime = maxTime;
        this.isActive = true;
        ShooterGame.getInstance().am.load("powerups.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        tex = ShooterGame.getInstance().am.get("powerups.png", Texture.class);
        reg = new TextureRegion(tex, 256, 256, 256, 256);
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
        player.damageMultiplier += 1.0f;
        System.out.println("Damage Boost Initiated: Multiplier is " + player.damageMultiplier);
    }

    @Override
    public void onTimeout(Player player) {
        player.damageMultiplier -= 1.0f;
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
        return "Damage Boost";
    }

    @Override
    public TextureRegion getItemTexture() {
        return reg;
    }

    public float getMaxTime() {
        return maxTime;
    }

}
