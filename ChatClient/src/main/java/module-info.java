module no.ntnu.datakomm.chat {
  requires javafx.controls;
  requires javafx.fxml;
  requires java.logging;

  opens no.ntnu.datakomm.chat to javafx.fxml;
  exports no.ntnu.datakomm.chat;
}