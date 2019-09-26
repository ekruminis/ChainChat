package sample;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Timer;

public class Controller {

    @FXML
    private HBox mainMenu;
    @FXML
    private VBox optionsMenu;

    static sample.MessagesController msgController;
    static sample.BlockchainController blockchainController;
    static sample.ContactsController contactsController;

    @FXML
    private HBox messages;
    @FXML
    private HBox contacts;
    @FXML
    private HBox settings;
    @FXML
    private HBox blockchain;
    @FXML
    private HBox exit;

    @FXML
    private BorderPane loadingMenu;
    @FXML
    private Text progressPercentage;
    @FXML
    private Text progressInfo;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Text waitText;
    @FXML
    private TextField bootstrapIP;
    @FXML
    private Button bootstrapButton;
    @FXML
    private TextField myPeerPort;

    // styles used for vboxes highlighting
    private final Background focusBackground = new Background( new BackgroundFill( Color.web("#7a1f0f"), CornerRadii.EMPTY, Insets.EMPTY ) );
    private final Background unfocusBackground = new Background( new BackgroundFill( Color.web("#c93318"), CornerRadii.EMPTY, Insets.EMPTY ) );

    public void initialize() {
        System.out.println("opened..");
        loading();
    }

    /** Setups the main screen */
    private void loading() {
        waitText.setText("Please enter the IP:Port pair of some peer to bootstrap to..");
        bootstrapButton.setOnAction((e) -> {
            // Check if inputs are valid (peer not empty and =4 numbers, ip is valid format)
            if(!bootstrapIP.getText().equals("") && myPeerPort.getText().length() == 4 && bootstrapIP.getText().substring(bootstrapIP.getText().length()-5, bootstrapIP.getText().length()-4).equals(":")) {

                // Get IP data
                String adr = bootstrapIP.getText();
                int targetPort = Integer.parseInt(adr.substring(adr.length()-4, adr.length()));
                String ip = adr.substring(0, adr.length()-5);

                // Check if its a valid IPv4 IP
                boolean validIP = false;
                try {
                    validIP = InetAddress.getByName(ip) instanceof Inet4Address;
                    System.out.println("ip valid: " + validIP);
                } catch(Exception ex) {
                    System.out.println("valid ip err: " + ex);
                }

                int myPort = Integer.parseInt(myPeerPort.getText());
                if(validIP) {
                    bootstrapButton.setDisable(true);
                    bootstrapIP.setDisable(true);
                    myPeerPort.setDisable(true);

                    waitText.setText("Please wait while the application finishes loading...");

                    // Boostrap to listed peer
                    new Thread(() -> {
                        System.out.println("cmd - connect");
                        sample.Main.network.quitPeer();
                        sample.Main.network.connect(ip, targetPort, myPort);
                    }).start();

                    progressBar.setVisible(true);
                }
                else {
                    progressInfo.setText("IP given is not a valid IPV4 address");
                }
            }
            else if(myPeerPort.getText().length() != 4){
                progressInfo.setText("Your port should consist of 4 numbers, eg. 4001");
            }
            else {
                progressInfo.setText("Check that the details you entered are correct and that the IP:Port pair is in the correct format, eg. 86.40.54.40:4001");
            }
        });

        // If not finished, disable all other input
        if(progressBar.getProgress() != 100) {
            optionsMenu.setOnMouseClicked((e) -> {
                System.out.println("clicked");
                waitText.setFill(Color.WHITE);
                final Timer t = new java.util.Timer();
                t.schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                waitText.setFill(Color.BLACK);
                                t.cancel();
                            }
                        },
                        100
                );
            });
        }
    }

    /** Set the level of the progress bar
     * @param percentage The percentage of the bar that should be filled */
    public void setProgressPercentage(int percentage) {
        progressPercentage.setText("Progress - " + percentage + "%");
        progressBar.setProgress((double)percentage/100);

        // Finished, so enable menu input
        if(percentage == 100) {
            waitText.setText("Finished loading..");
            optionsMenu.setDisable(false);
            listen();
        }
    }

    /** Set the info of the progress bar
     * @param text The text that should be displayed */
    public void setProgressInfo(String text) {
        progressInfo.setText(text);

        // Bootstrap valid, so enable menu input
        if(progressInfo.getText().equals("could not bootstrap to peer.. try again or continue using the application")) {
            waitText.setText("Finished loading..");
            bootstrapButton.setDisable(false);
            bootstrapIP.setDisable(false);
            myPeerPort.setDisable(false);
        }

        // Bootstrap not valid, allow user to bootstrap again
        else if(progressInfo.getText().equals("ERROR: You cannot bootstrap to yourself!")) {
            waitText.setText("Finished loading..");
            bootstrapButton.setDisable(false);
            bootstrapIP.setDisable(false);
            myPeerPort.setDisable(false);
            optionsMenu.setDisable(true);
        }
    }

    /** Enable menu controls */
    private void listen() {
        // Open messages screen
        messages.setOnMouseClicked( ( e ) -> {
            messages.requestFocus();
            messages.backgroundProperty().bind( Bindings
                    .when( messages.focusedProperty() )
                    .then( focusBackground )
                    .otherwise( unfocusBackground )
            );

            if(msgController != null) {
                msgController.loadConversations();
                if(msgController.selectedUser.getText() != null || !msgController.selectedUser.getText().equals("")) {
                    msgController.chatBox.getChildren().clear();
                    msgController.loadMessages();
                }
            }

            HBox hb = null;

            try{
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/messagesMenu.fxml"));
                hb = fxmlLoader.load();
                if(msgController == null) {
                    msgController = (sample.MessagesController) fxmlLoader.getController();
                }

            } catch(Exception er) {
                System.out.println("err: " + er);
            }

            if(hb != null) {
                hb.setHgrow(hb, Priority.ALWAYS);
                hb.setId("messages");
            }

            boolean exists = false;

            if(mainMenu.getChildren().contains(loadingMenu)) {
                optionsMenu.setOnMouseClicked(null);
                mainMenu.getChildren().remove(loadingMenu);
            }
            else {
                for(Node n : mainMenu.getChildren()) {
                    if(n.getId().equals("messages")) {
                        exists = true;
                    }

                    if(n.getId().equals("messages") || n.getId().equals("optionsMenu")) {
                        n.setVisible(true);
                        n.setManaged(true);
                    }
                    else {
                        n.setVisible(false);
                        n.setManaged(false);
                    }
                }
            }

            if(!exists) {
                mainMenu.getChildren().add(hb);
            }

        });

        // Open contacts menu
        contacts.setOnMouseClicked( ( e ) -> {
            contacts.requestFocus();
            contacts.backgroundProperty().bind( Bindings
                    .when( contacts.focusedProperty() )
                    .then( focusBackground )
                    .otherwise( unfocusBackground )
            );

            if(contactsController != null) {
                contactsController.reloadList();
            }

            VBox vb = null;

            try{
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/contactsMenu.fxml"));
                vb = fxmlLoader.load();
                if(contactsController == null) {
                    contactsController = (sample.ContactsController) fxmlLoader.getController();
                }
            } catch(Exception er) {
                System.out.println("err: " + er);
            }

            if(vb != null) {
                vb.setVgrow(vb, Priority.ALWAYS);
                vb.setId("contacts");
            }

            boolean exists = false;

            if(mainMenu.getChildren().contains(loadingMenu)) {
                optionsMenu.setOnMouseClicked(null);
                mainMenu.getChildren().remove(loadingMenu);
            }
            else {
                for (Node n : mainMenu.getChildren()) {
                    if(n.getId().equals("contacts")) {
                        exists = true;
                    }

                    if (n.getId().equals("contacts") || n.getId().equals("optionsMenu")) {
                        n.setVisible(true);
                        n.setManaged(true);
                    } else {
                        n.setVisible(false);
                        n.setManaged(false);
                    }
                }
            }

            if(!exists) {
                mainMenu.getChildren().add(vb);
            }

        });

        // Open settings menu
        settings.setOnMouseClicked( ( e ) -> {
            settings.requestFocus();
            settings.backgroundProperty().bind( Bindings
                    .when( settings.focusedProperty() )
                    .then( focusBackground )
                    .otherwise( unfocusBackground )
            );

            boolean exists = false;

            if(mainMenu.getChildren().contains(loadingMenu)) {
                optionsMenu.setOnMouseClicked(null);
                mainMenu.getChildren().remove(loadingMenu);
            }
            else {
                for (Node n : mainMenu.getChildren()) {
                    if(n.getId().equals("settings")) {
                        exists = true;
                    }

                    if (n.getId().equals("settings") || n.getId().equals("optionsMenu")) {
                        n.setVisible(true);
                        n.setManaged(true);
                    } else {
                        n.setVisible(false);
                        n.setManaged(false);
                    }
                }
            }

            if(!exists) {
                //mainMenu.getChildren().add(hb);
            }
        });

        // Open blockchain menu
        blockchain.setOnMouseClicked( ( e ) -> {
            blockchain.requestFocus();
            blockchain.backgroundProperty().bind( Bindings
                    .when( blockchain.focusedProperty() )
                    .then( focusBackground )
                    .otherwise( unfocusBackground )
            );

            VBox vb = null;

            try{
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/blockchainMenu.fxml"));
                vb = fxmlLoader.load();
                if(blockchainController == null) {
                    blockchainController = (sample.BlockchainController) fxmlLoader.getController();
                }
            } catch(Exception er) {
                System.out.println("err: " + er);
            }

            vb.setVgrow(vb, Priority.ALWAYS);
            vb.setId("blockchain");
            vb.setAlignment(Pos.CENTER);

            boolean exists = false;

            if(mainMenu.getChildren().contains(loadingMenu)) {
                optionsMenu.setOnMouseClicked(null);
                mainMenu.getChildren().remove(loadingMenu);
            }
            else {
                for (Node n : mainMenu.getChildren()) {
                    if(n.getId().equals("blockchain")) {
                        exists = true;
                    }

                    if (n.getId().equals("blockchain") || n.getId().equals("optionsMenu")) {
                        n.setVisible(true);
                        n.setManaged(true);
                    } else {
                        n.setVisible(false);
                        n.setManaged(false);
                    }
                }
            }

            if(!exists) {
                mainMenu.getChildren().add(vb);
            }
        });

        // Close window and app
        exit.setOnMouseClicked( ( e ) -> {
            System.out.println("exit clicked");
            exit.requestFocus();
            exit.backgroundProperty().bind( Bindings
                    .when( exit.focusedProperty() )
                    .then( focusBackground )
                    .otherwise( unfocusBackground )
            );
            System.exit(0);
        });
    }


}
