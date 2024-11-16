package game.shootergame.Network;

import game.shootergame.World;

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

    public Client (ConcurrentHashMap<Integer, RemotePlayer> remotePlayers) {
        this.remotePlayers = remotePlayers;
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
                thePlayer.x = x;
                thePlayer.y = y;
                thePlayer.dx = dx;
                thePlayer.dy = dy;
                thePlayer.rotation = rotation;
            }
        }

        void processNewPlayer(ByteBuffer buffer) {

        }

        @Override
        public void run() {
            try {
                while(true) {
                    System.out.println("waiting for bytes");
                    ByteBuffer buffer = ByteBuffer.wrap(in.readAllBytes());
                    System.out.println("bytes received");
                    while(buffer.hasRemaining()) {
                        switch (PacketInfo.getType(buffer.get())) {
                        case PLAYER_POSITION:
                            processPlayerPosition(buffer); break;
                        case NEW_PLAYER:      
                            processNewPlayer(buffer); break;
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
            } catch (IOException e) {
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
        
    }
}
