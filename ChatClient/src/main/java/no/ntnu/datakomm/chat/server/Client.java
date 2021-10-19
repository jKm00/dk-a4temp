package no.ntnu.datakomm.chat.server;

import java.net.Socket;

public class Client {
    private String username;
    private Socket clientSocket;

    public Client(String username, Socket clientSocket) {
        this.setUsername(username);
        this.clientSocket = clientSocket;
    }

    public String getUsername() {
        return this.username;
    }

    public Socket getClientSocket() {
        return this.clientSocket;
    }

    public void setUsername(String username) {
        if (username.isEmpty()) {
            username = null;
        }
        if (username == null) {
            throw new IllegalArgumentException("Username is null or empty");
        }
        this.username = username;
    }
}
