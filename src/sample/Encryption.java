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

public class Encryption {
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

    public void generateRSAKey() {
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

    public byte[] rsaEncrypt(String msg) {
        try{
            File f = new File("publicKey");
            FileInputStream fis = new FileInputStream(f);
            DataInputStream dis = new DataInputStream(fis);
            byte[] keyBytes = new byte[(int)f.length()];
            dis.readFully(keyBytes);
            dis.close();
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            Key pubkey = kf.generatePublic(spec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(cipher.ENCRYPT_MODE, pubkey);
            return cipher.doFinal(msg.getBytes());
        } catch(Exception e) {
            System.out.println("RSA ENCRPT ERROR: " + e);
            return null;
        }
    }

    public String rsaDecrypt(byte[] ciphertext) {
        try{
            File f = new File("privateKey");
            FileInputStream fis = new FileInputStream(f);
            DataInputStream dis = new DataInputStream(fis);
            byte[] keyBytes = new byte[(int)f.length()];
            dis.readFully(keyBytes);
            dis.close();
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            Key privkey = kf.generatePrivate(spec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(cipher.DECRYPT_MODE, privkey);
            return new String(cipher.doFinal(ciphertext), "UTF-8");
        } catch(Exception e) {
            System.out.println("RSA DECRPT ERROR: " + e);
            return null;
        }
    }


}
