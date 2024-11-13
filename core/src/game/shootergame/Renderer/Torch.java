package game.shootergame.Renderer;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import game.shootergame.ShooterGame;

public class Torch {
    public float x;
    public float y;
    public float z;
    public float radius;
    public Torch(float x, float y, float radius) {
        this.x = x; this.y = y; this.radius = radius;
    }

    static Texture tex;
    static TextureRegion reg;
    static void loadTexture() {
        ShooterGame.getInstance().am.load("torch.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        tex = ShooterGame.getInstance().am.get("torch.png", Texture.class);
        reg = new TextureRegion(tex, 0, 0, 128, 256);
    }
    static TextureRegion getTextureRegion() {
        return reg;
    }
}
