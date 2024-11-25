package game.shootergame.Enemy;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

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

    final float moveSpeed = 2.0f;
    final float jumpDistance = 0.75f;
    final float jumpCooldown = 0.3f;
    float jumpCooldownTimer = 0.0f;
    float amountJumped = 0.0f;
    boolean isJumping = false;
    Vector2 targetDirection;

    Texture tex;
    Texture texHealth;
    Texture texHealthBase;
    Sprite2_5D sprite;
    Sprite2_5D spriteHealth;
    Sprite2_5D spriteHealthBase;
    TextureRegion currentRegion;
    TextureRegion regHealth;
    TextureRegion regHealthBase;

    Animation<TextureRegion> animation;
    float animTime = 0.0f;

    public Slime(float x, float y) {
        this.x = x; this.y = y;
        dx = 0; dy = 0;
        health = maxHealth;

        ShooterGame.getInstance().am.load("slime.png", Texture.class);
        ShooterGame.getInstance().am.load("red_bar.png", Texture.class);
        ShooterGame.getInstance().am.load("bar.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        tex = ShooterGame.getInstance().am.get("slime.png", Texture.class);
        texHealth = ShooterGame.getInstance().am.get("red_bar.png", Texture.class);
        texHealthBase = ShooterGame.getInstance().am.get("bar.png", Texture.class);
        currentRegion = new TextureRegion(tex, 0, 0, 128, 128);
        regHealth = new TextureRegion(texHealth, 0, 0, 64, 64);
        regHealthBase = new TextureRegion(texHealthBase, 0, 0, 64, 64);


        TextureRegion[][] tempFrames = TextureRegion.split(tex, tex.getWidth() / 4, tex.getHeight() / 5);
        TextureRegion[] animFrames = new TextureRegion[4 * 5];
        int index = 0;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 4; j++) {
                animFrames[index++] = tempFrames[i][j];
            }
        }
        animation = new Animation<TextureRegion>(0.12f, animFrames);
        animation.setPlayMode(PlayMode.LOOP);

        sprite = new Sprite2_5D(currentRegion, x, y, -0.75f, 1.5f, 1.5f);
        Renderer.inst().addSprite(sprite);
        spriteHealthBase = new Sprite2_5D(regHealthBase, x, y, 0.0f, 0.01f, 0.35f);
        Renderer.inst().addSprite(spriteHealthBase);
        spriteHealth = new Sprite2_5D(regHealth, x, y, 0.0f, 0.01f, 0.35f);
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

        //TODO: make dyanmically choose this target
        currentTargetCollider = World.getPlayerCollider();
    }

    @Override
    public void update(float delta) {
        animTime += delta;
        currentRegion.setRegion(animation.getKeyFrame(animTime));
        x += dx;
        y += dy;

        if(currentTargetCollider != null && !isJumping && jumpCooldownTimer >= jumpCooldown) {
            targetDirection = new Vector2(currentTargetCollider.x, currentTargetCollider.y).sub(x, y);
            float dst = targetDirection.len();
            targetDirection.scl(1.0f / dst);
            isJumping = dst >= jumpDistance + 1.0f;
        }

        if(isJumping) {
            jumpCooldownTimer = 0.0f;
            dx = targetDirection.x * delta * moveSpeed;
            dy = targetDirection.y * delta * moveSpeed;
            collider.dx = dx;
            collider.dy = dy;
            amountJumped += Vector2.len(dx, dy);
            if(amountJumped >= jumpDistance) {
                amountJumped = 0.0f;
                isJumping = false;
            }

            float l = amountJumped / jumpDistance;
            sprite.z = -0.75f + (float)Math.sin(l * Math.PI);
            spriteHealthBase.z = sprite.z + 0.5f;
            spriteHealth.z = sprite.z + 0.5f;
        } else {
            jumpCooldownTimer += delta;
            collider.dx = dx = 0.0f;
            collider.dy = dy = 0.0f;
        }

        collider.x = x;
        collider.y = y;
        
        sprite.x = x;
        sprite.y = y;
        spriteHealthBase.x = x;
        spriteHealthBase.y = y;
        spriteHealth.x = x;
        spriteHealth.y = y;
    }

    @Override
    public void tickPathing() {
    }

    @Override
    public void onKill() {
        Renderer.inst().removeSprite(sprite);
        Renderer.inst().removeSprite(spriteHealthBase);
        Renderer.inst().removeSprite(spriteHealth);
        World.getPhysicsWorld().removeCollider(collider);
    }

    @Override
    public boolean isAlive() {
        return health > 0.0f;
    }
    
}
