package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
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

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("ChainChat");
        primaryStage.setScene(new Scene(root, 300, 275));
        primaryStage.show();
    }

    public static void main(String[] args) throws InterruptedException, IOException, ParseException {
        //launch(args);
        //sample.Network n = new sample.Network();
        sample.Messenger m = new sample.Messenger();
        sample.Mining mine = new sample.Mining();
        sample.Blockchain b = new sample.Blockchain();

        LinkedList<String> msg = new LinkedList<>();
        msg.add("aa");
        msg.add("bb");
        msg.add("cc");
        msg.add("dd");
        msg.add("ee");

        System.out.println(mine.genMerkleRoot(msg));


                //"192.168.1.11" "86.40.62.40"
        //n.connect("192.168.1.11", 4001);
        //m.sendMessage("Alice", "Bob", "hello", n);

//        String date1 = sample.Block.makeDate();
//        System.out.println(date1);
//        Thread.sleep(1000);
//        String date2 = sample.Block.makeDate();
//        System.out.println(date2);
//
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
//        Date d1 = sdf.parse(date1);
//        Date d2 = sdf.parse(date2);
//
//        if(d1.after(d2)) {
//            System.out.println(date1 + " > " + date2);
//            System.out.println("fucking lies");
//        }
//        if(d1.before(d2)) {
//            System.out.println(date1 + " < " + date2);
//            System.out.println("true, yeahs that pretty true");
//        }

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

        // TODO something on shutdown? eg. delete messages.txt
//        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
//            public void run() {
//                // what you want to do
//            }
//        }));
    }
}