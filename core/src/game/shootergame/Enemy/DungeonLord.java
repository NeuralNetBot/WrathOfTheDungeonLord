package game.shootergame.Enemy;

import java.util.ArrayList;
import java.util.HashMap;
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

public class DungeonLord implements Enemy{

    float x, y;
    final float homeX, homeY;
    float dx, dy;
    float rotation;
    final float maxHealth = 300.0f;
    float health;
    Collider collider;

    Collider currentTargetCollider = null;

    final float moveSpeed = 2.5f;

    Sprite2_5D spriteLow;
    Sprite2_5D spriteHigh;
    Sprite2_5D spriteWeapon;
    Sprite2_5D spriteLunge;
    Sprite2_5D spriteSlam;
    Texture texWalk;
    Texture texLunge;
    Texture texSlam;
    Texture texRock;
    TextureRegion regLow;

    ArrayList<Vector2> navPath;
    int targetIndex = 0;


    Sound damaged;

    //for walk
    @SuppressWarnings("unchecked")
    Animation<TextureRegion>[] animationsWalk = new Animation[8];
    TextureRegion[] highRegions = new TextureRegion[8];
    Vector2[] highSizes = new Vector2[8];
    float[] highZ = new float[8];
    TextureRegion[] weaponRegions = new TextureRegion[8];
    Vector2[] weaponSizes = new Vector2[8];
    Vector2[] weaponOffset = new Vector2[8];
    boolean[] weaponOffsetSide = new boolean[8];
    
    //for lunge attack
    @SuppressWarnings("unchecked")
    Animation<TextureRegion>[] animationLungeAttack = new Animation[8];
    float[] lungeAttackSpriteOffset = new float[8];

    //for slam attack
    @SuppressWarnings("unchecked")
    Animation<TextureRegion>[] animationSlamAttack = new Animation[8];

    TextureRegion[] rockRegions = new TextureRegion[4];

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
    final float rangeHateRange = 30.0f;

    boolean isAggro = false;

    boolean isAttacking = false;
    float attackCooldown = 0.0f;
    final float attackCooldownMax = 0.2f;
    final float damage = 10.0f;
    boolean hasDoneDamage = false;

    final float returnHomeAfterAggroTimeMax = 3.0f;
    float returnHomeAfterAggroTime = 0.0f;

    final float lungeDistance = 8.0f;
    final float lungeTime = 0.66f;
    final float lungeBeginTime = 0.54f;
    float lungeTimer = 0.0f;

    boolean isSlaming = false;
    float slamTimer = 0.0f;
    final float slamTimeMaxAir = 1.5f;
    final float slamTimeMinAir = 0.9f;
    final float slamHoldTime = 0.8f;
    float slamHoldTimer = 0.0f;
    final float baseSlamZ = 1.28f;

    final int slamRockCount = 90;
    final float slamRockRange = 10.0f;

    int prevRockCount = 0;
    
    Sprite2_5D[] rockSprites = new Sprite2_5D[slamRockCount];

    final float slamDamage = 5.0f;
    final float lungeDamage = 20.0f;

    boolean damageable = false;

