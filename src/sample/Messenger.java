package sample;

import javax.crypto.spec.SecretKeySpec;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.security.Key;
import java.util.Base64;

public class Messenger {

    /** Creates a message object, stores it in users own to-mine messages file and broadcast the messages to other peers so they can add it too
     * @param snd The senders public key
     * @param rcv The receivers public key
     * @param msg The message to be transmitted
     * @param n The sample.Network object that the user is connected on
     */
    public void sendMessage(String snd, String rcv, String msg, sample.Network n) {
        // Create object and transmit it
        sample.Message m = new sample.Message(snd, rcv, msg);
        n.announce(m);

        // Save object to our own messages file
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(n.getMessagesFile(), true))) {
            oos.writeObject(m);
            System.out.println("MSG RECEIVED: " + m.toString());
        } catch (Exception e) {
            System.out.println("SAMPLE.MSG RCV ERROR: " + e);
        }

        // TODO if message not official yet, resend message (to new miners)
    }

    /** Validates the contents of the message object and then decrypts it
     * @param msg The message object we want to read
     * @param key The public key of the user (usually our own public key)
     * @return The decrypted, plaintext message
     */
    public String readMessage(sample.Message msg, String key) {
        System.out.println("decrypting msg..");
        String m = null;

        // If we are the receiver of the message, use the receiverKey part of the object
        if(msg.getReceiver().equals(key)) {
            // Validate signature
            if(sample.Main.encryption.verifySignature( Base64.getDecoder().decode(msg.getMessage()), Base64.getDecoder().decode(msg.getSignature()), msg.getSender())) {
                System.out.println("signature valid..");
                String encKey = msg.getReceiverKey();
                String decKey = sample.Main.encryption.rsaDecrypt(Base64.getDecoder().decode(encKey));
                Key finalKey = new SecretKeySpec(Base64.getDecoder().decode(decKey), "AES");

                m = sample.Main.encryption.aesDecrypt(Base64.getDecoder().decode(msg.getMessage()), finalKey);
            }
        }

        // If we are the sender of the message, use the senderKey part of the object
        else if(msg.getSender().equals(key)) {
            // Validate signature
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