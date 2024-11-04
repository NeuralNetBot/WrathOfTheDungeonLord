package game.shootergame;

import java.util.ArrayList;
import java.util.LinkedList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;

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

    public static void update(float delta) {
        instance.itemPrompt = null;
        instance.physicsWorld.update();

        instance.player.update(delta);
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
    }

    private void init() {
        walls.add(new Wall(1.5177f, 3.4569f, 3.9219f, 3.5477f));
        walls.add(new Wall(3.9219f, 3.5477f, 3.8766f, 0.9279f));
        walls.add(new Wall(3.8766f, 0.9279f, 4.0353f, -1.5330f));
        walls.add(new Wall(1.3929f, 1.0187f, 1.5177f, 3.4569f));
        walls.add(new Wall(-1.0000f, 1.0527f, 1.3929f, 1.0187f));
        walls.add(new Wall(-1.0227f, -1.0113f, -1.0000f, 1.0527f));
        walls.add(new Wall(4.0353f, -1.5330f, 1.2908f, -1.2608f));
        walls.add(new Wall(1.2908f, -1.2608f, 1.3702f, -4.4703f));
        walls.add(new Wall(1.3702f, -4.4703f, -5.9900f, -3.8465f));
        walls.add(new Wall(-5.9900f, -3.8465f, -5.8539f, -1.3175f));
        walls.add(new Wall(-5.8539f, -1.3175f, -3.1434f, 2.2662f));
        walls.add(new Wall(-3.1434f, 2.2662f, -3.4836f, -1.7712f));
        walls.add(new Wall(-3.4836f, -1.7712f, -2.4630f, -1.8959f));
        walls.add(new Wall(-2.4630f, -1.8959f, -2.4403f, 3.4683f));
        walls.add(new Wall(-2.4403f, 3.4683f, 0.3949f, 3.5023f));
        walls.add(new Wall(0.3949f, 3.5023f, 0.2475f, 2.2095f));
        walls.add(new Wall(0.2475f, 2.2095f, -1.6805f, 2.2321f));
        walls.add(new Wall(-1.6805f, 2.2321f, -1.0227f, -1.0113f));
        walls.get(0).height = 2.0f;
        walls.get(0).textureID = 1.0f;
        walls.get(0).transparentDoor = true;

        physicsWorld = new PhysicsWorld(walls);
        itemPrompt = null;

        player = new Player(new SwordWeapon());

        items.add(new ItemPickup(5.0f, 0.0f, (new DamagePowerup())));
        items.add(new ItemPickup(-5.0f, 0.0f, (new HealthPowerup())));
        items.add(new ItemPickup(0.0f, 0.0f, (new DamageResistPowerup())));
        items.add(new ItemPickup(10.0f, 0.0f, (new AttackSpeedPowerup())));
    }
}
