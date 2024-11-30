package game.shootergame.Item.Powerups;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import game.shootergame.Item.Powerup;
import game.shootergame.Player;
import game.shootergame.ShooterGame;
import game.shootergame.World;

public class CrossbowAmmoPowerup implements Powerup {
    private boolean isActive;
    Texture tex;
    TextureRegion reg;

    public CrossbowAmmoPowerup() {
        this.isActive = true;
        ShooterGame.getInstance().am.load("ranged_ammo_pickups.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        tex = ShooterGame.getInstance().am.get("ranged_ammo_pickups.png", Texture.class);
        reg = new TextureRegion(tex, 0, 0, 256, 256);
    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void onActivate(Player player) {
        World.getPlayer().getRangedWeapon().setAmmo(10);
        onTimeout(player);
        System.out.println("Current Crossbow Ammo: " + World.getPlayer().getRangedWeapon().getAmmo());
    }

    @Override
    public void onTimeout(Player player) {
        isActive = false;
    }

    @Override
    public float getRemainingTime() {
        return 0;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public String getName() {
        return "Crossbow Ammo";
    }

    @Override
    public TextureRegion getItemTexture() {
        return reg;
    }

    @Override
    public float getMaxTime() {
        return 0;
    }
}
