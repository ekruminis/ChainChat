package sample;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

public class Block implements Serializable {
    public long index;
    public String date;
    public String previousHash;
    public long nonce;
    public long difficultyLevel;
    public String merkleRoot;
    public long totalDifficulty;
    private TreeSet<sample.Message> messages;

    /** Returns the difficulty level of block */
    public long getDifficultyLevel() {
        return difficultyLevel;
    }

    /** Returns current date */
    public static String makeDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return sdf.format(new Date().getTime());
    }

    /** Returns the date of the block as a String */
    public String getDate() { return date; }

    /** Returns the index of the block as a long */
    public long getIndex() {
        return index;
    }

    /** Return the hash of the child (previous block) block as a String */
    public String getPreviousHash() {
        return previousHash;
    }

    /** Returns the nonce value of the block as a long */
    public long getNonce() {
        return nonce;
    }

    /** Returns the hash of the merkle root as a String */
    public String getMerkleRoot() { return merkleRoot; }

    /** Returns the approximate average number of hashes required to solve the proof-of-work challenge */
    public long getTotalDifficulty() {
        return totalDifficulty;
    }

    /** Constructor v1*/
    public Block(String mr, long i, String ph, long dl, long tdl) {
        this.index = i;
        this.date = makeDate();
        this.previousHash = ph;
        this.nonce = 0;
        this.difficultyLevel = dl;
        this.merkleRoot = mr;
        this.totalDifficulty = tdl;
    }

    /** Constructor v2*/
    public Block(long i, String d, String ph, long n, long dl, String mr, long tdl) {
        this.index = i;
        this.date = d;
        this.previousHash = ph;
        this.nonce = n;
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

    /** Sets the messages of the block inside a TreeSet */
    public void setMessages(TreeSet<sample.Message> mlist) {
        this.messages = mlist;
    }

    /** Returns the message of the block inside a TreeSet */
    public TreeSet<sample.Message> getMessages() {
        return messages;
    }
}
