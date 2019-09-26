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
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteOptions;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.iq80.leveldb.impl.Iq80DBFactory.asString;
import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

public class MessagesController {

    @FXML
    private VBox conversationList;
    @FXML
    public Text selectedUser;
    @FXML
    private Button refreshButton;

    @FXML
    public VBox chatBox;
    @FXML
    private TextField msgField;
    @FXML
    private Button sendButton;

    @FXML
    private Button startNewConversationButton;

    private String userPubKey = null;

    // styles used for highlighting vboxes
    private final Background focusBackground = new Background( new BackgroundFill( Color.web("#d1cfcf"), CornerRadii.EMPTY, Insets.EMPTY ) );
    private final Background unfocusBackground = new Background( new BackgroundFill( Color.web("#ffffff"), CornerRadii.EMPTY, Insets.EMPTY ) );

    private Options options = new Options();
    private DB contactsDB;

    public void initialize() {
        System.out.println("messages opened..");
        loadConversations();

        listen();
    }

    /** Returns the users public key
     * @return Returns the public key of the current user
     */
    private String getMyPublicKey() {
        return Base64.getEncoder().encodeToString( sample.Main.encryption.getRSAPublic().getEncoded() );
    }

    /** Returns selected users public key by querying the contactsDB
     * @param u The username of the user
     * @return Returns the public key of the chosen user
     */
    private String getUserPubKey(String u) {
        userPubKey = asString(read(getContactsDB(), u.getBytes()));
        return userPubKey;
    }

