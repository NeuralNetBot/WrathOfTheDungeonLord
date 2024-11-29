package game.shootergame;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;

import game.shootergame.Enemy.Enemy;
import game.shootergame.Enemy.Goblin;
import game.shootergame.Enemy.NavMesh;
import game.shootergame.Enemy.NavMesh.Triangle;
import game.shootergame.Enemy.Slime;
import game.shootergame.Item.ItemPickup;
import game.shootergame.Item.MeleeWeapons.HalberdWeapon;
import game.shootergame.Item.MeleeWeapons.MaceWeapon;
import game.shootergame.Item.MeleeWeapons.SwordWeapon;
import game.shootergame.Item.Powerups.AttackSpeedPowerup;
import game.shootergame.Item.Powerups.DamagePowerup;
import game.shootergame.Item.Powerups.DamageResistPowerup;
import game.shootergame.Item.Powerups.HealthPowerup;
import game.shootergame.Item.RangedWeapons.CrossbowWeapon;
import game.shootergame.Network.Client;
import game.shootergame.Network.RemotePlayer;
import game.shootergame.Network.Server;
import game.shootergame.Physics.Collider;
import game.shootergame.Physics.PhysicsWorld;
import game.shootergame.Renderer.RegionIndexCuller;
import game.shootergame.Renderer.Torch;

public class World {

    static World instance;

    PhysicsWorld physicsWorld;

    Player player;

    ItemPickup itemPrompt;

    ArrayList<Wall> walls;
    ArrayList<Torch> torches;
    RegionIndexCuller torchRegionIndexCuller;
    ArrayList<Integer> doors;

    LinkedList<ItemPickup> items;

    LinkedList<Enemy> enemies;
    int pathTickIndex = 0;

    NavMesh navMesh;

    ConcurrentHashMap<Integer, RemotePlayer> remotePlayers;

    Server server;
    Client client;
    Sound ambient;

    public static void createInstance() {
        instance = new World();
        instance.init();
    }

    public static Collider getPlayerCollider() {
        return instance.player.getCollider();
    }

    public static void processInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            new Thread(new Server(instance.remotePlayers)).start();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            instance.client = new Client(instance.remotePlayers);
        }
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

        int index = 0;
        for (Entry<Integer, RemotePlayer> entry : instance.remotePlayers.entrySet()) {
            entry.getValue().update(delta);
        }

        Iterator<Enemy> it = instance.enemies.iterator();
        while(it.hasNext()) {
            Enemy enemy = it.next();
            enemy.update(delta);
            if(index == instance.pathTickIndex)
            enemy.tickPathing();
            index++;

            if(!enemy.isAlive()) {
                enemy.onKill();
                it.remove();
            }
        }

        instance.pathTickIndex++;
        if(instance.pathTickIndex >= instance.enemies.size()) {
            instance.pathTickIndex = 0;
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

    public static ArrayList<Torch> getTorches() {
        return instance.torches;
    }

    public static RegionIndexCuller getTorchRegionIndexCuller() {
        return instance.torchRegionIndexCuller;
    }

    public static Player getPlayer() {
        return instance.player;
    }

    public static PhysicsWorld getPhysicsWorld() {
        return instance.physicsWorld;
    }

    public static NavMesh getNavMesh() {
        return instance.navMesh;
    }

    private World() {
        walls = new ArrayList<>();
        torches = new ArrayList<>();
        torchRegionIndexCuller = new RegionIndexCuller();
        doors = new ArrayList<>();
        items = new LinkedList<>();
        enemies = new LinkedList<>();
        navMesh = new NavMesh();
        remotePlayers = new ConcurrentHashMap<>();

        ShooterGame.getInstance().am.load("dungeon_ambient.mp3", Sound.class);
        ShooterGame.getInstance().am.finishLoading();
        ambient = ShooterGame.getInstance().am.get("dungeon_ambient.mp3", Sound.class);
        ambient.loop(0.15f);
    }

    private void loadFromFile(String mapName) {
        try (BufferedReader br = new BufferedReader(new FileReader(mapName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");

                String type = parts[0];

                if(type.equals("wall")) {
                    if(parts.length < 8) {
                        System.err.println("ERROR malformed map wall read");
                    }
                    float ax = -Float.parseFloat(parts[1]);
                    float ay = Float.parseFloat(parts[2]);
                    float bx = -Float.parseFloat(parts[3]);
                    float by = Float.parseFloat(parts[4]);
                    float height = Float.parseFloat(parts[5]);
                    float textureID = Float.parseFloat(parts[6]);
                    boolean isDoor = Boolean.parseBoolean(parts[7]);
                    Wall wall = new Wall(ax, ay, bx, by, height, textureID, isDoor);
                    walls.add(wall);
                    if(parts.length == 9) { wall.yOffset = Float.parseFloat(parts[8]); }
                    else if(isDoor) {
                        doors.add(walls.size() - 1);
                    }
                } else if(type.equals("torch")) {
                    float x = -Float.parseFloat(parts[1]);
                    float y = Float.parseFloat(parts[2]);
                    float radius = Float.parseFloat(parts[3]);
                    torches.add(new Torch(x, y, radius));
                } else if(type.equals("region")) {
                    float minX = -Float.parseFloat(parts[1]);
                    float minY = Float.parseFloat(parts[2]);
                    float maxX = -Float.parseFloat(parts[3]);
                    float maxY = Float.parseFloat(parts[4]);
                    RegionIndexCuller.Region region = new RegionIndexCuller.Region(minX, minY, maxX, maxY);
                    for (int i = 5; i < parts.length; i++) {
                        region.indices.add(Integer.parseInt(parts[i]));
                    }
                    torchRegionIndexCuller.regions.add(region);
                } else if(type.equals("nav")) {
                    float ax = -Float.parseFloat(parts[1]);
                    float ay = Float.parseFloat(parts[2]);
                    float bx = -Float.parseFloat(parts[3]);
                    float by = Float.parseFloat(parts[4]);
                    float cx = -Float.parseFloat(parts[5]);
                    float cy = Float.parseFloat(parts[6]);
                    navMesh.addTriangle(new Triangle(ax, ay, bx, by, cx, cy));
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

        player = new Player(new HalberdWeapon());
        player.setRangedWeapon(new CrossbowWeapon());

        items.add(new ItemPickup(1.0f, 1.0f, (new DamagePowerup())));
        items.add(new ItemPickup(-1.0f, -1.0f, (new HealthPowerup())));
        items.add(new ItemPickup(0.0f, 0.0f, (new DamageResistPowerup())));
        items.add(new ItemPickup(-1.0f, 1.0f, (new AttackSpeedPowerup())));

        enemies.add(new Goblin(0.0f, 0.0f));
        enemies.add(new Slime(1.0f, 0.0f));
    }
}
