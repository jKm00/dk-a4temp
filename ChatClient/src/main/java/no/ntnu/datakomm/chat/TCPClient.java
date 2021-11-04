package no.ntnu.datakomm.chat;

import java.io.*;
import java.net.*;
import java.util.IllformedLocaleException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TCPClient {
    private PrintWriter toServer;
    private BufferedReader fromServer;
    private Socket connection;
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private final static String loginok = "loginok";
    private final static String userAlreadyInUse = "loginerr username already in use";

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
        // TODO Step 1: implement this method DONE!
        // Hint: Remember to process all exceptions and return false on error
        // Hint: Remember to set up all the necessary input/output stream variables
        boolean connected = false;
        try {
            this.connection = new Socket(host, port); // Creates a new socket and connects to a given host with port
            this.fromServer = new BufferedReader(new InputStreamReader(this.connection.getInputStream())); // Reads messages from the server
            this.toServer = new PrintWriter(this.connection.getOutputStream(), true); // Writes messages to the server
            connected = true;
        } catch (IOException e) {
            this.logger.log(Level.WARNING, "Something went wrong when establishing a socket: " + e.getMessage());
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
        // TODO: step 4 DONE!
        // Keyword synchronized to make sure no two threads call this method in parallel.
        // If one thread is executing a synchronized method for an object, all other threads that invoke synchronized
        // methods for the same object block until the first thread is done with the object.
        if(isConnectionActive()){ // checks if the connection is active
            try {
                this.toServer=null;
                this.fromServer=null;
                this.connection.close();
                this.connection=null;
                onDisconnect();
            } catch (IOException e) {
                this.logger.log(Level.WARNING, "Error while disconnecting: " + e.getMessage());
            }
        }
    }

    /**
     * @return true if the connection is active (opened), false if not.
     */
    public boolean isConnectionActive() {
        return this.connection != null;
    }

    /**
     * Send a command to server.
     *
     * @param cmd A command. It should include the command word and optional attributes, according to the protocol.
     * @return true on success, false otherwise
     */
    private boolean sendCommand(String cmd) {
        // TODO Step 2: Implement this method DONE!
        // Hint: Remember to check if connection is active
        boolean success=false;
        if(isConnectionActive()){ // Checks if the connection is active
            this.toServer.println(cmd); // Sends cmd to server
            success=true;
        }
        return success;
    }

    /**
     * Send a public message to all the recipients.
     *
     * @param message Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPublicMessage(String message) {
        // TODO Step 2: implement this method DONE!
        // Hint: Reuse sendCommand() method
        // Hint: update lastError if you want to store the reason for the error.
        if(sendCommand("msg"+" "+message)){ // Uses sendCommand method to check if the message was sent successfully, if so return true
            return true;
        } else {
            this.logger.log(Level.WARNING, "Error sending public message");
            return false;
        }
    }

    /**
     * Send a login request to the chat server.
     *
     * @param username Username to use
     */
    public void tryLogin(String username) {
        // TODO Step 3: implement this method DONE!
        // Hint: Reuse sendCommand() method
        if(this.isConnectionActive()){
            sendCommand("login" + " " + username);
        }
    }

    /**
     * Send a request for latest user list to the server. To get the new users,
     * clear your current user list and use events in the listener.
     */
    public void refreshUserList() {
        // TODO Step 5: implement this method DONE!
        // Hint: Use Wireshark and the provided chat client reference app to find out what commands the
        // client and server exchange for user listing.
        sendCommand("users");
    }
    /**
     * Send a private message to a single recipient.
     *
     * @param recipient username of the chat user who should receive the message
     * @param message   Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPrivateMessage(String recipient, String message) {
        // TODO Step 6: Implement this method DONE!
        // Hint: Reuse sendCommand() method
        // Hint: update lastError if you want to store the reason for the error.
        try {
            sendCommand("privmsg " + recipient + " " + message);
            return true;
        }
        catch (Exception e) {
            lastError = e.getMessage();
            return false;
        }
    }


    /**
     * Send a request for the list of commands that server supports.
     */
    public void askSupportedCommands() {
        // TODO Step 8: Implement this method DONE!
        // Hint: Reuse sendCommand() method
        sendCommand("help");
    }


    /**
     * Wait for chat server's response
     *
     * @return one line of text (one command) received from the server
     */
    private String waitServerResponse() {
        // TODO Step 3: Implement this method DONE!
        // TODO Step 4: If you get I/O Exception or null from the stream, it means that something has gone wrong DONE!
        // with the stream and hence the socket. Probably a good idea to close the socket in that case.
        String response=null;
        try {
            response = this.fromServer.readLine();
        } catch (IOException e) {
            this.logger.log(Level.WARNING, "Error while waiting for server response: " + e.getMessage());
            disconnect(); // Disconnects if something wrong happens when waiting for response
        }
        return response;
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
            // TODO Step 3: Implement this method DONE!
            // Hint: Reuse waitServerResponse() method
            // Hint: Have a switch-case (or other way) to check what type of response is received from the server
            // and act on it.
            // Hint: In Step 3 you need to handle only login-related responses.
            // Hint: In Step 3 reuse onLoginResult() method
            String response = waitServerResponse();
            String[] responseList = response.split(" ");
            String responseCmd = responseList[0];

            String userMessage = createMessage(responseList);
            String errorMessage = createErrorMsg(responseList);

            switch(responseCmd){
                case LOGINOK: onLoginResult(true, "");
                    break;

                case LOGINERR: onLoginResult(false, "Username is already in use...");
                    break;

                // TODO Step 5: update this method, handle user-list response from the server DONE!
                // Hint: In Step 5 reuse onUserList() method

                case USERS: ;
                    onUsersList(createContentList(responseList));
                    break;

                // TODO Step 7: add support for incoming chat messages from other users (types: msg, privmsg) DONE!
                case RCVPRIV_MSG:
                    onMsgReceived(true, responseList[1], userMessage);
                    break;

                case RCVPBLC_MSG:
                    onMsgReceived(false, responseList[1], userMessage);
                    break;

                case SUCCESS_MSG:
                    this.logger.log(Level.INFO, "Message sent successfully");
                    break;

                // TODO Step 7: add support for incoming message errors (type: msgerr) DONE!
                case ERROR_MSG:
                    onMsgError(errorMessage);
                    this.lastError = errorMessage;
                    break;

                // TODO Step 7: add support for incoming command errors (type: cmderr) DONE!
                case ERROR_CMD:
                    onCmdError(errorMessage);
                    this.lastError = errorMessage;
                    break;

                // TODO Step 8: add support for incoming supported command list (type: supported) DONE!
                case HELP:
                    onSupported(createContentList(responseList));
                    break;

                default:
                    System.out.println("Unrecognized msg..");
                //TODO: handle response=null
            }
        }
    }

    /**
     * Takes the third index in the responselist (usually the message to be sent)
     * @param responseList a list created of the response
     * returns the message as a String
     * @return the message as a String
     */
    public String createMessage(String[] responseList){
        String message="";
        int i=2;
        while(i<responseList.length){
            message = message + " " + responseList[i];
            i++;
        }
        return message;
    }

    /**
     * Takes all indexes except the first one (protocol command) and creates a new string with only Content.
     * The new string is then split into a list (userList)
     * @param responseList list of users
     * returns a list of only users
     * @return a list of only users
     */
    public String[] createContentList(String[] responseList){
        ArrayList<String> content = new ArrayList<>();
        String contentString = "";
        int i=1;
        while(i<responseList.length){
            contentString += ";" + responseList[i];
            i++;
        }
        String[] contentList = contentString.split(";");
        return contentList;
    }

    /**
     * Creates an error message from the responseList
     * @param responseList List to be converted to error message
     * returns error message
     * @return error message
     */
    public String createErrorMsg(String[] responseList){
        String errorMsg = "";
        int i = 1;
        while(i<responseList.length){
            errorMsg = errorMsg + " " + responseList[i];
            i++;
        }
        return errorMsg;
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
        // TODO Step 4: Implement this method DONE!
        // Hint: all the onXXX() methods will be similar to onLoginResult()
        for (ChatListener l : listeners){
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
    }

    /**
     * Notify listeners that our message was not delivered
     *
     * @param errMsg Error description returned by the server
     */
    private void onMsgError(String errMsg) {
        // TODO Step 7: Implement this method
    }

    /**
     * Notify listeners that command was not understood by the server.
     *
     * @param errMsg Error message
     */
    private void onCmdError(String errMsg) {
        // TODO Step 7: Implement this method
    }

    /**
     * Notify listeners that a help response (supported commands) was received
     * from the server
     *
     * @param commands Commands supported by the server
     */
    private void onSupported(String[] commands) {
        // TODO Step 8: Implement this method
    }
}
