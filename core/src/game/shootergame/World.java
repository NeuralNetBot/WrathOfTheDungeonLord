package game.shootergame;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;

import game.shootergame.Enemy.DungeonLord;
import game.shootergame.Enemy.Enemy;
import game.shootergame.Enemy.Goblin;
import game.shootergame.Enemy.GoblinCrossbow;
import game.shootergame.Enemy.NavMesh;
import game.shootergame.Enemy.NavMesh.Triangle;
import game.shootergame.Enemy.Slime;
import game.shootergame.Item.ItemPickup;
import game.shootergame.Item.MeleeWeapons.BrassKnucklesWeapon;
import game.shootergame.Item.MeleeWeapons.HalberdWeapon;
import game.shootergame.Item.MeleeWeapons.MaceWeapon;
import game.shootergame.Item.MeleeWeapons.NullWeapon;
import game.shootergame.Item.MeleeWeapons.SwordWeapon;
import game.shootergame.Item.Powerups.AttackSpeedPowerup;
import game.shootergame.Item.Powerups.DamagePowerup;
import game.shootergame.Item.Powerups.DamageResistPowerup;
import game.shootergame.Item.Powerups.HealthPowerup;
import game.shootergame.Item.Powerups.MusketAmmoPowerup;
import game.shootergame.Item.Powerups.CrossbowAmmoPowerup;
import game.shootergame.Item.RangedWeapons.CrossbowWeapon;
import game.shootergame.Item.RangedWeapons.MusketWeapon;
import game.shootergame.Network.Client;
import game.shootergame.Network.RemotePlayer;
import game.shootergame.Network.Server;
import game.shootergame.Physics.Collider;
import game.shootergame.Physics.PhysicsWorld;
import game.shootergame.Renderer.RegionIndexCuller;
import game.shootergame.Renderer.Renderer;
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

    ConcurrentHashMap<Integer, ItemPickup> items;

    ConcurrentHashMap<Integer, Enemy> enemies;
    int pathTickIndex = 0;

    NavMesh navMesh;

    ConcurrentHashMap<Integer, RemotePlayer> remotePlayers;

    Server server;
    Client client;
    Sound ambient;

    MainMenu mainMenu;

    enum NetworkMode {
        SERVER,
        CLIENT;
    }
    NetworkMode networkMode;

    boolean onBossMap = false;

    public static void createInstance() {
        instance = new World();
        instance.init();
    }

    public static Collider getPlayerCollider() {
        return instance.player.getCollider();
    }

    public static void processInput() {
        if(instance.itemPrompt != null) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                instance.itemPrompt.addAndRemoveFromWorld();
                int key = 0;
                for (Entry<Integer, ItemPickup> entry : instance.items.entrySet()) {
                    if(entry.getValue() == instance.itemPrompt) {
                        key = entry.getKey();
                        break;
                    }
                }
                if(instance.networkMode == NetworkMode.SERVER) {
                    instance.server.broadcastNewItem(key, 0, 0, false, null, null, null);
                } else if(instance.networkMode == NetworkMode.CLIENT) {
                    instance.client.handleItemInteract(key);
                }
            }
        }

        instance.player.processInput();
    }

    public static void startAsServer() {
        instance.server = new Server(instance.remotePlayers, instance.items, instance.enemies);
        new Thread(instance.server).start();
        instance.networkMode = NetworkMode.SERVER;
    }

    public static void startAsClient() {
        instance.client = new Client(instance.mainMenu.getIP(), instance.mainMenu.getPort(), instance.remotePlayers, instance.items, instance.enemies);
        instance.networkMode = NetworkMode.CLIENT;
    }

    public static void startMainMenu(MainMenu menu) {
        instance.mainMenu = menu;
    }

    private void serverLoadNewMap(String map) {
        instance.server.broadcastLoadMap(map);
                
        World.loadFromFile(map);
        Renderer.inst().buildLightmap(World.getTorches());
        Renderer.inst().setTorchRegionIndexCuller(World.getTorchRegionIndexCuller());

        for (Entry<Integer, Enemy> entry : instance.enemies.entrySet()) {
            float x = entry.getValue().getX();
            float y = entry.getValue().getY();
            instance.server.broadcastNewEnemy(entry.getKey(), x, y, true, entry.getValue().getName());
        }
        for (Entry<Integer, ItemPickup> entry : instance.items.entrySet()) {
            String payload = null;
            String subtype = null;
            if(entry.getValue().getPayloadType() == ItemPickup.Payload.POWERUP) {
                String name = entry.getValue().getPowerup().getName();
                payload = "powerup";
                if(name.equals(AttackSpeedPowerup.getSName())) {
                    subtype = "attackspeed";
                } else if(name.equals(DamagePowerup.getSName())) {
                    subtype = "damage";
                } else if(name.equals(HealthPowerup.getSName())) {
                    subtype = "health";
                } else if(name.equals(DamageResistPowerup.getSName())) {
                    subtype = "resist";
                } else {
                    subtype = "";
                }
            } else if(entry.getValue().getPayloadType() == ItemPickup.Payload.RANGED_WEAPON) {
                String name = entry.getValue().getRangedWeapon().getName();
                payload = "weapon";
                if(name.equals("Crossbow")) {
                    subtype = "crossbow";
                } else if(name.equals("Musket")) {
                    subtype = "musket";
                } else {
                    subtype = "";
                }
            }
            float x = entry.getValue().collider.x;
            float y = entry.getValue().collider.y;
            instance.server.broadcastNewItem(entry.getKey(), x, y, true, payload, subtype, null);
        }

        instance.server.broadcastReadyPlay();
    }

    float wx = 0.0f;
    
    boolean doOnce = true;
    boolean doOnceLaunch = true;
    public static void update(float delta) {

        if(instance.mainMenu.isDone() && instance.doOnce) {
            if(instance.mainMenu.getSelectedMode()) {
                World.startAsServer();
            } else {
                World.startAsClient();
            }
            instance.mainMenu.setRemotePlayers(instance.remotePlayers);
            instance.doOnce = false;
        }
        if(instance.mainMenu.shouldRunGame() && instance.doOnceLaunch) {
            switch (instance.mainMenu.getSelectedWeapon()) {
                case 0: World.getPlayer().setMeleeWeapon(new SwordWeapon()); break;
                case 1: World.getPlayer().setMeleeWeapon(new HalberdWeapon()); break;
                case 2: World.getPlayer().setMeleeWeapon(new MaceWeapon()); break;
                case 3: World.getPlayer().setMeleeWeapon(new BrassKnucklesWeapon()); break;
            }
            if(instance.networkMode == NetworkMode.SERVER) {
                instance.serverLoadNewMap("assets/map0.data");
            } else if(instance.networkMode == NetworkMode.CLIENT) {
                //nothing happens until client recieves packets from server
            }


            instance.doOnceLaunch = false;
        }
        if(instance.networkMode == NetworkMode.SERVER) {
            Server.RemoveItemHandler itemHandlertem;
            while((itemHandlertem = instance.server.getRemoveItemQueue().poll()) != null) {
                itemHandlertem.callback();
            }
        }
        else if(instance.networkMode == NetworkMode.CLIENT) {
            if(instance.client.hasMapLoad()) {
                World.loadFromFile(instance.client.getMapName());
                Renderer.inst().buildLightmap(World.getTorches());
                Renderer.inst().setTorchRegionIndexCuller(World.getTorchRegionIndexCuller());
            }
            if(instance.client.isReadyToPlay()) {
                instance.mainMenu.setClientConnected(true);
            }

            Client.NewItemHandler newItem;
            while((newItem = instance.client.getNewItemQueue().poll()) != null) {
                newItem.callback();
            }
        }

        if(!instance.mainMenu.shouldRunGame()) return;

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
            if(instance.networkMode == NetworkMode.SERVER) {
                float recentDamage = entry.getValue().GetRecentDamage();
                if(recentDamage != 0.0f) {
                    instance.server.sendClientRecieveDamage(entry.getKey(), recentDamage);
                }
            }
        }

        Iterator<Map.Entry<Integer,Enemy>> it = instance.enemies.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<Integer,Enemy> enemy = it.next();
            enemy.getValue().update(delta);
            if(index == instance.pathTickIndex)
            enemy.getValue().tickPathing();
            index++;

            if(instance.networkMode == NetworkMode.CLIENT) {
                float recentDamage = enemy.getValue().getRecentDamage();
                if(recentDamage != 0) {
                    instance.client.sendPlayerAttacks(enemy.getKey(), recentDamage);
                }
            }

            if(!enemy.getValue().isAlive()) {
                if(instance.networkMode == NetworkMode.SERVER) {
                    instance.server.broadcastNewEnemy(enemy.getKey(), 0, 0, false, null);
                }
                enemy.getValue().onKill();
                it.remove();
            }
        }

        instance.pathTickIndex++;
        if(instance.pathTickIndex >= instance.enemies.size()) {
            instance.pathTickIndex = 0;
        }

        Renderer.inst().update(instance.player.x(), instance.player.y(), instance.player.rotation(), delta);

        if(instance.networkMode == NetworkMode.SERVER) {
            if(instance.enemies.size() == 0 && !instance.onBossMap) {
                instance.serverLoadNewMap("assets/map1.data");
                instance.onBossMap = true;
            }
        }
    }

    public static void render() {
        if(instance.mainMenu.shouldRunGame()) {
            instance.player.render();
        } else {
            if(instance.networkMode == NetworkMode.SERVER) {
                instance.mainMenu.setIP(instance.server.getIP());
                instance.mainMenu.setPort(instance.server.getPort());
            }
            instance.mainMenu.update();
        }
    }

    public static void renderHud() {
        if(!instance.mainMenu.shouldRunGame()) {
            instance.mainMenu.updateText();
            return;
        }

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

    public static ConcurrentHashMap<Integer, RemotePlayer> getRemotePlayers() {
        return instance.remotePlayers;
    }

    private World() {
        walls = new ArrayList<>();
        torches = new ArrayList<>();
        torchRegionIndexCuller = new RegionIndexCuller();
        doors = new ArrayList<>();
        items = new ConcurrentHashMap<>();
        enemies = new ConcurrentHashMap<>();
        navMesh = new NavMesh();
        remotePlayers = new ConcurrentHashMap<>();

        RemotePlayer.initTextures();

        ShooterGame.getInstance().am.load("dungeon_ambient.mp3", Sound.class);
        ShooterGame.getInstance().am.finishLoading();
        ambient = ShooterGame.getInstance().am.get("dungeon_ambient.mp3", Sound.class);
        ambient.loop(0.15f);
    }

    public static void loadFromFile(String mapName) {

        instance.player.x = 0.0f;
        instance.player.y = 0.0f;

        instance.walls.clear();
        instance.torches.clear();

        if(instance.torchRegionIndexCuller != null)
            instance.torchRegionIndexCuller = new RegionIndexCuller();

        instance.doors.clear();

        for (Entry<Integer, ItemPickup> item : instance.items.entrySet()) {
            item.getValue().removeFromWorld();
        }
        instance.items.clear();
    
        for (Entry<Integer, Enemy> enemy : instance.enemies.entrySet()) {
            enemy.getValue().onKill();
        }
        instance.enemies.clear();

        if(instance.navMesh != null)
            instance.navMesh = new NavMesh();

        Reader file = null;
        try {
            file = new FileReader(mapName);
        } catch (FileNotFoundException e) {
            try {
                file = new FileReader(mapName.split("/")[1]);
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }
        }
        try (BufferedReader br = new BufferedReader(file)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");

                String type = parts[0];

                if(type.equals("height")) {
                    float h = Float.parseFloat(parts[1]);
                    Renderer.inst().setCeilHeight(h);
                } else if(type.equals("ambient")) {
                    float a = Float.parseFloat(parts[1]);
                    Renderer.inst().setAmbient(a);
                } else if(type.equals("wall")) {
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
                    instance.walls.add(wall);
                    if(parts.length == 9) { wall.yOffset = Float.parseFloat(parts[8]); }
                    else if(isDoor) {
                        instance.doors.add(instance.walls.size() - 1);
                    }
                } else if(type.equals("torch")) {
                    float x = -Float.parseFloat(parts[1]);
                    float y = Float.parseFloat(parts[2]);
                    float radius = Float.parseFloat(parts[3]);
                    instance.torches.add(new Torch(x, y, radius));
                } else if(type.equals("region")) {
                    float minX = -Float.parseFloat(parts[1]);
                    float minY = Float.parseFloat(parts[2]);
                    float maxX = -Float.parseFloat(parts[3]);
                    float maxY = Float.parseFloat(parts[4]);
                    RegionIndexCuller.Region region = new RegionIndexCuller.Region(minX, minY, maxX, maxY);
                    for (int i = 5; i < parts.length; i++) {
                        region.indices.add(Integer.parseInt(parts[i]));
                    }
                    instance.torchRegionIndexCuller.regions.add(region);
                } else if(type.equals("nav") && instance.networkMode == NetworkMode.SERVER) {
                    float ax = -Float.parseFloat(parts[1]);
                    float ay = Float.parseFloat(parts[2]);
                    float bx = -Float.parseFloat(parts[3]);
                    float by = Float.parseFloat(parts[4]);
                    float cx = -Float.parseFloat(parts[5]);
                    float cy = Float.parseFloat(parts[6]);
                    instance.navMesh.addTriangle(new Triangle(ax, ay, bx, by, cx, cy));
                } else if(type.equals("slime") && instance.networkMode == NetworkMode.SERVER) {
                    float x = -Float.parseFloat(parts[1]);
                    float y = Float.parseFloat(parts[2]);
                    int id = ThreadLocalRandom.current().nextInt();
                    instance.enemies.put(id, new Slime(x, y, false));
                } else if(type.equals("goblin") && instance.networkMode == NetworkMode.SERVER) {
                    float x = -Float.parseFloat(parts[1]);
                    float y = Float.parseFloat(parts[2]);
                    int id = ThreadLocalRandom.current().nextInt();
                    instance.enemies.put(id, new Goblin(x, y, false));
                } else if(type.equals("goblincrossbow") && instance.networkMode == NetworkMode.SERVER) {
                    float x = -Float.parseFloat(parts[1]);
                    float y = Float.parseFloat(parts[2]);
                    int id = ThreadLocalRandom.current().nextInt();
                    instance.enemies.put(id, new GoblinCrossbow(x, y, false));
                } else if(type.equals("boss") && instance.networkMode == NetworkMode.SERVER) {
                    float x = -Float.parseFloat(parts[1]);
                    float y = Float.parseFloat(parts[2]);
                    int id = ThreadLocalRandom.current().nextInt();
                    instance.enemies.put(id, new DungeonLord(x, y, false));
                } else if(type.equals("powerup") && instance.networkMode == NetworkMode.SERVER) {
                    float x = -Float.parseFloat(parts[1]);
                    float y = Float.parseFloat(parts[2]);
                    int id = ThreadLocalRandom.current().nextInt();
                    if(parts[3].equals("damage")) {
                        instance.items.put(id, new ItemPickup(x, y, 1.0f, new DamagePowerup()));
                    } else if(parts[3].equals("health")) {
                        instance.items.put(id, new ItemPickup(x, y, 1.0f, new HealthPowerup()));
                    } else if(parts[3].equals("resist")) {
                        instance.items.put(id, new ItemPickup(x, y, 1.0f, new DamageResistPowerup()));
                    } else if(parts[3].equals("attackspeed")) {
                        instance.items.put(id, new ItemPickup(x, y, 1.0f, new AttackSpeedPowerup()));
                    }
                } else if(type.equals("weapon") && instance.networkMode == NetworkMode.SERVER) {
                    float x = -Float.parseFloat(parts[1]);
                    float y = Float.parseFloat(parts[2]);
                    int id = ThreadLocalRandom.current().nextInt();

                    if(parts[3].equals("crossbow")) {
                        instance.items.put(id, new ItemPickup(x, y, 2.0f, new CrossbowWeapon()));
                    } else if(parts[3].equals("musket")) {
                        instance.items.put(id, new ItemPickup(x, y, 2.0f, new MusketWeapon()));
                    } else if(parts[3].equals("crossbowammo")) {
                        instance.items.put(id, new ItemPickup(x, y, 1.0f, new CrossbowAmmoPowerup()));
                    } else if(parts[3].equals("musketammo")) {
                        instance.items.put(id, new ItemPickup(x, y, 1.0f, new MusketAmmoPowerup()));
                    }
                }

            }
            System.out.println("Map: '" + mapName + "' loaded");
            System.out.println((instance.walls.size() - instance.doors.size()) + " walls");
            System.out.println(instance.doors.size() + " doors");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void init() {
        physicsWorld = new PhysicsWorld();

        physicsWorld.setWalls(walls);
        itemPrompt = null;

        player = new Player(new NullWeapon());
        player.setRangedWeapon(new MusketWeapon());
    }

    public static boolean killAll() {
        if(instance.networkMode != NetworkMode.SERVER) return false;
        for (Entry<Integer, Enemy> enemy : instance.enemies.entrySet()) {
            enemy.getValue().doDamage(Float.MAX_VALUE, 0);
        }
        return true;
    }
}
