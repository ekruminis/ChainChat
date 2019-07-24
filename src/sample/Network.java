package sample;

import fr.rhaz.ipfs.IPFSDaemon;
import io.ipfs.api.*;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.List;

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
        } catch(Exception e) {
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

        } catch(Exception e) {
            System.out.println("NETWORK ERROR: " + e);
            return null;
        }
    }
}
