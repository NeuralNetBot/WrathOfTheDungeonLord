package game.shootergame.Network;

import game.shootergame.World;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client implements Runnable {

    private final String hostName = "localhost";
    private final int portNumber = 42069;

    public Client () {

    }

    @Override
    public void run() {
        try (Socket socket = new Socket(hostName, portNumber)) {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            while (true) {
                out.writeFloat(World.getPlayer().x());
                out.writeFloat(World.getPlayer().y());
                out.writeFloat(World.getPlayer().dx());
                out.writeFloat(World.getPlayer().dy());
                out.flush();
            }
        }
        catch (IOException e) {

        }
    }
}
