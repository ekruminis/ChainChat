package sample;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.Range;
import org.iq80.leveldb.impl.FileMetaData;
import org.jetbrains.annotations.NotNull;

import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.security.Key;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.iq80.leveldb.impl.Filename.FileType.LOG;
import static org.iq80.leveldb.impl.Iq80DBFactory.*;

// extends Application
public class Main extends Application {

    static sample.Controller mainController;

    static sample.Network network = new sample.Network();
    static sample.Messenger messenger = new sample.Messenger();
    static sample.Mining mining = new sample.Mining();
    static sample.Encryption encryption = new sample.Encryption();

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/sample.fxml"));
        Parent root = fxmlLoader.load();
        mainController = (sample.Controller)fxmlLoader.getController();

        primaryStage.setTitle("ChainChat");
        primaryStage.setScene(new Scene(root, 1024, 721));
        //primaryStage.setResizable(false);
        primaryStage.setMinWidth(1063);
        primaryStage.setMinHeight(760);
        primaryStage.show();
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                System.exit(0);
            }
        });
    }

    public static void main(String[] args) throws InterruptedException, IOException, ParseException {
        // TODO something on shutdown? eg. delete messages.txt
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                System.out.println("updating chaintip data..");
                if(network.getChain().getChainTip() != null) {
                    network.getChain().add(network.getChain().getIndexDB(), "chaintip".getBytes(), SerializationUtils.serialize(network.getChain().getChainTip()));
                    System.out.println("chaintip data updated..");
                }
            }
        }));

//        long count  = 0;
//        for(int x = 0; x < 1; x++) {
//            sample.Block b = new sample.Block(1, sample.Block.makeDate(), "oldHash", 0, 300, "merkle", 10);
//            sample.Block b2 = mining.mineBlock(b);
//            System.out.println("finished with nonce: " + b2.getNonce());
//            count = count + b2.getNonce();
//        }
//
//        System.out.println("average: " + count);

        //System.exit(0);
        launch(args);

//        factory.destroy(new File("blocks0"), new Options());
//        factory.destroy(new File("chain"), new Options());
//        factory.destroy(new File("index"), new Options());
//        System.out.println("finished destroying");

        //m.sendMessage("Alice", "Bob", "hello", n);


        // TODO JSON test
//        ObjectMapper om = new ObjectMapper();
//        sample.Block b = new sample.Block("hallo");
//        System.out.println("JSON:");
//        System.out.println(om.writeValueAsString(b));
//        System.out.println("JSON end:");

        // TODO fetching test
//        for (; ; ) {
//            mine.fetchMessages(n.getMessagesFile());
//            Thread.sleep(2000);
//        }
    }
}