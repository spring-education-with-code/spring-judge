package org.example;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// 출처 : https://wiki1.kr/index.php/SHA256#cite_note-8
public class Sha256 {

    public String encrypt(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(text.getBytes());
            return bytesToHex(md.digest());
        }catch(NoSuchAlgorithmException e){
            e.printStackTrace();
            return "NoSuchAlgorithmException";
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
