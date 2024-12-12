package game.shootergame.Network;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import game.shootergame.ShooterGame;
import game.shootergame.World;
import game.shootergame.Physics.Collider;
import game.shootergame.Renderer.Renderer;
import game.shootergame.Renderer.Sprite2_5D;

public class RemotePlayer {
    float x, y;
    float dx, dy;
    float rotation;
    Sprite2_5D sprite;

    Collider collider;

    static Texture tex;

    float recentDamage = 0.0f;

    public static void initTextures() {
        ShooterGame.getInstance().am.load("player.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        tex = ShooterGame.getInstance().am.get("player.png", Texture.class);
    }

    RemotePlayer() {
        TextureRegion reg = new TextureRegion(tex, 0, 0, 256, 512);

        sprite = new Sprite2_5D(reg, x, y, -1.0f, 3.0f, 1.5f);
        Renderer.inst().addSprite(sprite);

        collider = new Collider(x, y, 0.5f, (Collider collider, float newDX, float newDY, float damage)->{
            if(!collider.isPlayer) {
                recentDamage += damage;
            }
        });
        collider.isPlayer = true;
        World.getPhysicsWorld().addCollider(collider);
    }

    public void update(float delta) {
        x = x + dx * delta;
        y = y + dy * delta;

        sprite.x = x;
        sprite.y = y;
        
        collider.dx = dx;
        collider.dy = dy;
        collider.x = x;
        collider.y = y;
    }

    public void updateNetwork(float x, float y, float dx, float dy, float rotation) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.rotation = rotation;
    }

    public void kill() {
        Renderer.inst().removeSprite(sprite);
        World.getPhysicsWorld().removeCollider(collider);
    }

    public Collider getCollider() {
        return collider;
    }

    public float GetRecentDamage() {
        float dmg = recentDamage;
        recentDamage = 0.0f;
        return dmg;
    }
}
