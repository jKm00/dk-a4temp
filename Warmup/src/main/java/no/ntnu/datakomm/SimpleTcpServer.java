package no.ntnu.datakomm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;

/**
 * A Simple TCP server, used as a warm-up exercise for assignment A4.
 */
public class SimpleTcpServer {

    private static final int PORT = 1301;

    //private ServerSocket welcomeSocket;

    public static void main(String[] args) {
        SimpleTcpServer server = new SimpleTcpServer();
        log("Simple TCP server starting");
        server.run();
        log("ERROR: the server should never go out of the run() method! After handling one client");
    }

    public void run() {
        // TODO - implement the logic of the server, according to the protocol.
        // Take a look at the tutorial to understand the basic blocks: creating a listening socket,
        // accepting the next client connection, sending and receiving messages and closing the connection
        try {
            ServerSocket welcomeSocket = new ServerSocket(PORT);

            boolean mustRun = true;

            while (mustRun) {
                Socket clientSocket = welcomeSocket.accept();

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a server socket with the PORT specified as a constant. If an error
     * occurred when trying create the server socket, an error message will be printed
     * out and {@code null} will be returned.
     *
     * @return the socket created
     */
    private ServerSocket createServerSocket() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            System.out.println("Could not create server socket: " + e.getMessage());
        }
        return serverSocket;
    }

    /**
     * Reads a request sent from a client. The request will be returned, unless an error
     * occurs while reading. If so {@code null} will be returned.
     *
     * @param reader the BufferedReader created when the socket was opened.
     * @return the request sent by the client, or {@code null} if an error occurred while
     * trying to read the request
     */
    private String readRequest(BufferedReader reader) {
        String request = null;

        try {
            request = reader.readLine();
        } catch (IOException e) {
            System.out.println("Could not read request: " + e.getMessage());
        }

        return request;
    }

    /**
     * Sends a message to the client.
     *
     * @param writer the PrintWriter created when the socket was opened.
     * @param response the response that the server is going to send to the client
     * @return {@code true} if the response was sent, {@code false} otherwise
     */
    private boolean sendResponse(Socket client, PrintWriter writer, String response) {
        boolean responseSent = false;

        if (response.equalsIgnoreCase("closing socket")) {
            try {
                writer.println(response);
                this.closeClientSocket(client);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }else {
            try {
                writer.println(response);
                responseSent = true;
            } catch (Exception e) {
                System.out.println("Could not send response: " + e.getMessage());
            }
        }

        return responseSent;
    }

    private String createResponse(String request) {
        String response = null;

        if (request.equalsIgnoreCase("game over")) {
            response = "Closing socket";
        } else {
            response = request.toUpperCase(Locale.ROOT);
        }

        return response;
    }

    private boolean closeClientSocket(Socket clientSocket) {
        boolean socketClosed = false;

        try {
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Could not close client socket: " + e.getMessage());
        }

        return socketClosed;
    }


    /**
     * Log a message to the system console.
     *
     * @param message The message to be logged (printed).
     */
    private static void log(String message) {
        System.out.println(message);
    }
}
