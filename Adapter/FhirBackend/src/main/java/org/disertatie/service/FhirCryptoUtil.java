package org.disertatie.service;

import COSE.CoseException;
import COSE.Encrypt0Message;
import COSE.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
}