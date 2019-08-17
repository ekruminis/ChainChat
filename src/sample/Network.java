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
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Network {
    // TODO download() - download entire blockchain
    // TODO getUsers() - download list of current users + their PubKeys
    // TODO register() - generate userID and RSA key, distribute details to others
    // TODO delete()   - delete RSA keys from user

    private PeerDHT pdht;
    private File messagesFile;
    private sample.Blockchain chain;

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

            // ask peers for their chaintip
            announce("rct");

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
                            // Verify block has been mined
                            if(new sample.Mining().verifyBlock( (sample.Block) o )) {
                                // Store block in LevelDB
                                // TODO (more verification/validation on blocks eg. check signatures, check it reaches genesis)
                                String h = new sample.Mining().hash((sample.Block)o);
                                chain.storeBlock(h, (sample.Block) o);
                            }
                            else {
                                System.out.println("Block received not valid");
                            }
                        } catch(Exception e) {
                            System.out.println("SAMPLE.BLOCK RCV ERROR: " + e);
                        }
                    }

                    else if(o.getClass().getName().equals(LinkedList.class.getName())) {
                        try {
                            // TODO (start validation+sync of chain headers)
                        } catch(Exception e) {
                            System.out.println("LINKED LIST RCV ERROR: " + e);
                        }
                    }

                    else if(o.getClass().getName().equals(sample.indexBlock.class.getName())) {
                        // TODO (add pa+res pair to memory, then call method to act on highest chaintip)
                    }

                    // String message received
                    else if(o.getClass().getName().equals(String.class.getName())) {
                        try{
                            String msg = o.toString();
                            // Request ChainTip msg received
                            if(msg.equals("rct")) {
                                // TODO (send own chaintip)
                                sample.indexBlock ib = chain.getChainTip();
                                announce(ib, pa);
                            }

                            // Chain Tip msg received
                            else if(msg.startsWith("ct-")) {
                                String res = msg.substring(3);
                                // TODO (add pa+res pair to memory, then call method to act on highest chaintip)
                            }

                            // Request Block msg received
                            else if(msg.startsWith("b-")) {
                                String hash = msg.substring(2);
                                byte[] res = chain.read(chain.getBlockDB(), hash.getBytes());
                                sample.Block b = (sample.Block) SerializationUtils.deserialize(res);
                                announce(b, pa);
                            }
                            // Request all Chain Headers msg received
                            else if(msg.equals("rch")) {
                                // TODO (send own chain headers to pa as a LinkedList<sample.indexBlock> -> get head then keep moving down from prevHash value )
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


    /** Sends a message(text) directly to all peers it knows */
    public void announce(sample.Message msg) {
        try{
            System.out.println("p adr: " + pdht.peer().peerBean().peerMap().all());
            for(PeerAddress pa : pdht.peer().peerBean().peerMap().all()) {
                FutureDirect fd = pdht.peer().sendDirect(pa).object(msg).start();
                fd.awaitUninterruptibly();
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
                fd.awaitUninterruptibly();
            }

        } catch(Exception e) {
            System.out.println("PUBLIC BLOCK ANNON ERROR: " + e);
        }
    }

    /** Sends a mined block directly to a particular peer */
    public void announce(sample.Block b, PeerAddress pa) {
        try{
            FutureDirect fd = pdht.peer().sendDirect(pa).object(b).start();
            fd.awaitUninterruptibly();

        } catch(Exception e) {
            System.out.println("PARTICULAR BLOCK ANNON ERROR : " + e);
        }
    }

    public void announce(sample.indexBlock ib, PeerAddress pa) {
        try{
            FutureDirect fd = pdht.peer().sendDirect(pa).object(ib).start();
            fd.awaitUninterruptibly();
        } catch(Exception e) {
            System.out.println("PARTICULAR BLOCK ANNON ERROR : " + e);
        }
    }

    /** Sends a String msg to all peers it knows */
    public void announce(String s) {
        try{
            System.out.println("p adr: " + pdht.peer().peerBean().peerMap().all());
            for (PeerAddress pa : pdht.peer().peerBean().peerMap().all()) {
                FutureDirect fd = pdht.peer().sendDirect(pa).object(s).start();
                fd.awaitUninterruptibly();
            }
        } catch(Exception e) {
            System.out.println("PUBLIC STRING ANNON ERROR: " + e);
        }
    }

}