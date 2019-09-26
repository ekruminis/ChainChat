package sample;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteOptions;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import static org.iq80.leveldb.impl.Iq80DBFactory.asString;
import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

public class ContactsController {

    @FXML
    private VBox contactsList;

    @FXML
    private Button newContactButton;
    @FXML
    private Button getMyPK;

    private Options options = new Options();
    private DB contactsDB;

    /** Setup the contacts screen */
    public void initialize() {
        // Refresh the contacts list
        reloadList();

        // On 'add' press, open up new window where the user can input the details
        newContactButton.setOnAction( (e) -> {
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.WINDOW_MODAL);

            GridPane gp = null;
            try{
                gp = FXMLLoader.load(getClass().getResource("/startNewConversation.fxml"));
            } catch(Exception er) {
                System.out.println("new convo err: " + er);
            }

            VBox v1 = (VBox)gp.getChildren().get(0);
            HBox hb1 = (HBox)gp.getChildren().get(1);
            Button cancel = (Button)hb1.getChildren().get(1);

            HBox hb2 = (HBox)hb1.getChildren().get(2);
            Button add = (Button)hb2.getChildren().get(0);

            dialogStage.setScene(new Scene(gp));
            dialogStage.show();

            // Close the window
            cancel.setOnAction(c -> {
                dialogStage.close();
            });

            // Read user input, add the new contact to contactsDB, and refresh the contacts list
            add.setOnAction(a -> {
                HBox h1 = (HBox)v1.getChildren().get(1);
                TextField tf1 = (TextField)h1.getChildren().get(1);
                String alias = tf1.getText();

                HBox h2 = (HBox)v1.getChildren().get(2);
                TextField tf2 = (TextField)h2.getChildren().get(1);
                String pubKey = tf2.getText();

                //add to contactsDB
                add(getContactsDB(), alias.getBytes(), pubKey.getBytes());
                dialogStage.close();
                reloadList();
            });
        });

        // Opens up new window that contains the public key of the user
        getMyPK.setOnAction( (e2) -> {
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.WINDOW_MODAL);

            TextArea box = null;
            try{
                box = FXMLLoader.load(getClass().getResource("/myPublicKeyBox.fxml"));
            } catch(Exception er) {
                System.out.println("contact box error: " + er);
            }

            box.setText( Base64.getEncoder().encodeToString( sample.Main.encryption.getRSAPublic().getEncoded() ) );

            dialogStage.setScene(new Scene(box));
            dialogStage.show();
        });
    }

    /** Loop over contacts database and add any contacts to the list */
    public void reloadList() {
        // clear list first..
        contactsList.getChildren().clear();

        DB cdb = getContactsDB();
        DBIterator iterator = cdb.iterator();
        try {
            // loop over DB
            for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                // Get key and val
                String key = asString(iterator.peekNext().getKey());
                String value = asString(iterator.peekNext().getValue());

                VBox vb = null;
                try{
                    vb = FXMLLoader.load(getClass().getResource("/contactBox.fxml"));
                } catch(Exception er) {
                    System.out.println("contact box error: " + er);
                }

                HBox hb1 = (HBox)vb.getChildren().get(0);
                Text user = (Text)hb1.getChildren().get(0);
                TextArea pubKey = (TextArea)hb1.getChildren().get(1);

                VBox vbb2 = (VBox)hb1.getChildren().get(2);
                Button edit = (Button)vbb2.getChildren().get(0);
                Button remove = (Button)vbb2.getChildren().get(1);

                // Removes the current data from the Db
                remove.setOnAction( (e3) -> {
                    remove(getContactsDB(), user.getText().substring(0, user.getText().length()-1).getBytes());
                    reloadList();
                });

                // Opens up a new screen where the user can update the contact details
                edit.setOnAction( (e) -> {
                    Stage dialogStage = new Stage();
                    dialogStage.initModality(Modality.WINDOW_MODAL);

                    String chosenUser = user.getText().substring(0, user.getText().length()-1);

                    VBox vb2 = null;
                    try{
                        vb2 = FXMLLoader.load(getClass().getResource("/editContact.fxml"));
                    } catch(Exception er) {
                        System.out.println("contact box error: " + er);
                    }

                    HBox hbb1 = (HBox)vb2.getChildren().get(0);
                    TextField tf1 = (TextField)hbb1.getChildren().get(1);
                    tf1.setText(user.getText().substring(0, user.getText().length()-1));

                    HBox hbb2 = (HBox)vb2.getChildren().get(1);
                    TextField tf2 = (TextField)hbb2.getChildren().get(1);
                    tf2.setText(pubKey.getText());

                    Button update = (Button)vb2.getChildren().get(2);

                    dialogStage.setScene(new Scene(vb2));
                    dialogStage.show();

                    // Remove current object and set the new updated one
                    update.setOnAction( (e2) -> {
                        remove(getContactsDB(), chosenUser.getBytes());

                        add(getContactsDB(), tf1.getText().getBytes(), tf2.getText().getBytes());
                        reloadList();
                        dialogStage.close();
                    });

                });

                // Set info and add to scene
                user.setText(key + ":");
                pubKey.setText(value);
                contactsList.getChildren().add(vb);
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
