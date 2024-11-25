package game.shootergame.Enemy;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import game.shootergame.ShooterGame;
import game.shootergame.World;
import game.shootergame.Physics.Collider;
import game.shootergame.Renderer.Renderer;
import game.shootergame.Renderer.Sprite2_5D;

public class Slime implements Enemy{

    float x, y;
    float dx, dy;
    final float maxHealth = 10.0f;
    float health;
    Collider collider;

    Collider currentTargetCollider = null;

    final float moveSpeed = 1.5f;

    Texture tex;
    Sprite2_5D sprite;
    TextureRegion currentRegion;

    Animation<TextureRegion> animation;
    float animTime = 0.0f;

    public Slime(float x, float y) {
        this.x = x; this.y = y;
        dx = 0; dy = 0;
        health = maxHealth;

        ShooterGame.getInstance().am.load("slime.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        tex = ShooterGame.getInstance().am.get("slime.png", Texture.class);
        currentRegion = new TextureRegion(tex, 0, 0, 128, 128);

        TextureRegion[][] tempFrames = TextureRegion.split(tex, tex.getWidth() / 4, tex.getHeight() / 5);
        TextureRegion[] animFrames = new TextureRegion[4 * 5];
        int index = 0;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 4; j++) {
                animFrames[index++] = tempFrames[i][j];
            }
        }
        animation = new Animation<TextureRegion>(0.08167f, animFrames);
        animation.setPlayMode(PlayMode.LOOP);

        sprite = new Sprite2_5D(currentRegion, x, y, -0.75f, 1.5f, 1.5f);
        Renderer.inst().addSprite(sprite);

        collider = new Collider(x, y, 0.5f,  (Collider collider, float newDX, float newDY, float damage)->{
            if(collider == null) { //wall coll
                dx = newDX; dy = newDY;
            }
            if(damage != 0.0f) {
                health -= damage;
                //spriteHealth.width = 0.35f * health/maxHealth;
                System.out.println(health);
            }
        }, false, 1.3f);
        World.getPhysicsWorld().addCollider(collider);
    }

    @Override
    public void update(float delta) {
        animTime += delta;
        currentRegion.setRegion(animation.getKeyFrame(animTime));
        x += dx;
        y += dy;

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

    @Override
    public boolean isAlive() {
        return health > 0.0f;
    }
    
}
