package sample;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.lang3.SerializationUtils;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;

import static org.iq80.leveldb.impl.Iq80DBFactory.asString;
import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

public class MessagesController {

    @FXML
    private VBox conversationList;
    @FXML
    private Text selectedUser;
    @FXML
    private Button refreshButton;

    @FXML
    private VBox chatBox;
    @FXML
    private TextField msgField;
    @FXML
    private Button sendButton;

    @FXML
    private Button startNewConversationButton;

    private String userPubKey = null;

    // styles used for vboxes
    private final Background focusBackground = new Background( new BackgroundFill( Color.web("#d1cfcf"), CornerRadii.EMPTY, Insets.EMPTY ) );
    private final Background unfocusBackground = new Background( new BackgroundFill( Color.web("#ffffff"), CornerRadii.EMPTY, Insets.EMPTY ) );

    private Options options = new Options();
    private DB contactsDB;

    public void initialize() {
        System.out.println("messages opened..");
        loadConversations();
        listen();
    }

    private String getMyPublicKey() {
        return Base64.getEncoder().encodeToString( sample.Main.encryption.getRSAPublic().getEncoded() );
    }

    private String getUserPubKey(String u) {
        userPubKey = asString(read(getContactsDB(), u.getBytes()));
        return userPubKey;
    }

