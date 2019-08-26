package sample;

import com.sun.corba.se.impl.orbutil.ObjectWriter;
import com.sun.security.ntlm.Server;
import fr.rhaz.ipfs.IPFSDaemon;
import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import net.tomp2p.connection.Bindings;
import net.tomp2p.connection.PeerConnection;
import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.FuturePut;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDirect;
import net.tomp2p.futures.FutureDiscover;
import net.tomp2p.message.Message;
import net.tomp2p.nat.FutureNAT;
import net.tomp2p.nat.FutureRelayNAT;
import net.tomp2p.nat.PeerBuilderNAT;
import net.tomp2p.nat.PeerNAT;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.peers.PeerSocketAddress;
import net.tomp2p.relay.BaseRelayClient;
import net.tomp2p.relay.RelayClientConfig;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;
import net.tomp2p.tracker.PeerTracker;
import org.apache.commons.lang3.SerializationUtils;
import org.codehaus.jackson.map.ObjectMapper;
import sun.nio.ch.Net;

import java.io.*;
import java.net.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.*;

public class Network {
    // TODO download() - download entire blockchain
    // TODO getUsers() - download list of current users + their PubKeys
    // TODO register() - generate userID and RSA key, distribute details to others
    // TODO delete()   - delete RSA keys from user

    private PeerDHT pdht;
    private File messagesFile;
    private sample.Blockchain chain;
    private LinkedList<sample.chainTip> chainTips = new LinkedList<>();
    private boolean start = true;

