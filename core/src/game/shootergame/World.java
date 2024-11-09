package game.shootergame;

import java.util.ArrayList;
import java.util.LinkedList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;

import game.shootergame.Enemy.Enemy;
import game.shootergame.Enemy.Goblin;
import game.shootergame.Item.ItemPickup;
import game.shootergame.Item.MeleeWeapons.SwordWeapon;
import game.shootergame.Item.Powerups.AttackSpeedPowerup;
import game.shootergame.Item.Powerups.DamagePowerup;
import game.shootergame.Item.Powerups.DamageResistPowerup;
import game.shootergame.Item.Powerups.HealthPowerup;
import game.shootergame.Physics.Collider;
import game.shootergame.Physics.PhysicsWorld;

public class World {

    static World instance;

    PhysicsWorld physicsWorld;

    Player player;

    ItemPickup itemPrompt;

    ArrayList<Wall> walls;

    LinkedList<ItemPickup> items;

    LinkedList<Enemy> enemies;

    public static void createInstance() {
        instance = new World();
        instance.init();
    }

    public static Collider getPlayerCollider() {
        return instance.player.getCollider();
    }

    public static void processInput() {
        instance.player.processInput();
    }

    float wx = 0.0f;
    public static void update(float delta) {
        instance.itemPrompt = null;
        
        instance.physicsWorld.update();

        instance.player.update(delta);

        instance.wx += 1.0f / 144.0f;

        instance.walls.get(15).yOffset = (float)Math.sin(instance.wx) + 1.0f;

        instance.walls.get(15).height = 1.0f - instance.walls.get(15).yOffset / 2.0f;

        for (Enemy enemy : instance.enemies) {
            enemy.update(delta);
        }
    }

    public static void render() {
        instance.player.render();
    }

    public static void renderHud() {
        if(instance.itemPrompt != null) {
            String name = instance.itemPrompt.getName();
            BitmapFont font = ShooterGame.getInstance().am.get(ShooterGame.RSC_MONO_FONT);
            GlyphLayout layout = new GlyphLayout(font, "Press (E) to pickup: " + name);
            font.draw(ShooterGame.getInstance().coreBatch, layout, (Gdx.graphics.getWidth() / 2) - (layout.width / 2), 100.0f);
        }
    }

    public static void showPickupPrompt(ItemPickup item) {
        instance.itemPrompt = item;
    }

    public static ArrayList<Wall> getWalls() {
        return instance.walls;
    }

    public static Player getPlayer() {
        return instance.player;
    }

    public static PhysicsWorld getPhysicsWorld() {
        return instance.physicsWorld;
    }

    private World() {
        walls = new ArrayList<>();
        items = new LinkedList<>();
        enemies = new LinkedList<>();
    }