    public DungeonLord(float x, float y, boolean isRemote) {
        homeX = x;
        homeY = y;
        this.isRemote = isRemote;

        ShooterGame.getInstance().am.load("boss_walk.png", Texture.class);
        ShooterGame.getInstance().am.load("boss_lunge.png", Texture.class);
        ShooterGame.getInstance().am.load("boss_slam.png", Texture.class);
        ShooterGame.getInstance().am.load("rock.png", Texture.class);
        ShooterGame.getInstance().am.load("boss_damaged.mp3", Sound.class);
        ShooterGame.getInstance().am.finishLoading();
        texWalk = ShooterGame.getInstance().am.get("boss_walk.png", Texture.class);
        texLunge = ShooterGame.getInstance().am.get("boss_lunge.png", Texture.class);
        texSlam = ShooterGame.getInstance().am.get("boss_slam.png", Texture.class);
        texRock = ShooterGame.getInstance().am.get("rock.png", Texture.class);
        damaged = ShooterGame.getInstance().am.get("boss_damaged.mp3", Sound.class);
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

        highRegions[0] = new TextureRegion(texWalk, 3200 - 90, 0, 90, 90);      highSizes[0] = new Vector2(2.25f, 2.25f);  highZ[0] = 2.125f;
        highRegions[1] = new TextureRegion(texWalk, 3200 - 100, 320, 100, 75);  highSizes[1] = new Vector2(3.25f, 2.125f); highZ[1] = 2.0625f;
        highRegions[2] = new TextureRegion(texWalk, 3200 - 90, 640, 90, 90);    highSizes[2] = new Vector2(2.25f, 2.25f);  highZ[2] = 2.125f;
        highRegions[3] = new TextureRegion(texWalk, 3200 - 90, 960, 90, 90);    highSizes[3] = new Vector2(2.25f, 2.25f);  highZ[3] = 2.125f;
        highRegions[4] = new TextureRegion(texWalk, 3200 - 90, 1280, 90, 90);   highSizes[4] = new Vector2(2.25f, 2.25f);  highZ[4] = 2.125f;
        highRegions[5] = new TextureRegion(texWalk, 3200 - 100, 1600, 100, 75); highSizes[5] = new Vector2(3.25f, 2.125f); highZ[5] = 2.0625f;
        highRegions[6] = new TextureRegion(texWalk, 3200 - 90, 1920, 90, 90);   highSizes[6] = new Vector2(2.25f, 2.25f);  highZ[6] = 2.125f;
        highRegions[7] = new TextureRegion(texWalk, 3200 - 90, 2240, 90, 90);   highSizes[7] = new Vector2(2.25f, 2.25f);  highZ[7] = 2.125f;

        
        weaponRegions[0] = new TextureRegion(texWalk, 3200 - 120, 160, 120, 96);  weaponSizes[0] = new Vector2(2.82f, 2.25f); weaponOffset[0] = new Vector2(1.33f, 0.08f); weaponOffsetSide[0] = true;
        weaponRegions[1] = new TextureRegion(texWalk, 3200 - 120, 480, 120, 96);  weaponSizes[1] = new Vector2(2.82f, 2.25f); weaponOffset[1] = new Vector2(1.33f, 0.08f); weaponOffsetSide[1] = true;
        weaponRegions[2] = new TextureRegion(texWalk, 3200 - 96, 800, 96, 120);   weaponSizes[2] = new Vector2(2.25f, 2.82f); weaponOffset[2] = new Vector2(0.9f, -0.32f);   weaponOffsetSide[2] = true;
        weaponRegions[3] = new TextureRegion(texWalk, 3200 - 120, 1120, 120, 96); weaponSizes[3] = new Vector2(2.82f, 2.25f); weaponOffset[3] = new Vector2(-1.33f, 0.08f);  weaponOffsetSide[3] = true;
        weaponRegions[4] = new TextureRegion(texWalk, 3200 - 120, 1440, 120, 96); weaponSizes[4] = new Vector2(2.82f, 2.25f); weaponOffset[4] = new Vector2(-1.33f, 0.04f);  weaponOffsetSide[4] = true;
        weaponRegions[5] = new TextureRegion(texWalk, 3200 - 120, 1760, 120, 96); weaponSizes[5] = new Vector2(2.82f, 2.25f); weaponOffset[5] = new Vector2(-1.33f, 0.08f);  weaponOffsetSide[5] = true;
        weaponRegions[6] = new TextureRegion(texWalk, 3200 - 96, 2080, 96, 120);  weaponSizes[6] = new Vector2(2.25f, 2.82f); weaponOffset[6] = new Vector2(-0.3f, 0.36f);   weaponOffsetSide[6] = true;
        weaponRegions[7] = new TextureRegion(texWalk, 3200 - 120, 2400, 120, 96); weaponSizes[7] = new Vector2(2.82f, 2.25f); weaponOffset[7] = new Vector2(-0.1f, 0.65f);  weaponOffsetSide[7] = false;


        {
            TextureRegion[][] tempFrames = TextureRegion.split(texLunge, texLunge.getWidth() / 18, texLunge.getHeight() / (8*2));
            
            for (int i = 0; i < 8; i++) {
                TextureRegion[] animFrames = new TextureRegion[18 * 2];
                for (int j = 0; j < 2; j++) {
                    for (int k = 0; k < 18; k++) {
                        animFrames[k + (18 * j)] = tempFrames[i * 2 + j][k];
                    }
                }
                animationLungeAttack[i] = new Animation<TextureRegion>(0.06f, animFrames);
                animationLungeAttack[i].setPlayMode(PlayMode.LOOP);
            }
        }

        lungeAttackSpriteOffset[0] = 1.37f;
        lungeAttackSpriteOffset[1] = -0.9f;
        lungeAttackSpriteOffset[2] = 1.45f;
        lungeAttackSpriteOffset[3] = -1.37f;
        lungeAttackSpriteOffset[4] = -1.37f;
        lungeAttackSpriteOffset[5] = -0.1f;
        lungeAttackSpriteOffset[6] = -1.37f;
        lungeAttackSpriteOffset[7] = 1.37f;

        {
            TextureRegion[][] tempFrames = TextureRegion.split(texSlam, texSlam.getWidth() / 19, texSlam.getHeight() / (8*2));
            
            for (int i = 0; i < 8; i++) {
                TextureRegion[] animFrames = new TextureRegion[19 * 2];
                for (int j = 0; j < 2; j++) {
                    for (int k = 0; k < 19; k++) {
                        animFrames[k + (19 * j)] = tempFrames[i * 2 + j][k];
                    }
                }
                animationSlamAttack[i] = new Animation<TextureRegion>(0.06f, animFrames);
                animationSlamAttack[i].setPlayMode(PlayMode.LOOP);
            }
        }

        rockRegions = TextureRegion.split(texRock, texRock.getWidth() / 4, texRock.getHeight())[0];

        health = maxHealth;

        currentTargetCollider = null;
        navPath = null;

        this.x = x; this.y = y;
        dx = 0; dy = 0;
        spriteLow = new Sprite2_5D(regLow, x, y, -1.0f, 4.0f, 3.2f);
        Renderer.inst().addSprite(spriteLow);
        spriteHigh = new Sprite2_5D(highRegions[0], x, y, 2.125f, 4.0f, 3.2f);
        Renderer.inst().addSprite(spriteHigh);
        spriteWeapon = new Sprite2_5D(weaponRegions[0], x, y, 0.08f, 4.0f, 3.2f);
        Renderer.inst().addSprite(spriteWeapon);
        
        spriteLunge = new Sprite2_5D(weaponRegions[0], x, y, 1.28f, 8.6588f, 6.9271f);
        Renderer.inst().addSprite(spriteLunge);

        spriteSlam = new Sprite2_5D(weaponRegions[0], x, y, baseSlamZ, 8.6588f, 8.6588f);
        Renderer.inst().addSprite(spriteSlam);

        for (int i = 0; i < slamRockCount; i++) {
            int rockIndex = ShooterGame.getInstance().random.nextInt(4);
            rockSprites[i] = new Sprite2_5D(rockRegions[rockIndex], x, y, -1.5f, 1.0f, 1.0f);
            rockSprites[i].forceHide = true;
            Renderer.inst().addSprite(rockSprites[i]);
        }

        if(isRemote) {
            collider = new Collider(x, y, 0.5f,  (Collider collider, float newDX, float newDY, float damage)->{
                recentDamage += damage;
            }, false, 1.3f);
        } else {
            collider = new Collider(x, y, 0.5f,  (Collider collider, float newDX, float newDY, float damage)->{
                if(collider == null) { //wall coll
                    //dx = newDX; dy = newDY;
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

        World.getPlayer().setBossBarEnabled(true);
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

        spriteLow.setRegion(animationsWalk[realIndex].getKeyFrame(animTime));
        spriteHigh.setRegion(highRegions[realIndex]);
        spriteHigh.width = highSizes[realIndex].x;
        spriteHigh.height = highSizes[realIndex].y;
        spriteHigh.z = highZ[realIndex];
        spriteWeapon.setRegion(weaponRegions[realIndex]);
        float offsetX = weaponOffset[realIndex].x;

        float alignAxisX = -(float)Math.sin(Math.toRadians(World.getPlayer().rotation()));
        float alignAxisY = (float)Math.cos(Math.toRadians(World.getPlayer().rotation()));
        float leftRightAlignMul = (offsetX > 0.0f ? 1.0f : -1.0f);
        if(weaponOffsetSide[realIndex]) {
            offsetX = Math.abs(offsetX);
        } else {
            leftRightAlignMul = 1.0f;
        }
        float wx = x + (alignAxisX * 1.6f) * leftRightAlignMul;
        float wy = y + (alignAxisY * 1.6f) * leftRightAlignMul;
        spriteWeapon.width = weaponSizes[realIndex].x;
        spriteWeapon.height = weaponSizes[realIndex].y;
        spriteWeapon.x = wx + (alignAxisX * offsetX / 2) * leftRightAlignMul;
        spriteWeapon.y = wy + (alignAxisY * offsetX / 2) * leftRightAlignMul;

        spriteWeapon.z = weaponOffset[realIndex].y;

        spriteLunge.setRegion(animationLungeAttack[realIndex].getKeyFrame(lungeTimer));
        spriteLunge.x = x + (alignAxisX * lungeAttackSpriteOffset[realIndex]);
        spriteLunge.y = y + (alignAxisY * lungeAttackSpriteOffset[realIndex]);

        spriteSlam.setRegion(animationSlamAttack[realIndex].getKeyFrame(slamTimer));
        
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

            if(currentTargetCollider != null) {

                float distanceToTarget = Vector2.dst(x, y, currentTargetCollider.x, currentTargetCollider.y);
                if(distanceToTarget > lungeDistance && lungeTimer == 0.0f && slamTimer == 0.0f) {
                    isAttacking = false;
                    isSlaming = false;
                    spriteHigh.forceHide = spriteLow.forceHide = spriteWeapon.forceHide = false;
                    spriteLunge.forceHide = spriteSlam.forceHide = true;
                    dx = -(float)Math.cos(rotation) * moveSpeed * delta;
                    dy = -(float)Math.sin(rotation) * moveSpeed * delta;
                    rotation = angleToSprite;
                } else {
                    //select random attack
                    if(isAttacking == false && isSlaming == false) {
                        isAttacking = ShooterGame.getInstance().random.nextBoolean();
                        isSlaming = !isAttacking;
                    }
                    if(isAttacking) { //lunge
                        spriteHigh.forceHide = spriteLow.forceHide = spriteWeapon.forceHide = spriteSlam.forceHide = true;
                        spriteLunge.forceHide = false;
                        lungeTimer += delta;
                        if(animationLungeAttack[realIndex].isAnimationFinished(lungeTimer)) {
                            isAttacking = false;
                            hasDoneDamage = false;
                            lungeTimer = 0.0f;
                            rotation = angleToSprite;
                            damageable = false;
                        }
                        if(!hasDoneDamage && lungeTimer > 1.0f) {
                            World.getPhysicsWorld().runAngleSweep(collider, x, y, rotation + 3.1415f, 0.34f, 4.0f, lungeDamage);
                            hasDoneDamage = true;
                            damageable = true;
                        }
                        
                        if(lungeTimer < lungeTime + lungeBeginTime && lungeTimer > lungeBeginTime && distanceToTarget > 2.0f) {
                            float lungeDistPerSecond = lungeDistance / lungeTime;
                            dx = -(float)Math.cos(rotation) * lungeDistPerSecond * delta;
                            dy = -(float)Math.sin(rotation) * lungeDistPerSecond * delta;
                        } else {
                            dx = 0.0f;
                            dy = 0.0f;
                        }
                    } else { //slam
                        spriteHigh.forceHide = spriteLow.forceHide = spriteWeapon.forceHide = spriteLunge.forceHide = true;
                        spriteSlam.forceHide = false;
                        if(slamTimer > slamTimeMaxAir && slamHoldTimer < slamHoldTime) {
                            damageable = true;
                            
                            slamHoldTimer += delta;
                            float ratio = slamHoldTimer / slamHoldTime;
                            int count = (int)(ratio * (slamRockCount / 3));
                            spawnRocks(delta, count);
                            if(prevRockCount != count) {
                                float sweepDistance = (float)count * 0.5f;
                                World.getPhysicsWorld().runAngleSweep(collider, x, y, rotation + 3.1415f, 0.17f, sweepDistance, slamDamage, sweepDistance - 2.0f);
                            }
                            prevRockCount = count;
                        } else {
                            slamTimer += delta;
                        }
                        if(animationSlamAttack[realIndex].isAnimationFinished(slamTimer)) {
                            isSlaming = false;
                            slamTimer = 0.0f;
                            slamHoldTimer = 0.0f;
                            rotation = angleToSprite;
                            damageable = false;
                            for (int i = 0; i < slamRockCount; i++) {
                                rockSprites[i].forceHide = true;
                            }
                        }
                        updateSlamHeight();

                        dx = 0.0f;
                        dy = 0.0f;
                    }

                }
            }

        } else {
            if(isAttacking) {
                spriteHigh.forceHide = spriteLow.forceHide = spriteWeapon.forceHide = spriteSlam.forceHide = true;
                spriteLunge.forceHide = false;
                lungeTimer += delta;
                if(lungeTimer > 1.0f) {
                    damageable = true;
                }
            } else if(isSlaming) {
                spriteHigh.forceHide = spriteLow.forceHide = spriteWeapon.forceHide = spriteLunge.forceHide = true;
                spriteSlam.forceHide = false;
                slamTimer += delta;
                updateSlamHeight();
                if(slamTimer > slamTimeMaxAir && slamHoldTimer < slamHoldTime) {
                    damageable = true;
                    
                    slamHoldTimer += delta;
                    float ratio = slamHoldTimer / slamHoldTime;
                    int count = (int)(ratio * (slamRockCount / 3));
                    spawnRocks(delta, count);
                }

            } else {
                spriteHigh.forceHide = spriteLow.forceHide = spriteWeapon.forceHide = false;
                spriteLunge.forceHide = spriteSlam.forceHide = true;
                lungeTimer = 0.0f;
                slamTimer = 0.0f;
                slamHoldTimer = 0.0f;
                damageable = false;
                for (int i = 0; i < slamRockCount; i++) {
                    rockSprites[i].forceHide = true;
                }
            }
        }

        collider.x = x;
        collider.y = y;
        
        spriteLow.x = x;
        spriteLow.y = y;
        spriteHigh.x = x;
        spriteHigh.y = y;

        spriteSlam.x = x;
        spriteSlam.y = y;
        
        World.getPlayer().setBossBarHealth(health, maxHealth);
        World.getPlayer().setBossBarHitable(damageable);
    }

    private void spawnRocks(float delta, int count) {
        float rdx = -(float)Math.cos(rotation);
        float rdy = -(float)Math.sin(rotation);
        float lrdx = -(float)Math.cos(rotation - 0.17f);
        float lrdy = -(float)Math.sin(rotation - 0.17f);
        float rrdx = -(float)Math.cos(rotation + 0.17f);
        float rrdy = -(float)Math.sin(rotation +- 0.17f);
        for (int i = 0; i < count; i++) {
            int idx = i * 3;
            float dst = ((float)i) * 0.5f;
            rockSprites[idx].forceHide = false;
            rockSprites[idx].x = x + (rdx * dst) + (rdx * 3.0f);
            rockSprites[idx].y = y + (rdy * dst) + (rdy * 3.0f);
            rockSprites[idx+1].forceHide = false;
            rockSprites[idx+1].x = x + (lrdx * dst) + (lrdx * 3.0f);
            rockSprites[idx+1].y = y + (lrdy * dst) + (lrdy * 3.0f);
            rockSprites[idx+2].forceHide = false;
            rockSprites[idx+2].x = x + (rrdx * dst) + (rrdx * 3.0f);
            rockSprites[idx+2].y = y + (rrdy * dst) + (rrdy * 3.0f);
        }
    }

    private void updateSlamHeight() {
        if(slamTimer < slamTimeMaxAir && slamTimer > slamTimeMinAir) {
            float slamDelta = slamTimer - slamTimeMinAir;
            float slamMaxDelta = slamTimeMaxAir - slamTimeMinAir;
            spriteSlam.z = baseSlamZ + 3.0f * (float)Math.sin((slamDelta / slamMaxDelta) * Math.PI);
        } else {
            spriteSlam.z = baseSlamZ;
        }
    }

    @Override
    public void updateFromNetwork(float x, float y, float z, float dx, float dy, float rotation, float health) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.rotation = rotation;
        this.health = health;
        if(z == 2.0f) {
            isSlaming = true;
            isAttacking = false;
        } else if(z == 1.0f) {
            isAttacking = true;
            isSlaming = false;
        } else {
            isAttacking = false;
            isSlaming = false;
        }
    }

    @Override
    public void tickPathing() {
    }

    @Override
    public void onKill() {
        Renderer.inst().removeSprite(spriteLow);
        Renderer.inst().removeSprite(spriteHigh);
        Renderer.inst().removeSprite(spriteWeapon);
        Renderer.inst().removeSprite(spriteLunge);
        World.getPhysicsWorld().removeCollider(collider);
        World.getPlayer().setBossBarEnabled(false);
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
        return "boss";
    }

    @Override
    public float getZ() {
        return isSlaming ? 2.0f : (isAttacking ? 1.0f : 0.0f);
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
        if(damageable)
            health -= damage;

        damaged.play(0.3f);
    }

    @Override
    public float getRecentDamage() {
        float dmg = recentDamage;
        recentDamage = 0.0f;
        return dmg;
    }

}
