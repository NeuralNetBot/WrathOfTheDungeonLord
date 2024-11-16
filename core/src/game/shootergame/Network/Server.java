package game.shootergame.Network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Server implements Runnable{

    private final int port = 42069;
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private ConcurrentHashMap<Integer, RemotePlayer> remotePlayers;

    private class ClientHandler {

        private Socket socket;
        private RemotePlayer remotePlayer;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                Thread inputThread = new Thread(new InputHandler(in, this));
                Thread outputThread = new Thread(new OutputHandler(out));
                inputThread.start();
                outputThread.start();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
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
                    remotePlayer = new RemotePlayer();
                    remotePlayers.put(ThreadLocalRandom.current().nextInt(), remotePlayer);
                    while (true) {
                        ByteBuffer buffer = ByteBuffer.wrap(in.readAllBytes());
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
                    remotePlayers.remove(remotePlayer);
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
                ByteBuffer buffer = ByteBuffer.allocate(2 + (remotePlayers.size()+1) *24);
                buffer.put(PacketInfo.getByte(PacketInfo.PLAYER_POSITION));
                buffer.put((byte)remotePlayers.size());
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
        }

    }

    public Server (ConcurrentHashMap<Integer, RemotePlayer> remotePlayers) {
        this.remotePlayers = remotePlayers;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port: " + port + " IP: " + serverSocket.getInetAddress().getHostAddress());
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
