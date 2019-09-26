package sample;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.commons.lang3.SerializationUtils;

public class Main extends Application {

    // Static objects so they remain the same through all classes
    static sample.Controller mainController;
    static sample.Network network = new sample.Network();
    static sample.Messenger messenger = new sample.Messenger();
    static sample.Mining mining = new sample.Mining();
    static sample.Encryption encryption = new sample.Encryption();

    /** Setups JavaFX and opens the window */
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

    public static void main(String[] args) {
        // Updates chaintip data inside indexDB before closing app
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                System.out.println("updating chaintip data..");
                if(network.getChain().getChainTip() != null) {
                    network.getChain().add(network.getChain().getIndexDB(), "chaintip".getBytes(), SerializationUtils.serialize(network.getChain().getChainTip()));
                    System.out.println("chaintip data updated..");
                }
            }
        }));

        launch(args);
    }
}