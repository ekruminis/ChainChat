package sample;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Mining {
    // TODO uploadBlock()   - distribute mined block

    public TreeSet<sample.Message> m = new TreeSet<sample.Message>();
    private int numMessages = 0;

    /** Reads messages from file and prints them */
    public void fetchMessages(File f) {
        try {
            FileInputStream fis = new FileInputStream(f);
            ObjectInputStream ois = new ObjectInputStream(fis);

            Object obj = null;
            while((obj = ois.readObject()) != null) {
                sample.Message msg = (sample.Message)obj;
                if(!m.contains(msg)) m.add(msg);
                ois = new ObjectInputStream(fis);
            }

            fis.close();
        } catch(EOFException eof) {
            //System.out.println(m.toString());
            numMessages = m.size();

        } catch(Exception e) {
            System.out.println("FETCHING ERROR: " + e);
        }
    }

    /** Hashes a block and returns its hash as String */
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

    /** Hashes a block header and returns its hash as String */
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

    /** Hashes a String and returns its hash as String */
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

    /** Creates a block with the current messages from file */
    // TODO fix "difficulty" input
    public sample.Block createBlock(sample.Blockchain chain) {
        System.out.println("CREATING BLOCK..");
        System.out.println("generating merkle root");
        String mr = genMerkleRoot(m);
        System.out.println("merkle gened: " + mr);

        long difficulty = chain.getDifficulty();
        System.out.println("difficulty is: " + difficulty);

        sample.Block block = new sample.Block(mr, chain.getCurrentIndex()+1, chain.getCurrentHash(), difficulty, chain.getCurrentDifficultyTotal()+difficulty);
        System.out.println("block created: " + block.toString());
        block.setMessages(m);
        System.out.println("messages count: " + m.size());

        return block;
    }

    /** Mines the block until the difficulty level is satisfied, then returns the block */
    public sample.Block mineBlock(sample.Block b) {

        // TODO (recalculate no. of 0's hash should start from difficulty level)
        long hashRate = b.getDifficultyLevel();
        // formula: (2^256) / (2^ (256-(4*difficultyLevel))) -> avg. number of hashes required..
        int lvl = (int) ((Math.log10(hashRate) / Math.log10(2) ) / 4);

        if(lvl < 1) {
            lvl = 1;
        }

        String target = new String(new char[lvl]).replace('\0', '0');
        System.out.println("target: " + target);
        System.out.println("starting to mine..");
        while(!hash(b).substring(0,lvl).equals(target)) {
            System.out.println("n: " + b.nonce + "," + hash(b) + ",    len=" + hash(b).length());
            b.nonce++;
        }
        return b;
    }

    /** Checks if the block has been successfully mined*/
    public boolean verifyMined(sample.Block b) {
        long hashRate = b.getDifficultyLevel();
        // formula: (2^256) / (2^ (256-(4*difficultyLevel))) -> avg. number of hashes required..
        int lvl = (int) ((Math.log10(hashRate) / Math.log10(2) ) / 4);

        if(lvl < 1) {
            lvl = 1;
        }

        String target = new String(new char[lvl]).replace('\0', '0');
        if(hash(b).substring(0,lvl).equals(target)) return true;
        else return false;
    }

    /** Checks if the block header has been successfully mined -> creates Block object from header data */
    public boolean verifyMined(sample.indexBlock header) {

        sample.Block b = new sample.Block(header.getIndex(), header.getDate(), header.getPrevHash(), header.getNonce(), header.getDifficultyLevel(), header.getMerkleRoot(), header.getTotalDifficulty());

        System.out.println("checking if mined..");
        System.out.println("header is: " + header.toString());
        System.out.println("\nblock is: " + b.toString());

        long hashRate = b.getDifficultyLevel();
        // formula: (2^256) / (2^ (256-(4*difficultyLevel))) -> avg. number of hashes required..
        int lvl = (int) ((Math.log10(hashRate) / Math.log10(2) ) / 4);

        if(lvl < 1) {
            lvl = 1;
        }

        String target = new String(new char[lvl]).replace('\0', '0');

        if(hash(b).substring(0,lvl).equals(target)) return true;
        else return false;
    }

    /** Hashes the Merkle tree children to make parents */
    private LinkedList<String> genParentHash(LinkedList<String> children) {
        LinkedList<String> parents = new LinkedList<>();

        if( (children.size() != 1) && (children.size() % 2 != 0) ) {
            children.add(children.getLast());
        }

       for(int x = 0; x < children.size(); x=x+2) {
           String left = children.get(x);
           String right = children.get(x+1);

           parents.add(hash(left+right));
       }

        return parents;
    }

    /** Generates MerkleRoot of all messages */
    public String genMerkleRoot(TreeSet<sample.Message> mlist) {
        LinkedList<String> hashes = new LinkedList<>();

        if(mlist.isEmpty()) {
            return "empty";
        }

        if(mlist.size() % 2 != 0) {
            mlist.add(mlist.last());
        }

        for (sample.Message message : mlist) {
            hashes.add(hash(message.toString()));
        }

        while(hashes.size() != 1) {
            hashes = genParentHash(hashes);
        }

        return hashes.get(0);
    }

    // TODO MerkleTree verification


    public int getNumberOfMessages() {
        return numMessages;
    }

    public void clearMessages() {
        m.clear();
    }
}

