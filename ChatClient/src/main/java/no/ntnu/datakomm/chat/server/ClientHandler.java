package no.ntnu.datakomm.chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread {
    private final Socket clientSocket;
    private BufferedReader fromClient;
    private PrintWriter toClient;

    private String lastError;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            this.fromClient = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            this.toClient = new PrintWriter(this.clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            this.lastError = e.getMessage();
        }
        this.parseIncomingRequest();
    }

    /**
     * Takes the request from the client and response in the correct manner
     */
    private void parseIncomingRequest() {
        while (this.clientSocket.isConnected()) {
            String clientRequest = this.waitClientRequest();
            this.toClient.println("Got request: " + clientRequest);
        }
        this.closeSocket();
    }

    /**
     * Closes the socket with the client
     *
     * @return {@code true} if socket is closed, {@code false} otherwise
     */
    private boolean closeSocket() {
        boolean socketClosed = false;
        try {
            this.clientSocket.close();
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
            try {
                this.clientSocket.close();
                this.lastError = e.getMessage();
            } catch (IOException io) {
                this.lastError = "Could not close socket: " + io.getMessage();
            }
        }
        return clientRequest;
    }
}
