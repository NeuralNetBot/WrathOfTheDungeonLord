package game.shootergame.Enemy;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import game.shootergame.ShooterGame;
import game.shootergame.World;
import game.shootergame.Physics.Collider;
import game.shootergame.Renderer.Renderer;
import game.shootergame.Renderer.Sprite2_5D;

public class Goblin implements Enemy{

    float x, y;
    float dx, dy;
    float health;
    Sprite2_5D sprite;
    Collider collider;

    Collider currentTargetCollider = null;

    final float moveSpeed = 1.0f;

    Texture tex;
    TextureRegion reg;

    public Goblin(float x, float y) {

        ShooterGame.getInstance().am.load("debugtex.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        tex = ShooterGame.getInstance().am.get("debugtex.png", Texture.class);
        reg = new TextureRegion(tex, 0, 0, 1024, 1024);


        health = 100.0f;

        //TODO: make dyanmically choose this target
        currentTargetCollider = World.getPlayerCollider();

        this.x = x; this.y = y;
        dx = 0; dy = 0;
        sprite = new Sprite2_5D(reg, x, y, 0.0f, 0.3f, 2.0f);
        Renderer.inst().addSprite(sprite);

        collider = new Collider(x, y, 0.5f,  (Collider collider, float newDX, float newDY, float damage)->{
            if(collider == null) { //wall coll
                dx = newDX; dy = newDY;
            }
            if(damage != 0.0f) {
                health -= damage;
            }
        }, false, 1.3f);
        World.getPhysicsWorld().addCollider(collider);
    }

    @Override
    public void update(float delta) {
        x += dx;
        y += dy;

        if(currentTargetCollider != null) {
            //TODO: make this use path finding instead of "dumb" moves
            Vector2 move = new Vector2(currentTargetCollider.x, currentTargetCollider.y).sub(x, y).nor();
            move.scl(delta * moveSpeed);
            dx = move.x;
            dy = move.y;
            collider.dx = dx;
            collider.dy = dy;
        }

        collider.x = x;
        collider.y = y;
        
        sprite.x = x;
        sprite.y = y;
    }

    @Override
    public void tickPathing() {
    }

    @Override
    public void onKill() {
        Renderer.inst().removeSprite(sprite);
        World.getPhysicsWorld().removeCollider(collider);
    }
    
}
