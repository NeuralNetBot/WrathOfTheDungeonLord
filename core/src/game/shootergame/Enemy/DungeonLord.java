package game.shootergame.Enemy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Map.Entry;

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

public class DungeonLord implements Enemy{

    float x, y;
    final float homeX, homeY;
    float dx, dy;
    float rotation;
    final float maxHealth = 35.0f;
    float health;
    Collider collider;

    Collider currentTargetCollider = null;

    final float moveSpeed = 1.5f;

    Sprite2_5D spriteLow;
    Sprite2_5D spriteHigh;
    Texture texWalk;
    TextureRegion regLow;

    ArrayList<Vector2> navPath;
    int targetIndex = 0;

    @SuppressWarnings("unchecked")
    Animation<TextureRegion>[] animationsWalk = new Animation[8];
    TextureRegion[] highRegions = new TextureRegion[8];
    Vector2[] highSizes = new Vector2[8];
    float[] highZ = new float[8];

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
    final float rangeHateRange = 12.0f;

    boolean isAggro = false;

    boolean isAttacking = false;
    float attackCooldown = 0.0f;
    final float attackCooldownMax = 0.2f;
    final float damage = 10.0f;
    boolean hasDoneDamage = false;

    final float returnHomeAfterAggroTimeMax = 3.0f;
    float returnHomeAfterAggroTime = 0.0f;

    public DungeonLord(float x, float y, boolean isRemote) {
        homeX = x;
        homeY = y;
        this.isRemote = isRemote;
        if(!isRemote) {
            rotation = Objects.hash(x, y);
            rotation = (rotation + 2 * 3.141592653f) % (2 * 3.141592653f);
        }
        ShooterGame.getInstance().am.load("boss_walk.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        texWalk = ShooterGame.getInstance().am.get("boss_walk.png", Texture.class);
        regLow = new TextureRegion(texWalk, 0, 0, 128, 160);

        {
            //walk anims
            //24 wide +1 for other parts 23 wide at second layer
            //2 tall
            //8 sides stacked
            TextureRegion[][] tempFrames = TextureRegion.split(texWalk, texWalk.getWidth() / 25, texWalk.getHeight() / (8*2));
            
            for (int i = 0; i < 8; i++) {
                int index = 0;
                TextureRegion[] animFrames = new TextureRegion[(24 * 2) - 1];
                for (int j = 0; j < 2; j++) {
                    for (int k = 0; k < 24; k++) {
                        if(j == 1 && k == 23) break;
                        animFrames[index] = tempFrames[i * 2 + j][k];
                        index++;
                    }
                }
                animationsWalk[i] = new Animation<TextureRegion>(0.06f, animFrames);
                animationsWalk[i].setPlayMode(PlayMode.LOOP);
            }
        }
        {
            highRegions[0] = new TextureRegion(texWalk, 3200 - 90, 0, 90, 90);      highSizes[0] = new Vector2(2.25f, 2.25f);  highZ[0] = 2.125f;
            highRegions[1] = new TextureRegion(texWalk, 3200 - 100, 320, 100, 75);  highSizes[1] = new Vector2(3.25f, 2.125f); highZ[1] = 2.0625f;
            highRegions[2] = new TextureRegion(texWalk, 3200 - 90, 640, 90, 90);    highSizes[2] = new Vector2(2.25f, 2.25f);  highZ[2] = 2.125f;
            highRegions[3] = new TextureRegion(texWalk, 3200 - 90, 960, 90, 90);    highSizes[3] = new Vector2(2.25f, 2.25f);  highZ[3] = 2.125f;
            highRegions[4] = new TextureRegion(texWalk, 3200 - 90, 1280, 90, 90);   highSizes[4] = new Vector2(2.25f, 2.25f);  highZ[4] = 2.125f;
            highRegions[5] = new TextureRegion(texWalk, 3200 - 100, 1600, 100, 75); highSizes[5] = new Vector2(3.25f, 2.125f); highZ[5] = 2.0625f;
            highRegions[6] = new TextureRegion(texWalk, 3200 - 90, 1920, 90, 90);   highSizes[6] = new Vector2(2.25f, 2.25f);  highZ[6] = 2.125f;
            highRegions[7] = new TextureRegion(texWalk, 3200 - 90, 2240, 90, 90);   highSizes[7] = new Vector2(2.25f, 2.25f);  highZ[7] = 2.125f;
        }

        health = maxHealth;

        currentTargetCollider = null;
        navPath = null;

        this.x = x; this.y = y;
        dx = 0; dy = 0;
        spriteLow = new Sprite2_5D(regLow, x, y, -1.0f, 4.0f, 3.2f);
        Renderer.inst().addSprite(spriteLow);
        spriteHigh = new Sprite2_5D(highRegions[0], x, y, 2.125f, 4.0f, 3.2f);
        Renderer.inst().addSprite(spriteHigh);

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


        spriteLow.setRegion(animationsWalk[realIndex].getKeyFrame(animTime));
        spriteHigh.setRegion(highRegions[realIndex]);
        spriteHigh.width = highSizes[realIndex].x;
        spriteHigh.height = highSizes[realIndex].y;
        spriteHigh.z = highZ[realIndex];

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

        }

        collider.x = x;
        collider.y = y;
        
        spriteLow.x = x;
        spriteLow.y = y;
        spriteHigh.x = x;
        spriteHigh.y = y;
    }

    @Override
    public void updateFromNetwork(float x, float y, float z, float dx, float dy, float rotation, float health) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.rotation = rotation;
        this.health = health;

        isAttacking = z != 0.0f;
    }

    @Override
    public void tickPathing() {
    }

    @Override
    public void onKill() {
        Renderer.inst().removeSprite(spriteLow);
        Renderer.inst().removeSprite(spriteHigh);
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
        return "lord";
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
