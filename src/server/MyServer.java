package server;

import com.MyUtilities;
import com.sun.net.httpserver.*;

import javax.net.ssl.*;
import javax.security.cert.X509Certificate;
import java.awt.*;
import java.io.*;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.*;
import java.util.List;

public class MyServer {
    private static final String HOSTNAME = "localhost";
    private int port = 8080;
    private static final int BACKLOG = 1;

    private static BasicAuthenticator basicAuthenticator;

    private static final String HEADER_ALLOW = "Allow";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private static final int STATUS_OK = 200;
    private static final int STATUS_METHOD_NOT_ALLOWED = 405;

    private static final int NO_RESPONSE_LENGTH = -1;

    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String METHOD_PUT = "PUT";
    private static final String METHOD_OPTIONS = "OPTIONS";
    private static final String ALLOWED_METHODS = METHOD_GET + "," + METHOD_POST + "," + METHOD_OPTIONS;

    private static HttpsServer server = null;
    private static SSLContext sslContext = null;

    public MyServer(int port) throws IOException {
        this.port = port;
        server = HttpsServer.create(new InetSocketAddress(HOSTNAME, this.port), BACKLOG);

        createContexts();
        basicAuthenticator = new BasicAuthenticator(METHOD_GET) {
            @Override
            public boolean checkCredentials(String s, String s1) {
                return false;
            }
        };

        try {
            KeyStore keyStore = null;
            KeyStore trustStore = null;
            boolean keyStoreCreated = false;
            boolean trustStoreCreated = false;
            String aliasKeystore = "server";
            String passwordKeystore = "sdisServer";
            String keyStorePath = "keys/server/keystoreServer";
            String keystoreFilename = "keystoreServer";

            String passwordTruststore = "truststore";
            String trustStorePath = "keys/truststore";

            File keystoreFile = new File(keyStorePath);
            File truststoreFile = new File(trustStorePath);

            if(!keystoreFile.exists()) {

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

            //https://docs.oracle.com/javase/7/docs/jre/api/net/httpserver/spec/com/sun/net/httpserver/HttpsConfigurator.html
            server.setHttpsConfigurator (new HttpsConfigurator(sslContext) {
                public void configure (HttpsParameters params) {
                    SSLContext c = sslContext;
                    // get the default parameters
                    SSLParameters sslparams = c.getDefaultSSLParameters();
                    params.setSSLParameters(sslparams);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void start() {

        server.setExecutor(null);
        server.start();
    }

    public void createContexts() {
        server.createContext("/", new RootHandler()).setAuthenticator(basicAuthenticator);
        server.createContext("/func1", hed -> {
            HttpsExchange he = (HttpsExchange)hed;
            try {
                final Headers headers = he.getResponseHeaders();
                final String requestMethod = he.getRequestMethod().toUpperCase();
                switch (requestMethod) {
                    case METHOD_PUT:
                        System.out.println("PUT");
                        Set<Map.Entry<String, List<String>>> entries = headers.entrySet();
                        String responseTest = "";
                        for (Map.Entry<String, List<String>> entry : entries)
                            responseTest += entry.toString() + "\n";
                        he.sendResponseHeaders(200, responseTest.length());
                        OutputStream os = he.getResponseBody();
                        os.write(responseTest.toString().getBytes());
                        os.close();
                        break;
                    case METHOD_GET:
                        System.out.println("received func1/GET from " +  he.getRemoteAddress().getHostName());
                        final HashMap<String, String> results = new HashMap<>();
                        headers.set(HEADER_ALLOW, ALLOWED_METHODS);
                        headers.set(HEADER_CONTENT_TYPE, "text/plain");
                        MyUtilities.DecodeQuery(he.getRequestURI(), results);
                        String finalResponse = "";
                        Iterator it = results.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry pair = (Map.Entry)it.next();
                            finalResponse += pair.getKey().toString() + "=" + pair.getValue().toString();
                            it.remove();
                        }
                        he.sendResponseHeaders(STATUS_OK, finalResponse.getBytes().length);
                        he.getResponseBody().write(finalResponse.getBytes());
                        break;
                    case METHOD_POST:
                        headers.set(HEADER_ALLOW, ALLOWED_METHODS);
                        headers.set(HEADER_CONTENT_TYPE, "application/json");
                        InputStream is = he.getRequestBody();
                        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                        StringBuffer response = new StringBuffer(); // or StringBuffer if Java version 5+
                        String line;
                        while ((line = rd.readLine()) != null) {
                            response.append(line);
                            response.append('\r');
                        }
                        rd.close();
                        he.sendResponseHeaders(STATUS_OK, response.toString().getBytes().length);
                        he.getResponseBody().write(response.toString().getBytes());
                        break;
                    case METHOD_OPTIONS:
                        headers.set(HEADER_ALLOW, ALLOWED_METHODS);
                        he.sendResponseHeaders(STATUS_OK, NO_RESPONSE_LENGTH);
                        break;
                    default:
                        headers.set(HEADER_ALLOW, ALLOWED_METHODS);
                        he.sendResponseHeaders(STATUS_METHOD_NOT_ALLOWED, NO_RESPONSE_LENGTH);
                        break;
                }
            } finally {
                he.close();
            }
        }).setAuthenticator(basicAuthenticator);
    }

    public class RootHandler implements HttpHandler {

        @Override

        public void handle(HttpExchange hed) throws IOException {
            HttpsExchange he = (HttpsExchange)hed;
            String response = "<h1>Server start success if you see this message</h1>" + "<h1>Port: " + port + "</h1>";
            he.sendResponseHeaders(200, response.length());
            OutputStream os = he.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

}
