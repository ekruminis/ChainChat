package sample;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Mining {
    // TODO uploadBlock()   - distribute mined block

    private int difficulty = 2;
    private TreeSet<sample.Message> m = new TreeSet<sample.Message>();

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
            System.out.println(m.toString());
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
    // TODO fix inputs
    public sample.Block createBlock() {
        String mr = genMerkleRoot(m);

        sample.Block block = new sample.Block(mr,0, "genesis", difficulty, 1);
        block.setMessages(m);

        return block;
    }

    /** Mines the block until the difficulty level is satisfied, then returns the block */
    public sample.Block mineBlock(sample.Block b) {
        int lvl = b.getDifficultyLevel();
        String target = new String(new char[lvl]).replace('\0', '0');
        System.out.println("target: " + target);
        while(!hash(b).substring(0,lvl).equals(target)) {
            System.out.println("n: " + b.nonce + "," + hash(b) + ",    len=" + hash(b).length());
            b.nonce++;
        }
        return b;
    }

    /** Checks if the block has been successfully mined */
    public boolean verifyBlock(sample.Block b) {
        int lvl = b.getDifficultyLevel();
        String target = new String(new char[lvl]).replace('\0', '0');
        if(hash(b).substring(0,lvl).equals(target)) return true;
        else return false;
    }

    public LinkedList<String> genParentHash(LinkedList<String> children) {
        String left = "";
        String right = "";

        LinkedList<String> parents = new LinkedList<>();

        if( (children.size() != 1) && (children.size() % 2 != 0) ) {
            children.add(children.getLast());
        }

       for(int x = 0; x < children.size(); x=x+2) {
           left = children.get(x);
           right = children.get(x+1);

           parents.add(hash(left+right));
       }

        return parents;
    }

    public String genMerkleRoot(TreeSet<sample.Message> mlist) {
        LinkedList<String> hashes = new LinkedList<>();
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
}

