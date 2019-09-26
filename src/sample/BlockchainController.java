package sample;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import java.io.PrintWriter;
import java.util.TreeSet;
import static sample.Main.mining;
import static sample.Main.network;

public class BlockchainController {

    @FXML
    private Button mineButton;

    /** Setups the blockchain screen */
    public void initialize() {
        System.out.println("blockchain opened..");

        // When 'mine' button is pressed, start mining process
        mineButton.setOnAction( (e) -> {
            Platform.runLater(() -> {
                System.out.println("mining..");
                mineButton.setText("Mining.. please wait");

                mineButton.setDisable(true);
                mining.fetchMessages(network.getMessagesFile());

                Platform.runLater(() -> {
                    sample.Block block = mining.createBlock(network.getChain());
                    sample.Block minedBlock = mining.mineBlock(block);
                    try {
                        new PrintWriter(network.getMessagesFile()).close();
                        mining.m =  new TreeSet<sample.Message>();
                    } catch(Exception er) {
                        System.out.println("FAIL DELETING MSG FILE CONTENTS: " + er);
                    }
                    System.out.println("msgs in mine button: " + minedBlock.getMessages().size());
                    network.getChain().storeBlock(mining.hash(minedBlock), minedBlock);
                    network.announce(minedBlock);
                });

                mineButton.setText("Mine");
                mineButton.setDisable(false);
            });
        });
    }

}
