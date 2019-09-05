package sample;

import io.ipfs.multibase.Multibase;

import javax.crypto.spec.SecretKeySpec;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public class Messenger {
    // TODO readMessage() - checks blockchain and searches for blocks containing messages to user (if msg has either sender/receiver as me)
    // TODO searchUser()  - search for user by specifying ID or PubKey
    // TODO addFriend()   - add user as a friend (local file?)

    /** Creates a message object, stores it in users own messageFile and broadcast the messages to other peers so they can add it too */
    public void sendMessage(String snd, String rcv, String msg, sample.Network n) {
        sample.Message m = new sample.Message(snd, rcv, msg);
        n.announce(m);

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(n.getMessagesFile(), true))) {
            oos.writeObject(m);
            System.out.println("MSG RECEIVED: " + m.toString());
        } catch (Exception e) {
            System.out.println("SAMPLE.MSG RCV ERROR: " + e);
        }

        // TODO if message not official yet, resend message (to new miners) - check date of msg, and then search for blocks with date posted later than msg
    }

    public String readMessage(sample.Message msg, String key) {
        System.out.println("decrypting msg..");
        String m = null;
        if(msg.getReceiver().equals(key)) {
            if(sample.Main.encryption.verifySignature( Base64.getDecoder().decode(msg.getMessage()), Base64.getDecoder().decode(msg.getSignature()), msg.getSender())) {
                System.out.println("signature valid..");
                String encKey = msg.getReceiverKey();
                String decKey = sample.Main.encryption.rsaDecrypt(Base64.getDecoder().decode(encKey));
                Key finalKey = new SecretKeySpec(Base64.getDecoder().decode(decKey), "AES");

                m = sample.Main.encryption.aesDecrypt(Base64.getDecoder().decode(msg.getMessage()), finalKey);
            }
        }
        else if(msg.getSender().equals(key)) {
            if(sample.Main.encryption.verifySignature( Base64.getDecoder().decode(msg.getMessage()), Base64.getDecoder().decode(msg.getSignature()), msg.getSender())) {
                System.out.println("signature is valid..");
                String encKey = msg.getSenderKey();
                String decKey = sample.Main.encryption.rsaDecrypt(Base64.getDecoder().decode(encKey));
                Key finalKey = new SecretKeySpec(Base64.getDecoder().decode(decKey), "AES");

                m = sample.Main.encryption.aesDecrypt(Base64.getDecoder().decode(msg.getMessage()), finalKey);
            }
        }

        return m;
    }

}