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
public class Main {

//    @Override
//    public void start(Stage primaryStage) throws Exception{
//        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
//        primaryStage.setTitle("ChainChat");
//        primaryStage.setScene(new Scene(root, 300, 275));
//        primaryStage.show();
//    }

    public static void main(String[] args) throws InterruptedException, IOException, ParseException {
        //launch(args);
        sample.Network n = new sample.Network();
        sample.Messenger m = new sample.Messenger();
        sample.Mining mine = new sample.Mining();
        sample.Encryption e = new sample.Encryption();

        int port = Integer.parseInt(args[0]);

        sample.Blockchain bc = new sample.Blockchain();
        factory.destroy(new File("blocks0"), new Options());
        factory.destroy(new File("chain"), new Options());
        factory.destroy(new File("index"), new Options());

        Scanner reader = new Scanner(System.in);
                    while (true) {
                        if (reader.next().equals("connect")) {
                            new Thread(() -> {
                                System.out.println("cmd - connect");
                                n.connect("192.168.1.15", port);
                            }).start();
                        }

                        if (reader.next().equals("msg")) {
                            new Thread(() -> {
                                System.out.println("cmd - msg");
                                m.sendMessage("Alice", "Bob", "hello", n);
                                System.out.println("msg finished");
                            }).start();
                        }

                        if (reader.next().equals("mine")) {
                            new Thread(() -> {
                                System.out.println("cmd - mine");
                                mine.fetchMessages(n.getMessagesFile());
                                sample.Block block = mine.createBlock(n.getChain());
                                sample.Block minedBlock = mine.mineBlock(block);
                                n.getChain().storeBlock(mine.hash(minedBlock), minedBlock);
                                n.announce(minedBlock);
                            }).start();
                        }

                        if (reader.next().equals("showall1")) {
                            new Thread(() -> {
                                System.out.println("cmd - showall");
                                n.getChain().getAll();
                                System.out.println("showall finished");
                            }).start();
                        }

                        if (reader.next().equals("msg2")) {
                            new Thread(() -> {
                                System.out.println("cmd - msg");
                                m.sendMessage("sender", "receiver", "hello again", n);
                                System.out.println("msg finished");
                            }).start();
                        }

                        if (reader.next().equals("mine2")) {
                            new Thread(() -> {
                                System.out.println("cmd - mine");
                                mine.fetchMessages(n.getMessagesFile());
                                sample.Block block = mine.createBlock(n.getChain());
                                sample.Block minedBlock = mine.mineBlock(block);
                                n.getChain().storeBlock(mine.hash(minedBlock), minedBlock);
                                n.announce(minedBlock);
                            }).start();
                        }

                        if (reader.next().equals("showall2")) {
                            new Thread(() -> {
                                System.out.println("cmd - showall");
                                n.getChain().getAll();
                                System.out.println("showall finished");
                            }).start();
                        }
                    }

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

        // TODO something on shutdown? eg. delete messages.txt
//        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
//            public void run() {
//                // what you want to do
//            }
//        }));
    }
}