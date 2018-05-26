package com;

import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class MySSLConnectionFactory {

    private String HOSTNAME = "";

    public MySSLConnectionFactory(String hostname) {
        HOSTNAME = hostname;
    }

    public boolean createKeyStore(String aliasKeystore, String passwordKeystore, String keystoreFilename) throws IOException, InterruptedException {
        System.out.println("Keystore missing. Creating...");

        //Create a new keystore and self-signed certificate with corresponding public and private keys
        String[] commandKeystore = {"keytool", "-genkeypair", "-alias", aliasKeystore , "-keyalg", "RSA", "-validity", "7", "-keystore", keystoreFilename};

        ProcessBuilder pb = new ProcessBuilder(commandKeystore);

        File serverDirectory = new File("keys/"+ aliasKeystore);

        if (!serverDirectory.exists())
            serverDirectory.mkdir();

        pb.directory(serverDirectory);

        //log from https://docs.oracle.com/javase/7/docs/api/java/lang/ProcessBuilder.html
        File log = new File("keys/log");
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));

        Process p = pb.start();
        OutputStreamWriter out = new OutputStreamWriter (p.getOutputStream());
        out.write(passwordKeystore + '\n');    /*Enter keystore password: */
        out.write(passwordKeystore + '\n');    /*Re-enter new password:*/
        out.write(HOSTNAME + '\n');    /*What is your first and last name?*/
        out.write("sdis1718-blackjack" + '\n');        /*What is the name of your organizational unit?*/
        out.write("FEUP" + '\n');      /*What is the name of your organization?*/
        out.write("Porto" + '\n');     /*What is the name of your City or Locality?*/
        out.write("Porto" + '\n');     /*What is the name of your State or Province?*/
        out.write("PT" + '\n');        /*What is the two-letter country code for this unit?*/
        out.write("yes" + '\n');       /*Data confirmation*/
        out.write('\n');                /*(RETURN if same as keystore password):*/
        out.flush();
        out.close();
        p.waitFor();
        System.out.println("HOSTNAME: -" + HOSTNAME + "-");
        System.out.println("Keystore created.");

        System.out.println("Exporting certificate...");
        //Export the self-signed certificate.
        String[] commandCer = {"keytool", "-export", "-alias", aliasKeystore, "-keystore", keystoreFilename , "-rfc", "-file", keystoreFilename + ".cer" };
        pb = new ProcessBuilder(commandCer);
        pb.directory(serverDirectory);
        p = pb.start();
        out = new OutputStreamWriter (p.getOutputStream());
        out.write(passwordKeystore + '\n');/*Enter keystore password:*/
        out.write('\n');
        out.flush();
        out.close();
        p.waitFor();
        System.out.println("Certificate exported.");

        return true;
    }

    public boolean createTrustStore(File truststoreFile, String keystoreFilename, String aliasKeystore, String passwordTruststore) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, InterruptedException {

        KeyStore trustStore = null;
        boolean trustStoreCreated = false;
        if (!truststoreFile.exists()) {
            trustStoreCreated = true;
        } else {
            trustStore = KeyStore.getInstance("JKS");
            trustStore.load(new FileInputStream("keys/truststore"),"truststore".toCharArray());
            //Not working currently: keytool error: java.lang.Exception: Certificate not imported, alias <keystoreServercert> already exists even after delete
            if (trustStore.containsAlias(keystoreFilename+"cert")) {
                trustStore.deleteEntry(keystoreFilename + "cert");
                System.out.println("Certificate with same alias deleted.");
            }

        }

        System.out.println("Adding certicate to truststore...");

        String truststoreFilename = "truststore";

        //Create a new keystore and self-signed certificate with corresponding public and private keys
        String[] commandTruststore = {"keytool", "-import", "-alias", keystoreFilename+"cert" , "-file", aliasKeystore + "/" + keystoreFilename + ".cer", "-keystore", truststoreFilename};

        ProcessBuilder pb = new ProcessBuilder(commandTruststore);

        File serverDirectory = new File("keys");

        pb.directory(serverDirectory);

        //log from https://docs.oracle.com/javase/7/docs/api/java/lang/ProcessBuilder.html
        File log = new File("keys/log");
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));

        Process p = pb.start();
        OutputStreamWriter out = new OutputStreamWriter (p.getOutputStream());
        out.write(passwordTruststore + '\n');    /*Enter keystore password: */
        if (trustStoreCreated) {
            out.write(passwordTruststore + '\n');    /*Re-enter new password:*/
            System.out.println("Trustore missing so it was created.");
        }
        out.write("yes" + '\n');       /*Trust this certificate*/
        out.write('\n');                /*(RETURN if same as keystore password):*/
        out.flush();
        out.close();
        p.waitFor();
        System.out.println("Certificate added to truststore.");

        return true;
    }
}
