package sample;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Encryption {

    /** Creates signature for a message (SHA512withRSA)
     * @param message The message itself
     * @return Returns the signature as a byte[]
     */
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

    /** Checks if signature is valid (SHA512withRSA)
     * @param message The message itself
     * @param signature The signature of the message
     * @param pubKey The public key of the signature signee
     * @return Returns whether the signature is valid (true) or not (false)
     */
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

    /** AES encryption of string message (AES256/CBC/PKCS5Padding)
     * @param message The message that needs to be encrypted
     * @param key The key with which to encrypt
     * @return Returns the encrypted text as a byte[]
     */
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
    /** AES decryption of ciphertext (AES256/CBC/PKCS5Padding)
     * @param ciphertext The encrypted text
     * @param key The key with which to decrypt
     * @return Returns the encrypted message as a String
     */
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

    /** Randomly creates an AES key (AES256)
     * @return Returns the key as a Key object
     */
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

    /** RSA encryption of key (RSA2048/ECB/PKCS1Padding)
     *  @param key The key with which to encrypt (recipients public key)
     *  @param msg The message which is to be encrypted
     *  @return Returns the encrypted message as a byte[]
     */
    public byte[] rsaEncrypt(String key, String msg) {
        try{
            byte[] keyBytes = Base64.getDecoder().decode(key.getBytes());
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            Key pubkey = keyFactory.generatePublic(spec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(cipher.ENCRYPT_MODE, pubkey);
            return cipher.doFinal(msg.getBytes());
        } catch(Exception e) {
            System.out.println("RSA ENCRPT ERROR: " + e);
            return null;
        }
    }

    /** RSA decryption of cipherkey (RSA2048/ECB/PKCS1Padding)
     * @param ciphertext The message which needs to be encrypted
     * @return Returns the decrypted message
     */
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

    /** Returns users public key
     * @return Returns the users public key as a Key object
     */
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

    /** Returns users private key
     * @return Returns the users private key as a Key object
     */
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
