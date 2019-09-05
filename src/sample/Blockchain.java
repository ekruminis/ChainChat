package sample;

import net.tomp2p.peers.PeerAddress;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.iq80.leveldb.*;
import org.jetbrains.annotations.NotNull;

import static org.iq80.leveldb.impl.Iq80DBFactory.*;
import java.io.*;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.*;

public class Blockchain {
    private DB blockDB;
    private DB indexDB;
    private Options options = new Options();
    private int fileNum = 0;

    private long currentIndex = 0;                  // current chain index
    private String currentHash = "GENESIS";                 // current chain top block hash
    private long currentDifficultyTotal = 0;        // current chain difficulty

    private sample.indexBlock chainTip = null;     // header of current chain tip

    private LinkedList<sample.unofficial> unofficial = new LinkedList<sample.unofficial>();
    private ArrayList<String> request = new ArrayList<String>();
    private String myPubKey = Base64.getEncoder().encodeToString( sample.Main.encryption.getRSAPublic().getEncoded() );


    /** index db store:     f+filename  : indexFile     eg. fblocks0 : file.header
     *                      b+blockhash : indexBlock    eg. b00f3c.. : block.header
     *
     *  block db store:     blockhash   : block
     *
     *  contacts db store:  username    : pubKey
     *
     *                      */

    public Blockchain() {
        if(chainTip == null) {
            byte[] ct = read(getIndexDB(), "chaintip".getBytes());
            if(ct != null) {
                chainTip = (sample.indexBlock) SerializationUtils.deserialize(ct);
                currentHash = new sample.Mining().hash(chainTip);
                currentIndex = chainTip.getIndex();
                currentDifficultyTotal = chainTip.getTotalDifficulty();
            }
        }
        System.out.println("current chaintip: " + currentHash);
    }

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

