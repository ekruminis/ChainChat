package sample;

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

}