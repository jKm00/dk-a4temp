package no.ntnu.datakomm.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {

    private static final int PORT = 1300;

    private ServerSocket welcomeSocket;
    private ClientRegister register;

    private int anonymousUserIndex = 0;

    public Server() {
        this.register = new ClientRegister();
    }

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

                Client client = new Client(this.createUsername(), clientSocket);
                if (this.register.addClient(client)) {
                    ClientHandler clientHandler = new ClientHandler(client, this.register);
                    clientHandler.start();
                }
            }
            welcomeSocket.close();
        } catch (IOException e) {
            System.out.println("Error when creating welcome socket: " + e.getMessage());
        }
    }

    /**
     * Creates a username for an anonymous user
     *
     * @return a string with the username
     */
    private String createUsername() {
        String name = "Anonymous" + this.anonymousUserIndex;
        this.anonymousUserIndex++;
        return name;
    }
}
