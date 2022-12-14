/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jackrabbit.core.security.user;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to generate and compare password hashes.
 */
public class PasswordUtility {

    private static final Logger log = LoggerFactory.getLogger(PasswordUtility.class);

    private static final char DELIMITER = '-';
    private static final int NO_ITERATIONS = 1;    
    private static final Charset ENCODING = StandardCharsets.UTF_8;

    public static final String DEFAULT_ALGORITHM = "SHA-256";
    public static final int DEFAULT_SALT_SIZE = 8;
    public static final int DEFAULT_ITERATIONS = 1000;

    /**
     * Avoid instantiation
     */
    private PasswordUtility() {}
 
    /**
     * Generates a hash of the specified password with the default values
     * for algorithm, salt-size and number of iterations.
     *
     * @param password The password to be hashed.
     * @return The password hash.
     * @throws NoSuchAlgorithmException If {@link #DEFAULT_ALGORITHM} is not supported.
     */
    public static String buildPasswordHash(String password) throws NoSuchAlgorithmException {
        return buildPasswordHash(password, DEFAULT_ALGORITHM, DEFAULT_SALT_SIZE, DEFAULT_ITERATIONS);
    }

    /**
     * Generates a hash of the specified password using the specified algorithm,
     * salt size and number of iterations into account.
     *
     * @param password The password to be hashed.
     * @param algorithm The desired hash algorithm.
     * @param saltSize The desired salt size. If the specified integer is lower
     * that {@link #DEFAULT_SALT_SIZE} the default is used.
     * @param iterations The desired number of iterations. If the specified
     * integer is lower than 1 the {@link #DEFAULT_ITERATIONS default} value is used.
     * @return  The password hash.
     * @throws NoSuchAlgorithmException If the specified algorithm is not supported.
     */
    public static String buildPasswordHash(String password, String algorithm,
                                           int saltSize, int iterations) throws NoSuchAlgorithmException {
        if (password == null) {
            throw new IllegalArgumentException("Password may not be null.");
        }
        if (iterations < NO_ITERATIONS) {
            iterations = DEFAULT_ITERATIONS;
        }
        if (saltSize < DEFAULT_SALT_SIZE) {
            saltSize = DEFAULT_SALT_SIZE;
        }
        String salt = generateSalt(saltSize);
        String alg = (algorithm == null) ? DEFAULT_ALGORITHM : algorithm;
        return generateHash(password, alg, salt, iterations);
    }

    /**
     * Returns {@code true} if the specified string doesn't start with a
     * valid algorithm name in curly brackets.
     *
     * @param password The string to be tested.
     * @return {@code true} if the specified string doesn't start with a
     * valid algorithm name in curly brackets.
     */
    public static boolean isPlainTextPassword(String password) {
        return extractAlgorithm(password) == null;
    }

    /**
     * Returns {@code true} if hash of the specified {@code password} equals the
     * given hashed password.
     *
     * @param hashedPassword Password hash.
     * @param password The password to compare.
     * @return If the hash of the specified {@code password} equals the given
     * {@code hashedPassword} string.
     */
    public static boolean isSame(String hashedPassword, String password) {
        try {
            String algorithm = extractAlgorithm(hashedPassword);
            if (algorithm != null) {
                int startPos = algorithm.length()+2;
                String salt = extractSalt(hashedPassword, startPos);
                int iterations = NO_ITERATIONS;
                if (salt != null) {
                    startPos += salt.length()+1;
                    iterations = extractIterations(hashedPassword, startPos);
                }

                String hash = generateHash(password, algorithm, salt, iterations);
                return compareSecure(hashedPassword, hash);
            } // hashedPassword is plaintext -> return false
        } catch (NoSuchAlgorithmException e) {
            log.warn(e.getMessage());
        }
        return false;
    }

