/*
 * The MIT License
 *
 * Copyright (c) 2017, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.jenkins.blueocean.service.embedded.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;

/**
 * Utilities to generate a private key and perform necessary conversions
 * on it
 * @author kzantow
 */
public class SSHKeyUtils {
    
    /**
     * Generates a new RSA key with specified keySize
     * @param keySize size to use for the key
     * @return a public/private key pair
     */
    public static KeyPair generateRSAKey(int keySize) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(keySize);
            return generator.genKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Encodes the public key according to some spec somewhere
     * @param key public key to use
     * @return the ssh-rsa bytes
     */
    public static byte[] encodePublicKey(RSAPublicKey key) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            /* encode the "ssh-rsa" string */
            byte[] sshrsa = new byte[] { 0, 0, 0, 7, 's', 's', 'h', '-', 'r', 's', 'a' };
            out.write(sshrsa);
            /* Encode the public exponent */
            BigInteger e = key.getPublicExponent();
            byte[] data = e.toByteArray();
            encodeUInt32(data.length, out);
            out.write(data);
            /* Encode the modulus */
            BigInteger m = key.getModulus();
            data = m.toByteArray();
            encodeUInt32(data.length, out);
            out.write(data);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void encodeUInt32(int value, OutputStream out) throws IOException {
        byte[] tmp = new byte[4];
        tmp[0] = (byte) ((value >>> 24) & 0xff);
        tmp[1] = (byte) ((value >>> 16) & 0xff);
        tmp[2] = (byte) ((value >>> 8) & 0xff);
        tmp[3] = (byte) (value & 0xff);
        out.write(tmp);
    }    
}
