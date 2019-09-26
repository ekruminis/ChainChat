package sample;

import net.tomp2p.connection.Bindings;
import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDirect;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;
import org.apache.commons.lang3.SerializationUtils;
import org.codehaus.jackson.map.ObjectMapper;
import java.io.*;
import java.net.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.*;

public class Network {
    private PeerDHT pdht;
    private File messagesFile;
    private sample.Blockchain chain;
    private LinkedList<sample.chainTip> chainTips = new LinkedList<>();
    private boolean start = true;
    private Peer peer;

    /** Creates RSA key pair for user */
    public void generateRSAKey() {
        try {
            //  If we don't already have a public/private key pair, create one
            if(!new File("publicKey").exists() && !new File("privateKey").exists()) {
                sample.Main.mainController.setProgressPercentage(4);
                sample.Main.mainController.setProgressInfo("generating RSA keys..");

                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(2048);
                KeyPair pair = keyGen.generateKeyPair();
                FileOutputStream fos = new FileOutputStream("publicKey");
                fos.write(pair.getPublic().getEncoded());
                fos.close();

                fos = new FileOutputStream("privateKey");
                fos.write(pair.getPrivate().getEncoded());
                fos.close();
            }
        } catch (Exception e) {
            System.out.println("GEN RSA KEY ERROR: " + e);
        }
    }

    /** Gets msg from P2P network's distributed hash table
     * @param name The key we want to read
     * @return Returns the value of the message
     */
    public String get(String name) {
        try {
            FutureGet futureGet = pdht.get(Number160.createHash(name)).start();
            futureGet.awaitUninterruptibly();
            if (futureGet.isSuccess()) {
                return futureGet.dataMap().values().iterator().next().object().toString();
            }
            return "not found";
        } catch(Exception e) {
            System.out.println("GET ERROR: " + e);
            return null;
        }
    }

    /** Stores msg from P2P network's distributed hash table
     * @param name The key value
     * @param ip The ip of the user
     */
    public void store(String name, String ip) {
        try {
            pdht.put(Number160.createHash(name)).data(new Data(ip)).start().awaitUninterruptibly();
        } catch(Exception e) {
            System.out.println("STORE ERROR:" + e);
        }
    }

    /** JSON Encoding of an object
     * @param o The object we want to encode
     * @return Returns JSON representation of the object
     */
    public String encode(Object o) {
        try {
            ObjectMapper om = new ObjectMapper();
            return om.writeValueAsString(o);
        } catch(Exception e) {
            System.out.println("ENCODING ERR: " + e);
            return null;
        }
    }

    /** Returns the temporary messages file
     * @return Returns the temporary messages file as a File object
     */
    public File getMessagesFile() {
        return messagesFile;
    }

    /** Shutdowns the current peer if one exists */
    public void quitPeer() {
        if(peer != null) {
            peer.shutdown();
        }
    }

