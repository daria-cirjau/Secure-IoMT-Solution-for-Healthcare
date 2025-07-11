package org.disertatie;

import COSE.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import com.upokecenter.cbor.CBORObject;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;

public class FhirCryptoUtil {
    private static final byte[] AES_KEY = loadKey();

    private static byte[] loadKey() {
        try {
            Path path = Paths.get("/app/certs/aes.key");
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't load AES key", e);
        }
    }

    private static final CBORMapper cborMapper = new CBORMapper();

    public static JsonNode decryptCose(byte[] coseBytes) throws CoseException, IOException {
        Encrypt0Message msg = (Encrypt0Message) Message.DecodeFromBytes(coseBytes);
        byte[] decrypted = msg.decrypt(AES_KEY);
        return cborMapper.readTree(decrypted);
    }

    public static byte[] encryptFhirJson(JsonNode jsonNode) throws Exception {
        byte[] payload = cborMapper.writeValueAsBytes(jsonNode);

        Encrypt0Message msg = new Encrypt0Message();
        msg.addAttribute(HeaderKeys.Algorithm, AlgorithmID.AES_GCM_256.AsCBOR(), Attribute.PROTECTED);

        msg.SetContent(payload);
        msg.encrypt(AES_KEY);

        return msg.EncodeToBytes();
    }

}