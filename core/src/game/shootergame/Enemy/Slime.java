package game.shootergame.Enemy;

import java.util.Iterator;
import java.util.Map;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import game.shootergame.ShooterGame;
import game.shootergame.World;
import game.shootergame.Network.RemotePlayer;
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

    final float damage = 5.0f;

    final float moveSpeed = 2.0f;
    final float jumpDistance = 0.75f;
    final float jumpCooldown = 0.5f;
    float jumpCooldownTimer = 0.0f;
    float amountJumped = 0.0f;
    boolean isJumping = false;
    boolean jumpOnceDamage = false;
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

    final float agressionRange = 10.0f;
    final float aggroSightLossTimeout = 2.0f;
    float aggroLossTimer = 0.0f;
    boolean isAgrro = false;

    boolean isRemote;

    float recentDamage = 0.0f;

    public Slime(float x, float y, boolean isRemote) {
        this.isRemote = isRemote;
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
        if(isRemote) {
            collider = new Collider(x, y, 0.5f, (Collider collider, float newDX, float newDY, float damage)->{ recentDamage += damage; }, false, 1.3f);
        } else {
            collider = new Collider(x, y, 0.5f,  (Collider collider, float newDX, float newDY, float damage)->{
                if(collider == null) { //wall coll
                    dx = newDX; dy = newDY;
                } else {
                    //TODO: check if its any player collider including remote
                    if(isJumping && jumpOnceDamage && collider == World.getPlayerCollider()) {
                        jumpOnceDamage = false;
                        collider.Callback(this.collider, 0.0f, 0.0f, this.damage);
                    }
                }
                if(damage != 0.0f) {
                    health -= damage;
                    spriteHealth.width = 0.35f * health/maxHealth;
                    System.out.println(health);
                }
            }, false, 1.3f);
        }
        World.getPhysicsWorld().addCollider(collider);

        currentTargetCollider = null;
    }

    @Override
    public void update(float delta) {
        animTime += delta;
        currentRegion.setRegion(animation.getKeyFrame(animTime));

        if(!isRemote) {
            if(currentTargetCollider == null || aggroLossTimer >= aggroSightLossTimeout) {
                float closest = Float.MAX_VALUE;
                Collider close = null;
                Iterator<Map.Entry<Integer, RemotePlayer>> iterator = World.getRemotePlayers().entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Integer, RemotePlayer> entry = iterator.next();
                    Collider eCollider = entry.getValue().getCollider();
                    float dst = Vector2.dst(eCollider.x, eCollider.y, x, y);
                    if(dst < agressionRange && dst < closest) {
                        closest = dst;
                        close = eCollider;
                        isAgrro = true;
                        aggroLossTimer = 0.0f;
                    }
                }
                Collider playerC = World.getPlayerCollider();
                float dst = Vector2.dst(playerC.x, playerC.y, x, y);
                if(dst < agressionRange && dst < closest) {
                    closest = dst;
                    close = playerC;
                    isAgrro = true;
                    aggroLossTimer = 0.0f;
                }
                currentTargetCollider = close;
            }

            if(aggroLossTimer >= aggroSightLossTimeout) {
                isAgrro = false;
                currentTargetCollider = null;
            }

            if(currentTargetCollider != null && Vector2.dst(currentTargetCollider.x, currentTargetCollider.y, x, y) > agressionRange) {
                aggroLossTimer += delta;
            } else {
                aggroLossTimer = 0.0f;
            }
        }   

        x += dx;
        y += dy;

        if(!isRemote) {
            if(currentTargetCollider != null && !isJumping && jumpCooldownTimer >= jumpCooldown) {
                targetDirection = new Vector2(currentTargetCollider.x, currentTargetCollider.y).sub(x, y).nor();
                isJumping = true;
                jumpOnceDamage = true;
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
        }

        collider.x = x;
        collider.y = y;
        
        sprite.x = x;
        sprite.y = y;
        spriteHealthBase.x = x;
        spriteHealthBase.y = y;
        spriteHealth.x = x;
        spriteHealth.y = y;

        if(isRemote) {
            spriteHealthBase.z = sprite.z + 0.5f;
            spriteHealth.z = sprite.z + 0.5f;
            spriteHealth.width = 0.35f * health/maxHealth;
        }
    }

    @Override
    public void updateFromNetwork(float x, float y, float z, float dx, float dy, float rotation, float health) {
        this.x = x;
        this.y = y;
        sprite.z = z;
        this.dx = dx;
        this.dy = dy;
        this.health = health;
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

    @Override
    public boolean isAggro() {
        return isAgrro;
    }

    @Override
    public float getX() {
        return x;
    }

    @Override
    public float getY() {
        return y;
    }

    @Override
    public float getZ() {
        return sprite.z;
    }

    @Override
    public float getDX() {
        return dx;
    }

    @Override
    public float getDY() {
        return dy;
    }

    @Override
    public float getRotation() {
        return 0.0f;
    }

    @Override
    public String getName() {
        return "slime";
    }
    
    @Override
    public float getHealth() {
        return health;
    }

    @Override
    public void doDamage(float damage) {
        health -= damage;
    }

    @Override
    public float getRecentDamage() {
        float dmg = recentDamage;
        recentDamage = 0.0f;
        return dmg;
    }
}
