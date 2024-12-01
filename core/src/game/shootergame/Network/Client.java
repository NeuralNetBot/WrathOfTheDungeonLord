package game.shootergame.Network;

import game.shootergame.Item.ItemPickup;
import game.shootergame.Item.Powerups.AttackSpeedPowerup;
import game.shootergame.Item.Powerups.DamagePowerup;
import game.shootergame.Item.Powerups.DamageResistPowerup;
import game.shootergame.Item.Powerups.HealthPowerup;
import game.shootergame.Item.RangedWeapon;
import game.shootergame.World;
import org.w3c.dom.ranges.Range;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

public class Client {

    private final String hostName = "localhost";
    private final int portNumber = 42069;
    private Socket socket;
    private ConcurrentHashMap<Integer, RemotePlayer> remotePlayers;
    private ConcurrentHashMap<Integer, ItemPickup> items;

    public Client (ConcurrentHashMap<Integer, RemotePlayer> remotePlayers, ConcurrentHashMap<Integer, ItemPickup> items) {
        this.remotePlayers = remotePlayers;
        this.items = items;
        try {
            socket = new Socket(hostName, portNumber);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            Thread inputThread = new Thread(new InputHandler(in));
            Thread outputThread = new Thread(new OutputHandler(out));
            inputThread.start();
            outputThread.start();
        }
        catch (IOException e) {

        }
    }

    private class InputHandler implements Runnable {
        private final DataInputStream in;
        public InputHandler(DataInputStream in) {
            this.in = in;
        }

        void processPlayerPosition(ByteBuffer buffer) {
            byte amount = buffer.get();
            for (int i = 0; i < amount; i++) {
                int id = buffer.getInt();
                float x = buffer.getFloat();
                float y = buffer.getFloat();
                float dx = buffer.getFloat();
                float dy = buffer.getFloat();
                float rotation = buffer.getFloat();
                RemotePlayer thePlayer = remotePlayers.get(id);
                if(thePlayer == null) continue;
                thePlayer.x = x;
                thePlayer.y = y;
                thePlayer.dx = dx;
                thePlayer.dy = dy;
                thePlayer.rotation = rotation;
            }
        }

        void processNewPlayer(ByteBuffer buffer) {
            byte add = buffer.get();
            int ID = buffer.getInt();
            System.out.println("recieved new player " + ID);
            if(add == 0x01) {
                remotePlayers.put(ID, new RemotePlayer());
            } else if(add == 0x00) {
                remotePlayers.remove(ID);
            }
        }

        void processNewItem(ByteBuffer buffer) {
            byte add = buffer.get();
            int ID = buffer.getInt();
            float x = buffer.getFloat();
            float y = buffer.getFloat();
            int payload = buffer.getInt();
            int subtype = buffer.getInt();
            if (add == 0x01) {
                switch (payload) {
                    case 0x01:
                        //ItemPickup.Payload.NONE;
                        break;
                    case 0x02:
                        switch (subtype) {
                            case 0x00000001:
                                //CrossbowWeapon crossbow = new CrossbowWeapon();
                                //items.put(ID, new ItemPickup(x, y crossbow));
                                //break;
                            case 0x00000002:
                                //MusketWeapon musket = new MusketWeapon();
                                //items.put(ID, new ItemPickup(x, y, musket));
                                //break;
                        }
                        break;
                    case 0x03:
                        switch (subtype) {
                            case 0x00000001:
                                items.put(ID, new ItemPickup(x, y, 1.0f, (new AttackSpeedPowerup())));
                                break;
                            case 0x00000002:
                                items.put(ID, new ItemPickup(x, y, 1.0f, (new DamagePowerup())));
                                break;
                            case 0x00000003:
                                items.put(ID, new ItemPickup(x, y, 1.0f, (new DamageResistPowerup())));
                                break;
                            case 0x00000004:
                                items.put(ID, new ItemPickup(x, y, 1.0f, (new HealthPowerup())));
                                break;
                        }
                        break;
                }
            }

            else {
                items.remove(ID);
            }
        }

        @Override
        public void run() {
            try {
                while(true) {

                    byte[] buf = new byte[1024];
                    int amount = in.read(buf);
                    if(amount <= 0) continue;

                    ByteBuffer buffer = ByteBuffer.wrap(buf, 0, amount);
                    while(buffer.hasRemaining()) {
                        switch (PacketInfo.getType(buffer.get())) {
                        case PLAYER_POSITION:
                            processPlayerPosition(buffer); break;
                        case NEW_PLAYER:      
                            processNewPlayer(buffer); break;
                        case ENEMY_UPDATE:    break;
                        case NEW_ENEMY:       break;
                        case NEW_ITEM:
                            processNewItem(buffer); break;
                        case PLAYER_UPDATE:   break;
                        case PLAYER_ATTACK:   break;
                        case ITEM_INTERACT:   break;
                        default: break;
                        }
                    }

                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }

    }

    private class OutputHandler implements Runnable {
        private final DataOutputStream out;
        public OutputHandler(DataOutputStream out) {
            this.out = out;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    try {
                        Thread.sleep(16);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    ByteBuffer buffer = ByteBuffer.allocate(21);
                    buffer.put(PacketInfo.getByte(PacketInfo.PLAYER_POSITION));
                    buffer.putFloat(World.getPlayer().x());
                    buffer.putFloat(World.getPlayer().y());
                    buffer.putFloat(World.getPlayer().dx());
                    buffer.putFloat(World.getPlayer().dy());
                    buffer.putFloat(World.getPlayer().rotation());
                    out.write(buffer.array());
                    out.flush();
                }
            } catch (IOException e) {
                System.out.println("Client Disconnecting: " + e.getMessage());
            }
        }

        public void handleItemInteract(int id, DataOutputStream out) {
            System.out.println("writing " + id + " item interact");
            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.put(PacketInfo.getByte(PacketInfo.ITEM_INTERACT));
            buffer.putInt(id);
            try {
                out.write(buffer.array());
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
