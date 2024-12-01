package game.shootergame.Network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

import game.shootergame.Item.ItemPickup;
import game.shootergame.Player;
import game.shootergame.World;

public class Server implements Runnable{

    private final int port = 42069;
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private ConcurrentHashMap<Integer, RemotePlayer> remotePlayers;
    private ConcurrentHashMap<Integer, ItemPickup> items;

    private class ClientHandler {

        private Socket socket;
        private RemotePlayer remotePlayer;
        private int remotePlayerID;
        private ConcurrentLinkedQueue<ByteBuffer> outputQueue = new ConcurrentLinkedQueue<>();

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                onConnect(in, out);

                Thread inputThread = new Thread(new InputHandler(in, this));
                Thread outputThread = new Thread(new OutputHandler(out));
                inputThread.start();
                outputThread.start();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void onConnect(DataInputStream in, DataOutputStream out) {

            handleNewPlayer(0, true, out);//server player id
            for (Entry<Integer, RemotePlayer> entry : remotePlayers.entrySet()) {
                handleNewPlayer(entry.getKey(), true, out);
            }

            remotePlayer = new RemotePlayer();
            remotePlayerID = ThreadLocalRandom.current().nextInt();
            remotePlayers.put(remotePlayerID, remotePlayer);
            System.out.println("Added player ID: " + remotePlayerID);

            for (ClientHandler clientHandler : clients) {
                if(clientHandler == this) continue;
                clientHandler.handleNewPlayer(remotePlayerID, true, out);
            }

        }

        public void handleNewPlayer(int id, boolean add, DataOutputStream out) {
            System.out.println("writing " + id + " to new client");
            ByteBuffer buffer = ByteBuffer.allocate(6);
            buffer.put(PacketInfo.getByte(PacketInfo.NEW_PLAYER));
            buffer.put(add ? (byte)0x01 : (byte)0x00);
            buffer.putInt(id);
            try {
                out.write(buffer.array());
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendBytes(ByteBuffer bytes) {
            outputQueue.add(bytes);
        }

        private class InputHandler implements Runnable {
            private final DataInputStream in;
            private final ClientHandler client;
            public InputHandler(DataInputStream in, ClientHandler client) {
                this.in = in;
                this.client = client;
            }

            @Override
            public void run() {
                try {
                    while (true) {
                        byte[] buf = new byte[1024];
                        int amount = in.read(buf);
                        if(amount <= 0) continue;

                        ByteBuffer buffer = ByteBuffer.wrap(buf, 0, amount);
                        while(buffer.hasRemaining()) {
                            switch (PacketInfo.getType(buffer.get())) {
                            case PLAYER_POSITION:
                                float x = buffer.getFloat();
                                float y = buffer.getFloat();
                                float dx = buffer.getFloat();
                                float dy = buffer.getFloat();
                                float rotation = buffer.getFloat();
                                remotePlayer.updateNetwork(x, y, dx, dy, rotation);
                            case NEW_PLAYER:      break;
                            case ENEMY_UPDATE:    break;
                            case NEW_ENEMY:       break;
                            case NEW_ITEM:        break;
                            case PLAYER_UPDATE:   break;
                            case PLAYER_ATTACK:   break;
                            case ITEM_INTERACT:   break;
                            default: break;
                            }
                        }
                        
                    }
                }
                catch (IOException e) {
                    System.out.println("Client Disconnected" + e.getMessage());
                }
                finally {
                    try {
                        socket.close();
                    }
                    catch (IOException e) {
                        System.out.println("Cannot Close Socket" + e.getMessage());
                    }
                    clients.remove(client);
                    remotePlayers.remove(remotePlayerID);
                    System.out.println("Removed player ID: " + remotePlayerID);
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
                while(true) {

                    try {
                        Thread.sleep(16);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    ByteBuffer itemBuffer;
                    while((itemBuffer = outputQueue.poll()) != null) {
                        try {
                            out.write(itemBuffer.array());
                            out.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    ByteBuffer buffer = ByteBuffer.allocate(2 + (remotePlayers.size()+1) *24);
                    buffer.put(PacketInfo.getByte(PacketInfo.PLAYER_POSITION));
                    buffer.put((byte)(remotePlayers.size() + 1));

                    buffer.putInt(0);//server is always player ID 0
                    buffer.putFloat(World.getPlayer().x());
                    buffer.putFloat(World.getPlayer().y());
                    buffer.putFloat(World.getPlayer().dx());
                    buffer.putFloat(World.getPlayer().dy());
                    buffer.putFloat(World.getPlayer().rotation());

                    for (Entry<Integer, RemotePlayer> entry : remotePlayers.entrySet()) {
                        buffer.putInt(entry.getKey());
                        buffer.putFloat(entry.getValue().x);
                        buffer.putFloat(entry.getValue().y);
                        buffer.putFloat(entry.getValue().dx);
                        buffer.putFloat(entry.getValue().dy);
                        buffer.putFloat(entry.getValue().rotation);
                    }
                    try {
                        out.write(buffer.array());
                        out.flush();
                    } catch (IOException e) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                        System.out.println(e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            
        }

    }

    public Server (ConcurrentHashMap<Integer, RemotePlayer> remotePlayers) {
        this.remotePlayers = remotePlayers;
    }

    public void broadcastLoadMap(String mapPath) {
        byte[] strBytes;
        try {
            strBytes = mapPath.getBytes("UTF-8");
            ByteBuffer buffer = ByteBuffer.allocate(2 + strBytes.length);
            buffer.put(PacketInfo.getByte(PacketInfo.LOAD_MAP));
            buffer.put((byte)strBytes.length);
            buffer.put(strBytes);
            for (ClientHandler client : clients) {
                client.sendBytes(buffer);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void broadcastReadyPlay() {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.put(PacketInfo.getByte(PacketInfo.READY_PLAY));
        for (ClientHandler client : clients) {
            client.sendBytes(buffer);
        }
    }

    public void broadcastNewItem(int id, float x, float y, boolean add, String payload, String subtype, DataOutputStream out) {
        int payloadI = 0x00000000;
        int subtypeI = 0x00000000;
        if(payload.equals("weapon")) {
            payloadI = 0x00000002;
            if(subtype.equals("crossbow")) {
                subtypeI = 0x00000001;
            } else if(subtype.equals("musket")) {
                subtypeI = 0x00000002;
            }
        } else if(payload.equals("powerup")) {
            payloadI = 0x00000003;
            if(subtype.equals("damage")) {
                subtypeI = 0x00000002;
            } else if(subtype.equals("health")) {
                subtypeI = 0x00000004;
            } else if(subtype.equals("resist")) {
                subtypeI = 0x00000003;
            } else if(subtype.equals("attackspeed")) {
                subtypeI = 0x00000001;
            }
        }

        ByteBuffer buffer = ByteBuffer.allocate(22);
        buffer.put(PacketInfo.getByte(PacketInfo.NEW_ITEM));
        buffer.put(add ? (byte)0x01 : (byte)0x00);
        buffer.putInt(id);
        buffer.putFloat(x);
        buffer.putFloat(y);
        buffer.putInt(payloadI);
        buffer.putInt(subtypeI);
        for (ClientHandler client : clients) {
            client.sendBytes(buffer);
        }
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.getInetAddress();
            System.out.println("Server is listening on port: " + port + " IP: " + InetAddress.getLocalHost().getHostAddress());
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected");
                ClientHandler clientHandler = new ClientHandler(socket);
                clients.add(clientHandler);
            }
        }

        catch (IOException e) {

        }
    }
}