    public String searchContacts(String pk) {
        DB cdb = getContactsDB();
        DBIterator iterator = cdb.iterator();
        String user = null;
        try {
            for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                String key = asString(iterator.peekNext().getKey());

                if(asString(iterator.peekNext().getValue()).equals(pk)) {
                    user = key;
                }
            }
        }
        catch(Exception e) {
            System.out.println("LOAD CONVERSATIONS ERROR: " + e);
            finish(cdb);
        }
        finally {
            // Make sure you close the iterator to avoid resource leaks.
            try {
                iterator.close();
                finish(cdb);
            } catch(Exception e) {
                System.out.println("CLOSING ITERATOR ERROR: " + e);
            }
            return user;
        }
    }

    public void loadConversations() {
        conversationList.getChildren().clear();

        System.out.println("starting to load conversations..");
        LinkedList<String> list = new LinkedList<>(sample.Main.network.getChain().getChats("me"));


        for(String m : list) {
            String user = searchContacts(m);
            if(user != null && !user.isEmpty()) {
                System.out.println("we know this user from our contactsDB..");
                addConversationBox(user);
            }
            else {
                System.out.println("we don't know this user..");
                int count = 1;
                boolean end = false;
                while(!end) {
                    String u = asString(read(getContactsDB(), ("unknown"+count).getBytes()));
                    if(u == null) {
                        end = true;
                    }
                    count++;
                }
                System.out.println("unknown user so we add him to our contactsDB..");
                add(getContactsDB(), ("unknown"+count).getBytes(), m.getBytes());
                addConversationBox(("unknown"+count));
            }
        }

        System.out.println("finished loading conversations");
    }

    private void loadMessages() {
        System.out.println("starting to load messages..");
        sample.MessagesList ml = sample.Main.network.getChain().getMessages(selectedUser.getText(), userPubKey);

        LinkedList<sample.Message> list = new LinkedList<>(ml.getMessagesList());
        LinkedList<String> colours = new LinkedList<>(ml.getColoursList());

        Platform.runLater(() -> {
            for(int x = 0; x < list.size(); x++) {
                if(list.get(x).getSender().equals(getMyPublicKey())) {
                    System.out.println("im sender!");
                    String msg = new sample.Messenger().readMessage(list.get(x), getMyPublicKey());
                    addRightBubble(msg, colours.get(x));
                }
                else if(list.get(x).getReceiver().equals(getMyPublicKey())) {
                    System.out.println("im receiver!");
                    String msg = new sample.Messenger().readMessage(list.get(x), getMyPublicKey());
                    addLeftBubble(msg, colours.get(x));
                }
            }
        });

        System.out.println("finished loading messages");
    }

    private void addLeftBubble(String m, String c) {
        HBox hb = null;

        try {
            hb = FXMLLoader.load(getClass().getResource("/leftBubble.fxml"));
        } catch (Exception er) {
            System.out.println("err: " + er);
        }

        Circle status = (Circle) hb.getChildren().get(0);
        Text msg = (Text) hb.getChildren().get(1);

        msg.setText(m);
        if(c.equals("GREEN")) {
            status.setFill(Color.GREEN);
        }
        else if(c.equals("BLUE")) {
            status.setFill(Color.BLUE);
        }
        else if(c.equals("RED")) {
            status.setFill(Color.RED);
        }
        else {
            status.setVisible(false);
        }

        chatBox.getChildren().add(hb);
    }

    private void addRightBubble(String m, String c) {
        HBox hb = null;

        try {
            hb = FXMLLoader.load(getClass().getResource("/rightBubble.fxml"));
        } catch (Exception er) {
            System.out.println("err: " + er);
        }

        Text msg = (Text) hb.getChildren().get(0);
        Circle status = (Circle) hb.getChildren().get(1);

        msg.setText(m);
        if(c.equals("GREEN")) {
            status.setFill(Color.GREEN);
        }
        else if(c.equals("BLUE")) {
            status.setFill(Color.BLUE);
        }
        else if(c.equals("RED")) {
            status.setFill(Color.RED);
        }
        else {
            status.setVisible(false);
        }

        chatBox.getChildren().add(hb);
    }

    private void listen() {
        conversationList.setOnMouseClicked( (e) -> {
            System.out.println("user: " + selectedUser.getText() + ", pubKey: " + userPubKey);
        });

        // On "send" button press..
        sendButton.setOnAction(send -> {
            if(!msgField.getText().equals("")) {
                String text = msgField.getText();
                sample.Main.messenger.sendMessage(getMyPublicKey(), getUserPubKey(selectedUser.getText()), text, sample.Main.network);

                msgField.clear();

            }
        });

        // If user presses "ENTER" on textfield
        msgField.setOnKeyPressed(enter -> {
            if(enter.getCode().equals(KeyCode.ENTER)) {
                sendButton.fire();
            }
        });

        // On "+" button press
        startNewConversationButton.setOnAction(pressed -> {
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.WINDOW_MODAL);

            VBox vb = null;
            try{
                vb = FXMLLoader.load(getClass().getResource("/selectContact.fxml"));
            } catch(Exception e) {
                System.out.println("new convo err: " + e);
            }

            ChoiceBox contacts = (ChoiceBox)vb.getChildren().get(1);
            Button fin = (Button)vb.getChildren().get(2);
            Text tb = (Text)vb.getChildren().get(3);

            ArrayList<String> contactList = getContacts();

            ObservableList<String> list = FXCollections.observableArrayList(contactList);

            contacts.setItems(list);

            dialogStage.setScene(new Scene(vb));
            dialogStage.show();

            fin.setOnAction( (e) -> {
                if(!contacts.getSelectionModel().isEmpty()) {
                    boolean add = true;

                    for(Node n : conversationList.getChildren()) {
                        if(n.getId().equals((String)contacts.getValue())) {
                            add = false;
                        }
                    }

                    if(add) {
                        addConversationBox((String) contacts.getValue());
                        dialogStage.close();
                    }
                    else {
                        tb.setText("Conversation with this user is already active!");
                    }
                }
            });
        });

        refreshButton.setOnAction( (e) -> {
            chatBox.getChildren().clear();
            loadMessages();
        });
    }

    public ArrayList<String> getContacts() {
        DB cdb = getContactsDB();
        DBIterator iterator = cdb.iterator();
        ArrayList<String> list = new ArrayList<>();

        try {
            for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                String key = asString(iterator.peekNext().getKey());
                list.add(key);
            }

        }
        catch(Exception e) {
            System.out.println("GET ALL ERROR: " + e);
            finish(cdb);
        }
        finally {
            // Make sure you close the iterator to avoid resource leaks.
            try {
                iterator.close();
                finish(cdb);
            } catch(Exception e) {
                System.out.println("CLOSING ITERATOR ERROR: " + e);
            }
            return list;
        }
    }

    public void addConversationBox(String user) {
        try{
            HBox hb = FXMLLoader.load(getClass().getResource("/openConversations.fxml"));

            VBox vb = (VBox)hb.getChildren().get(0);
            Text u = (Text)vb.getChildren().get(0);
            Text m = (Text)vb.getChildren().get(1);
            u.setText(user);
            m.setText("");
            vb.getChildren().set(0, u);
            vb.getChildren().set(1, m);

            hb.setId(user);

            hb.setOnMouseClicked( (e) -> {
                hb.requestFocus();
                selectedUser.setText(user);
                userPubKey = getUserPubKey(user);
                chatBox.getChildren().clear();
                Platform.runLater(()-> {loadMessages();});
            });

            hb.backgroundProperty().bind( Bindings
                    .when( hb.focusedProperty() )
                    .then( focusBackground )
                    .otherwise( unfocusBackground )
            );

            conversationList.getChildren().add(0, hb);

        } catch(Exception er) {
            System.out.println("err: " + er);
        }
    }

    /** Only removes the first instance it can find, not all of them.. */
    public void removeConversationBox(String user) {
        try {
            conversationList.getChildren().forEach((node) -> {
                if (node.getId().equals(user)) {
                    conversationList.getChildren().remove(node);
                }
            });
        } catch(ConcurrentModificationException cme) {
            System.out.println("don't care..");
        }
    }

    public DB getContactsDB() {
        try {
            options.createIfMissing(true);
            contactsDB = factory.open(new File("contacts"), options);
            return contactsDB;
        } catch(IOException ioe) {
            System.out.println("GET CONTACTS_DB IOE ERROR: " + ioe);
            return null;
        }
    }

    public void add(DB database, byte[] key, byte[] value) {
        try {
            database.put(key, value);
        } finally {
            finish(database);
        }
    }

    // return asString(database.get(key));
    public byte[] read(DB database, byte[] key) {
        try {
            return database.get(key);
        } finally {
            finish(database);
        }
    }

    public void remove(DB database, byte[] key) {
        try {
            WriteOptions wo = new WriteOptions();
            database.delete(key, wo);
        } finally {
            finish(database);
        }
    }

    public void finish(DB database) {
        try {
            database.close();
        } catch(IOException ioe) {
            System.out.println("FINISH IOE ERROR: " + ioe);
        }
    }


}
