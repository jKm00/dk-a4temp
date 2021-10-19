package no.ntnu.datakomm.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static final int PORT = 1300;

    private ServerSocket welcomeSocket;

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }

    public void run() {
        try {
            welcomeSocket = new ServerSocket(PORT);
            boolean mustRun = true;
            while (mustRun) {
                Socket clientSocket = welcomeSocket.accept();

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.start();
            }
            welcomeSocket.close();
        } catch (IOException e) {
            System.out.println("Error when creating welcome socket: " + e.getMessage());
        }
    }
}
