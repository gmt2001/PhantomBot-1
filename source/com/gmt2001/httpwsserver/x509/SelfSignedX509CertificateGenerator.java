/*
 * Copyright (C) 2016-2023 phantombot.github.io/PhantomBot
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gmt2001.httpwsserver.x509;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Generates a SelfSigned X.509 Certificate
 *
 * @author gmt2001
 */
public final class SelfSignedX509CertificateGenerator {
    private static final Provider PROVIDER = new BouncyCastleProvider();
    /**
     * Recommended private key size
     */
    public static final int RECOMMENDED_KEY_SIZE = 2048;
    /**
     * Recommended signature algorithm
     */
    public static final String RECOMMENDED_SIG_ALGO = "SHA512withRSA";
    /**
     * Recommended certificate validity time, in days
     */
    public static final int RECOMMENDED_VALIDITY_DAYS = 60;
    /**
     * Recommended certificate renewal interval, in days
     */
    public static final int RECOMMENDED_RENEWAL_DAYS = 45;

    private SelfSignedX509CertificateGenerator() {
    }

    /**
     * Create a self-signed X.509 Certificate
     *
     * @param dn the X.509 Distinguished Name, eg "CN=Test, L=London, C=GB"
     * @param pair the KeyPair
     * @param days how many days from now the Certificate is valid for
     * @param algorithm the signing algorithm, eg "SHA256withRSA"
     * @return the certificate
     * @throws OperatorCreationException if the BC provider is unable to create a certificate signer
     * @throws SignatureException if there is a signature verification error
     * @throws NoSuchProviderException if the BC provider can not be found
     * @throws NoSuchAlgorithmException if the signature algorithm is not supported
     * @throws CertificateException if a certificate is unable to be made or an encoding error is encountered
     * @throws InvalidKeyException if the keypair does not match
     */
    public static X509Certificate generateCertificate(String dn, KeyPair pair, int days, String algorithm)
        throws OperatorCreationException, InvalidKeyException, CertificateException, NoSuchAlgorithmException,
        NoSuchProviderException, SignatureException {
        X500Name name = new X500Name(dn);

        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name,
            new BigInteger(64, new SecureRandom()), Date.from(Instant.now()),
            Date.from(Instant.now().plus(days, ChronoUnit.DAYS)), name,
            pair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder(algorithm).build(pair.getPrivate());

        X509CertificateHolder holder = builder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter().setProvider(PROVIDER).getCertificate(holder);
        cert.verify(pair.getPublic());
        return cert;
    }

    /**
     * Generate a DN string with just the CN
     *
     * @param commonName the Common Name
     * @return the formatted string
     */
    public static String generateDistinguishedName(String commonName) {
        return "CN=" + commonName.replace('=', '-').replace(',', '-');
    }

    /**
     * Generate a Key Pair
     *
     * @param keySize the key size, in bits
     * @return the generated key pair
     * @throws NoSuchAlgorithmException if no provider can be found which supports the requested algorithm
     */
    public static KeyPair generateKeyPair(int keySize) throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", PROVIDER);
        keyPairGenerator.initialize(keySize);
        return keyPairGenerator.generateKeyPair();
    }
}