    public void getAll() {
        DB bdb = getBlockDB();
        DBIterator iterator = bdb.iterator();
        try {
            for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                String key = asString(iterator.peekNext().getKey());

                sample.Block block = (sample.Block) SerializationUtils.deserialize(iterator.peekNext().getValue());
                String value = block.toString();

                System.out.println(key+" = "+value);
            }

            System.out.println("chaintip: " + getChainTip().toString());
        }
        catch(Exception e) {
            System.out.println("GET ALL ERROR: " + e);
        }
        finally {
            // Make sure you close the iterator to avoid resource leaks.
            try {
                iterator.close();
                finish(bdb);
            } catch(Exception e) {
                System.out.println("CLOSING ITERATOR ERROR: " + e);
            }
        }
    }

    public sample.MessagesList getMessages(String user, String pubKey) {
        System.out.println("getting messages for user:" + user);

        LinkedList<sample.Message> list = new LinkedList<>();
        LinkedList<String> colours = new LinkedList<>();

        int count = 1;
        String ch = currentHash;

        while(!ch.equals("GENESIS")) {
            byte[] contents = read(getBlockDB(), ch.getBytes());
            sample.Block b = (sample.Block)SerializationUtils.deserialize(contents);
            System.out.println("got block with hash: " + ch);
            for(sample.Message m : b.getMessages()) {
                if(m.getReceiver().equals(pubKey) || m.getSender().equals(pubKey) || m.getReceiver().equals(myPubKey) || m.getSender().equals(myPubKey)) {
                    System.out.println("adding msg");
                    if (list.isEmpty()) {
                        list.addFirst(m);
                        if(count > 15) {
                            colours.addFirst("GREEN");
                        }
                        else {
                            colours.addFirst("BLUE");
                        }
                    } else {
                        boolean toAdd = true;
                        for (sample.Message m2 : list) {
                            if (m2.getMessage().equals(m.getMessage())) {
                                toAdd = false;
                            }
                        }

                        if(toAdd) {
                            System.out.println("new, so we add..");
                            list.addFirst(m);
                            if(count > 15) {
                                colours.addFirst("GREEN");
                            }
                            else {
                                colours.addFirst("BLUE");
                            }
                        }
                    }
                }
            }
            ch = b.getPreviousHash();
            count++;
        }

        sample.MessagesList ml = new sample.MessagesList(list, colours);

//        if(ch.equals("GENESIS")) {
//            byte[] contents = read(getBlockDB(), ch.getBytes());
//            b = (sample.Block)SerializationUtils.deserialize(contents);
//            System.out.println("got block with hash: " + ch);
//            for(sample.Message m : b.getMessages()) {
//                if(m.getReceiver().equals(pubKey) || m.getSender().equals(pubKey) || m.getReceiver().equals(myPubKey) || m.getSender().equals(myPubKey)) {
//                    list.addFirst(m);
//                }
//            }
//        }
        System.out.println("-----------------------------MESSAGES-----------------------------");
        System.out.println(list);
        return ml;
    }

    public LinkedList<String> getChats(String user) {
        System.out.println("getting messages for user:" + user);

        LinkedList<String> list = new LinkedList<>();

        String ch = currentHash;

        while(!ch.equals("GENESIS")) {
            byte[] contents = read(getBlockDB(), ch.getBytes());
            sample.Block b = (sample.Block)SerializationUtils.deserialize(contents);
            System.out.println("got block with hash: " + ch);
            for(sample.Message m : b.getMessages()) {
                if(m.getReceiver().equals(myPubKey)) {
                    System.out.println("adding msg");
                    if (list.isEmpty()) {
                        list.add(m.getSender());
                    } else {
                        boolean toAdd = true;
                        for (String m2 : list) {
                            if (m2.equals(m.getSender())) {
                                toAdd = false;
                            }
                        }
                    }
                }
                else if(m.getSender().equals(myPubKey)) {
                    System.out.println("adding msg");
                    if (list.isEmpty()) {
                        list.add(m.getReceiver());
                    } else {
                        boolean toAdd = true;
                        for (String m2 : list) {
                            if (m2.equals(m.getReceiver())) {
                                toAdd = false;
                            }
                        }
                    }
                }
            }
            ch = b.getPreviousHash();
        }

        System.out.println("-----------------------------CHATTING WITH-----------------------------");
        System.out.println(list);
        return list;
    }

    /** Saves block inside block DB */
    public void storeBlock(String hash, sample.Block block) {
        // TODO - atomicity -> either all changes made or none..
        System.out.println("storing block: " + hash);
        try {
            // Adds block to blockDB if it's not already there
            if( read(getBlockDB(), hash.getBytes()) == null) {
                System.out.println("we don't already have this block..");
                byte[] bytesBlock = SerializationUtils.serialize(block);
                add(getBlockDB(), hash.getBytes(), bytesBlock);
                System.out.println("added to blockDB");
            }
            else{
                System.out.println("we already have this block stored..");
            }

            // Adds block metadata to indexDB if it's not already there
            if(read(getIndexDB(), ("b"+hash).getBytes()) == null) {
                // Adds header to indexDB
                indexBlock blockHeader = new indexBlock(block.getIndex(), block.getDifficultyLevel(), block.getTotalDifficulty(), getFileNum(), block.getDate(), block.getPreviousHash(), block.getMerkleRoot(), block.getNonce());
                System.out.println("header: " + blockHeader.toString());
                byte[] bytesHeader = SerializationUtils.serialize(blockHeader);
                add(getIndexDB(), ("b" + hash).getBytes(), bytesHeader);
                System.out.println("header added to indexDB");
                // Check if this block is a new chain tip/head
                // TODO (check if we already have a block in indexFile with higher difficulty - should be unlikely, but..)
                System.out.println("checking chaintip");
                if(getChainTip() == null) {
                    System.out.println("chaintip set..");
                    setChainTip(blockHeader);
                    setCurrentDifficultyTotal(blockHeader.getTotalDifficulty());
                    setCurrentHash(hash);
                    setCurrentIndex(blockHeader.getIndex());
                }
                else if(getChainTip().getTotalDifficulty() < blockHeader.getTotalDifficulty()) {
                    System.out.println("---------------------updating chaintip...");
                    setChainTip(blockHeader);
                    setCurrentDifficultyTotal(blockHeader.getTotalDifficulty());
                    setCurrentHash(hash);
                    setCurrentIndex(blockHeader.getIndex());
                }
                System.out.println("chaintip checked..");

                System.out.println("checking fileheader data");
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
                    String earlyDate = oldIndex.getEarlyDate();
                    String lateDate = oldIndex.getLateDate();

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

            else {
                System.out.println("block metadata already stored..");
            }

            System.out.println("finished storing block..");

        } catch(Exception e) {
            System.out.println("STORE BLOCK ERR: " + e);

            // TODO remove changes so it reverts to a state before error
        } finally {
            finish(getBlockDB());
            finish(getIndexDB());
        }

    }

    /** Add blockchain to LevelDB if it's one of the blocks requested */
    public boolean syncBlocks(LinkedList<sample.Block> blocks) {
        System.out.println("\n\nSyncing blocks..");

        sample.Mining mine = new sample.Mining();

        for (sample.Block b : blocks) {
            String h = mine.hash(b);
            System.out.println("block hash: " + h);
            if(getRequest().contains(h)) {
                System.out.println("we requested this block so start storing it..");
                storeBlock(h, b);
                System.out.println("finished storing it..");
                getRequest().remove(h);
                System.out.println("removing it from request list");
            }
            else {
                System.out.println("not in requested list..");
            }
        }

        // If some blocks were not sent by the user, request them again
        if(!getRequest().isEmpty()) {
            System.out.println("not all blocks received, so return false..");
            return false;
        }

        else {
            System.out.println("all fine and finished, return true..");
            return true;
        }
    }

    /** Check if the list of headers is valid */
    public boolean validateHeaders(LinkedList<sample.indexBlock> headerList) {
        System.out.println("validating headers..");
        boolean valid = false;
        // check mined, check chain is linked, check difficulty levels)
        sample.Mining mine = new sample.Mining();

        for (sample.indexBlock header : headerList) {
            int index = headerList.indexOf(header);
            if(index != 0) {
                index = index - 1;
            }

            System.out.println("checking mined..");
            // Check if mined successfully
            if( !(mine.verifyMined(header)) ) {
                System.out.println("not mined");
                request.clear();
                System.out.println("request cleared..");
                return false;
            }
            System.out.println("checking linked..");
            // Check if chain is linked
            if( !(validateInChain(header, mine.hash(headerList.get(index)) )) ) {
                System.out.println("not linked..");
                request.clear();
                System.out.println("request cleared..");
                return false;
            }

            int index2 = 0;
            // Check difficulty is correct
            if(index-1 > 0 ) {
                index2 = index-1;
            }

            if( !(validateDifficulty(header, headerList.get(index), headerList.get(index2))) ) {
                System.out.println("not correct difficulty..");
                request.clear();
                System.out.println("request cleared..");
                return false;
            }

            System.out.println("adding new request: " + mine.hash(header) );
            request.add(0, mine.hash(header));

        }
        System.out.println("header validation finished...");
        valid = true;

        return valid;
    }

    /** Check if prevHash value of block is in users LevelDB */
    public boolean validateInChain(sample.Block b) {
        String pHash = b.getPreviousHash();

        sample.Block pBlock = getBlock(pHash);

        if (pHash.equals("GENESIS") && b.getIndex() == 0) {
            return true;
        }

        else if(pBlock != null) {
            return true;
        }

        else {
            return false;
        }
    }

    /** Check if prevHash value of block is in users LevelDB */
    public boolean validateInChain(sample.indexBlock header, String inChain) {
        System.out.println("validating if in chain..");
        String pHash = header.getPrevHash();

        sample.Block pBlock = getBlock(pHash);

        if (pHash.equals("GENESIS") && header.getIndex() == 1) {
            System.out.println("ph is genesis or index is 1");
            return true;
        }

        else if(pBlock != null || pHash.equals(inChain)) {
            System.out.println("no block found or ph equals hash given");
            return true;
        }

        else {
            System.out.println("none, so return false..");
            return false;
        }
    }

    /** Check that the difficulty levels in block are valid, ie. totalDifficulty count is correct, and individual difficultyLevel matches current consensus level */
    public boolean validateDifficulty(sample.Block b) {
        boolean valid = false;

        try {
            if( !b.getPreviousHash().equals("GENESIS") && (b.getIndex() >= 3) ) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

                byte[] res = read(getIndexDB(), ("b"+b.getPreviousHash()).getBytes());
                indexBlock prevBlock = (indexBlock) SerializationUtils.deserialize(res);
                Date parent = sdf.parse(prevBlock.getDate());

                byte[] res2 = read(getIndexDB(), ("b"+prevBlock.getPrevHash()).getBytes());
                indexBlock prevBlock2 = (indexBlock) SerializationUtils.deserialize(res2);
                Date child = sdf.parse(prevBlock2.getDate());

                long diffSeconds = (parent.getTime() - child.getTime()) / 1000 % 60;
                System.out.println("------------------------------------difference in seconds between blocks: " + diffSeconds);

                if (diffSeconds < 20) {
                    long val = prevBlock.getDifficultyLevel() * 2;
                    System.out.println("dif should be: " + val);
                    if(b.getDifficultyLevel() == val) {
                        System.out.println("difficulty is correct");
                        if(b.getTotalDifficulty() == (prevBlock.getTotalDifficulty() + b.getDifficultyLevel())) {
                            System.out.println("total dif is right..");
                            return true;
                        }
                        else {
                            System.out.println("total not right..");
                            return false;
                        }
                    }
                    else {
                        System.out.println("dif not right..");
                        return false;
                    }
                } else if (diffSeconds > 20) {
                    long val = prevBlock.getDifficultyLevel() / 2;
                    System.out.println("dif should be: " + val);
                    if(b.getDifficultyLevel() == val) {
                        System.out.println("difficulty is correct");
                        if(b.getTotalDifficulty() == (prevBlock.getTotalDifficulty() + b.getDifficultyLevel())) {
                            System.out.println("total dif is right..");
                            return true;
                        }
                        else {
                            System.out.println("total not right..");
                            return false;
                        }
                    }
                    else {
                        System.out.println("dif not right..");
                        return false;
                    }
                }
            }
            else {
                if(b.getDifficultyLevel() == 1) {
                    System.out.println("dif is 1..");
                    return true;
                }
                else {
                    System.out.println("dif is not 1, so false..");
                    return false;
                }
            }
        }
        catch(Exception e) {
            System.out.println("get difficulty err: " + e);
        }

        return valid;
    }

    /** Check that the difficulty levels in block header are valid, ie. totalDifficulty count is correct, and individual difficultyLevel matches current consensus level */
    public boolean validateDifficulty(sample.indexBlock h, sample.indexBlock h2, sample.indexBlock h3) {
        boolean valid = false;

        try {
            if( !h.getPrevHash().equals("GENESIS") && (h.getIndex() >= 3) ) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

                Date parent = sdf.parse(h2.getDate());

                Date child = sdf.parse(h3.getDate());

                long diffSeconds = (parent.getTime() - child.getTime()) / 1000 % 60;
                System.out.println("------------------------------------difference in seconds between blocks: " + diffSeconds);

                if (diffSeconds < 20) {
                    System.out.println("difficulty is increased..");
                    long val = h2.getDifficultyLevel() * 2;
                    System.out.println("dif should be: " + val);
                    if(h.getDifficultyLevel() == val) {
                        System.out.println("difficulty is correct");
                        if(h.getTotalDifficulty() == (h2.getTotalDifficulty() + h.getDifficultyLevel())) {
                            System.out.println("total dif is right..");
                            return true;
                        }
                        else {
                            System.out.println("total not right..");
                            return false;
                        }
                    }
                    else {
                        System.out.println("dif not right..");
                        return false;
                    }
                } else if (diffSeconds > 20) {
                    System.out.println("difficulty is decreased..");
                    long val = h2.getDifficultyLevel() / 2;
                    System.out.println("dif should be: " + val);
                    if(h.getDifficultyLevel() == val) {
                        System.out.println("difficulty is correct");
                        if(h.getTotalDifficulty() == (h2.getTotalDifficulty() + h.getDifficultyLevel())) {
                            System.out.println("total dif is right..");
                            return true;
                        }
                        else {
                            System.out.println("total not right..");
                            return false;
                        }
                    }
                    else {
                        System.out.println("dif not right..");
                        return false;
                    }
                }
            }
            else {
                if(h.getDifficultyLevel() == 1) {
                    System.out.println("dif is 1..");
                    return true;
                }
                else {
                    System.out.println("dif is not 1, so false..");
                    return false;
                }
            }
        }
        catch(Exception e) {
            System.out.println("get difficulty err: " + e);
        }

        return valid;
    }

    public long getDifficulty() {
        long val = 1;
        try {
            if(chainTip != null) {
                if(!chainTip.getPrevHash().equals("GENESIS")) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    Date parent = sdf.parse(chainTip.getDate());
                    byte[] res = read(getIndexDB(), ("b"+chainTip.getPrevHash()).getBytes());
                    indexBlock prevBlock = (indexBlock) SerializationUtils.deserialize(res);
                    Date child = sdf.parse(prevBlock.getDate());

                    long diffSeconds = (parent.getTime() - child.getTime()) / 1000 % 60;
                    System.out.println("------------------------------------difference in seconds between blocks: " + diffSeconds);

                    if (diffSeconds < 20) {
                        System.out.println("difficulty is increased..");
                        val = chainTip.getDifficultyLevel() * 2;
                    } else if (diffSeconds > 20) {
                        System.out.println("difficulty is decreased..");
                        val = chainTip.getDifficultyLevel() / 2;
                    }
                }
            }
        }
        catch(Exception e) {
            System.out.println("get difficulty err: " + e);
        }
        return val;
    }

    public void addUnofficial(sample.Block b, PeerAddress pa) {
        sample.unofficial uo = new sample.unofficial(b, pa);
        unofficial.addFirst(uo);
    }

    public void checkUnofficial(PeerAddress pa) {
        try {
            for (sample.unofficial ub : unofficial) {
                if (ub.getPeer().equals(pa)) {
                    if (validateInChain(ub.getBlock())) {
                        if(validateDifficulty(ub.getBlock())) {
                            storeBlock(new sample.Mining().hash(ub.getBlock()), ub.getBlock());
                            unofficial.remove(ub);
                        }
                        else {
                            throw new Exception();
                        }
                    } else {
                        throw new Exception();
                    }
                }
            }
        }
        catch(Exception e) {
            System.out.println("Unofficial blocks not yet synced");
        }
    }

    /** Query current chaintip of the user and go back the chain for 30 blocks and return that blocks hash
     *  see ->     https://stackoverflow.com/questions/49065176/how-many-confirmations-should-i-have-on-ethereum */
    public String getConfirmed() {
        int count = 1;
        String hash = "GENESIS";
        if(getChainTip() != null) {
            hash = getChainTip().getPrevHash();
        }

        while(count != 30) {
            sample.Block b = getBlock(hash);

            // Reached end of chain, ie. user doesn't have 30 blocks in its DB so we just go as far as possible
            if(b == null) {
                return hash;
            }

            // else, just keep looping back
            else {
                hash = b.getPreviousHash();
                count++;
            }
        }

        return hash;
    }

    public LinkedList<sample.Block> getRequestedBlocks(ArrayList<String> hashes) {
        System.out.println("\n\ngetting requested blocks..");
        System.out.println("blocks wanted: " + hashes);

        LinkedList<sample.Block> blockList = new LinkedList<>();

        for (String h : hashes) {
            System.out.println("we need hash: " + h);
            byte[] contents = read(getBlockDB(), h.getBytes() );
            sample.Block b = (sample.Block) SerializationUtils.deserialize(contents);
            System.out.println("got block with ph: " + b.getPreviousHash());
            blockList.addLast(b);
            System.out.println("block added: " + new sample.Mining().hash(b));
        }

        System.out.println("finished adding blocks, now returning..");
        return blockList;
    }

    public LinkedList<sample.indexBlock> getHeaders(String h) {
        System.out.println("Getting chain headers..");
        LinkedList<sample.indexBlock> headerList = new LinkedList<>();
        //sample.Mining mine = new sample.Mining();

        // add chainTip
        sample.indexBlock header = getChainTip();
        headerList.addFirst(header);
        System.out.println("added chaintip header..");
        // check if we need to go further down the chain
        while( !(header.getPrevHash().equals(h)) ) {
            String prevHash = header.getPrevHash();
            byte[] contents = read(getIndexDB(), ("b"+prevHash).getBytes() );
            header = (sample.indexBlock) SerializationUtils.deserialize(contents);
            headerList.addFirst(header);
            System.out.println("added: " + header.toString());
        }
        System.out.println("finished getting headers..");

        return headerList;
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

//        try {
//            // 60,480 -> number of blocks in 2 weeks when block time is 20sec ( 3b/min, 180b/hour, 4320b/day, 30240b/week )
//            while(checkFileLength("blocks") >= 60480) {
//                fileNum++;
//            }
//        } catch(Exception e) {
//            // blockDB doesn't exist yet
//            fileNum = 0;
//        }

        try {
            options.createIfMissing(true);
            blockDB = factory.open(new File(("blocks" + fileNum)), options);
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

    /** Returns chain tip */
    public sample.indexBlock getChainTip() {
        return chainTip;
    }

    /** Update chain tip */
    public void setChainTip(sample.indexBlock newTip) {
        this.chainTip = newTip;
    }

    public long getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(long currentIndex) {
        this.currentIndex = currentIndex;
    }

    public String getCurrentHash() {
        return currentHash;
    }

    public void setCurrentHash(String currentHash) {
        this.currentHash = currentHash;
    }

    public long getCurrentDifficultyTotal() {
        return currentDifficultyTotal;
    }

    public void setCurrentDifficultyTotal(long currentDifficultyTotal) {
        this.currentDifficultyTotal = currentDifficultyTotal;
    }

    public ArrayList<String> getRequest() {
        return request;
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
    public int fileNumber;
    public String date;
    public String merkleRoot;

    public String previousHash;
    public long nonce;
    public long totalDifficulty;

    public indexBlock(long index, long difficultyLevel, long totalWork, int fileNumber, String date, String prevHash, String mr, long n) {
        this.index = index;
        this.difficultyLevel = difficultyLevel;
        this.totalDifficulty = totalWork;
        this.fileNumber = fileNumber;
        this.date = date;
        this.previousHash = prevHash;
        this.merkleRoot = mr;
        this.nonce = n;
    }

    public long getIndex() {
        return index;
    }

    public long getDifficultyLevel() {
        return difficultyLevel;
    }

    public long getTotalDifficulty() {
        return totalDifficulty;
    }

    public int getFileNumber() {
        return fileNumber;
    }

    public String getDate() {
        return date;
    }

    public String getPrevHash() {
        return previousHash;
    }

    public String getMerkleRoot() { return merkleRoot; }

    public long getNonce() { return nonce; }

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
}

class unofficial {
    private final sample.Block block;
    private final PeerAddress peer;

    public unofficial(sample.Block b, PeerAddress pa) {
        this.block = b;
        this.peer = pa;
    }

    public sample.Block getBlock() {
        return block;
    }

    public PeerAddress getPeer() {
        return peer;
    }
}

class MessagesList {
    private LinkedList<sample.Message> ml;
    private LinkedList<String> cl;

    public MessagesList(LinkedList<sample.Message> m, LinkedList<String> c) {
        this.ml = new LinkedList<>(m);
        this.cl = new LinkedList<>(c);
    }

    public LinkedList<sample.Message> getMessagesList() {
        return ml;
    }

    public LinkedList<String> getColoursList() {
        return cl;
    }
}