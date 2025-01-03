package game.shootergame.Enemy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Map.Entry;

import com.badlogic.gdx.audio.Sound;
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

public class GoblinCrossbow implements Enemy{

    float x, y;
    final float homeX, homeY;
    float dx, dy;
    float rotation;
    final float maxHealth = 35.0f;
    float health;
    Collider collider;

    Collider currentTargetCollider = null;

    final float moveSpeed = 1.5f;
    final float moveSpeedFast = 2.5f;

    Sprite2_5D spriteLow;
    Sprite2_5D spriteHigh;
    Sprite2_5D spriteHealth;
    Sprite2_5D spriteHealthBase;
    Texture texFull;
    Texture texHealth;
    Texture texHealthBase;
    TextureRegion regLow;
    TextureRegion regHigh;
    TextureRegion regHealth;
    TextureRegion regHealthBase;

    ArrayList<Vector2> navPath;
    int targetIndex = 0;

    @SuppressWarnings("unchecked")
    Animation<TextureRegion>[] animationsWalk = new Animation[8];
    TextureRegion[] regionsHigh = new TextureRegion[8];
    float animTime = 0.0f;
    float animAttackTime = 0.0f;

    boolean isRemote;
    float recentDamage = 0.0f;

    private class Hate {
        public Collider collider;
        public float rangeHate;
        public float damageHate;
        public float getTotal() { return rangeHate + damageHate; }
        public Hate(Collider collider) { this.collider = collider; rangeHate = 0.0f; damageHate = 0.0f; }
    }
    HashMap<Integer, Hate> hateMap = new HashMap<>();

    final float damageHateLossPerSecond = 2.0f;
    final float rangeHateRange = 16.0f;

    boolean isAggro = false;

    boolean isAttacking = false;
    float attackCooldown = 0.0f;
    final float attackCooldownMax = 3.0f;
    final float damage = 9.0f;
    boolean hasDoneDamage = false;

    final float returnHomeAfterAggroTimeMax = 3.0f;
    float returnHomeAfterAggroTime = 0.0f;

    Sound soundFire;

    public GoblinCrossbow(float x, float y, boolean isRemote) {
        homeX = x;
        homeY = y;
        this.isRemote = isRemote;
        if(!isRemote) {
            rotation = Objects.hash(x, y);
            rotation = (rotation + 2 * 3.141592653f) % (2 * 3.141592653f);
        }
        ShooterGame.getInstance().am.load("goblin_crossbow.png", Texture.class);
        ShooterGame.getInstance().am.load("red_bar.png", Texture.class);
        ShooterGame.getInstance().am.load("bar.png", Texture.class);
        ShooterGame.getInstance().am.load("crossbow_fire.mp3", Sound.class);
        ShooterGame.getInstance().am.finishLoading();
        texFull = ShooterGame.getInstance().am.get("goblin_crossbow.png", Texture.class);
        texHealth = ShooterGame.getInstance().am.get("red_bar.png", Texture.class);
        texHealthBase = ShooterGame.getInstance().am.get("bar.png", Texture.class);
        regLow = new TextureRegion(texFull, 0, 0, 128, 80);
        regHigh = new TextureRegion(texFull, 0, 0, 128, 80);
        regHealth = new TextureRegion(texHealth, 0, 0, 64, 64);
        regHealthBase = new TextureRegion(texHealthBase, 0, 0, 64, 64);
        soundFire = ShooterGame.getInstance().am.get("crossbow_fire.mp3", Sound.class);

        {
            TextureRegion[][] tempFrames = TextureRegion.split(texFull, texFull.getWidth() / 27, texFull.getHeight() / 8);
            
            for (int i = 0; i < 8; i++) {
                TextureRegion[] animFrames = new TextureRegion[26];
                regionsHigh[i] = tempFrames[i][0];
                for (int j = 1; j < 27; j++) {
                    animFrames[j-1] = tempFrames[i][j];
                }
                animationsWalk[i] = new Animation<TextureRegion>(0.06f, animFrames);
                animationsWalk[i].setPlayMode(PlayMode.LOOP);
            }
        }

        health = maxHealth;

        currentTargetCollider = null;
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

        if(isRemote) {
            collider = new Collider(x, y, 0.5f,  (Collider collider, float newDX, float newDY, float damage)->{
                recentDamage += damage;
            }, false, 1.3f);
        } else {
            collider = new Collider(x, y, 0.5f,  (Collider collider, float newDX, float newDY, float damage)->{
                if(collider == null) { //wall coll
                    dx = newDX; dy = newDY;
                }
                if(damage != 0.0f && collider.isPlayer) {
                    doDamage(damage, 0);
                    spriteHealth.width = 0.35f * health/maxHealth;
                    System.out.println(health);
                }
            }, false, 1.3f);
        }
        World.getPhysicsWorld().addCollider(collider);

        getHateForPlayer(0);
        for (Entry<Integer, RemotePlayer> player : World.getRemotePlayers().entrySet()) {
            getHateForPlayer(player.getKey());
        }
    }
    
