package sample;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Mining {
    public String hash(Block b) {
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
            System.out.println("ERROR: " + nsae);
            return null;
        }
    }

    // getMessages() - fetch messages, oldest first

    public void mineBlock(Block b, int dl) {
        String target = new String(new char[dl]).replace('\0', '0');
        System.out.println("target: " + target);
        while(!hash(b).substring(0,dl).equals(target)) {
            System.out.println("n: " + b.nonce + "," + hash(b) + ",    len=" + hash(b).length());
            b.nonce++;
        }
    }
}
