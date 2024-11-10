package game.shootergame;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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
    ArrayList<Integer> doors;

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

        for (Integer door : instance.doors) {
            instance.walls.get(door).yOffset = (float)Math.sin(instance.wx) + 1.0f;
            instance.walls.get(door).height = 1.0f - instance.walls.get(door).yOffset / 2.0f;
        }

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
        doors = new ArrayList<>();
        items = new LinkedList<>();
        enemies = new LinkedList<>();
    }

    private void loadFromFile(String mapName) {
        try (BufferedReader br = new BufferedReader(new FileReader(mapName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");

                String type = parts[0];

                if(type.equals("wall")) {
                    if(parts.length != 8) {
                        System.err.println("ERROR malformed map wall read");
                    }
                    float ax = -Float.parseFloat(parts[1]);
                    float ay = Float.parseFloat(parts[2]);
                    float bx = -Float.parseFloat(parts[3]);
                    float by = Float.parseFloat(parts[4]);
                    float height = Float.parseFloat(parts[5]);
                    float textureID = Float.parseFloat(parts[6]);
                    boolean isDoor = Boolean.parseBoolean(parts[7]);
                    walls.add(new Wall(ax, ay, bx, by, height, textureID, isDoor));
                    if(isDoor) {
                        doors.add(walls.size() - 1);
                    }
                }

            }
            System.out.println("Map: '" + mapName + "' loaded");
            System.out.println((walls.size() - doors.size()) + " walls");
            System.out.println(doors.size() + " doors");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void init() {
        loadFromFile("assets/map0.data");

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
