package game.shootergame.Network;

import game.shootergame.World;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client {

    private final String hostName = "localhost";
    private final int portNumber = 42069;
    private Socket socket;

    public Client () {
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

        @Override
        public void run() {
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
                    out.writeFloat(World.getPlayer().x());
                    out.writeFloat(World.getPlayer().y());
                    out.writeFloat(World.getPlayer().dx());
                    out.writeFloat(World.getPlayer().dy());
                    out.flush();
                }
            } catch (IOException e) {
                System.out.println("Client Disconnecting: " + e.getMessage());
            }
        }
        
    }
}