    private void init() {
        walls.add(new Wall(-1.1262f, 5.6620f, 0.0000f, 5.7729f));
        walls.add(new Wall(-2.2092f, 5.3334f, -1.1262f, 5.6620f));
        walls.add(new Wall(-3.2072f, 4.8000f, -2.2092f, 5.3334f));
        walls.add(new Wall(-4.0820f, 4.0820f, -3.2072f, 4.8000f));
        walls.add(new Wall(-4.8000f, 3.2072f, -4.0820f, 4.0820f));
        walls.add(new Wall(-5.3334f, 2.2092f, -4.8000f, 3.2072f));
        walls.add(new Wall(-5.6620f, 1.1262f, -5.3334f, 2.2092f));
        walls.add(new Wall(-5.7729f, -0.0000f, -5.6620f, 1.1262f));
        walls.add(new Wall(-5.6620f, -1.1262f, -5.7729f, -0.0000f));
        walls.add(new Wall(-5.3334f, -2.2092f, -5.6620f, -1.1262f));
        walls.add(new Wall(-4.8000f, -3.2072f, -5.3334f, -2.2092f));
        walls.add(new Wall(-4.0820f, -4.0820f, -4.8000f, -3.2072f));
        walls.add(new Wall(-3.2072f, -4.8000f, -4.0820f, -4.0820f));
        walls.add(new Wall(-2.2092f, -5.3334f, -3.2072f, -4.8000f));
        walls.add(new Wall(-1.1262f, -5.6620f, -2.2092f, -5.3334f));
        walls.add(new Wall(-1.1262f, -5.6620f, 1.1262f, -5.6620f));//
        walls.add(new Wall(2.2092f, -5.3334f, 1.1262f, -5.6620f));
        walls.add(new Wall(3.2072f, -4.8000f, 2.2092f, -5.3334f));
        walls.add(new Wall(4.0820f, -4.0820f, 3.2072f, -4.8000f));
        walls.add(new Wall(4.8000f, -3.2072f, 4.0820f, -4.0820f));
        walls.add(new Wall(5.3334f, -2.2092f, 4.8000f, -3.2072f));
        walls.add(new Wall(5.6620f, -1.1262f, 5.3334f, -2.2092f));
        walls.add(new Wall(5.7729f, -0.0000f, 5.6620f, -1.1262f));
        walls.add(new Wall(5.6620f, 1.1262f, 5.7729f, -0.0000f));
        walls.add(new Wall(5.3334f, 2.2092f, 5.6620f, 1.1262f));
        walls.add(new Wall(4.8000f, 3.2072f, 5.3334f, 2.2092f));
        walls.add(new Wall(4.0820f, 4.0820f, 4.8000f, 3.2072f));
        walls.add(new Wall(3.2072f, 4.8000f, 4.0820f, 4.0820f));
        walls.add(new Wall(2.2092f, 5.3334f, 3.2072f, 4.8000f));
        walls.add(new Wall(1.1262f, 5.6620f, 2.2092f, 5.3334f));
        walls.add(new Wall(0.0000f, 5.7729f, 1.1262f, 5.6620f));
        walls.add(new Wall(1.1262f, -5.6620f, 1.1262f, -11.7762f));
        walls.add(new Wall(-1.1262f, -14.1082f, 6.4566f, -14.1082f));
        walls.add(new Wall(1.1262f, -11.7762f, 6.4566f, -11.7762f));
        walls.add(new Wall(-1.1262f, -12.0725f, -1.1262f, -14.1082f));
        walls.add(new Wall(13.0804f, -9.1815f, 13.0804f, -16.7029f));
        walls.add(new Wall(6.4566f, -9.1815f, 13.0804f, -9.1815f));
        walls.add(new Wall(13.0804f, -16.7029f, 6.4566f, -16.7029f));
        walls.add(new Wall(6.4566f, -16.7029f, 6.4566f, -14.1082f));
        walls.add(new Wall(6.4566f, -11.7762f, 6.4566f, -9.1815f));
        walls.add(new Wall(-1.1262f, -5.6620f, -1.1262f, -6.8295f));
        walls.add(new Wall(-1.1262f, -12.0725f, -14.7606f, -12.0725f));
        walls.add(new Wall(-30.4897f, -22.5585f, -30.4897f, -12.0725f));
        walls.add(new Wall(-30.4897f, -12.0725f, -35.7327f, -12.0725f));
        walls.add(new Wall(-25.2467f, -22.5585f, -25.2467f, -12.0725f));
        walls.add(new Wall(-20.0037f, -22.5585f, -20.0037f, -12.0725f));
        walls.add(new Wall(-20.0037f, -12.0725f, -25.2467f, -12.0725f));
        walls.add(new Wall(-14.7606f, -22.5585f, -14.7606f, -12.0725f));
        walls.add(new Wall(-17.3822f, 3.6566f, -12.1391f, 3.6566f));
        walls.add(new Wall(-27.8682f, -6.8295f, -35.7327f, -6.8295f));
        walls.add(new Wall(-17.3822f, -6.8295f, -22.6252f, -6.8295f));
        walls.add(new Wall(-1.1262f, -6.8295f, -12.1391f, -6.8295f));
        walls.add(new Wall(-27.8682f, -6.8295f, -27.8682f, 3.6566f));
        walls.add(new Wall(-27.8682f, 3.6566f, -22.6252f, 3.6566f));
        walls.add(new Wall(-22.6252f, -6.8295f, -22.6252f, 3.6566f));
        walls.add(new Wall(-35.7327f, -12.0725f, -35.7327f, -6.8295f));
        walls.add(new Wall(-17.3822f, -6.8295f, -17.3822f, 3.6566f));
        walls.add(new Wall(-20.0037f, -22.5585f, -14.7606f, -22.5585f));
        walls.add(new Wall(-12.1391f, -6.8295f, -12.1391f, 3.6566f));
        walls.add(new Wall(-30.4897f, -22.5585f, -25.2467f, -22.5585f));

        walls.get(15).height = 2.0f;
        walls.get(15).textureID = 1.0f;
        walls.get(15).transparentDoor = true;

        physicsWorld = new PhysicsWorld(walls);
        itemPrompt = null;

        player = new Player(new SwordWeapon());

        items.add(new ItemPickup(1.0f, 1.0f, (new DamagePowerup())));
        items.add(new ItemPickup(-1.0f, -1.0f, (new HealthPowerup())));
        items.add(new ItemPickup(0.0f, 0.0f, (new DamageResistPowerup())));
        items.add(new ItemPickup(-1.0f, 1.0f, (new AttackSpeedPowerup())));

        enemies.add(new Goblin(0.0f, 0.0f));
    }
}
