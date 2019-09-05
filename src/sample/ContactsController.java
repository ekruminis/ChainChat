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
import org.apache.commons.lang3.SerializationUtils;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteOptions;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedList;

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

    public void initialize() {
        System.out.println("opened contacts..");

        reloadList();

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

            cancel.setOnAction(c -> {
                dialogStage.close();
            });

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

    public void reloadList() {
        contactsList.getChildren().clear();

        DB cdb = getContactsDB();
        DBIterator iterator = cdb.iterator();
        try {
            for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
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

                remove.setOnAction( (e3) -> {
                    remove(getContactsDB(), user.getText().substring(0, user.getText().length()-1).getBytes());
                    reloadList();
                });

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

                    update.setOnAction( (e2) -> {
                        remove(getContactsDB(), chosenUser.getBytes());

                        add(getContactsDB(), tf1.getText().getBytes(), tf2.getText().getBytes());
                        reloadList();
                        dialogStage.close();
                    });

                });

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