    /** Searches the contactsDB for the username of a user by their public key
     * @param pk The public key of the user
     * @return The username of the user with that public key
     */
    public String searchContacts(String pk) {
        DB cdb = getContactsDB();
        DBIterator iterator = cdb.iterator();
        String user = null;

        try {
            for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                String key = asString(iterator.peekNext().getKey());

                if(asString(iterator.peekNext().getValue()).equals(pk)) {
                    user = key;
                    break;
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

    /** Searches through the active chain to find any active conversation the current user is having */
    public void loadConversations() {
        conversationList.getChildren().clear();

        // Search through the user to find the users public keys and the last messages in those conversations
        System.out.println("starting to load conversations..");
        sample.ConversationsList convos = sample.Main.network.getChain().getChats("me");
        LinkedList<String> list = convos.getUsersList();
        LinkedList<sample.Message> messages = convos.getMessagesList();

        // Loop through list to find more information
        for(int x=0;x<list.size();x++) {
            // Find the username of the user
            String user = searchContacts(list.get(x));

            // If we have this user listed in our contacts database..
            if(user != null && !user.isEmpty()) {
                System.out.println("we know this user from our contactsDB..");
                String msg = "";

                // Set the 'last message' portion of the UI
                if(messages.get(x).getSender().equals(getMyPublicKey())) {
                    msg = sample.Main.messenger.readMessage(messages.get(x), getMyPublicKey());
                    msg = "me: " + msg;
                }

                // Set the 'last message' portion of the UI
                else if(messages.get(x).getReceiver().equals(getMyPublicKey())) {
                    msg = sample.Main.messenger.readMessage(messages.get(x), getMyPublicKey());
                    msg = user + ": " + msg;
                }

                // Add box to UI
                addConversationBox(user, msg);
            }

            // If we don't know this user, assign a random name to it
            else {
                System.out.println("we don't know this user..");
                int count = 1;
                boolean end = false;

                // Loop until we find an unused name in the format of 'unknownX', eg. 'unknown1', then 'unknown2', 'unknown3', etc.
                while(!end) {
                    String u = asString(read(getContactsDB(), ("unknown"+count).getBytes()));
                    if(u == null) {
                        end = true;
                    }
                    count++;
                }

                // Add this unknown user to our contacts
                add(getContactsDB(), ("unknown"+count).getBytes(), list.get(x).getBytes());
                String msg = "";

                // Set the 'last message' portion of the UI
                if(messages.get(x).getSender().equals(getMyPublicKey())) {
                    msg = sample.Main.messenger.readMessage(messages.get(x), getMyPublicKey());
                    msg = "me: " + msg;
                }

                // Set the 'last message' portion of the UI
                else if(messages.get(x).getReceiver().equals(getMyPublicKey())) {
                    msg = sample.Main.messenger.readMessage(messages.get(x), getMyPublicKey());
                    msg = user + ": " + msg;
                }

                // Add box to UI
                addConversationBox(("unknown"+count), msg);
            }
        }

        System.out.println("finished loading conversations");
    }

    /** Searches through the active chain for any messages between the users */
    public void loadMessages() {
        System.out.println("starting to load messages..");

        System.out.println("chaintip is: " + sample.Main.network.getChain().getChainTip());

        // Get messages and their state between the current user and the selected user
        sample.MessagesList ml = sample.Main.network.getChain().getMessages(selectedUser.getText(), userPubKey);

        // The message objects
        LinkedList<sample.Message> list = new LinkedList<>(ml.getMessagesList());
        // The state of those messages
        LinkedList<String> colours = new LinkedList<>(ml.getColoursList());

        // Sort messages by date
        list.sort(Comparator.comparing(sample.Message::getDate));

        Platform.runLater(() -> {
            for(int x = 0; x < list.size(); x++) {
                // User is the sender, so add right-sided bubble
                if(list.get(x).getSender().equals(getMyPublicKey())) {
                    // Check if message is valid and decrypt it
                    String msg = new sample.Messenger().readMessage(list.get(x), getMyPublicKey());
                    addRightBubble(msg, colours.get(x), list.get(x));
                }
                // User is the receiver, so add left-sided bubble
                else if(list.get(x).getReceiver().equals(getMyPublicKey())) {
                    // Check if message is valid and decrypt it
                    String msg = new sample.Messenger().readMessage(list.get(x), getMyPublicKey());
                    addLeftBubble(msg, colours.get(x), list.get(x));
                }
            }
        });

        System.out.println("finished loading messages");
    }

    /** Adds the message and its state on the left side of the messages UI box
     * @param m The message itself
     * @param c The state of the message (eg. red/green/blue)
     * @param info The metadata of the message
     */
    private void addLeftBubble(String m, String c, sample.Message info) {
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

        // Add metadata that can be seen by hovering over message
        Tooltip tt = new Tooltip(info.toString());
        tt.setPrefWidth(850);
        tt.setWrapText(true);
        Tooltip.install(hb, tt);

        chatBox.getChildren().add(hb);
    }

    /** Adds the message and its state on the right side of the messages UI box
     * @param m The message itself
     * @param c The state of the message (eg. red/green/blue)
     * @param info The metadata of the message
     */
    private void addRightBubble(String m, String c, sample.Message info) {
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

        // Add metadata that can be seen by hovering over message
        Tooltip tt = new Tooltip(info.toString());
        tt.setPrefWidth(850);
        tt.setWrapText(true);
        Tooltip.install(hb, tt);

        chatBox.getChildren().add(hb);
    }

    /** Initialises the UI elements */
    private void listen() {
        // On "send" button press, create message object and transmit it
        sendButton.setOnAction(send -> {
            if(!msgField.getText().equals("")) {
                String text = msgField.getText();
                sample.Main.messenger.sendMessage(getMyPublicKey(), getUserPubKey(selectedUser.getText()), text, sample.Main.network);

                msgField.clear();

            }
        });

        // If user presses "ENTER" on textfield, send the message
        msgField.setOnKeyPressed(enter -> {
            if(enter.getCode().equals(KeyCode.ENTER)) {
                sendButton.fire();
            }
        });

        // On "+" button press, allow user to start new conversation from a list of contacts
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

            // Add contacts to list
            ArrayList<String> contactList = getContacts();
            ObservableList<String> list = FXCollections.observableArrayList(contactList);
            contacts.setItems(list);

            dialogStage.setScene(new Scene(vb));
            dialogStage.show();

            // If finished, open conversation
            fin.setOnAction( (e) -> {
                if(!contacts.getSelectionModel().isEmpty()) {
                    boolean add = true;

                    for(Node n : conversationList.getChildren()) {
                        if(n.getId().equals((String)contacts.getValue())) {
                            add = false;
                        }
                    }

                    if(add) {
                        addConversationBox((String) contacts.getValue(), "");
                        dialogStage.close();
                    }
                    else {
                        tb.setText("Conversation with this user is already active!");
                    }
                }
            });
        });

        // Reload the messages list
        refreshButton.setOnAction( (e) -> {
            if(!selectedUser.getText().equals("")) {
                chatBox.getChildren().clear();
                loadMessages();
            }
        });
    }

    /** Loops through contactsDB to find all the saved contact user has
     * @return Returns the public keys of the contacts in an ArrayList of Strings
     */
    public ArrayList<String> getContacts() {
        DB cdb = getContactsDB();
        DBIterator iterator = cdb.iterator();
        ArrayList<String> list = new ArrayList<>();

        try {
            // Loop through DB and add keys
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

    /** Creates a conversation box which is used for currently active conversations
     * @param user The username of the user
     * @param message The last transmitted message between us and this user
     */
    public void addConversationBox(String user, String message) {
        try{
            HBox hb = FXMLLoader.load(getClass().getResource("/openConversations.fxml"));

            VBox vb = (VBox)hb.getChildren().get(0);
            Text u = (Text)vb.getChildren().get(0);
            Text m = (Text)vb.getChildren().get(1);
            u.setText(user);
            m.setText(message);
            vb.getChildren().set(0, u);
            vb.getChildren().set(1, m);

            hb.setId(user);

            // On box click, load the messages between us and this user
            hb.setOnMouseClicked( (e) -> {
                hb.requestFocus();
                selectedUser.setText(user);
                userPubKey = getUserPubKey(user);
                chatBox.getChildren().clear();
                Platform.runLater(()-> {loadMessages();});
            });

            // Highlight box if it is clicked on..
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

    /** Removes the conversation box from the UI
     * NOTE: Only removes the first instance it can find, not all of them..
     * @param user The username of the user*/
    public void removeConversationBox(String user) {
        try {
            conversationList.getChildren().forEach((node) -> {
                if (node.getId().equals(user)) {
                    conversationList.getChildren().remove(node);
                }
            });
        } catch(ConcurrentModificationException cme) {
            System.out.println("REMOVE BOX ERR: " + cme);
        }
    }

    /** Gets the contacts database or creates one if it doesn't already exist
     * @return Returns the contacts database */
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

    /** Stores a key:value pair inside a specified database
     * @param database The database we want to use
     * @param key The key of the object as a byte[]
     * @param value The value of the object as a byte[]
     */
    public void add(DB database, byte[] key, byte[] value) {
        try {
            database.put(key, value);
        } finally {
            finish(database);
        }
    }

    /** Returns the value inside a specified database using its key as a byte[]
     * @param database The database we want to use
     * @param key The key of the object as a byte[]
     * @return Returns the value of the object associated with that key */
    public byte[] read(DB database, byte[] key) {
        try {
            return database.get(key);
            // return asString(database.get(key)) -> returns val as a String representation
        } finally {
            finish(database);
        }
    }

    /** Removes the key:value pair inside a specified database
     * @param database The database we want to use
     * @param key The key of the object as a byte[]
     */
    public void remove(DB database, byte[] key) {
        try {
            WriteOptions wo = new WriteOptions();
            database.delete(key, wo);
        } finally {
            finish(database);
        }
    }

    /** Closes the specified database (necessary for multithreading)
     * @param database The database we want to use
     */
    public void finish(DB database) {
        try {
            database.close();
        } catch(IOException ioe) {
            System.out.println("FINISH IOE ERROR: " + ioe);
        }
    }


}
