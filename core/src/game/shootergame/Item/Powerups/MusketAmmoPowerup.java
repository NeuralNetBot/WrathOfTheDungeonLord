package game.shootergame.Item.Powerups;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import game.shootergame.Item.Powerup;
import game.shootergame.Player;
import game.shootergame.ShooterGame;
import game.shootergame.World;

public class MusketAmmoPowerup implements Powerup {

    private boolean isActive;
    Texture tex;
    TextureRegion reg;

    public MusketAmmoPowerup() {

        this.isActive = true;
        ShooterGame.getInstance().am.load("ranged_ammo_pickups.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        tex = ShooterGame.getInstance().am.get("ranged_ammo_pickups.png", Texture.class);
        reg = new TextureRegion(tex, 256, 0, 256, 256);
    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void onActivate(Player player) {
        World.getPlayer().getRangedWeapon().setAmmo(3);
        onTimeout(player);
        System.out.println("Current Musket Ammo: " + World.getPlayer().getRangedWeapon().getAmmo());
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

    public static String getSName() {
        return "Musket Ammo";
    }

    @Override
    public String getName() {
        return getSName();
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
