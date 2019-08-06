package sample;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;

public class Block {
    public int index;
    public String date;
    public String previousHash;
    public long nonce;
    public int difficultyLevel;
    public HashSet<sample.message> messages;

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public HashSet<sample.message> getMessages() {
        return messages;
    }

    public static String getDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return sdf.format(new Date().getTime());
    }

    public Block(HashSet<sample.message> set, int i, String ph, int dl) {
        this.index = i;
        this.date = getDate();
        this.previousHash = ph;
        this.nonce = 0;
        this.difficultyLevel = dl;
        this.messages = set;
    }

    @Override
    public String toString() {
        return "Block{" +
                "index=" + index +
                ", date='" + date + '\'' +
                ", previousHash='" + previousHash + '\'' +
                ", nonce=" + nonce +
                ", difficultyLevel=" + difficultyLevel +
                ", messages=" + messages +
                '}';
    }
}
