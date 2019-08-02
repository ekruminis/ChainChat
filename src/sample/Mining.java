package sample;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

public class Mining {
    // TODO fetchMessages() - download array of messages that need to be mined into a block
    // TODO generateBlock() - create block with messages that need mining
    // TODO uploadBlock()   - distribute mined block

    public void fetchMessages(File f) {
        try {
            FileInputStream fis = new FileInputStream(f);
            ObjectInputStream ois = new ObjectInputStream(fis);
            HashMap<String, sample.message> m = new HashMap<String, sample.message>();

            Object obj = null;
            while((obj = ois.readObject()) != null) {
                sample.message msg = (sample.message)obj;
                m.put(msg.getMessage(), msg);
                ois = new ObjectInputStream(fis);
            }
            System.out.println(m.values());
            fis.close();

        } catch(Exception e) {
            System.out.println("FETCHING ERORR: " + e);
        }
    }

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

    // getMessages() - fetch messages, oldest first

    public void mineBlock(sample.Block b) {
        int lvl = b.getDifficultyLevel();
        String target = new String(new char[lvl]).replace('\0', '0');
        System.out.println("target: " + target);
        while(!hash(b).substring(0,lvl).equals(target)) {
            System.out.println("n: " + b.nonce + "," + hash(b) + ",    len=" + hash(b).length());
            b.nonce++;
        }
    }

    public boolean verifyBlock(sample.Block b) {
        int lvl = b.getDifficultyLevel();
        String target = new String(new char[lvl]).replace('\0', '0');
        if(hash(b).substring(0,lvl).equals(target)) return true;
        else return false;
    }
}
