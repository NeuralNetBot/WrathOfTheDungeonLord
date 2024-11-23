package game.shootergame.Enemy;

import java.util.ArrayList;

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

    final float moveSpeed = 3.5f;

    Texture tex;
    TextureRegion reg;

    ArrayList<Vector2> navPath;
    int targetIndex = 0;

    public Goblin(float x, float y) {

        ShooterGame.getInstance().am.load("debugtex.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        tex = ShooterGame.getInstance().am.get("debugtex.png", Texture.class);
        reg = new TextureRegion(tex, 0, 0, 1024, 1024);


        health = 100.0f;

        //TODO: make dyanmically choose this target
        currentTargetCollider = World.getPlayerCollider();
        navPath = null;

        this.x = x; this.y = y;
        dx = 0; dy = 0;
        sprite = new Sprite2_5D(reg, x, y, -0.75f, 3.0f, 0.5f);
        Renderer.inst().addSprite(sprite);

        collider = new Collider(x, y, 0.5f,  (Collider collider, float newDX, float newDY, float damage)->{
            if(collider == null) { //wall coll
                dx = newDX; dy = newDY;
            }
            if(damage != 0.0f) {
                health -= damage;
                System.out.println(health);
            }
        }, false, 1.3f);
        World.getPhysicsWorld().addCollider(collider);
    }

    @Override
    public void update(float delta) {
        x += dx;
        y += dy;

        if(currentTargetCollider != null && navPath != null && targetIndex < navPath.size()) {
            Vector2 targetNode = navPath.get(targetIndex).cpy();
            Vector2 direction = targetNode.cpy().sub(x, y);
            float dist = direction.len();
            
            if(dist < moveSpeed * delta) {
                x = targetNode.x;
                y = targetNode.y;
                targetIndex++;
            } else {
                direction.nor().scl(moveSpeed * delta);
                collider.dx = direction.x;
                collider.dy = direction.y;
            }
        } else {
            collider.dx = 0.0f;
            collider.dy = 0.0f;
        }

        collider.x = x;
        collider.y = y;
        
        sprite.x = x;
        sprite.y = y;
    }

    @Override
    public void tickPathing() {
        if(currentTargetCollider != null) {
            navPath = World.getNavMesh().pathFind(new Vector2(x, y), new Vector2(currentTargetCollider.x, currentTargetCollider.y));
            if(navPath == null) { //path find failed, so enter "dumb search" mode i.e. direct light on sight path
                navPath = new ArrayList<>();
                navPath.add(new Vector2(x, y));
                navPath.add(new Vector2(currentTargetCollider.x, currentTargetCollider.y));
            }
        } else {
            navPath = null;
        }
    }

    @Override
    public void onKill() {
        Renderer.inst().removeSprite(sprite);
        World.getPhysicsWorld().removeCollider(collider);
    }
    
}
