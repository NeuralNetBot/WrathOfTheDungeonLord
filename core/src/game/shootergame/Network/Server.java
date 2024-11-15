package game.shootergame.Network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server implements Runnable{

    private final int port = 42069;
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<RemotePlayer> remotePlayers;

    private class ClientHandler implements Runnable {

        private Socket socket;
        private DataInputStream in;
        private DataOutputStream out;
        private RemotePlayer remotePlayer;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                this.in = new DataInputStream(socket.getInputStream());
                this.out = new DataOutputStream(socket.getOutputStream());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                remotePlayer = new RemotePlayer();
                remotePlayers.add(remotePlayer);
                while (true) {
                    float x = in.readFloat();
                    float y = in.readFloat();
                    float dx = in.readFloat();
                    float dy = in.readFloat();
                    remotePlayer.updateNetwork(x, y, dx, dy);
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
                clients.remove(this);
                remotePlayers.remove(remotePlayer);
            }
        }
    }

    public Server (CopyOnWriteArrayList<RemotePlayer> remotePlayers) {
        this.remotePlayers = remotePlayers;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected");
                ClientHandler clientHandler = new ClientHandler(socket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        }

        catch (IOException e) {

        }
    }
}