    /** Creates RSA key pair for user */
    public void generateRSAKey() {
        // TODO warn about overriding? or just create with new name
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair pair = keyGen.generateKeyPair();
            FileOutputStream fos = new FileOutputStream("publicKey");
            fos.write(pair.getPublic().getEncoded());
            fos.close();

            fos = new FileOutputStream("privateKey");
            fos.write(pair.getPrivate().getEncoded());
            fos.close();
        } catch (Exception e) {
            System.out.println("GEN RSA KEY ERROR: " + e);
        }
    }

    // IPFS -> https://github.com/ipfs/java-ipfs-http-client
    //      -> https://ianopolous.github.io/java/IPFS
    //
    // Daemon -> https://github.com/hazae41/jvm-ipfs-daemon
    /** Uploads block into IPFS network */
    public String ipfsUpload(sample.Block b) {
        IPFS ipfs = new IPFS(new MultiAddress("/ip4/127.0.0.1/tcp/5001"));

        try {
            NamedStreamable ns = new NamedStreamable.ByteArrayWrapper(b.toString().getBytes());
            List<MerkleNode> tree = ipfs.add(ns);
            return tree.get(0).hash.toString();

        } catch (Exception e) {
            System.out.println("NETWORK ERROR: " + e);
            return null;
        }
    }

    /** Gets msg from P2P network's distributed hash table */
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

    /** Stores msg from P2P network's distributed hash table */
    public void store(String name, String ip) {
        try {
            pdht.put(Number160.createHash(name)).data(new Data(ip)).start().awaitUninterruptibly();
        } catch(Exception e) {
            System.out.println("STORE ERROR:" + e);
        }
    }

    /** JSON Encoding of object */
    public String encode(Object o) {
        try {
            ObjectMapper om = new ObjectMapper();
            return om.writeValueAsString(o);
        } catch(Exception e) {
            System.out.println("ENCODING ERR: " + e);
            return null;
        }
    }

    /** Returns temp messages file */
    public File getMessagesFile() {
        return messagesFile;
    }

    /** Joins the P2P network, creates temp file, and listens for messages saving them in file */
    public void connect(String adr, int port) {
        try {
            this.messagesFile = File.createTempFile("messages-", ".txt");
            System.out.println("filename: " + messagesFile.getName());
            messagesFile.deleteOnExit();

            this.chain = new sample.Blockchain();

            Random rnd = new Random();
            Bindings b = new Bindings();
            //b.addInterface("eth3");
            Peer peer = new PeerBuilder(new Number160(rnd)).bindings(b).ports(port).start();

            pdht = new PeerBuilderDHT(peer).start();

            System.out.println("My Address: " + peer.peerAddress().inetAddress() + ":" + peer.peerAddress().tcpPort());
//                for (; ; ) {
//                    for (PeerAddress pa : peer.peerBean().peerMap().all()) {
//                        System.out.println("peer online (TCP):" + pa);
//                    }
//                    Thread.sleep(2000);
//                }
            FutureBootstrap futureBootstrap = peer.bootstrap().inetAddress(InetAddress.getByName(adr)).ports(4001).start();
            futureBootstrap.awaitUninterruptibly();

            generateRSAKey();

            // ask peers for their chaintip
            announce("rct");
            System.out.println("'rct' -> asking peers for their chaintip");

            peer.objectDataReply(new ObjectDataReply() {
                @Override
                public Object reply(PeerAddress pa, Object o) throws Exception {

                    // Message received
                    if(o.getClass().getName().equals(sample.Message.class.getName())) {
                        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(getMessagesFile(), true))) {
                            oos.writeObject(o);
                            System.out.println("MSG RECEIVED: " + o.toString());
                        } catch (Exception e) {
                            System.out.println("SAMPLE.MSG RCV ERROR: " + e);
                        }
                    }

                    // Mined block received
                    else if(o.getClass().getName().equals(sample.Block.class.getName())) {
                        try {
                            System.out.println("\n\nBlock received, checking its validity");
                            // Verify block has been mined successfully ie. hash starts with correct no. of 0's
                            if(new sample.Mining().verifyMined( (sample.Block) o ) ) {
                                System.out.println("block received is mined..");
                                if(chain.validateDifficulty( (sample.Block) o )) {
                                    System.out.println("block received is in correct difficulty..");
                                    if (chain.validateInChain((sample.Block) o)) {
                                        System.out.println("block is linked, so we add it");
                                        // Store block in LevelDB
                                        // TODO (more verification/validation on blocks eg. check signatures? )
                                        String h = new sample.Mining().hash((sample.Block) o);
                                        chain.storeBlock(h, (sample.Block) o);
                                        chain.checkUnofficial(pa);
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
                                    System.out.println("Block received not correct difficulty");
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

                                if (chain.validateHeaders((LinkedList<sample.indexBlock>) o)) {
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
                                System.out.println("Chain of blocks received from " + pa);
                                boolean success = chain.syncBlocks( (LinkedList<sample.Block>) o );

                                // if some blocks are missing, request them again
                                if(!success) {
                                    System.out.println("LinkedList -> not all blockchain blocks received");
                                    announce(chain.getRequest(), pa);
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

                    // ChainTip received -> add tips, start countdown (wait 10sec for replies, then start evaluating them)
                    else if(o.getClass().getName().equals(sample.indexBlock.class.getName())) {
                        System.out.println("chaintip received from " + pa + "\nheader: " + o);
                        sample.chainTip ct = new sample.chainTip( (sample.indexBlock)o, pa);
                        chainTips.add(ct);

                        System.out.println("chainTips: " + chainTips);
                        if(start) {
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
                                announce(ib, pa);
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
        }
    }

    /** Wait for 4sec, then start evaluating chain tips */
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
                    4000
            );
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

        announce(("rch-"+hash), peer);
        System.out.println("rch-"+hash+", sent to " + peer);
        // if no/incorrect response, start evaluation again..
    }


    /** Sends a message(text) directly to all peers it knows */
    public void announce(sample.Message msg) {
        try{
            System.out.println("p adr: " + pdht.peer().peerBean().peerMap().all());
            for(PeerAddress pa : pdht.peer().peerBean().peerMap().all()) {
                FutureDirect fd = pdht.peer().sendDirect(pa).object(msg).start();
                //fd.awaitUninterruptibly();
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

    /** Sends a mined block directly to all peers it knows */
    public void announce(sample.Block b) {
        try{
            System.out.println("p adr: " + pdht.peer().peerBean().peerMap().all());
            for(PeerAddress pa : pdht.peer().peerBean().peerMap().all()) {
                FutureDirect fd = pdht.peer().sendDirect(pa).object(b).start();
                //fd.awaitUninterruptibly();
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

    /** Sends a mined block directly to a particular peer */
    public void announce(sample.Block b, PeerAddress pa) {
        try{
            FutureDirect fd = pdht.peer().sendDirect(pa).object(b).start();
            //fd.awaitUninterruptibly();
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

    /** Sends a block header to a particular peer */
    public void announce(sample.indexBlock ib, PeerAddress pa) {
        try{
            FutureDirect fd = pdht.peer().sendDirect(pa).object(ib).start();
            //fd.awaitUninterruptibly();
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

    /** Sends a LinkedList of block headers to a particular peer */
    public void announce(LinkedList lib, PeerAddress pa) {
        try{
            FutureDirect fd = pdht.peer().sendDirect(pa).object(lib).start();
            //fd.awaitUninterruptibly();
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

    /** Sends a LinkedList of block hashes to a particular peer */
    public void announce(ArrayList lib, PeerAddress pa) {
        try{
            FutureDirect fd = pdht.peer().sendDirect(pa).object(lib).start();
            //fd.awaitUninterruptibly();
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

    /** Sends a String msg to all peers it knows */
    public void announce(String s) {
        try{
            System.out.println("p adr: " + pdht.peer().peerBean().peerMap().all());
            for (PeerAddress pa : pdht.peer().peerBean().peerMap().all()) {
                FutureDirect fd = pdht.peer().sendDirect(pa).object(s).start();
                //fd.awaitUninterruptibly();
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

    /** Sends a String msg to a particular peer */
    public void announce(String s, PeerAddress pa) {
        try{
            FutureDirect fd = pdht.peer().sendDirect(pa).object(s).start();
            //fd.awaitUninterruptibly();
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

    public sample.Blockchain getChain() {
        return chain;
    }
}

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