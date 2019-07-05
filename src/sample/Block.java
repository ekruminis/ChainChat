package sample;

import sun.misc.BASE64Encoder;

import java.security.Key;
import java.security.KeyPair;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public class Block {
    private int index;
    private String date;
    private String previousHash;
    public long nonce;
    private int difficultyLevel;
    private String sender;
    private String receiver;
    private String message;
    private String key;
    private String sessionID;
    private String signature;

    public static String getDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return sdf.format(new Date().getTime());
    }

    public Block(String m) {
        this.index = 0;
        this.date = getDate();
        this.previousHash = "empty";
        this.nonce = 0;
        this.difficultyLevel = 0;
        this.sender = "sender";
        this.receiver = "receiver";
        this.message = m;
        this.key = "key";
        this.sessionID = "sid";
        this.signature = "sig";
    }

    @Override
    public String toString() {
        return "Block{" +
                "index=" + index +
                ", date='" + date + '\'' +
                ", previousHash='" + previousHash + '\'' +
                ", nonce=" + nonce +
                ", difficultyLevel=" + difficultyLevel +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", message='" + message + '\'' +
                ", key='" + key + '\'' +
                ", sessionID='" + sessionID + '\'' +
                ", signature='" + signature + '\'' +
                '}';
    }

    public static void main(String[] args) {
        Encryption e = new Encryption();
        e.generateRSAKey();
        byte[] ciphertext = e.rsaEncrypt("hello world");
        System.out.println("\n\n" + Base64.getEncoder().encodeToString(ciphertext));
        System.out.println("\n\n" + e.rsaDecrypt(ciphertext));
    }
}
