package no.ntnu.datakomm.chat;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.ConnectException;

import static java.lang.Thread.sleep;

public class MyGUIController implements ChatListener {

    private TextField serverIP;
    private TextField serverPort;
    private Button connectBtn;
    private TextField usernameInput;
    private Button loginBtn;
    private TextField messageInput;
    private Button sendBtn;
    private VBox userList;
    private VBox chatWindow;
    private Label statusLabel;

    private TCPClient tcpClient;

    private Thread userPollThread;


    public MyGUIController(TextField serverIP, TextField serverPort, Button connectBtn, TextField usernameInput, Button loginBtn, TextField messageInput, Button sendBtn, VBox userList, VBox chatWindow, Label statusLabel, TCPClient tcpClient) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.connectBtn = connectBtn;
        this.usernameInput = usernameInput;
        this.loginBtn = loginBtn;
        this.messageInput = messageInput;
        this.sendBtn = sendBtn;
        this.userList = userList;
        this.chatWindow = chatWindow;
        this.statusLabel = statusLabel;

        this.tcpClient = tcpClient;
    }

    @Override
    public void onDisconnect() {

    }

    @Override
    public void onLoginResult(boolean success, String errMsg) {

    }

    @Override
    public void onMessageReceived(TextMessage message) {

    }

    @Override
    public void onMessageError(String errMsg) {

    }

    @Override
    public void onUserList(String[] usernames) {
        Platform.runLater(() -> {
            this.userList.getChildren().clear();
            for (String user : usernames) {
                this.userList.getChildren().add(new Label(user));
            }
        });
    }

    @Override
    public void onSupportedCommands(String[] commands) {

    }

    @Override
    public void onCommandError(String errMsg) {

    }

    @Override
    public void onJokeReceived(String joke) {

    }

    /**
     * Connect to chat server. If already connected, disconnect
     */
    public void doCheckConnect() throws IllegalArgumentException, NumberFormatException, ConnectException {
        if (this.tcpClient.isConnectionActive()) {
            this.tcpClient.disconnect();
            this.updateButtons(false);
            this.clearWindows();
        } else {
            try {
                this.setupConnection();
            } catch (Exception e) {
                throw e;
            }
        }
    }

    /**
     * Clears all the windows
     */
    private void clearWindows() {
        this.userList.getChildren().clear();
        var noUserLabel = new Label("No users available");
        noUserLabel.setAlignment(Pos.CENTER);
        noUserLabel.setOpacity(0.5);
        this.userList.getChildren().add(noUserLabel);

        this.chatWindow.getChildren().clear();
        var tipLabelWrapper = new HBox();
        tipLabelWrapper.setAlignment(Pos.CENTER);
        var tipLabel = new Label("Type /privmsg \"username\" \"message\" to send a private message");
        tipLabel.setOpacity(0.5);
        tipLabelWrapper.getChildren().add(tipLabel);
        this.chatWindow.getChildren().add(tipLabelWrapper);
    }

    /**
     * Updates the buttons and the status message.
     * @param {@code true} when we want to update buttons to a connected state. {@code false} when we want
     * to update buttons to a disconnected state.
     */
    private void updateButtons(boolean connectionState) {
        String status;
        String connBtnText;
        if (connectionState) {
            status = "Connected";
            connBtnText = "Disconnect";
        } else {
            status = "Not connected: " + this.tcpClient.getLastError();
            connBtnText = "Connect";
        }

        Platform.runLater(() -> {
            this.statusLabel.setText(status);
            this.connectBtn.setText(connBtnText);

            this.loginBtn.setDisable(!connectionState);
            this.sendBtn.setDisable(!connectionState);
        });
    }

    /**
     * Sets up a connection with the ip and port given in text fields
     */
    private void setupConnection() {
        this.statusLabel.setText("Trying to connect...");
        this.statusLabel.setText("Connection...");

        var serverIP = this.serverIP.getText();
        var serverPort = this.serverPort.getText();

        int serverPortInt;
        try {
            serverPortInt = Integer.parseInt(serverPort);
        } catch (NumberFormatException e) {
            throw e;
        }

        Thread connThread = new Thread(() -> {
            boolean connected = this.tcpClient.connect(serverIP, serverPortInt);
            if (connected) {
                this.tcpClient.addListener(this);
                this.tcpClient.startListenThread();
                this.startUserPolling();
            }
            updateButtons(connected);
        });
        connThread.start();
    }

    /**
     * Starts a new thread that will poll the server for currently active users
     */
    private void startUserPolling() {
        if (userPollThread == null) {
            userPollThread = new Thread(() -> {
                long threadId = Thread.currentThread().getId();
                System.out.println("Started user polling in Thread " + threadId);
                while (tcpClient.isConnectionActive()) {
                    tcpClient.refreshUserList();
                    try {
                        sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                System.out.println("User polling thread " + threadId + " exiting...");
                userPollThread = null;
            });

            userPollThread.start();
        }
    }

    /**
     * Logs in to the server
     */
    public void doLogin() {
    }
}
