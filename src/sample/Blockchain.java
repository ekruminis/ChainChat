package sample;

import org.apache.commons.lang3.SerializationUtils;
import org.iq80.leveldb.*;
import static org.iq80.leveldb.impl.Iq80DBFactory.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Blockchain {
    // TODO getLast()               - to get index and its hash for prevHash
    // TODO getBlockchain()         - return active blockchain
    // TODO addBlock()              - add block to chain
    // TODO calculateDifficulty     - calculate global difficulty level

    private DB blockDB;
    private DB indexDB;
    private DB chainDB;
    private Options options = new Options();
    private int fileNum = 0;

    private long currentIndex;      // current chain index
    private String pHash;           // current chain top block
    private long levelDif;          // difficulty blocks should be mined at
    private long totalDif;          // current chain difficulty

    /** index db store:     f+filename  : indexFile
     *                      b+blockhash : indexBlock
     *
     *  block db store:     blockhash   : block
     *
     *  chain db store:     valid       : latest consensus valid block hash (index valid after 90 blocks (~30min w. 20sec bt) have been mined in front of it?)
     *                      index       : block hash (store its metadata in memory eg. pHash, currentIndex)
     *                      totalDif    : total difficulty of this chain (if another node has higher value, then start sync+update)
     *
     *                      */

    public void add(DB database, byte[] key, byte[] value) {
        try {
            database.put(key, value);
        } finally {
            finish(database);
        }
    }

    // return asString(database.get(key));
    public byte[] read(DB database, byte[] key) {
        try {
            return database.get(key);
        } finally {
            finish(database);
        }
    }

    public void remove(DB database, byte[] key) {
        try {
            WriteOptions wo = new WriteOptions();
            database.delete(key, wo);
        } finally {
            finish(database);
        }
    }

    public void finish(DB database) {
        try {
            database.close();
        } catch(IOException ioe) {
            System.out.println("FINISH IOE ERROR: " + ioe);
        }
    }

    /** Saves block inside block DB
     * TODO - (checking/validation MUST BE COMPLETED ELSEWHERE) */
    public void storeBlock(String hash, sample.Block block) {
        // TODO - atomicity -> either all changes made or none..
        try {
            // Adds block to blockDB
            if(read(getBlockDB(), hash.getBytes()) == null) {
                byte[] bytesBlock = SerializationUtils.serialize(block);
                add(getBlockDB(), hash.getBytes(), bytesBlock);
            }

            if(read(getIndexDB(), ("b"+hash).getBytes()) == null) {
                // Adds header to indexDB
                indexBlock blockHeader = new indexBlock(block.getIndex(), block.getDifficultyLevel(), block.getTotalDifficulty(), getFileNum(), block.date, block.getPreviousHash());
                byte[] bytesHeader = SerializationUtils.serialize(blockHeader);
                add(getIndexDB(), ("b" + hash).getBytes(), bytesHeader);

                // Updates fileheader info in indexDB (if relevant)
                if (read(getIndexDB(), ("fblocks" + fileNum).getBytes()) == null) {
                    indexFile fileHeader = new indexFile(1, block.getIndex(), block.getIndex(), block.getTotalDifficulty(), block.getTotalDifficulty(), block.getDate(), block.getDate());
                    byte[] bytesFile = SerializationUtils.serialize(fileHeader);
                    add(getIndexDB(), ("fblocks" + fileNum).getBytes(), bytesFile);
                } else {
                    byte[] res = read(getIndexDB(), ("fblocks" + fileNum).getBytes());
                    indexFile oldIndex = (indexFile) SerializationUtils.deserialize(res);

                    long lowIndex = oldIndex.getLowIndex();
                    long highIndex = oldIndex.getHighIndex();
                    long lowWork = oldIndex.getLowWork();
                    long highWork = oldIndex.getHighWork();
                    String earlyDate = oldIndex.earlyDate;
                    String lateDate = oldIndex.lateDate;

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    Date oldEarly = sdf.parse(earlyDate);
                    Date oldLate = sdf.parse(lateDate);

                    Date newDate = sdf.parse(block.getDate());

                    if (lowIndex > block.getIndex()) {
                        lowIndex = block.getIndex();
                    } else if (highIndex < block.getIndex()) {
                        highIndex = block.getIndex();
                    }

                    if (lowWork > block.getTotalDifficulty()) {
                        lowWork = block.getTotalDifficulty();
                    } else if (highWork < block.getTotalDifficulty()) {
                        highWork = block.getTotalDifficulty();
                    }

                    if (oldEarly.after(newDate)) {
                        earlyDate = block.getDate();
                    } else if (oldLate.before(newDate)) {
                        lateDate = block.getDate();
                    }

                    indexFile newIndex = new indexFile(oldIndex.getNumBlocks() + 1, lowIndex, highIndex, lowWork, highWork, earlyDate, lateDate);
                    byte[] bytesIndex = SerializationUtils.serialize(newIndex);
                    add(getIndexDB(), ("fblocks" + fileNum).getBytes(), bytesIndex);
                }
            }

            // Updates chain info in chainDB (if relevant)



        } catch(Exception e) {
            System.out.println("STORE BLOCK ERR: " + e);

            // TODO remove changes so it reverts to a state before error
        } finally {
            finish(getBlockDB());
            finish(getIndexDB());
            finish(getChainDB());
        }

    }

    /** Retrieves block from block DB */
    public sample.Block getBlock(String keyHash) {
        try {
            byte[] contents = read(getBlockDB(), keyHash.getBytes());
            sample.Block b = (sample.Block) SerializationUtils.deserialize(contents);
            return b;
        } catch(Exception e) {
            System.out.println("GET BLOCK ERR: " + e);
            return null;
        } finally {
            finish(getBlockDB());
        }
    }

    /** Create/Open LevelDB that stores block - creates new DB every ~2 weeks */
    public DB getBlockDB() {

        try {
            // 60,480 -> number of blocks in 2 weeks when block time is 20sec ( 3b/min, 180b/hour, 4320b/day, 30240b/week )
            if(checkFileLength("blocks") >= 60480) {
                fileNum++;
            }
        } catch(Exception e) {
            // blockDB doesn't exist yet
            fileNum = 0;
        }

        try {
            options.createIfMissing(true);
            blockDB = factory.open(new File(("/blocks/block" + fileNum)), options);
            return blockDB;
        } catch(IOException ioe) {
            System.out.println("GET BLOCK_DB IOE ERROR: " + ioe);
            return null;
        }
    }

    public DB getIndexDB() {
        try {
            options.createIfMissing(true);
            indexDB = factory.open(new File("index"), options);
            return indexDB;
        } catch(IOException ioe) {
            System.out.println("GET INDEX_DB IOE ERROR: " + ioe);
            return null;
        }
    }

    public DB getChainDB() {
        try {
            options.createIfMissing(true);
            chainDB = factory.open(new File("chain"), options);
            return chainDB;
        } catch(IOException ioe) {
            System.out.println("GET CHAIN_DB IOE ERROR: " + ioe);
            return null;
        }
    }

    public int getFileNum() {
        return fileNum;
    }

    /** Returns number of files in directory (NOTE: not actually accurate in terms of counting levelDB entries)
     *
     *  READ: https://stackoverflow.com/questions/39715607/by-java-how-to-count-file-numbers-in-a-directory-without-list
     * */
    private int checkFileLength(String name) {

        if(name.equals("blocks")) {
            File hugeDir = new File(("blocks" + fileNum));
            int numberFiles = hugeDir.list().length;
            return numberFiles;
        }

        else if(name.equals("chain") || name.equals("index")) {
            File hugeDir = new File(name);
            int numberFiles = hugeDir.list().length;
            return numberFiles;
        }

        else {
            return -1;
        }
    }

    // TODO (return chainTip)
    public sample.indexBlock getChainTip() {
        sample.indexBlock ib = new sample.indexBlock(0,0,0,0,"0", "0");
        return ib;
    }
}