    /** Joins the P2P network, creates temp file, and listens for messages from other peers
     * @param adr The IPv4 of the peer we want to bootstrap to
     * @param targetPort The port value of the peer we want to bootstrap to
     * @param myPort The port value of my own port that I want to listen from
     */
    public void connect(String adr, int targetPort, int myPort) {
        try {
            sample.Main.mainController.setProgressPercentage(0);
            sample.Main.mainController.setProgressInfo("");

            // Create temporary file to hold to-mine messages, which is deleted on app exit
            this.messagesFile = File.createTempFile("messages-", ".txt");
            System.out.println("msg's filename: " + messagesFile.getName());
            messagesFile.deleteOnExit();

            Random rnd = new Random();
            Bindings b = new Bindings();
            //b.addInterface("eth3");

            sample.Main.mainController.setProgressPercentage(2);
            sample.Main.mainController.setProgressInfo("creating own peer..");

            // Create and start my own peer client
            peer = new PeerBuilder(new Number160(rnd)).bindings(b).ports(myPort).start();
            pdht = new PeerBuilderDHT(peer).start();

            System.out.println("My Address: " + peer.peerAddress().inetAddress() + ":" + peer.peerAddress().tcpPort());

            // Check if we are trying to bootstrap to ourselves and throw exception if so
            if(peer.peerAddress().inetAddress().toString().substring(1, peer.peerAddress().inetAddress().toString().length()).equals(adr) && peer.peerAddress().tcpPort() == targetPort) {
                sample.Main.mainController.setProgressPercentage(100);
                sample.Main.mainController.setProgressInfo("ERROR: You cannot bootstrap to yourself!");
                throw new Exception("You cannot bootstrap to yourself!");
            }

//                for (; ; ) {
//                    for (PeerAddress pa : peer.peerBean().peerMap().all()) {
//                        System.out.println("peer online (TCP):" + pa);
//                    }
//                    Thread.sleep(2000);
//                }

            // Generating RSA key pair if needed
            sample.Main.mainController.setProgressPercentage(3);
            sample.Main.mainController.setProgressInfo("checking RSA key exist..");
            generateRSAKey();

            this.chain = new sample.Blockchain();

            sample.Main.mainController.setProgressPercentage(5);
            sample.Main.mainController.setProgressInfo("bootstrapping to another peer..");

            // Trying to bootstrap to chosen peer
            FutureBootstrap futureBootstrap = peer.bootstrap().inetAddress(InetAddress.getByName(adr)).ports(targetPort).start();
            futureBootstrap.awaitUninterruptibly();

            // Check if bootstrap was successful
            if(peer.peerBean().peerMap().all().size() == 0) {
                sample.Main.mainController.setProgressPercentage(100);
                sample.Main.mainController.setProgressInfo("could not bootstrap to peer.. try again or continue using the application");
            }
            else {
                sample.Main.mainController.setProgressPercentage(10);
                sample.Main.mainController.setProgressInfo("asking peers for their chaintip..");

                // ask peers for their chaintip
                announce("rct");
                System.out.println("'rct' -> asking peers for their chaintip");
            }

            // Listen for any messages in the network
            peer.objectDataReply(new ObjectDataReply() {
                @Override
                public Object reply(PeerAddress pa, Object o) throws Exception {

                    // Message object received, so verify its signature and if valid add it to our messages file
                    if(o.getClass().getName().equals(sample.Message.class.getName())) {
                        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(getMessagesFile(), true))) {
                            sample.Message msg = (sample.Message) o;
                            System.out.println("MSG RECEIVED: " + o.toString());
                            if(sample.Main.encryption.verifySignature( Base64.getDecoder().decode(msg.getMessage()), Base64.getDecoder().decode(msg.getSignature()), msg.getSender())) {
                                oos.writeObject(o);
                                System.out.println("signature of received message is VALID!");
                            }
                            else {
                                System.out.println("signature of received message is not valid!");
                            }
                        } catch (Exception e) {
                            System.out.println("SAMPLE.MSG RCV ERROR: " + e);
                        }
                    }

                    // Mined block received, so validate it and if so store it
                    else if(o.getClass().getName().equals(sample.Block.class.getName())) {
                        try {
                            System.out.println("\n\nBlock received, checking its validity");
                            // Verify block has been mined successfully ie. hash starts with correct no. of 0's
                            if(new sample.Mining().verifyMined( (sample.Block) o ) ) {
                                System.out.println("block received is mined..");
                                if (chain.validateInChain((sample.Block) o)) {
                                    System.out.println("block is linked, so we add it");
                                    if(chain.validateDifficulty((sample.Block) o)) {
                                        // Store block in LevelDB
                                        String h = new sample.Mining().hash((sample.Block) o);
                                        chain.storeBlock(h, (sample.Block) o);
                                        chain.checkUnofficial(pa);
                                    }
                                    else {
                                        System.out.println("block difficulty levels are not correct!");
                                    }
                                }
                                // Block not in chain, so we ask peer for prevBlock
                                else {
                                    System.out.println("block received is not in chain..");
                                    chain.addUnofficial((sample.Block) o, pa);
                                    System.out.println("asking peer for block: " + ((sample.Block) o).getPreviousHash() );
                                    announce(("b-" + ((sample.Block) o).getPreviousHash()), pa);
                                }
                            }
                            else {
                                System.out.println("Block received not mined");
                            }
                        } catch(Exception e) {
                            System.out.println("SAMPLE.BLOCK RCV ERROR: " + e);
                        }
                    }

                    // LinkedList received
                    else if(o.getClass().getName().equals(LinkedList.class.getName())) {
                        try {
                            Object test = ((LinkedList) o).getFirst();

                            // Check if we received list of chain headers
                            if(test instanceof sample.indexBlock) {
                                System.out.println("Chain headers received from " + pa);
                                System.out.println("\n\nheaders: " + (LinkedList<sample.indexBlock>)o);

                                sample.Main.mainController.setProgressPercentage(35);
                                sample.Main.mainController.setProgressInfo("chain headers received, beginning evaluation of full chain..");

                                if (chain.validateHeaders((LinkedList<sample.indexBlock>) o)) {

                                    sample.Main.mainController.setProgressPercentage(60);
                                    sample.Main.mainController.setProgressInfo("chain headers validated, asking peer for full blockchain..");

                                    System.out.println("headers validated");
                                    System.out.println("\nBlocks to request: " + chain.getRequest() );
                                    announce(chain.getRequest(), pa);
                                } else {
                                    System.out.println("evaluating chaintips agane..");
                                    evaluateTips();
                                }
                            }

                            // Check if we received a list of blocks
                            else if(test instanceof sample.Block) {

                                sample.Main.mainController.setProgressPercentage(64);
                                sample.Main.mainController.setProgressInfo("full blockchain received, storing valid blocks..");

                                System.out.println("Chain of blocks received from " + pa);
                                boolean success = chain.syncBlocks( (LinkedList<sample.Block>) o );

                                // if some blocks are missing, request them again
                                if(!success) {

                                    sample.Main.mainController.setProgressPercentage(70);
                                    sample.Main.mainController.setProgressInfo("not all blocks received, asking peer for missing blocks..");

                                    System.out.println("LinkedList -> not all blockchain blocks received");
                                    announce(chain.getRequest(), pa);
                                }

                                // Initial connection finished, allow user to fully use the app
                                else if(success) {
                                    sample.Main.mainController.setProgressPercentage(100);
                                    sample.Main.mainController.setProgressInfo("SUCCESS!! Blockchain data syncing finished, you can now begin messaging..");
                                }
                            }

                        } catch(Exception e) {
                            System.out.println("LINKED LIST RCV ERROR: " + e);
                        }
                    }

                    // List of blocks requested by peer received
                    else if(o.getClass().getName().equals(ArrayList.class.getName())) {
                        System.out.println("\npeer " + pa + " ; requested my blockchain");
                        LinkedList<sample.Block> blockchain = chain.getRequestedBlocks( (ArrayList<String>)o );

                        System.out.println("\n\nMy blockchain is: " + blockchain);
                        System.out.println("sending my blockchain linkedlist..");
                        announce(blockchain, pa);
                    }

                    // ChainTip received -> add tips, start countdown (wait X seconds for replies, then start evaluating them)
                    else if(o.getClass().getName().equals(sample.indexBlock.class.getName())) {
                        sample.Main.mainController.setProgressPercentage(15);
                        sample.Main.mainController.setProgressInfo("storing chaintip headers..");

                        System.out.println("chaintip received from " + pa + "\nheader: " + o);
                        sample.chainTip ct = new sample.chainTip( (sample.indexBlock)o, pa);
                        chainTips.add(ct);

                        System.out.println("chainTips: " + chainTips);
                        if(start) {
                            sample.Main.mainController.setProgressPercentage(17);
                            sample.Main.mainController.setProgressInfo("waiting for more chaintip headers..");
                            System.out.println("Chaintip countdown started..");
                        }
                        startCount(start);
                        start = false;

                    }

                    // String message received
                    else if(o.getClass().getName().equals(String.class.getName())) {
                        try{
                            String msg = o.toString();
                            System.out.println("message " + msg + " received from " + pa);

                            // Request ChainTip msg received
                            if(msg.equals("rct")) {
                                sample.indexBlock ib = chain.getChainTip();
                                // if no chaintip exists, send nothing
                                if(ib == null) {
                                    announce("ct-null", pa);
                                }
                                else {
                                    announce(ib, pa);
                                }
                            }

                            // Request Block msg received
                            else if(msg.startsWith("b-")) {
                                String hash = msg.substring(2);
                                byte[] res = chain.read(chain.getBlockDB(), hash.getBytes());
                                sample.Block b = (sample.Block) SerializationUtils.deserialize(res);
                                announce(b, pa);
                            }

                            // Request all Chain Headers msg received
                            else if(msg.startsWith("rch-")) {
                                String hash = msg.substring(4);
                                LinkedList<sample.indexBlock> toSend = chain.getHeaders(hash);
                                announce(toSend, pa);
                            }

                            else if(msg.equals("ct-null")) {
                                sample.Main.mainController.setProgressPercentage(100);
                                sample.Main.mainController.setProgressInfo("No chaintips were found.. You can now use the application");
                            }

                        } catch(Exception e) {
                            System.out.println("STRING RCV ERROR: " + e);
                        }
                    }

                    else{
                        System.out.println("Unknown MSG received: " + o.toString());
                    }

                    return null;
                }
            });

        } catch(Exception e) {
            System.out.println("CONNECT ERROR: " + e);
            peer.shutdown();
        }
    }

    /** Wait for 10sec, then start evaluating chain tips
     * @param start Boolean value used to make sure count is only executed once
     */
    private void startCount(boolean start) {
        if(start == true) {
            final Timer t = new java.util.Timer();
            t.schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            evaluateTips();
                            t.cancel();
                        }
                    },
                    10000
            );
            sample.Main.mainController.setProgressPercentage(18);
            sample.Main.mainController.setProgressInfo("beginning evaluation of chaintip headers..");
        }
    }
    /** Evaluates potential chaintips to find the one with the highest difficulty and then contacts the peer to get full header chain */
    public void evaluateTips() {
        System.out.println("started evaluating tips");
        sample.chainTip currentCT = null;

        System.out.println("all tips: " + chainTips);

        for (sample.chainTip ct : chainTips) {
            if( currentCT == null || (currentCT.getIb().getTotalDifficulty() < ct.getIb().getTotalDifficulty()) ) {
                currentCT = ct;
            }
        }
        System.out.println("finished and current highest tip: " + currentCT.toString());
        chainTips.remove(currentCT);
        System.out.println("removed from chainTips linked-list");

        PeerAddress peer = currentCT.getPa();
        String hash = chain.getConfirmed();

        System.out.println("data gathered");

        sample.Main.mainController.setProgressPercentage(25);
        sample.Main.mainController.setProgressInfo("highest chaintip found, asking peer for full chain of headers..");

        announce(("rch-"+hash), peer);
        System.out.println("rch-"+hash+", sent to " + peer);
        // if no/incorrect response, start evaluation again..
    }


    /** Sends a message(text) directly to all peers it knows
     * @msg The message object we want to transmit
     */
    public void announce(sample.Message msg) {
        try{
            System.out.println("p adr: " + pdht.peer().peerBean().peerMap().all());
            // Transmit message to each peer individually
            for(PeerAddress pa : pdht.peer().peerBean().peerMap().all()) {
                FutureDirect fd = pdht.peer().sendDirect(pa).object(msg).start();

                fd.addListener(new BaseFutureAdapter<FutureDirect>() {
                    @Override
                    public void operationComplete(FutureDirect future) throws Exception {
                        if(future.isSuccess()) { // this flag indicates if the future was successful
                            System.out.println("sample.Message sent successfully");
                        } else {
                            System.out.println("sample.Message sent fail");
                        }
                    }
                });
            }

        } catch(Exception e) {
            System.out.println("MSG ERROR: " + e);
        }
    }

    /** Sends a mined block directly to all peers it knows
     * @param b The block object we want to transmit
     */
    public void announce(sample.Block b) {
        try{
            System.out.println("p adr: " + pdht.peer().peerBean().peerMap().all());
            // Transmit message to each peer individually
            for(PeerAddress pa : pdht.peer().peerBean().peerMap().all()) {
                FutureDirect fd = pdht.peer().sendDirect(pa).object(b).start();

                fd.addListener(new BaseFutureAdapter<FutureDirect>() {
                    @Override
                    public void operationComplete(FutureDirect future) throws Exception {
                        if(future.isSuccess()) { // this flag indicates if the future was successful
                            System.out.println("sample.Block (mined) sent successfully");
                        } else {
                            System.out.println("sample.Block (mined) sent fail");
                        }
                    }
                });
            }

        } catch(Exception e) {
            System.out.println("PUBLIC BLOCK ANNON ERROR: " + e);
        }
    }

    /** Sends a mined block directly to a particular peer
     * @param b The block object we want to transmit
     * @param pa The address of the peer we want to send the object to
     */
    public void announce(sample.Block b, PeerAddress pa) {
        try{
            // Transmit message to peer directly
            FutureDirect fd = pdht.peer().sendDirect(pa).object(b).start();

            fd.addListener(new BaseFutureAdapter<FutureDirect>() {
                @Override
                public void operationComplete(FutureDirect future) throws Exception {
                    if(future.isSuccess()) { // this flag indicates if the future was successful
                        System.out.println("mined block to particular peer sent successfully");
                    } else {
                        System.out.println("mined block to particular sent fail");
                    }
                }
            });
        } catch(Exception e) {
            System.out.println("PARTICULAR BLOCK ANNON ERROR : " + e);
        }
    }

    /** Sends a block header to a particular peer
     * @param ib The block header object we want to transmit
     * @param pa The address of the peer we want to send the object to
     */
    public void announce(sample.indexBlock ib, PeerAddress pa) {
        try{
            // Transmit message to peer directly
            FutureDirect fd = pdht.peer().sendDirect(pa).object(ib).start();

            fd.addListener(new BaseFutureAdapter<FutureDirect>() {
                @Override
                public void operationComplete(FutureDirect future) throws Exception {
                    if(future.isSuccess()) { // this flag indicates if the future was successful
                        System.out.println("chaintip to particular peer sent successfully");
                    } else {
                        System.out.println("chaintip to particular peer sent fail");
                    }
                }
            });
        } catch(Exception e) {
            System.out.println("PARTICULAR INDEX BLOCK ANNON ERROR : " + e);
        }
    }

    /** Sends a LinkedList of block headers to a particular peer
     * @param lib The LinkedList we want to transmit
     * @param pa The address of the peer we want to send the object to
     */
    public void announce(LinkedList lib, PeerAddress pa) {
        try{
            // Transmit message to peer directly
            FutureDirect fd = pdht.peer().sendDirect(pa).object(lib).start();

            fd.addListener(new BaseFutureAdapter<FutureDirect>() {
                @Override
                public void operationComplete(FutureDirect future) throws Exception {
                    if(future.isSuccess()) { // this flag indicates if the future was successful
                        System.out.println("linkedlist to particular peer sent successfully");
                    } else {
                        System.out.println("linkedlist to particular peer sent fail");
                    }
                }
            });
        } catch(Exception e) {
            System.out.println("PARTICULAR LINKEDLIST ANNON ERROR : " + e);
        }
    }

    /** Sends a LinkedList of block hashes to a particular peer
     * @param lib The ArrayList we want to transmit
     * @param pa The address of the peer we want to send the object to
     */
    public void announce(ArrayList lib, PeerAddress pa) {
        try{
            // Transmit message to peer directly
            FutureDirect fd = pdht.peer().sendDirect(pa).object(lib).start();

            fd.addListener(new BaseFutureAdapter<FutureDirect>() {
                @Override
                public void operationComplete(FutureDirect future) throws Exception {
                    if(future.isSuccess()) { // this flag indicates if the future was successful
                        System.out.println("arraylist to particular peer sent successfully");
                    } else {
                        System.out.println("arraylist to particular peer sent fail");
                    }
                }
            });
        } catch(Exception e) {
            System.out.println("PARTICULAR ARRAYLIST ANNON ERROR : " + e);
        }
    }

    /** Sends a String msg to all peers it knows
     * @param s The String object we want to transmit*/
    public void announce(String s) {
        try{
            System.out.println("p adr: " + pdht.peer().peerBean().peerMap().all());
            // Transmit message to each peer individually
            for (PeerAddress pa : pdht.peer().peerBean().peerMap().all()) {
                FutureDirect fd = pdht.peer().sendDirect(pa).object(s).start();

                fd.addListener(new BaseFutureAdapter<FutureDirect>() {
                    @Override
                    public void operationComplete(FutureDirect future) throws Exception {
                        if(future.isSuccess()) { // this flag indicates if the future was successful
                            System.out.println("string sent successfully");
                        } else {
                            System.out.println("string sent fail");
                        }
                    }
                });
            }
        } catch(Exception e) {
            System.out.println("PUBLIC STRING ANNON ERROR: " + e);
        }
    }

    /** Sends a String msg to a particular peer
     * @param s The String object we want to transmit
     * @param pa The address of the peer want to send the object to
     */
    public void announce(String s, PeerAddress pa) {
        try{
            // Transmit message to peer directly
            FutureDirect fd = pdht.peer().sendDirect(pa).object(s).start();

            fd.addListener(new BaseFutureAdapter<FutureDirect>() {
                @Override
                public void operationComplete(FutureDirect future) throws Exception {
                    if(future.isSuccess()) { // this flag indicates if the future was successful
                        System.out.println("string to particular peer sent successfully");
                    } else {
                        System.out.println("string to particular peer sent fail");
                    }
                }
            });
        } catch(Exception e) {
            System.out.println("PARTICULAR STRING ANNON ERROR: " + e);
        }
    }

    /** Return the chain class object
     * @return Returns the current chain class object
     */
    public sample.Blockchain getChain() {
        return chain;
    }
}

/** The chaintip object which is used to determine which chaintip we want to further examine and which peer we wanted to contact to do so, used when first opening up app */
class chainTip {
    private final sample.indexBlock ib;
    private final PeerAddress pa;

    public chainTip(sample.indexBlock block, PeerAddress peer) {
        this.ib = block;
        this.pa = peer;
    }

    public sample.indexBlock getIb() {
        return ib;
    }

    public PeerAddress getPa() {
        return pa;
    }

    @Override
    public String toString() {
        return "chainTip{" +
                "ib=" + ib +
                ", pa=" + pa +
                '}';
    }
}