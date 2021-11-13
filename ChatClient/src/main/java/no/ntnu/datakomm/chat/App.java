package no.ntnu.datakomm.chat;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.net.URL;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * Class representing the main Graphical User Interface (GUI). JavaFX interface.
 */
public class App extends Application {

    private MyGUIController controller;

    private TextField serverIP;
    private TextField serverPort;
    private Button connectBtn;
    private TextField usernameInputField;
    private Button loginBtn;
    private TextField messageInput;
    private Button sendBtn;
    private VBox userList;
    private VBox chatWindow;

    private Label statusLabel;

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * This method is called automatically by JavaFX when the application is
     * launched
     *
     * @param primaryStage The main "stage" where the GUI will be rendered
     */
    @Override
    public void start(Stage primaryStage) {
        /*URL fxmlUrl = getClass().getResource("layout.fxml");
        URL cssUrl = getClass().getResource("styles/style.css");
        URL iconUrl = getClass().getResource("styles/ntnu.png");
        Parent root = null;
        boolean loaded = false;
        if (fxmlUrl != null && cssUrl != null && iconUrl != null) {
            try {
                root = FXMLLoader.load(fxmlUrl);
                Scene scene = new Scene(root, 600, 400);
                scene.getStylesheets().add(cssUrl.toURI().toString());
                primaryStage.setTitle("NTNU Ålesund - ChatClient");
                primaryStage.setScene(scene);
                Image anotherIcon = null;
                anotherIcon = new Image(iconUrl.toURI().toString());
                primaryStage.getIcons().add(anotherIcon);
                primaryStage.show();
                loaded = true;

                primaryStage.setOnCloseRequest(e -> {
                    Platform.exit();
                });
            } catch (URISyntaxException | IOException e) {
                System.out.println("Error while loading FXML: " + e.getMessage());
            }
        }
        if (!loaded) {
            if (fxmlUrl == null) {
                System.out.println("FXML file not found!");
            }
            if (cssUrl == null) {
                System.out.println("CSS file not found!");
            }
            if (iconUrl == null) {
                System.out.println("Icon file not found!");
            }
            Platform.exit();
        }*/

        BorderPane root = new BorderPane();

        root.setTop(this.createConnectionBar());
        root.setLeft(this.createUserPane());
        root.setCenter(this.createChatWindow());

        this.initialize();

        Scene scene = new Scene(root, 800, 500);

        primaryStage.setTitle("NTNU Ålesund - ChatClient");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Initializes the fields
     */
    private void initialize() {
        TCPClient tcpClient = new TCPClient();
        this.controller = new MyGUIController(this.serverIP, this.serverPort, this.connectBtn, this.usernameInputField, this.loginBtn, this.messageInput, this.sendBtn, this.userList, this.chatWindow, this.statusLabel, tcpClient);
    }

    /**
     * Creates the window where all the chat messages are displayed.
     *
     * @return a BorderPane where all the messages should be displayed.
     */
    private Node createChatWindow() {
        var chatWindowWrapper = new BorderPane();

        // Create messages window
        this.chatWindow = new VBox();
        // Create tip label wrapper
        var tipLabelWrapper = new HBox();
        tipLabelWrapper.setAlignment(Pos.CENTER);
        // Create tip label
        var tipLabel = new Label("Type /privmsg \"username\" \"message\" to send a private message");
        tipLabel.setOpacity(0.5);
        // Add label to wrapper
        tipLabelWrapper.getChildren().add(tipLabel);
        // Add tip to message window
        this.chatWindow.getChildren().add(tipLabelWrapper);

        // Create window where messages are typed and send
        var chatWindowSender = new HBox();
        chatWindowSender.setAlignment(Pos.CENTER);
        // Create field for typing in messages
        this.messageInput = new TextField();
        this.messageInput.setPromptText("Send a message");
        this.messageInput.setMinWidth(300);
        // Create send button
        this.sendBtn = new Button("Send");
        this.sendBtn.setDisable(true);

        // Add message field and send button to chat window sender pane
        chatWindowSender.getChildren().addAll(this.messageInput, this.sendBtn);

        // Add message window to center of wrapper
        chatWindowWrapper.setCenter(this.chatWindow);

        // Add chat window sender at the bottom of the wrapper
        chatWindowWrapper.setBottom(chatWindowSender);

        return chatWindowWrapper;
    }

    /**
     * Creates the connection menu at the top of the application
     *
     * @return a HBox with connections options to connect to a chat server
     */
    private Node createConnectionBar() {
        var connectionMenuWrapper = new VBox();

        var statusMsgWrapper = new HBox();
        statusMsgWrapper.setPadding(new Insets(10, 0, 0, 0));
        statusMsgWrapper.setAlignment(Pos.CENTER);
        this.statusLabel = new Label("Not connected");
        statusMsgWrapper.getChildren().addAll(new Label("Status: "),this.statusLabel);

        var connectionMenu = new HBox();
        connectionMenu.setAlignment(Pos.CENTER);
        connectionMenu.setSpacing(10);
        connectionMenu.setPadding(new Insets(10));

        // Server ip
        this.serverIP = new TextField();
        this.serverIP.setText("datakomm.work");
        // Server port
        this.serverPort = new TextField();
        this.serverPort.setText("1300");

        // Connect button
        this.connectBtn = new Button("Connect");
        this.connectBtn.setOnAction(e -> {
            try {
                this.controller.doCheckConnect();
            } catch (NumberFormatException nfe) {
                this.statusLabel.setText("Could not connect: make sure port is a number!");
            } catch (IllegalArgumentException iae) {
                this.statusLabel.setText("Could not connect: make no fields are empty!");
            } catch (ConnectException ce) {
                this.statusLabel.setText("Could not connect, please try again:)");
            }
        });

        connectionMenu.getChildren().addAll(new Label("Server IP:"), this.serverIP, new Label("Server port:"), this.serverPort, this.connectBtn);

        connectionMenuWrapper.getChildren().addAll(statusMsgWrapper, connectionMenu);

        return connectionMenuWrapper;
    }

    /**
     * Creates the pane that displays the user that are currently logged in to the chat server
     *
     * @return a BorderPane that displays the users currently available in the chat server
     */
    private BorderPane createUserPane() {
        BorderPane userPane = new BorderPane();

        // Create user list
        this.userList = new VBox();
        this.userList.setPadding(new Insets(10));
        // Create no user label
        var noUserLabel = new Label("No users available");
        noUserLabel.setAlignment(Pos.CENTER);
        noUserLabel.setOpacity(0.5);
        // Add no user label to user list
        this.userList.getChildren().add(noUserLabel);

        // Create login form
        HBox loginForm = new HBox();
        // Create input field for username
        this.usernameInputField = new TextField();
        // Creating login button
        this.loginBtn = new Button("Login");
        // Setting button inactive while connection is dead
        this.loginBtn.setDisable(true);
        // Setting login button action
        this.loginBtn.setOnAction(e -> this.controller.doLogin());
        loginForm.getChildren().addAll(this.usernameInputField, this.loginBtn);

        // Add user list to user pane
        userPane.setCenter(userList);
        // Add login form to user pane
        userPane.setBottom(loginForm);

        return userPane;
    }
}