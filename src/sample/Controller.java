package sample;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ConcurrentModificationException;
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

    // styles used for vboxes
    private final Background focusBackground = new Background( new BackgroundFill( Color.web("#7a1f0f"), CornerRadii.EMPTY, Insets.EMPTY ) );
    private final Background unfocusBackground = new Background( new BackgroundFill( Color.web("#c93318"), CornerRadii.EMPTY, Insets.EMPTY ) );

    public void initialize() {
        System.out.println("opened..");
        loading();
    }

    private void loading() {
        waitText.setText("Please enter the IP:Port pair of some peer to bootstrap to..");
        bootstrapButton.setOnAction((e) -> {
            if(!bootstrapIP.getText().equals("") && myPeerPort.getText().length() == 4 && bootstrapIP.getText().substring(bootstrapIP.getText().length()-5, bootstrapIP.getText().length()-4).equals(":")) {
                String adr = bootstrapIP.getText();

                int targetPort = Integer.parseInt(adr.substring(adr.length()-4, adr.length()));
                System.out.println("targetPort is: " + targetPort);

                String ip = adr.substring(0, adr.length()-5);
                System.out.println("targetIP is: " + ip);

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

    public void setProgressPercentage(int percentage) {
        progressPercentage.setText("Progress - " + percentage + "%");
        progressBar.setProgress((double)percentage/100);
        if(percentage == 100) {
            waitText.setText("Finished loading..");
            optionsMenu.setDisable(false);
            listen();
        }
    }

    public void setProgressInfo(String text) {
        progressInfo.setText(text);
        if(progressInfo.getText().equals("could not bootstrap to peer.. try again or continue using the application")) {
            waitText.setText("Finished loading..");
            bootstrapButton.setDisable(false);
            bootstrapIP.setDisable(false);
            myPeerPort.setDisable(false);
        }
        else if(progressInfo.getText().equals("ERROR: You cannot bootstrap to yourself!")) {
            waitText.setText("Finished loading..");
            bootstrapButton.setDisable(false);
            bootstrapIP.setDisable(false);
            myPeerPort.setDisable(false);
            optionsMenu.setDisable(true);
        }
    }

    private void listen() {
        messages.setOnMouseClicked( ( e ) -> {
            System.out.println("messages clicked");
            messages.requestFocus();
            messages.backgroundProperty().bind( Bindings
                    .when( messages.focusedProperty() )
                    .then( focusBackground )
                    .otherwise( unfocusBackground )
            );

            if(msgController != null) {
                msgController.loadConversations();
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

        contacts.setOnMouseClicked( ( e ) -> {
            System.out.println("contacts clicked");
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

        settings.setOnMouseClicked( ( e ) -> {
            System.out.println("settings clicked");
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

        blockchain.setOnMouseClicked( ( e ) -> {
            System.out.println("blockchain clicked");
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
