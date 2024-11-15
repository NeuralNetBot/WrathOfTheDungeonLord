package game.shootergame.Network;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import game.shootergame.ShooterGame;
import game.shootergame.Renderer.Renderer;
import game.shootergame.Renderer.Sprite2_5D;

public class RemotePlayer {
    float x, y;
    float dx, dy;
    Sprite2_5D sprite;

    RemotePlayer() {
        ShooterGame.getInstance().am.load("debugtex.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        Texture tex = ShooterGame.getInstance().am.get("debugtex.png", Texture.class);
        TextureRegion reg = new TextureRegion(tex, 0, 0, 1024, 1024);

        sprite = new Sprite2_5D(reg, x, y, -1.0f, 3.0f, 0.5f);
        Renderer.inst().addSprite(sprite);
    }

    public void update(float delta) {
        x = x + dx * delta;
        y = y + dy * delta;
        
        sprite.x = x;
        sprite.y = y;
    }

    public void updateNetwork(float x, float y, float dx, float dy) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
    }

    public void kill() {
        Renderer.inst().removeSprite(sprite);
    }
}
