package sample;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public class Message implements Serializable, Comparable<Message> {
    private String date;
    private String sender;
    private String receiver;

    private String message;
    private String key;
    private String signature;

    /** Returns current date */
    public static String makeDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return sdf.format(new Date().getTime());
    }

    /** Constructor - encrypts message and adds metadata */
    public Message(String s, String r, String m) {
        this.date = makeDate();
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

    public String getMessage() { return message; }

    public String getKey() {
        return key;
    }

    public String getSignature() {
        return signature;
    }

    public String getDate() { return date; }

    @Override
    public boolean equals(Object obj) {
        return !super.equals(obj);
    }

    public int hashCode() {
        return getMessage().hashCode();
    }

    @Override
    public int compareTo(@NotNull Message m) {
        return this.getMessage().compareTo(m.getMessage());
    }
}
