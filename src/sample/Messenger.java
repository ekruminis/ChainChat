package sample;

import java.io.Serializable;
import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public class Messenger {
    // TODO readMessage() - checks blockchain and searches for blocks containing messages to user
    // TODO searchUser()  - search for user by specifying ID or PubKey
    // TODO addFriend()   - add user as a friend (local file?)

    public void sendMessage(String snd, String rcv, String msg, sample.Network n) {
        message m = new message(snd, rcv, msg);
        n.msg(m);
        // TODO if message not official yet, resend message (to new miners)
    }

}

class message implements Serializable {
    private String date;
    private String sender;
    private String receiver;

    private String message;
    private String key;
    private String signature;

    public static String getDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return sdf.format(new Date().getTime());
    }

    public message(String s, String r, String m) {
        this.date = getDate();
        this.sender = s;
        this.receiver = r;

        sample.Encryption e = new sample.Encryption();
        Key k = e.generateAESKey();

        byte[] b1 = e.aesEncrypt(m, k);
        this.message = Base64.getEncoder().encodeToString( b1 );

        this.key = Base64.getEncoder().encodeToString( e.rsaEncrypt( Base64.getEncoder().encodeToString( k.getEncoded() ) ) );

        this.signature = Base64.getEncoder().encodeToString( e.generateSignature( this.message.getBytes() ) );
    }

    @Override
    public String toString() {
        return "message{" +
                "date='" + date + '\'' +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", message='" + message + '\'' +
                ", key='" + key + '\'' +
                ", signature='" + signature + '\'' +
                '}';
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getMessage() {
        return message;
    }

    public String getKey() {
        return key;
    }

    public String getSignature() {
        return signature;
    }

    @Override
    public boolean equals(Object obj) {
        return !super.equals(obj);
    }

    public int hashCode() {
        return getMessage().hashCode();
    }
}