    @Override
    public void update(float delta) {
        x += dx;
        y += dy;

        animTime += delta * 2;

        Vector2 v = new Vector2(x - World.getPlayer().x(), y - World.getPlayer().y()).nor();

        float angleToSprite = (float)Math.atan2(v.y, v.x);
        float relAngle = angleToSprite - rotation;
        relAngle = (relAngle + 2 * 3.141592653f) % (2 * 3.141592653f);
        int index = ((int)Math.floor((Math.toDegrees(relAngle) + 22.5f) / 45.0f)) % 8;
        if(index < 0) index += 8;
        int realIndex = 0;
        switch (index) {
            case 0: realIndex = 3; break;
            case 1: realIndex = 5; break;
            case 2: realIndex = 7; break;
            case 3: realIndex = 2; break;
            case 4: realIndex = 0; break;
            case 5: realIndex = 1; break;
            case 6: realIndex = 6; break;
            case 7: realIndex = 4; break;
        }

        spriteHigh.setRegion(regionsHigh[realIndex]);
        if(isAttacking && attackCooldown > attackCooldownMax) {
            if(animAttackTime == 0.0f) {
                attackCooldown = 0.0f;
            }
            if(!hasDoneDamage) {
                soundFire.play();
                if(!isRemote) {
                    Vector2 d = new Vector2(-(float)Math.cos(rotation), -(float)Math.sin(rotation)).nor();
                    Collider hitCollider = World.getPhysicsWorld().rayCast(collider, x, y, d.x, d.y);
                    if(hitCollider != null) {
                        hitCollider.Callback(collider, 0, 0, damage);
                    }
                }
                hasDoneDamage = true;
            }
            animAttackTime += delta;
            if(animAttackTime >= 0.1f) {
                isAttacking = false;
                hasDoneDamage = false;
                animAttackTime = 0.0f;
            }
        } else if(dx != 0.0f || dy != 0.0f) {
            attackCooldown += delta;
            spriteLow.setRegion(animationsWalk[realIndex].getKeyFrame(animTime));
        } else {
            attackCooldown += delta;
            spriteLow.setRegion(animationsWalk[realIndex].getKeyFrame(0.0f));
        }

        if(!isRemote) {
            {
                Hate hate = getHateForPlayer(0);
                float range = Vector2.dst(x, y, hate.collider.x, hate.collider.y);
                hate.rangeHate = Math.max((-range * 10.0f / rangeHateRange) + 10.0f, 0.0f);
                for (Entry<Integer, RemotePlayer> player : World.getRemotePlayers().entrySet()) {
                    hate = getHateForPlayer(player.getKey());
                    range = Vector2.dst(x, y, hate.collider.x, hate.collider.y);
                    hate.rangeHate = Math.max((-range * 10.0f / rangeHateRange) + 10.0f, 0.0f);
                }
            }
            float highest = 0.0f;
            Hate highestHate = null;
            for (Entry<Integer, Hate> hate : hateMap.entrySet()) {
                hate.getValue().damageHate = Math.max(hate.getValue().damageHate - (delta * damageHateLossPerSecond), 0.0f);

                float hateTotal = hate.getValue().getTotal();
                if(hateTotal > highest) {
                    highest = hateTotal;
                    highestHate = hate.getValue();
                }
            }
            if(highest == 0.0f) {
                isAggro = false;
                currentTargetCollider = null;
                returnHomeAfterAggroTime += delta;
            } else {
                isAggro = true;
                returnHomeAfterAggroTime = 0.0f;
                currentTargetCollider = highestHate.collider;
            }
            float currentMoveSpeed = moveSpeed;
            if(currentTargetCollider != null) {
                Vector2 targetV = new Vector2(x - currentTargetCollider.x, y - currentTargetCollider.y);
                if(isAttacking) {
                    Vector2 targetVNorm = targetV.cpy().nor();
                    rotation = (float)Math.atan2(targetVNorm.y, targetVNorm.x);
                    rotation = (rotation + 2 * 3.141592653f) % (2 * 3.141592653f);
                }
                
                float distanceToTarget = targetV.len();
                isAttacking = distanceToTarget < 10.0f;
            }
            if(!isAttacking && navPath != null && targetIndex < navPath.size()) {
                Vector2 targetNode = navPath.get(targetIndex).cpy();
                Vector2 direction = targetNode.cpy().sub(x, y);
                float dist = direction.len();
                
                Vector2 targetVNorm = direction.cpy().scl(-1.0f).nor();
                rotation = (float)Math.atan2(targetVNorm.y, targetVNorm.x);
                rotation = (rotation + 2 * 3.141592653f) % (2 * 3.141592653f);
                
                if(dist < currentMoveSpeed * delta) {
                    x = targetNode.x;
                    y = targetNode.y;
                    targetIndex++;
                } else {
                    direction.nor().scl(currentMoveSpeed * delta);
                    collider.dx = direction.x;
                    collider.dy = direction.y;
                }
            } else {
                targetIndex = 0;
                collider.dx = 0.0f;
                collider.dy = 0.0f;
            }
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
    public void updateFromNetwork(float x, float y, float z, float dx, float dy, float rotation, float health) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.rotation = rotation;
        this.health = health;
        spriteHealth.width = 0.35f * health/maxHealth;

        isAttacking = z != 0.0f;
    }

    @Override
    public void tickPathing() {
        if(currentTargetCollider != null || returnHomeAfterAggroTime > returnHomeAfterAggroTimeMax)
        {
            if(returnHomeAfterAggroTime > returnHomeAfterAggroTimeMax) {
                Vector2 pos = new Vector2(x, y);
                Vector2 home = new Vector2(homeX, homeY);
                if(pos.dst(home) < 1.0f) {
                    navPath = null;
                } else {
                    navPath = World.getNavMesh().pathFind(pos, home);
                }
            } else {
                navPath = World.getNavMesh().pathFind(new Vector2(x, y), new Vector2(currentTargetCollider.x, currentTargetCollider.y));
            }
            if(navPath == null) { //path find failed, so enter "dumb search" mode i.e. direct light on sight path
                navPath = new ArrayList<>();
                navPath.add(new Vector2(x, y));
                if(returnHomeAfterAggroTime > returnHomeAfterAggroTimeMax) {
                    navPath.add(new Vector2(homeX, homeY));
                } else {
                    navPath.add(new Vector2(currentTargetCollider.x, currentTargetCollider.y));
                }
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

    @Override
    public boolean isAggro() {
        return isAggro;
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
    public String getName() {
        return "range goblin";
    }

    @Override
    public float getZ() {
        return isAttacking ? 1.0f : 0.0f;
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
        return rotation;
    }
    
    @Override
    public float getHealth() {
        return health;
    }

    private Hate getHateForPlayer(int remotePlayerID) {
        Hate hate = hateMap.get(remotePlayerID);
        if(hate == null) {
            if(remotePlayerID == 0) {
                hate = new Hate(World.getPlayerCollider());
            } else {
                hate = new Hate(World.getRemotePlayers().get(remotePlayerID).getCollider());
            }
            hateMap.put(remotePlayerID, hate);
        }
        return hate;
    }
    
    @Override
    public void doDamage(float damage, int remotePlayerID) {
        getHateForPlayer(remotePlayerID).damageHate += damage;
        health -= damage;
    }

    @Override
    public float getRecentDamage() {
        float dmg = recentDamage;
        recentDamage = 0.0f;
        return dmg;
    }

}
