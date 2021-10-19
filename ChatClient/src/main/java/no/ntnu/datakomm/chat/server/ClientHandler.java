package no.ntnu.datakomm.chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread {
    private final ClientRegister register;
    private final Client client;
    private BufferedReader fromClient;
    private PrintWriter toClient;

    private String lastError;

    public ClientHandler(Client client, ClientRegister register) {
        this.client = client;
        this.register = register;
    }

    @Override
    public void run() {
        try {
            this.fromClient = new BufferedReader(new InputStreamReader(this.client.getClientSocket().getInputStream()));
            this.toClient = new PrintWriter(this.client.getClientSocket().getOutputStream(), true);
        } catch (IOException e) {
            this.lastError = e.getMessage();
        }
        this.parseIncomingRequest();
    }

    /**
     * Takes the request from the client and response in the correct manner
     */
    private void parseIncomingRequest() {
        while (this.client.getClientSocket().isConnected()) {
            String clientRequest = this.waitClientRequest();
            if (clientRequest != null) {
                String[] request = clientRequest.split(" ", 3);
                switch (request[0]) {
                    case "msg":
                        //this.sendPublicMessage(request[1], request[2]);
                        break;
                    case "users":
                        this.sendUserList();
                        break;
                    case "login":
                        if (this.tryLogin(request[1])) {
                            this.sendCommand("loginok");
                        } else {
                            this.sendCommand("loginerr username already in use");
                        }
                        break;
                    case "privmsg":
                        this.sendPrivateMessage(request[1], request[2]);
                        break;
                    default:
                        System.out.println("Request unrecognisable: " + request[0]);
                }
            }
        }
        this.closeSocket();
    }

    private void sendPrivateMessage(String receiver, String message) {
        if (!this.register.contains(receiver)) {
            this.sendCommand("msgerr incorrect recipient " + receiver);
        } else {
            Client receiverClient = this.register.getClientByName(receiver);
            try {
                PrintWriter toClient = new PrintWriter(receiverClient.getClientSocket().getOutputStream(), true);
                toClient.println("privmsg " + this.client.getUsername() + " " + message);
                this.sendCommand("msgok 1");
            } catch (IOException e) {
                this.sendCommand("msgerr message not sent");
            }
        }
    }

    /**
     * Sends a command to the client.
     *
     * @param command the command to be sent.
     * @return {@code true} if command was sent, {@code false} otherwise
     */
    private boolean sendCommand(String command) {
        boolean commandSent = false;
        if (this.client.getClientSocket().isConnected()) {
            this.toClient.println(command);
            commandSent = true;
        }
        return commandSent;
    }

    /**
     * Tries to login a user with the give username. If the username
     * is already in use the login will be canceled.
     *
     * @param username the username the clients wants to login as.
     */
    private boolean tryLogin(String username) {
        boolean loggedIn = false;
        if (!this.register.contains(username)) {
            this.register.removeClient(this.client.getUsername());
            this.client.setUsername(username);
            this.register.addClient(this.client);
            loggedIn = true;
        }
        return loggedIn;
    }

    /**
     * Sends a list of all users logged into the server as a string
     * formatted: users "username1" "username2" "username3" ...
     */
    private void sendUserList() {
        StringBuffer sb = new StringBuffer();
        sb.append("users");
        for (Client client : this.register.getClients().values()) {
            sb.append(" ");
            sb.append(client.getUsername());
        }
        this.toClient.println(sb.toString());
    }

    private void sendPublicMessage(String message1, String message2) {
        for (Client client : this.register.getClients().values()) {
            try {
                PrintWriter toClient = new PrintWriter(client.getClientSocket().getOutputStream(), true);
                toClient.println("msg " + this.client.getUsername() + " " + message1 + " " + message2);
            } catch (IOException e) {
                this.lastError = e.getMessage();
            }
        }
    }

    /**
     * Closes the socket with the client
     *
     * @return {@code true} if socket is closed, {@code false} otherwise
     */
    private boolean closeSocket() {
        boolean socketClosed = false;
        try {
            this.client.getClientSocket().close();
            this.register.removeClient(this.client.getUsername());
            socketClosed = true;
        } catch (IOException e) {
            this.lastError = e.getMessage();
        }
        return socketClosed;
    }

    /**
     * Waits for a request from the client and reads it.
     *
     * @return a string with the request from the client
     */
    private String waitClientRequest() {
        String clientRequest = null;
        try {
            clientRequest = this.fromClient.readLine();
        } catch (IOException e) {
            this.closeSocket();
            this.lastError = e.getMessage();
        }
        return clientRequest;
    }
}
