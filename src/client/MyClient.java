package client;

import com.MyUtilities;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import sun.misc.BASE64Encoder;

import javax.json.*;
import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.util.HashMap;

public class MyClient {

    private static final String HOSTNAME = "localhost";

    public static void main(String[] args) {
        HashMap<String, String> parametersMap = new HashMap<>();
        parametersMap.put("test", "name");
        parametersMap.put("another", "pass asda asd");
        System.out.println("Map size: " + parametersMap.size());
        SSLSocketFactory sslSocketFactory = makeSSLSocketFactory();
        String x = executeGet("http://localhost:8080/func1", parametersMap,sslSocketFactory);
        System.out.println("GET SENT\nresponse: " + x);
//        String x = executePut("http://localhost:8080/func1", parametersMap, sslSocketFactory);
//        String x = executePost("http://localhost:8080/func1", parametersMap, sslSocketFactory);
//        System.out.println("POST SENT");
//        System.out.println("reponse: " +  x);


    }

    private static SSLSocketFactory makeSSLSocketFactory(){

        try {
            SSLContext sslContext;

            KeyStore keyStore = null;
            KeyStore trustStore = null;
            boolean keyStoreCreated = false;
            boolean trustStoreCreated = false;
            String aliasKeystore = "client";
            String passwordKeystore = "sdisClient";
            String keyStorePath = "keys/client/keystoreClient";
            String keystoreFilename = "keystoreClient";

            String passwordTruststore = "truststore";
            String trustStorePath = "keys/truststore";

            File keystoreFile = new File(keyStorePath);
            File truststoreFile = new File(trustStorePath);

            if(!keystoreFile.exists()) {

                System.out.println("Keystore missing. Creating...");

                //Create a new keystore and self-signed certificate with corresponding public and private keys
                String[] commandKeystore = {"keytool", "-genkeypair", "-alias", aliasKeystore , "-keyalg", "RSA", "-validity", "7", "-keystore", keystoreFilename};

                ProcessBuilder pb = new ProcessBuilder(commandKeystore);

                File serverDirectory = new File("keys/" + aliasKeystore);

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

                keyStoreCreated = true;
            }

            keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream(keyStorePath), passwordKeystore.toCharArray());

            if(keyStoreCreated == true/*!truststoreFile.exists()*/) {

                if (!truststoreFile.exists()) {
                    trustStoreCreated = true;
                }else{
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

                trustStoreCreated = true;
            }

            trustStore = KeyStore.getInstance("JKS");
            trustStore.load(new FileInputStream("keys/truststore"),"truststore".toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, passwordKeystore.toCharArray());

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            trustManagerFactory.init(trustStore);

            //secure socket protocol implementation which acts as a factory for secure socket factories
            sslContext = SSLContext.getInstance("TLSv1");
            sslContext.init(keyManagerFactory.getKeyManagers(),trustManagerFactory.getTrustManagers(),null);

            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            return sslSocketFactory;//send over this


        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String executeGet(String targetURL, HashMap parameters, SSLSocketFactory sslSocketFactory) {
        HttpsURLConnection connection = null;
        //Caused by: java.security.cert.CertificateException: No name matching localhost found
//        HttpsURLConnection.setDefaultHostnameVerifier ((hostname, session) -> true);//Needed case server certifacte CN != hostname, in this case localhost
        try {

            //Create connection
            //workaround for sun.net.www.protocol.http.HttpURLConnection cannot be cast to javax.net.ssl.HttpsURLConnection
            String urlParameters = MyUtilities.EncodeQuery(parameters);
            URL url = new URL(null, targetURL + "?" + urlParameters, new sun.net.www.protocol.https.Handler());
//            URL url = new URL(targetURL);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setSSLSocketFactory(sslSocketFactory);
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");

            connection.setUseCaches(false);
            connection.setDoOutput(false);

            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuffer response = new StringBuffer(); // or StringBuffer if Java version 5+
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }


    }

    public static String executePost(String targetURL, HashMap parameters, SSLSocketFactory sslSocketFactory) {
        HttpsURLConnection connection = null;
        //Caused by: java.security.cert.CertificateException: No name matching localhost found
//        HttpsURLConnection.setDefaultHostnameVerifier ((hostname, session) -> true);//Needed case server certifacte CN != hostname, in this case localhost
        try {

            //Create connection
            //workaround for sun.net.www.protocol.http.HttpURLConnection cannot be cast to javax.net.ssl.HttpsURLConnection
            URL url = new URL(null, targetURL, new sun.net.www.protocol.https.Handler());
//            URL url = new URL(targetURL);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setSSLSocketFactory(sslSocketFactory);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    "application/json");

            BASE64Encoder enc = new sun.misc.BASE64Encoder();
            String userpassword = "username" + ":" + "password";
            String encodedAuthorization = enc.encode( userpassword.getBytes() );
            connection.setRequestProperty("Authorization", "Basic "+
                    encodedAuthorization);

            String jsonParameters = MyUtilities.EncodeJSON(parameters);

            connection.setRequestProperty("Content-Length",
                    Integer.toString(jsonParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream (
                    connection.getOutputStream());
            wr.writeBytes(jsonParameters);
            wr.close();

            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuffer response = new StringBuffer(); // or StringBuffer if Java version 5+
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static String executePut(String targetURL, HashMap parameters, SSLSocketFactory sslSocketFactory) {
        HttpsURLConnection connection = null;
        //Caused by: java.security.cert.CertificateException: No name matching localhost found
//        HttpsURLConnection.setDefaultHostnameVerifier ((hostname, session) -> true);//Needed case server certifacte CN != hostname, in this case localhost
        try {

            //Create connection
            //workaround for sun.net.www.protocol.http.HttpURLConnection cannot be cast to javax.net.ssl.HttpsURLConnection
            URL url = new URL(null, targetURL, new sun.net.www.protocol.https.Handler());
//            URL url = new URL(targetURL);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setSSLSocketFactory(sslSocketFactory);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type",
                    "application/json");

            BASE64Encoder enc = new sun.misc.BASE64Encoder();
            String userpassword = "username" + ":" + "password";
            String encodedAuthorization = enc.encode( userpassword.getBytes() );
            connection.setRequestProperty("Authorization", "Basic "+
                    encodedAuthorization);

            String jsonParameters = MyUtilities.EncodeJSON(parameters);

            connection.setRequestProperty("Content-Length",
                    Integer.toString(jsonParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream (
                    connection.getOutputStream());
            wr.writeBytes(jsonParameters);
            wr.close();

            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuffer response = new StringBuffer(); // or StringBuffer if Java version 5+
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
