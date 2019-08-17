package sample;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.*;

public class Block implements Serializable {
    // TODO - Merkle Tree of transactions

    public long index;
    public String date;
    public String previousHash;
    public long nonce;
    public int difficultyLevel;             // Should be 'long'?
    public String merkleRoot;
    public long totalDifficulty;
    private TreeSet<sample.Message> messages;

    /** Returns the difficulty level of block */
    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    /** Returns current date */
    public static String makeDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return sdf.format(new Date().getTime());
    }

    public String getDate() { return date; }

    public long getIndex() {
        return index;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public long getNonce() {
        return nonce;
    }

    public String getMerkleRoot() { return merkleRoot; }

    public long getTotalDifficulty() {
        return totalDifficulty;
    }

    /** Constructor */
    public Block(String mr, long i, String ph, int dl, long tdl) {
        this.index = i;
        this.date = makeDate();
        this.previousHash = ph;
        this.nonce = 0;
        this.difficultyLevel = dl;
        this.merkleRoot = mr;
        this.totalDifficulty = tdl;
    }

    @Override
    public String toString() {
        return "Block{" +
                "index=" + index +
                ", date='" + date + '\'' +
                ", previousHash='" + previousHash + '\'' +
                ", nonce=" + nonce +
                ", difficultyLevel=" + difficultyLevel +
                ", merkleRoot='" + merkleRoot + '\'' +
                ", totalDifficulty=" + totalDifficulty +
                '}';
    }

    public void setMessages(TreeSet<sample.Message> mlist) {
        this.messages = mlist;
    }

    public TreeSet<sample.Message> getMessages() {
        return messages;
    }
}
