package no.ntnu.datakomm.chat;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class TCPClient {
    private PrintWriter toServer;
    private BufferedReader fromServer;
    private Socket connection;

    private static final String LOGIN_ERROR = "loginerr";
    private static final String LOGIN_SUCCESS = "loginok";
    private static final String PUBLIC_MESSAGE = "msg";
    private static final String PRIVATE_MESSAGE = "privmsg";
    private static final String USERS = "users";
    private static final String MSGERROR = "msgerr";
    private static final String CMDERROR = "cmderr";
    private static final String SUPPORTED_CMD = "supported";
    private static final String JOKE = "joke";

    // Hint: if you want to store a message for the last error, store it here
    private String lastError = null;

    private final List<ChatListener> listeners = new LinkedList<>();

    /**
     * Connect to a chat server.
     *
     * @param host host name or IP address of the chat server
     * @param port TCP port of the chat server
     * @return True on success, false otherwise
     */
    public boolean connect(String host, int port) {
        // TODO Step 1: implement this method
        // Hint: Remember to process all exceptions and return false on error
        // Hint: Remember to set up all the necessary input/output stream variables
        boolean connected = false;
        try {
            this.connection = new Socket(host, port);
            this.toServer = new PrintWriter(this.connection.getOutputStream(), true);
            this.fromServer = new BufferedReader(new InputStreamReader(this.connection.getInputStream()));
            connected = true;
        } catch (IOException e) {
            lastError = "Error when establishing connection: " + e.getMessage();
        }
        return connected;
    }

    /**
     * Close the socket. This method must be synchronized, because several
     * threads may try to call it. For example: When "Disconnect" button is
     * pressed in the GUI thread, the connection will get closed. Meanwhile, the
     * background thread trying to read server's response will get error in the
     * input stream and may try to call this method when the socket is already
     * in the process of being closed. with "synchronized" keyword we make sure
     * that no two threads call this method in parallel.
     */
    public synchronized void disconnect() {
        // TODO Step 4: implement this method
        // Hint: remember to check if connection is active
        if (this.isConnectionActive()) {
            // Close connection
            try {
                this.connection.close();
                // Reset fields
                this.connection = null;
                this.fromServer = null;
                this.toServer = null;

                this.onDisconnect();
            } catch (IOException e) {
                this.lastError = e.getMessage();
            }
        }
    }

    /**
     * @return true if the connection is active (opened), false if not.
     */
    public boolean isConnectionActive() {
        return connection != null;
    }

    /**
     * Send a command to server.
     *
     * @param cmd A command. It should include the command word and optional attributes, according to the protocol.
     * @return true on success, false otherwise
     */
    private boolean sendCommand(String cmd) {
        // TODO Step 2: Implement this method
        // Hint: Remember to check if connection is active
        boolean commandSent = false;
        if (this.isConnectionActive()) {
            this.toServer.println(cmd);
            commandSent = true;
        }
        return commandSent;
    }

    /**
     * Send a public message to all the recipients.
     *
     * @param message Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPublicMessage(String message) {
        // TODO Step 2: implement this method
        // Hint: Reuse sendCommand() method
        // Hint: update lastError if you want to store the reason for the error.
        boolean msgSent = false;
        if (this.sendCommand("msg " + message)) {
            msgSent = true;
        }
        return msgSent;
    }

    /**
     * Send a login request to the chat server.
     *
     * @param username Username to use
     */
    public void tryLogin(String username) {
        // TODO Step 3: implement this method
        // Hint: Reuse sendCommand() method
        if (username.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (this.isConnectionActive()) {
            this.sendCommand("login " + username);
        }
    }

    /**
     * Send a request for latest user list to the server. To get the new users,
     * clear your current user list and use events in the listener.
     */
    public void refreshUserList() {
        // TODO Step 5: implement this method
        // Hint: Use Wireshark and the provided chat client reference app to find out what commands the
        // client and server exchange for user listing.
        this.sendCommand("users");
    }

    /**
     * Send a private message to a single recipient.
     *
     * @param recipient username of the chat user who should receive the message
     * @param message   Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPrivateMessage(String recipient, String message) {
        // TODO Step 6: Implement this method
        // Hint: Reuse sendCommand() method
        // Hint: update lastError if you want to store the reason for the error.
        boolean privateMsgSent = false;
        String privateMsg = "privmsg " + recipient + " " + message;
        if (this.sendCommand(privateMsg)) {
            privateMsgSent = true;
        }
        return privateMsgSent;
    }

    /**
     * Sends a request to the server asking for a joke
     *
     * @return true if request sent, false otherwise
     */
    public boolean sendJokeRequest() {
        boolean requestSent = false;
        if (this.sendCommand("joke")) {
            requestSent = true;
        }
        return requestSent;
    }

    /**
     * Send a request for the list of commands that server supports.
     */
    public void askSupportedCommands() {
        // TODO Step 8: Implement this method
        // Hint: Reuse sendCommand() method
        this.sendCommand("help");
    }


    /**
     * Wait for chat server's response
     *
     * @return one line of text (one command) received from the server
     */
    private String waitServerResponse() {
        // TODO Step 3: Implement this method
        // TODO Step 4: If you get I/O Exception or null from the stream, it means that something has gone wrong
        // with the stream and hence the socket. Probably a good idea to close the socket in that case.

        String serverResponse = null;
        try {
            serverResponse = this.fromServer.readLine();
        } catch (IOException e) {
            this.disconnect();
            this.lastError = e.getMessage();
        }
        return serverResponse;
    }

    /**
     * Get the last error message
     *
     * @return Error message or "" if there has been no error
     */
    public String getLastError() {
        if (lastError != null) {
            return lastError;
        } else {
            return "";
        }
    }

    /**
     * Start listening for incoming commands from the server in a new CPU thread.
     */
    public void startListenThread() {
        // Call parseIncomingCommands() in the new thread.
        Thread t = new Thread(() -> {
            parseIncomingCommands();
        });
        t.start();
    }

    /**
     * Read incoming messages one by one, generate events for the listeners. A loop that runs until
     * the connection is closed.
     */
    private void parseIncomingCommands() {
        while (isConnectionActive()) {
            // TODO Step 3: Implement this method
            // Hint: Reuse waitServerResponse() method
            // Hint: Have a switch-case (or other way) to check what type of response is received from the server
            // and act on it.
            // Hint: In Step 3 you need to handle only login-related responses.
            // Hint: In Step 3 reuse onLoginResult() method

            // Retrieve server response
            String serverResponse = this.waitServerResponse();
            System.out.println("Server response: " + serverResponse);

            // Check that the server response is not empty
            if (serverResponse != null) {
                // Split server response by space in to a string of words
                String[] responseArray = serverResponse.split(" ");

                // Check the first word
                switch (responseArray[0]) {
                    case LOGIN_SUCCESS:
                        this.onLoginResult(true, "");
                        break;
                    case LOGIN_ERROR:
                        this.onLoginResult(false, serverResponse);
                        break;
                    case PUBLIC_MESSAGE:
                        String text = this.createTextFromServerResponse(responseArray);
                        this.onMsgReceived(false, responseArray[1], text);
                        break;
                    case PRIVATE_MESSAGE:
                        String privMessage = this.createTextFromServerResponse(responseArray);
                        this.onMsgReceived(true, responseArray[1], privMessage);
                        break;
                    case USERS:
                        String[] users = Arrays.copyOfRange(responseArray,
                                1, responseArray.length);
                        this.onUsersList(users);
                        break;
                    case MSGERROR:
                        String msgError = this.createErrorMsg(responseArray);
                        this.onMsgError(msgError);
                        break;
                    case CMDERROR:
                        String cmdError = this.createErrorMsg(responseArray);
                        this.onCmdError(cmdError);
                        break;
                    case SUPPORTED_CMD:
                        this.onSupported(Arrays.copyOfRange(responseArray,
                                1, responseArray.length));
                        break;
                    case JOKE:
                        this.onJokeReceived(this.createJokeString(responseArray));
                        break;
                    default:
                        System.out.println("Server response unrecognisable: " + serverResponse);
                }
            }

            // TODO Step 5: update this method, handle user-list response from the server
            // Hint: In Step 5 reuse onUserList() method

            // TODO Step 7: add support for incoming chat messages from other users (types: msg, privmsg)
            // TODO Step 7: add support for incoming message errors (type: msgerr)
            // TODO Step 7: add support for incoming command errors (type: cmderr)
            // Hint for Step 7: call corresponding onXXX() methods which will notify all the listeners

            // TODO Step 8: add support for incoming supported command list (type: supported)

        }
    }

    /**
     * Creates a string with the joke given from the server. Removes all the
     * protocol commands that is not a part of the joke
     *
     * @param serverResponse a list of all the words from the server response
     * @return a string with the joke given from the server.
     */
    private String createJokeString(String[] serverResponse) {
        StringBuffer sb = new StringBuffer();
        int index = 1;
        while (index < serverResponse.length) {
            sb.append(serverResponse[index]);
            sb.append(" ");
            index++;
        }
        return sb.toString();
    }

    /**
     * Creates a string that only contains the error message received
     * from the server.
     *
     * @param responseArray an array containing all the words received from the server.
     * @return a string with the error message received from the server.
     */
    private String createErrorMsg(String[] responseArray) {
        StringBuffer sb = new StringBuffer();
        int index = 1;
        while (index < responseArray.length) {
            sb.append(responseArray[index]);
            sb.append(" ");
            index++;
        }
        return sb.toString();
    }

    /**
     * Creates a string that contains only the message that the user
     * typed inn.
     *
     * @param responseArray the array of words that the server responded with
     * @return String, returns a string containing only the message. All command words
     * are removed.
     */
    private String createTextFromServerResponse(String[] responseArray) {
        StringBuffer sb = new StringBuffer();
        int index = 2;
        while (index < responseArray.length) {
            sb.append(responseArray[index]);
            sb.append(" ");
            index++;
        }
        return sb.toString();
    }

    /**
     * Register a new listener for events (login result, incoming message, etc)
     *
     * @param listener
     */
    public void addListener(ChatListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Unregister an event listener
     *
     * @param listener
     */
    public void removeListener(ChatListener listener) {
        listeners.remove(listener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    // The following methods are all event-notificators - notify all the listeners about a specific event.
    // By "event" here we mean "information received from the chat server".
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Notify listeners that login operation is complete (either with success or
     * failure)
     *
     * @param success When true, login successful. When false, it failed
     * @param errMsg  Error message if any
     */
    private void onLoginResult(boolean success, String errMsg) {
        for (ChatListener l : listeners) {
            l.onLoginResult(success, errMsg);
        }
    }

    /**
     * Notify listeners that socket was closed by the remote end (server or
     * Internet error)
     */
    private void onDisconnect() {
        // TODO Step 4: Implement this method
        // Hint: all the onXXX() methods will be similar to onLoginResult()
        for (ChatListener l : listeners) {
            l.onDisconnect();
        }
    }

    /**
     * Notify listeners that server sent us a list of currently connected users
     *
     * @param users List with usernames
     */
    private void onUsersList(String[] users) {
        // TODO Step 5: Implement this method
        for (ChatListener l : this.listeners) {
            l.onUserList(users);
        }
    }

    /**
     * Notify listeners that a message is received from the server
     *
     * @param priv   When true, this is a private message
     * @param sender Username of the sender
     * @param text   Message text
     */
    private void onMsgReceived(boolean priv, String sender, String text) {
        // TODO Step 7: Implement this method
        TextMessage message = new TextMessage(sender, priv, text);
        for (ChatListener l : listeners) {
            l.onMessageReceived(message);
        }
    }

    /**
     * Notify listeners that our message was not delivered
     *
     * @param errMsg Error description returned by the server
     */
    private void onMsgError(String errMsg) {
        // TODO Step 7: Implement this method
        for (ChatListener l : listeners) {
            l.onMessageError(errMsg);
        }
    }

    /**
     * Notify listeners that command was not understood by the server.
     *
     * @param errMsg Error message
     */
    private void onCmdError(String errMsg) {
        // TODO Step 7: Implement this method
        for (ChatListener l : listeners) {
            l.onCommandError(errMsg);
        }
    }

    /**
     * Notify listeners that a help response (supported commands) was received
     * from the server
     *
     * @param commands Commands supported by the server
     */
    private void onSupported(String[] commands) {
        // TODO Step 8: Implement this method
        for (ChatListener l : listeners) {
            l.onSupportedCommands(commands);
        }
    }

    /**
     * Notify listeners that a joke response was received from the server.
     *
     * @param joke the joke received from the server
     */
    private void onJokeReceived(String joke) {
        for (ChatListener l : listeners) {
            l.onJokeReceived(joke);
        }
    }
}
