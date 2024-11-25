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
    final float maxHealth = 25.0f;
    float health;
    Collider collider;

    Collider currentTargetCollider = null;

    final float moveSpeed = 3.5f;

    Sprite2_5D spriteLow;
    Sprite2_5D spriteHigh;
    Sprite2_5D spriteHealth;
    Sprite2_5D spriteHealthBase;
    Texture texLow;
    Texture texHigh;
    Texture texHealth;
    Texture texHealthBase;
    TextureRegion regLow;
    TextureRegion regHigh;
    TextureRegion regHealth;
    TextureRegion regHealthBase;

    ArrayList<Vector2> navPath;
    int targetIndex = 0;

    public Goblin(float x, float y) {

        ShooterGame.getInstance().am.load("goblin_low.png", Texture.class);
        ShooterGame.getInstance().am.load("goblin_high.png", Texture.class);
        ShooterGame.getInstance().am.load("red_bar.png", Texture.class);
        ShooterGame.getInstance().am.load("bar.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        texLow = ShooterGame.getInstance().am.get("goblin_low.png", Texture.class);
        texHigh = ShooterGame.getInstance().am.get("goblin_high.png", Texture.class);
        texHealth = ShooterGame.getInstance().am.get("red_bar.png", Texture.class);
        texHealthBase = ShooterGame.getInstance().am.get("bar.png", Texture.class);
        regLow = new TextureRegion(texLow, 0, 0, 128, 80);
        regHigh = new TextureRegion(texHigh, 0, 0, 128, 80);
        regHealth = new TextureRegion(texHealth, 0, 0, 64, 64);
        regHealthBase = new TextureRegion(texHealthBase, 0, 0, 64, 64);


        health = maxHealth;

        //TODO: make dyanmically choose this target
        currentTargetCollider = World.getPlayerCollider();
        navPath = null;

        this.x = x; this.y = y;
        dx = 0; dy = 0;
        spriteLow = new Sprite2_5D(regLow, x, y, -1.35f, 1.25f, 2.0f);
        Renderer.inst().addSprite(spriteLow);
        spriteHigh = new Sprite2_5D(regHigh, x, y, -0.1f, 1.25f, 2.0f);
        Renderer.inst().addSprite(spriteHigh);
        spriteHealthBase = new Sprite2_5D(regHealthBase, x, y, 0.1f, 0.01f, 0.35f);
        Renderer.inst().addSprite(spriteHealthBase);
        spriteHealth = new Sprite2_5D(regHealth, x, y, 0.1f, 0.01f, 0.35f);
        Renderer.inst().addSprite(spriteHealth);

        collider = new Collider(x, y, 0.5f,  (Collider collider, float newDX, float newDY, float damage)->{
            if(collider == null) { //wall coll
                dx = newDX; dy = newDY;
            }
            if(damage != 0.0f) {
                health -= damage;
                spriteHealth.width = 0.35f * health/maxHealth;
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
        
        spriteLow.x = x;
        spriteLow.y = y;
        spriteHigh.x = x;
        spriteHigh.y = y;
        spriteHealth.x = x;
        spriteHealth.y = y;
        spriteHealthBase.x = x;
        spriteHealthBase.y = y;
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
        Renderer.inst().removeSprite(spriteLow);
        Renderer.inst().removeSprite(spriteHigh);
        Renderer.inst().removeSprite(spriteHealth);
        Renderer.inst().removeSprite(spriteHealthBase);
        World.getPhysicsWorld().removeCollider(collider);
    }

    @Override
    public boolean isAlive() {
        return health > 0.0f;
    }
    
}
