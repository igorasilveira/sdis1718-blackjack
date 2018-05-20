package server;

import com.MyUtilities;
import com.sun.net.httpserver.*;

import javax.net.ssl.*;
import javax.security.cert.X509Certificate;
import java.awt.*;
import java.io.*;
import java.net.Authenticator;
import java.net.InetSocketAddress;
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

            keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream("keys/server/keystoreServer"),"sdisServer".toCharArray());

            trustStore = KeyStore.getInstance("JKS");
            trustStore.load(new FileInputStream("keys/truststore"),"truststore".toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, "sdisServer".toCharArray());

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            trustManagerFactory.init(trustStore);

            //secure socket protocol implementation which acts as a factory for secure socket factories
            sslContext = SSLContext.getInstance("TLSv1");
            sslContext.init(keyManagerFactory.getKeyManagers(),trustManagerFactory.getTrustManagers(),null);

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