class indexFile implements Serializable {
    public int numBlocks;
    public long lowIndex;
    public long highIndex;
    public long lowWork;
    public long highWork;
    public String earlyDate;
    public String lateDate;

    public indexFile(int numBlocks, long lowIndex, long highIndex, long lowWork, long highWork, String earlyDate, String lateDate) {
        this.numBlocks = numBlocks;
        this.lowIndex = lowIndex;
        this.highIndex = highIndex;
        this.lowWork = lowWork;
        this.highWork = highWork;
        this.earlyDate = earlyDate;
        this.lateDate = lateDate;
    }

    public int getNumBlocks() {
        return numBlocks;
    }

    public long getLowIndex() {
        return lowIndex;
    }

    public long getHighIndex() {
        return highIndex;
    }

    public long getLowWork() {
        return lowWork;
    }

    public long getHighWork() {
        return highWork;
    }

    public String getEarlyDate() {
        return earlyDate;
    }

    public String getLateDate() {
        return lateDate;
    }

    @Override
    public String toString() {
        return "indexFile{" +
                "numBlocks=" + numBlocks +
                ", lowIndex=" + lowIndex +
                ", highIndex=" + highIndex +
                ", lowWork=" + lowWork +
                ", highWork=" + highWork +
                ", earlyDate='" + earlyDate + '\'' +
                ", lateDate='" + lateDate + '\'' +
                '}';
    }
}

class indexBlock implements Serializable {
    public long index;
    public long difficultyLevel;
    public long totalWork;
    public int fileNumber;
    public String date;
    public String prevHash;

    public indexBlock(long index, long difficultyLevel, long totalWork, int fileNumber, String date, String prevHash) {
        this.index = index;
        this.difficultyLevel = difficultyLevel;
        this.totalWork = totalWork;
        this.fileNumber = fileNumber;
        this.date = date;
        this.prevHash = prevHash;
    }

    public long getIndex() {
        return index;
    }

    public long getDifficultyLevel() {
        return difficultyLevel;
    }

    public long getTotalWork() {
        return totalWork;
    }

    public int getFileNumber() {
        return fileNumber;
    }

    public String getDate() {
        return date;
    }

    public String getPrevHash() {
        return prevHash;
    }

    @Override
    public String toString() {
        return "indexBlock{" +
                "index=" + index +
                ", difficultyLevel=" + difficultyLevel +
                ", totalWork=" + totalWork +
                ", fileName='" + fileNumber + '\'' +
                ", date='" + date + '\'' +
                ", prevHash='" + prevHash + '\'' +
                '}';
    }
}