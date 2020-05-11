/*
 * program  adapted from
 * acme4j - Java ACME client
 * Copyright (C) 2015 Richard "Shred" Körber
 *   http://acme4j.shredzone.org
 * Modifications
 * Copyright (C) 2020 Georg "geobe" Beier
 *   https://geobe.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package de.geobe.siteutil.acme

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.shredzone.acme4j.*
import org.shredzone.acme4j.challenge.Challenge
import org.shredzone.acme4j.challenge.Dns01Challenge
import org.shredzone.acme4j.exception.AcmeException
import org.shredzone.acme4j.util.CSRBuilder
import org.shredzone.acme4j.util.KeyPairUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.*
import java.security.KeyPair
import java.security.Security

/**
 * A simple acme client.
 * <p>
 * Pass the names of the domains as parameters.
 * <p>
 * All preset instance fields could be overwritten by Groovy's default initializing constructor
 */
class AcmeClient {
    // File name of the User Key Pair
    private File userKeyFile = new File("user.key")

    // File name of the Domain Key Pair
    private File domainKeyFile = new File("domain.key")

    // File name of the CSR
    private File domainCsrFile = new File("domain.csr")

    // File name of the signed certificate
    private File domainChainFile = new File("domain-chain.crt")

    // To create a session for Let's Encrypt.
    // Use "acme://letsencrypt.org" for production server
    private String acmeUri = 'acme://letsencrypt.org/staging'

    // RSA key size of generated key pairs
    private static final int KEY_SIZE = 2048

    private static final Logger LOG = LoggerFactory.getLogger(AcmeClient.class)

    private static class SessionData {
        KeyPair userKeyPair
        KeyPair domainKeyPair
        Session session
        Account acct
    }

    private static class ChallengeInfo {
        String domain
        String digest
        boolean stillValid
    }

    private SessionData sessionData = new SessionData()

    void prepare() {
        // Load the user key file. If there is no key file, create a new one.
        sessionData.userKeyPair = loadOrCreateKeyPair(userKeyFile)

        // Create a session for Let's Encrypt.
        // Use "acme://letsencrypt.org" for production server
        sessionData.session = new Session(acmeUri)

        // Load or create a key pair for the domains. This should not be the userKeyPair!
        sessionData.domainKeyPair = loadOrCreateKeyPair(domainKeyFile)
    }

    def requestChallenge(Collection<String> domains) throws IOException, AcmeException {
        // Get the Account.
        // If there is no account yet, create a new one.
        sessionData.acct = findOrRegisterAccount(sessionData.session, sessionData.userKeyPair)

        // Order the certificate
        Order order = sessionData.acct.newOrder().domains(domains).create()

        // Perform all required authorizations
        for (Authorization auth : order.getAuthorizations()) {
            authorize(auth)
        }


    }

    /**
     * Generates a certificate for the given domains. Also takes care for the registration
     * process.
     *
     * @param domains
     *            Domains to get a common certificate for
     */
    void fetchCertificate(Collection<String> domains) throws IOException, AcmeException {
        // Load the user key file. If there is no key file, create a new one.
        KeyPair userKeyPair = loadOrCreateKeyPair(userKeyFile)

        // Create a session for Let's Encrypt.
        // Use "acme://letsencrypt.org" for production server
        Session session = new Session(acmeUri)

        // Get the Account.
        // If there is no account yet, create a new one.
        Account acct = findOrRegisterAccount(session, userKeyPair)

        // Load or create a key pair for the domains. This should not be the userKeyPair!
        KeyPair domainKeyPair = loadOrCreateDomainKeyPair()

        // Order the certificate
        Order order = acct.newOrder().domains(domains).create()

        // Perform all required authorizations
        for (Authorization auth : order.getAuthorizations()) {
            authorize(auth)
        }

        // Generate a CSR for all of the domains, and sign it with the domain key pair.
        CSRBuilder csrb = new CSRBuilder()
        csrb.addDomains(domains)
        csrb.sign(domainKeyPair)

        // Write the CSR to a file, for later use.
        domainCsrFile.withWriter { out ->
//        try (Writer out = new FileWriter(DOMAIN_CSR_FILE)) {
            csrb.write(out)
        }

        // Order the certificate
        order.execute(csrb.getEncoded())

        // Wait for the order to complete
        try {
            int attempts = 10
            while (order.getStatus() != Status.VALID && attempts-- > 0) {
                // Did the order fail?
                if (order.getStatus() == Status.INVALID) {
                    throw new AcmeException("Order failed... Giving up.")
                }

                // Wait for a few seconds
                Thread.sleep(3000L)

                // Then update the status
                order.update()
            }
        } catch (InterruptedException ex) {
            LOG.error("interrupted", ex)
            Thread.currentThread().interrupt()
        }

        // Get the certificate
        Certificate certificate = order.getCertificate()

        LOG.info("Success! The certificate for domains {} has been generated!", domains)
        LOG.info("Certificate URL: {}", certificate.getLocation())

        // Write a combined file containing the certificate and chain.
        domainChainFile.withWriter { fw ->
//        try (FileWriter fw = new FileWriter(DOMAIN_CHAIN_FILE)) {
            certificate.writeCertificate(fw)
        }

        // That's all! Configure your web server to use the DOMAIN_KEY_FILE and
        // DOMAIN_CHAIN_FILE for the requested domans.
    }

