package game.shootergame.Item.Powerups;

import game.shootergame.Item.Powerup;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import game.shootergame.Player;
import game.shootergame.ShooterGame;

public class AttackSpeedPowerup implements Powerup {

    private float remainingTime;
    private boolean isActive;
    Texture tex;
    TextureRegion reg;
    float maxTime;

    public AttackSpeedPowerup() {
        this.maxTime = 15.f;
        this.remainingTime = maxTime;
        this.isActive = true;
        ShooterGame.getInstance().am.load("powerups.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        tex = ShooterGame.getInstance().am.get("powerups.png", Texture.class);
        reg = new TextureRegion(tex, 0, 0, 256, 256);
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
        player.attackSpeed += 1.0f;
        System.out.println("Attack Speed Boost Initiated: Multiplier is " + player.attackSpeed);
    }

    @Override
    public void onTimeout(Player player) {
        player.attackSpeed -= 1.0f;
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

    public static String getSName() {
        return "Attack Speed";
    }

    @Override
    public String getName() {
        return getSName();
    }

    @Override
    public TextureRegion getItemTexture() {
        return reg;
    }

    public float getMaxTime() {
        return maxTime;
    }

}
