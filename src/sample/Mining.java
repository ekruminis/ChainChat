package sample;

import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Mining {
    public TreeSet<sample.Message> m = new TreeSet<sample.Message>();
    private int numMessages = 0;

    /** Reads messages from file and adds them to a TreeSet for future use
     * @param f The file that contains the message objects */
    public void fetchMessages(File f) {
        try {
            // Open file
            FileInputStream fis = new FileInputStream(f);
            ObjectInputStream ois = new ObjectInputStream(fis);

            // Read messages and add them to TreeSet (no duplicates)
            Object obj = null;
            while((obj = ois.readObject()) != null) {
                sample.Message msg = (sample.Message)obj;
                if(!m.contains(msg)) m.add(msg);
                ois = new ObjectInputStream(fis);
            }

            fis.close();
        } catch(EOFException eof) {
            //System.out.println(m.toString());

            // Update number of messages in file (error validation)
            numMessages = m.size();

        } catch(Exception e) {
            System.out.println("FETCHING ERROR: " + e);
        }
    }

    /** Hashes a block and returns its hash as String (SHA256)
     * @param b The block object we want to hash
     * @return Returns the hash of the block object as a String
     */
    public String hash(sample.Block b) {
        String hashtext;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(b.toString().getBytes());
            BigInteger numb = new BigInteger(1, bytes);
            hashtext = numb.toString(16);
            while (hashtext.length() < 64) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        }
        catch(NoSuchAlgorithmException nsae) {
            System.out.println("HASHING ERROR: " + nsae);
            return null;
        }
    }

    /** Hashes a block header and returns its hash as String (SHA256)
     * @param header The block header object we want to hash
     * @return Returns the hash of the block header object as a String
     */
    public String hash(sample.indexBlock header) {
        String hashtext;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(header.toString().getBytes());
            BigInteger numb = new BigInteger(1, bytes);
            hashtext = numb.toString(16);
            while (hashtext.length() < 64) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        }
        catch(NoSuchAlgorithmException nsae) {
            System.out.println("HASHING ERROR: " + nsae);
            return null;
        }
    }

    /** Hashes a String and returns its hash as String (SHA256)
     * @param s The String we want to hash
     * @return Returns the hash of the String object as a String
     */
    public String hash(String s) {
        String hashtext;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(s.getBytes());
            BigInteger numb = new BigInteger(1, bytes);
            hashtext = numb.toString(16);
            while (hashtext.length() < 64) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        }
        catch(NoSuchAlgorithmException nsae) {
            System.out.println("HASHING ERROR: " + nsae);
            return null;
        }
    }

    /** Creates a block with the current messages from the messages file
     * @param chain The blockchain class object from which we gather information
     * @return Returns the created (**NOT MINED**) block object
     */
    public sample.Block createBlock(sample.Blockchain chain) {
        // Generate merkle root of messages
        String mr = genMerkleRoot(m);

        // Get currently difficulty level
        long difficulty = chain.getDifficulty();

        // Create block and adds its messages to block object
        sample.Block block = new sample.Block(mr, chain.getCurrentIndex()+1, chain.getCurrentHash(), difficulty, chain.getCurrentDifficultyTotal()+difficulty);
        System.out.println("block created: " + block.toString());
        block.setMessages(m);

        return block;
    }

    /** Mines the block until the difficulty level is satisfied, then returns the block
     * @param b The unmined block object
     * @return Returns a successfully mined block
     */
    public sample.Block mineBlock(sample.Block b) {
        // formula: (2^256) / (2^ (256-(4*difficultyLevel))) -> avg. number of hashes required..
        long hashRate = b.getDifficultyLevel();

        // Calculate how many 0's the hash should start with based on the current difficulty level
        final int lvl;

        // Minimum 0's is set at 1, else calculate what it should be
        if( ((int) ((Math.log10(hashRate) / Math.log10(2) ) / 4)) < 1) {
            lvl = 1;
        }
        else {
            lvl = (int) ((Math.log10(hashRate) / Math.log10(2) ) / 4);
        }

        String target = new String(new char[lvl]).replace('\0', '0');
        System.out.println("target: " + target);
        System.out.println("starting to mine..");

        // Show the user the hashing process in a separate window
        Stage dialogStage = new Stage();
        dialogStage.setMinHeight(400);
        dialogStage.setMinWidth(800);
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.setTitle("Mining console: Current difficulty (" + target + "), avg. number of hashes required = " + hashRate);
        TextArea ta = new TextArea("STARTING MINING PROCESS...");
        ta.setEditable(false);

        dialogStage.setScene(new Scene(ta));
        dialogStage.show();

        // Increase nonce value until the proof-of-work challenge is solved..
        while (!hash(b).substring(0, lvl).equals(target)) {
            String data = ("\n nonce: " + b.nonce + ",         hash: " + hash(b));

            // Append hash info
            updateInfo(data, ta);
            System.out.println(data);

            b.nonce++;
        }
        ta.appendText("\nFINISHED!!!");

        return b;
    }

    /** Appends text to a TextArea object
     * @param s The message to add
     * @param ta The TextArea UI object
     */
    private void updateInfo(String s, TextArea ta) {
        ta.appendText(s);
    }

    /** Checks if the block has been successfully mined
     * @param b The block object we want to validate
     * @return Returns whether the block is successfully mined (true) or not (false)
     */
    public boolean verifyMined(sample.Block b) {
        // formula: (2^256) / (2^ (256-(4*difficultyLevel))) -> avg. number of hashes required..
        long hashRate = b.getDifficultyLevel();

        // Calculate how many 0's the hash should start with based on the current difficulty level (minimum should be 1)
        int lvl = (int) ((Math.log10(hashRate) / Math.log10(2) ) / 4);
        if(lvl < 1) {
            lvl = 1;
        }

        // Check if block is mined
        String target = new String(new char[lvl]).replace('\0', '0');
        if(hash(b).substring(0,lvl).equals(target)) return true;
        else return false;
    }

    /** Checks if the block header has been successfully mined -> creates Block object from header data
     * @param header The block header object we want to validate
     * @return Returns whether the block is successfully mined (true) or not (false)
     */
    public boolean verifyMined(sample.indexBlock header) {
        // Create block objetc from header data
        sample.Block b = new sample.Block(header.getIndex(), header.getDate(), header.getPrevHash(), header.getNonce(), header.getDifficultyLevel(), header.getMerkleRoot(), header.getTotalDifficulty());

        // formula: (2^256) / (2^ (256-(4*difficultyLevel))) -> avg. number of hashes required..
        long hashRate = b.getDifficultyLevel();

        // Calculate how many 0's the hash should start with based on the current difficulty level (minimum should be 1)
        int lvl = (int) ((Math.log10(hashRate) / Math.log10(2) ) / 4);
        if(lvl < 1) {
            lvl = 1;
        }

        // Check if block is mined
        String target = new String(new char[lvl]).replace('\0', '0');
        if(hash(b).substring(0,lvl).equals(target)) return true;
        else return false;
    }

    /** Hashes the Merkle tree children to make parents
     * @param children The children hashes
     * @return Returns the hashed children pairs
     */
    private LinkedList<String> genParentHash(LinkedList<String> children) {
        LinkedList<String> parents = new LinkedList<>();

        // If number of children is odd, add the last child again to make it even
        if( (children.size() != 1) && (children.size() % 2 != 0) ) {
            children.add(children.getLast());
        }

        // Hash the children pairs to make a new parent, and add it to a new LinkedList
        for(int x = 0; x < children.size(); x=x+2) {
           String left = children.get(x);
           String right = children.get(x+1);

           parents.add(hash(left+right));
        }

        return parents;
    }

    /** Generates MerkleRoot of all messages
     * @param mlist The list of messages
     * @return Returns the root hash of the Merkle tree
     */
    public String genMerkleRoot(TreeSet<sample.Message> mlist) {
        LinkedList<String> hashes = new LinkedList<>();

        // If no messages are present, we have nothing to work with
        if(mlist.isEmpty()) {
            return "empty";
        }

        // If number of messages is odd, add the last message again to make it even
        if(mlist.size() % 2 != 0) {
            mlist.add(mlist.last());
        }

        // Hash each message object and add it to a LinkedList
        for (sample.Message message : mlist) {
            hashes.add(hash(message.toString()));
        }

        // Hash the children pairs until only one hash (the root) remains
        while(hashes.size() != 1) {
            hashes = genParentHash(hashes);
        }

        return hashes.get(0);
    }

    // TODO MerkleTree verification
}