    /**
     * Loads a user key pair from keyFile. If the file does not exist,
     * a new key pair is generated and saved.
     * <p>
     * Keep this key pair in a safe place! In a production environment, you will not be
     * able to access your account again if you should lose the key pair.
     * @param keyFile the file to read or create
     * @return a KeyPair.
     */
    private KeyPair loadOrCreateKeyPair(File keyFile) throws IOException {
        if (keyFile.exists()) {
            // If there is a key file, read it
            keyFile.withReader { fr ->
                return KeyPairUtils.readKeyPair(fr)
            }

        } else {
            // If there is none, create a new key pair and save it
            KeyPair keyPair = KeyPairUtils.createKeyPair(KEY_SIZE)
            keyFile.withWriter { fw ->
                KeyPairUtils.writeKeyPair(keyPair, fw)
            }
            return keyPair
        }
    }

    /**
     * Finds your {@link Account} at the ACME server. It will be found by your user's
     * public key. If your key is not known to the server yet, a new account will be
     * created.
     * <p>
     * This is a simple way of finding your {@link Account}. A better way is to get the
     * URL of your new account with {@link Account#getLocation()} and store it somewhere.
     * If you need to get access to your account later, reconnect to it via
     * {@link Session#login(URL, KeyPair)} by using the stored location.
     *
     * @param session
     * {@link Session} to bind with
     * @return {@link Login} that is connected to your account
     */
    private Account findOrRegisterAccount(Session session, KeyPair accountKey) throws AcmeException {
        // Ask the user to accept the TOS, if server provides us with a link.
        URI tos = session.getMetadata().getTermsOfService()
        if (tos != null) {
            LOG.info "see terms of service at $tos"
        }

        Account account = new AccountBuilder()
                .agreeToTermsOfService()
                .useKeyPair(accountKey)
                .create(session)
        LOG.info("Registered a new user, URL: ${account.getLocation()}")

        return account
    }

    /**
     * Authorize a domain. It will be associated with your account, so you will be able to
     * retrieve a signed certificate for the domain later.
     *
     * @param auth
     * {@link Authorization} to perform
     */
    private ChallengeInfo authorize(Authorization auth) throws AcmeException {
        LOG.info("Authorization for domain ${auth.identifier.domain}")
        ChallengeInfo info = new ChallengeInfo()
        info.domain = auth.identifier.domain
        // The authorization is already valid. No need to process a challenge.
        if (auth.getStatus() == Status.VALID) {
            info.stillValid = true
            return info
        }

        // Find the desired challenge and prepare it.
        Dns01Challenge challenge = auth.findChallenge(Dns01Challenge.TYPE)
        if (challenge == null) {
            throw new AcmeException("Found no ${Dns01Challenge.TYPE} challenge, don't know what to do...")
        }

        // If the challenge is already verified, there's no need to execute it again.
        if (challenge.getStatus() == Status.VALID) {
            info.stillValid = true
            return info
        }

        info.digest = challenge.digest
        info.stillValid = false
        return info
    }