    /**
     * Extract the algorithm from the given crypted password string. Returns the
     * algorithm or {@code null} if the given string doesn't have a
     * leading {@code algorithm} such as created by {@code buildPasswordHash}
     * or if the extracted string doesn't represent an available algorithm.
     *
     * @param hashedPwd The password hash.
     * @return The algorithm or {@code null} if the given string doesn't have a
     * leading {@code algorithm} such as created by {@code buildPasswordHash}
     * or if the extracted string isn't a supported algorithm.
     */
    public static String extractAlgorithm(String hashedPwd) {
        if (hashedPwd != null && hashedPwd.length() > 0) {
            int end = hashedPwd.indexOf('}');
            if (hashedPwd.charAt(0) == '{' && end > 0 && end < hashedPwd.length()-1) {
                String algorithm = hashedPwd.substring(1, end);
                try {
                    MessageDigest.getInstance(algorithm);
                    return algorithm;
                } catch (NoSuchAlgorithmException e) {
                    log.debug("Invalid algorithm detected " + algorithm);
                }
            }
        }

        // not starting with {} or invalid algorithm
        return null;
    }

    //------------------------------------------------------------< private >---

    private static String generateHash(String pwd, String algorithm, String salt, int iterations) throws NoSuchAlgorithmException {
        StringBuilder passwordHash = new StringBuilder();
        passwordHash.append('{').append(algorithm).append('}');
        if (salt != null && salt.length() > 0) {
            StringBuilder data = new StringBuilder();
            data.append(salt).append(pwd);

            passwordHash.append(salt).append(DELIMITER);
            if (iterations > NO_ITERATIONS) {
                passwordHash.append(iterations).append(DELIMITER);
            }
            passwordHash.append(generateDigest(data.toString(), algorithm, iterations));
        } else {
            // backwards compatible to jr 2.0: no salt, no iterations
            passwordHash.append(Text.digest(algorithm, pwd.getBytes(ENCODING)));
        }
        return passwordHash.toString();
    }

    private static String generateSalt(int saltSize) {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[saltSize];
        random.nextBytes(salt);

        StringBuilder res = new StringBuilder(salt.length * 2);
        for (byte b : salt) {
            res.append(Text.hexTable[(b >> 4) & 15]);
            res.append(Text.hexTable[b & 15]);
        }
        return res.toString();
    }

    private static String generateDigest(String data, String algorithm, int iterations) throws NoSuchAlgorithmException {
        byte[] bytes = data.getBytes(ENCODING);
        MessageDigest md = MessageDigest.getInstance(algorithm);

        for (int i = 0; i < iterations; i++) {
            md.reset();
            bytes = md.digest(bytes);
        }

        StringBuilder res = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            res.append(Text.hexTable[(b >> 4) & 15]);
            res.append(Text.hexTable[b & 15]);
        }
        return res.toString();
    }
    
    private static String extractSalt(String hashedPwd, int start) {
        int end = hashedPwd.indexOf(DELIMITER, start);
        if (end > -1) {
            return hashedPwd.substring(start, end);
        }
        // no salt
        return null;
    }

    private static int extractIterations(String hashedPwd, int start) {
        int end = hashedPwd.indexOf(DELIMITER, start);
        if (end > -1) {
            String str = hashedPwd.substring(start, end);
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                log.debug("Expected number of iterations. Found: " + str);
            }
        }

        // no extra iterations
        return NO_ITERATIONS;
    }

    /**
     * Compare two strings. The comparison is constant time: it will always loop
     * over all characters and doesn't use conditional operations in the loop to
     * make sure an attacker can not use a timing attack.
     *
     * @param a
     * @param b
     * @return true if both parameters contain the same data.
     */
    private static boolean compareSecure(String a, String b) {
        if ((a == null) || (b == null)) {
            return (a == null) && (b == null);
        }
        int len = a.length();
        if (len != b.length()) {
            return false;
        }
        if (len == 0) {
            return true;
        }
        // don't use conditional operations inside the loop
        int bits = 0;
        for (int i = 0; i < len; i++) {
            // this will never reset any bits
            bits |= a.charAt(i) ^ b.charAt(i);
        }
        return bits == 0;
    }
}
