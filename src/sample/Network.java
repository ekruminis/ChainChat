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
import sun.nio.ch.Net;

import java.io.*;
import java.net.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.List;
import java.util.Random;

public class Network {
    // TODO download() - download entire blockchain
    // TODO getUsers() - download list of current users + their PubKeys
    // TODO connect()  - connect to blockchain network
    // TODO register() - generate userID and RSA key, distribute details to others
    // TODO delete()   - delete RSA keys from user

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

    private PeerDHT pdht;

    public File getMessagesFile() {
        return messagesFile;
    }

    private File messagesFile;

    public void connect(String adr, int port) {
        try {
            this.messagesFile = File.createTempFile("messages-", ".txt");
            System.out.println("filename: " + messagesFile.getName());
            messagesFile.deleteOnExit();

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

            peer.objectDataReply(new ObjectDataReply() {
                @Override
                public Object reply(PeerAddress peerAddress, Object o) throws Exception {
//                    System.err.println("I'm " + peer.peerID() + " and I just got the message [" + o
//                            + "] from " + peerAddress.peerId());

//                    try(FileWriter fw = new FileWriter(messagesFile, true);
//                        BufferedWriter bw = new BufferedWriter(fw);
//                        PrintWriter out = new PrintWriter(bw))
//                    {
//                        out.println(o.toString());
//                        System.out.println("message received: " + o.toString());
//                    } catch (IOException e) {
//                        System.out.println("RCV MSG ERROR: " + e);
//                    }

                    try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(getMessagesFile(), true))) {
                        oos.writeObject(o);
                    } catch(Exception e) {
                        System.out.println("REPLY ERROR: " + e);
                    }

                    return null;
                }
            });

        } catch(Exception e) {
            System.out.println("CONNECT ERROR: " + e);
        }
    }

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

    public void store(String name, String ip) {
        try {
            pdht.put(Number160.createHash(name)).data(new Data(ip)).start().awaitUninterruptibly();
        } catch(Exception e) {
            System.out.println("STORE ERROR:" + e);
        }
    }

    public void msg(sample.message msg) {
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

}