    void triggerChallenge(Challenge challenge) {
        // Now trigger the challenge.
        challenge.trigger()

        // Poll for the challenge to complete.
        try {
            int attempts = 10
            while (challenge.getStatus() != Status.VALID && attempts-- > 0) {
                // Did the authorization fail?
                if (challenge.getStatus() == Status.INVALID) {
                    throw new AcmeException("Challenge failed... Giving up.")
                }

                // Wait for a few seconds
                Thread.sleep(3000L)

                // Then update the status
                challenge.update()
            }
        } catch (InterruptedException ex) {
            LOG.error("interrupted", ex)
            Thread.currentThread().interrupt()
        }

        // All reattempts are used up and there is still no valid authorization?
        if (challenge.getStatus() != Status.VALID) {
            throw new AcmeException("Failed to pass the challenge for domain "
                    + auth.getIdentifier().getDomain() + ", ... Giving up.")
        }

        LOG.info("Challenge has been completed. Remember to remove the validation resource.")
        completeChallenge("Challenge has been completed.\nYou can remove the resource again now.")
    }

    /**
     * Prepares a DNS challenge.
     * <p>
     * The verification of this challenge expects a TXT record with a certain content.
     * <p>
     * This example outputs instructions that need to be executed manually. In a
     * production environment, you would rather configure your DNS automatically.
     *
     * @param auth
     * {@link Authorization} to find the challenge in
     * @return {@link Challenge} to verify
     */
    Challenge dnsChallenge(Authorization auth) throws AcmeException {
        // Find a single dns-01 challenge
        Dns01Challenge challenge = auth.findChallenge(Dns01Challenge.TYPE)
        if (challenge == null) {
            throw new AcmeException("Found no " + Dns01Challenge.TYPE + " challenge, don't know what to do...")
        }

        // Output the challenge, wait for acknowledge...
        LOG.info("Please create a TXT record:")
        LOG.info("_acme-challenge.{}. IN TXT {}",
                auth.getIdentifier().getDomain(), challenge.getDigest())
        LOG.info("If you're ready, dismiss the dialog...")

        StringBuilder message = new StringBuilder()
        message.append("Please create a TXT record:\n\n")
        message.append("_acme-challenge.")
                .append(auth.getIdentifier().getDomain())
                .append(". IN TXT ")
                .append(challenge.getDigest())
        acceptChallenge(message.toString())

        return challenge
    }

    /**
     * Presents the instructions for preparing the challenge validation, and waits for
     * dismissal. If the user cancelled the dialog, an exception is thrown.
     *
     * @param message
     *            Instructions to be shown in the dialog
     */
    void acceptChallenge(String message) throws AcmeException {
        int option = JOptionPane.showConfirmDialog(null,
                message,
                "Prepare Challenge",
                JOptionPane.OK_CANCEL_OPTION)
        if (option == JOptionPane.CANCEL_OPTION) {
            throw new AcmeException("User cancelled the challenge")
        }
    }

    /**
     * Presents the instructions for removing the challenge validation, and waits for
     * dismissal.
     *
     * @param message
     *            Instructions to be shown in the dialog
     */
    void completeChallenge(String message) throws AcmeException {
        JOptionPane.showMessageDialog(null,
                message,
                "Complete Challenge",
                JOptionPane.INFORMATION_MESSAGE)
    }

    /**
     * Presents the user a link to the Terms of Service, and asks for confirmation. If the
     * user denies confirmation, an exception is thrown.
     *
     * @param agreement
     * {@link URI} of the Terms of Service
     */
    void acceptAgreement(URI agreement) throws AcmeException {
        int option = JOptionPane.showConfirmDialog(null,
                "Do you accept the Terms of Service?\n\n" + agreement,
                "Accept ToS",
                JOptionPane.YES_NO_OPTION)
        if (option == JOptionPane.NO_OPTION) {
            throw new AcmeException("User did not accept Terms of Service")
        }
    }

    /**
     * Invokes this example.
     *
     * @param args
     *            Domains to get a certificate for
     */
    static void main(String... args) {
        if (args.length == 0) {
            args = ['iot.geobe.de']
//            System.err.println("Usage: ClientTest <domain>...")
//            System.exit(1)
        }

        LOG.info("Starting up...")

        Security.addProvider(new BouncyCastleProvider())

        Collection<String> domains = Arrays.asList(args)

        try {
            AcmeClient ct = new AcmeClient()
            ct.fetchCertificate(domains)
        } catch (Exception ex) {
            LOG.error("Failed to get a certificate for domains " + domains, ex)
        }
    }

}
