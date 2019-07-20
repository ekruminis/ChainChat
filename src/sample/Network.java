package sample;

import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

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
}
