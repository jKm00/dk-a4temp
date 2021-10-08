package no.ntnu.datakomm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread {
    private final Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        BufferedReader reader = null;
        PrintWriter writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            writer = new PrintWriter(this.clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        boolean run = true;

        while (run) {
            String clientMessage = this.getClientMessage(reader);

            if (clientMessage != null) {
                if (clientMessage.equalsIgnoreCase("game over")) {
                    this.closeClientSocket();
                    run = false;
                } else {
                    String response = this.createResponse(clientMessage);
                    writer.println(response);
                }
            }
        }
    }

    private String createResponse(String clientMessage) {
        String response = "error";

        String[] parts = clientMessage.split("\\+");

        if (parts.length == 2) {
            try {
                int firstInt = Integer.parseInt(parts[0]);
                int secondInt = Integer.parseInt(parts[1]);
                response = "" + (firstInt + secondInt);
            } catch (NumberFormatException e) {
                response = "error";
            }
        }

        return response;
    }

    private void closeClientSocket() {
        try {
            this.clientSocket.close();
        } catch (IOException e) {
            System.out.println("Something went wrong when closing the client socket: " + e.getMessage());
        }
    }

    private String getClientMessage(BufferedReader reader) {
        String clientMessage = null;

        try {
            clientMessage = reader.readLine();
        } catch (IOException e) {
            System.out.println("Could not read from client");
        }

        return clientMessage;
    }
}
