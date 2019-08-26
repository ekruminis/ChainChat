package sample;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Encryption {

    /** Creates signature for message */
    public byte[] generateSignature(byte[] message) {
        try {
            Signature s = Signature.getInstance("SHA512withRSA");
            SecureRandom sr = new SecureRandom();
            s.initSign((PrivateKey)getRSAPrivate(), sr);

            s.update(message);
            return s.sign();
        } catch(Exception e) {
            System.out.println("SIGN ERORR: " + e);
            return null;
        }
    }

    /** Checks if signature is valid
     *
     * eg.     sample.Encryption e = new sample.Encryption();
     *         String pk = Base64.getEncoder().encodeToString(e.getRSAPublic().getEncoded());
     *         System.out.println(pk);
     *
     *         byte[] signature = e.generateSignature("hello".getBytes());
     *         System.out.println("\n\n" + Base64.getEncoder().encodeToString(signature));
     *         boolean ans = e.verifySignature("hello".getBytes(), signature, pk);
     *         System.out.println("\n\nvalid: " + ans);
     *
     *         */
    public boolean verifySignature(byte[] message, byte[] signature, String pubKey) {
        try {

            byte[] publicBytes = Base64.getDecoder().decode(pubKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
            PublicKey pk = KeyFactory.getInstance("RSA").generatePublic(keySpec);

            Signature s = Signature.getInstance("SHA512withRSA");
            s.initVerify(pk);

            s.update(message);
            return s.verify(signature);

        } catch(Exception e) {
            System.out.println("VERIFY SIGN ERROR: " + e);
            return false;
        }
    }

    /** AES encryption of string message */
    public byte[] aesEncrypt(String message, Key key){
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] ivByte = new byte[cipher.getBlockSize()];
            IvParameterSpec ivParams = new IvParameterSpec(ivByte);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivParams);
            byte[] ciphertext = cipher.doFinal(message.getBytes("UTF-8"));
            return ciphertext;
        } catch(Exception e) {
            System.out.println("ENCP ERROR: " + e);
            return null;
        }
    }
    /** AES decryption of ciphertext */
    public String aesDecrypt(byte[] ciphertext, Key key){
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] ivByte = new byte[cipher.getBlockSize()];
            IvParameterSpec ivParamsSpec = new IvParameterSpec(ivByte);
            cipher.init(Cipher.DECRYPT_MODE, key, ivParamsSpec);
            return new String(cipher.doFinal(ciphertext), "UTF-8");
        } catch(Exception e) {
            System.out.println("DECP ERROR: " + e);
            return null;
        }
    }

    /** Randomly creates an AES key */
    public Key generateAESKey(){
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            SecretKey key = generator.generateKey();
            return key;
        } catch(Exception e) {
            System.out.println("GENKEY ERROR: " + e);
            return null;
        }
    }

    /** RSA encryption of key */
    public byte[] rsaEncrypt(String msg) {
        try{
            Key pubkey = getRSAPublic();

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(cipher.ENCRYPT_MODE, pubkey);
            return cipher.doFinal(msg.getBytes());
        } catch(Exception e) {
            System.out.println("RSA ENCRPT ERROR: " + e);
            return null;
        }
    }

    /** RSA decryption of cipherkey */
    public String rsaDecrypt(byte[] ciphertext) {
        try{
            Key privkey = getRSAPrivate();

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(cipher.DECRYPT_MODE, privkey);
            return new String(cipher.doFinal(ciphertext), "UTF-8");
        } catch(Exception e) {
            System.out.println("RSA DECRPT ERROR: " + e);
            return null;
        }
    }

    /** Returns users public key */
    public Key getRSAPublic() {
        try {
            File f = new File("publicKey");
            FileInputStream fis = new FileInputStream(f);
            DataInputStream dis = new DataInputStream(fis);
            byte[] keyBytes = new byte[(int)f.length()];
            dis.readFully(keyBytes);
            dis.close();
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        } catch(Exception e) {
            System.out.println("GET RSA PRIV ERROR: " + e);
            return null;
        }
    }

    /** Returns users private key */
    private Key getRSAPrivate() {
        try {
            File f = new File("privateKey");
            FileInputStream fis = new FileInputStream(f);
            DataInputStream dis = new DataInputStream(fis);
            byte[] keyBytes = new byte[(int) f.length()];
            dis.readFully(keyBytes);
            dis.close();
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        } catch(Exception e) {
            System.out.println("GET RSA PUB ERROR: " + e);
            return null;
        }
    }


}
