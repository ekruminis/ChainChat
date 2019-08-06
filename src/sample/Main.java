package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.codehaus.jackson.map.ObjectMapper;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.Key;
import java.util.Base64;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("ChainChat");
        primaryStage.setScene(new Scene(root, 300, 275));
        primaryStage.show();
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        //launch(args);
        sample.Network n = new sample.Network();
        sample.Messenger m = new sample.Messenger();
        sample.Mining mine = new sample.Mining();

        n.connect("192.168.1.11", 4001);
        m.sendMessage("BAAIfdf", "JDIASIO", "test", n);

//        ObjectMapper om = new ObjectMapper();
//        sample.Block b = new sample.Block("hallo");
//        System.out.println("JSON:");
//        System.out.println(om.writeValueAsString(b));
//        System.out.println("JSON end:");

        for (; ; ) {
            mine.fetchMessages(n.getMessagesFile());
            Thread.sleep(2000);
        }

        // TODO something on shutdown? eg. delete messages.txt
//        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
//            public void run() {
//                // what you want to do
//            }
//        }));
    }
}
