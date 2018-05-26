package server;

import com.MySSLConnectionFactory;
import com.MyUtilities;
import com.sun.net.httpserver.*;

import javax.json.*;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;

public class MyServer {
    private static final String HOSTNAME = "localhost";
    private int port = 8080;
    private static final int BACKLOG = 1;

    private static MyDatabase DATABASE = null;

    private static final String HEADER_ALLOW = "Allow";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private static final BasicAuthenticator guestAuthenticator = new BasicAuthenticator("basic") {
        @Override
        public boolean checkCredentials(String username, String password) {
            return username.equals("guest") && password.equals("guest");
        }
    };

    private static final int STATUS_OK = 200;
    private static final int STATUS_CREATED = 201;
    private static final int STATUS_METHOD_NOT_ALLOWED = 405;
    private static final int STATUS_CONFLICT = 409;
    private static final int STATUS_INTERNAL_ERROR = 500;

    private static final int NO_RESPONSE_LENGTH = -1;

    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String METHOD_PUT = "PUT";
    private static final String METHOD_OPTIONS = "OPTIONS";
    private static final String ALLOWED_METHODS = METHOD_GET + "," + METHOD_POST + "," + METHOD_OPTIONS;

    private static HttpsServer server = null;
    private static SSLContext sslContext = null;
    private static MySSLConnectionFactory sslConnection = null;

    public MyServer(int port) throws IOException {
        this.port = port;
        server = HttpsServer.create(new InetSocketAddress(HOSTNAME, this.port), BACKLOG);

        DATABASE = new MyDatabase();
        DATABASE.createConnection();

        URL whatismyip = new URL("http://checkip.amazonaws.com");
        BufferedReader in = new BufferedReader(new InputStreamReader(
                whatismyip.openStream()));

        String outIP = in.readLine(); //you get the IP as a String

        sslConnection = new MySSLConnectionFactory(HOSTNAME, outIP);

        createContexts();
    }

    public void start() {
        server.setExecutor(null);
        server.start();
        System.out.println("Server running...");
    }

    public void startSSLContext() throws IOException, InterruptedException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
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
            keyStoreCreated = sslConnection.createKeyStore(aliasKeystore, passwordKeystore, keystoreFilename);
        }

        keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(keyStorePath), passwordKeystore.toCharArray());

        if(keyStoreCreated == true/*!truststoreFile.exists()*/) {
            trustStoreCreated = sslConnection.createTrustStore(truststoreFile, keystoreFilename, aliasKeystore, passwordTruststore);
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
    }

    public void createContexts() {
        server.createContext("/", new RootHandler());
        HttpContext hc1 = server.createContext("/users/login", new LoginHandler());
        hc1.setAuthenticator(guestAuthenticator);

        HttpContext hc2 = server.createContext("/users/register", new RegisterHandler());
        hc2.setAuthenticator(guestAuthenticator);
    }

    public class RootHandler implements HttpHandler {

        @Override

        public void handle(HttpExchange he) throws IOException {
            String response = "<h1>Server start success if you see this message</h1>" + "<h1>Port: " + port + "</h1>";
            he.sendResponseHeaders(200, response.length());
            OutputStream os = he.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    public class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HttpsExchange he = (HttpsExchange) exchange;
            try {
                JsonObject jsonObject = null;
                final Headers headers = he.getResponseHeaders();
                final String requestMethod = he.getRequestMethod().toUpperCase();
                switch (requestMethod) {
                    case METHOD_GET:
                        System.out.println("received users/GET from " +  he.getRemoteAddress().getHostName());
                        final HashMap<String, String> results = new HashMap<>();
                        headers.set(HEADER_ALLOW, ALLOWED_METHODS);
                        headers.set(HEADER_CONTENT_TYPE, "text/plain");
                        MyUtilities.DecodeQuery(he.getRequestURI(), results);
                        String finalResponse = "";
                        Iterator it = results.entrySet().iterator();
                        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
                        while (it.hasNext()) {
                            Map.Entry pair = (Map.Entry)it.next();
                            finalResponse += pair.getKey().toString() + "=" + pair.getValue().toString();
                            jsonObjectBuilder.add(pair.getKey().toString(), pair.getValue().toString());
                            it.remove();
                        }
                        jsonObjectBuilder.add("Success", JsonValue.TRUE);
                        JsonObject jsonObjectjsonObject = jsonObjectBuilder.build();
                        finalResponse = jsonObject.toString();
                        he.sendResponseHeaders(STATUS_OK, finalResponse.getBytes().length);
                        he.getResponseBody().write(finalResponse.getBytes());
                        break;
                    case METHOD_POST:
                        System.out.println("received users/register/POST from " +  he.getRemoteAddress().getHostName());
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
                        JsonReader jsonReader = Json.createReader(new StringReader(response.toString()));
                        jsonObject = jsonReader.readObject();

                        byte[] salt = MyPasswords.getNextSalt();
                        byte[] password = MyPasswords.hash(jsonObject.getString("password").toCharArray(), salt);

                        int result =  DATABASE.insertUser(jsonObject.getString("username"), password, salt);

                        switch (result) {
                            case 0:
                                // registered successfully
                                he.sendResponseHeaders(STATUS_CREATED, 0);
                                break;
                            case 1:
                                // username exists
                                he.sendResponseHeaders(STATUS_CONFLICT, 0);
                                break;
                            default:
                                // database error
                                he.sendResponseHeaders(STATUS_INTERNAL_ERROR, 0);
                        }
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
        }
    }

    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HttpsExchange he = (HttpsExchange) exchange;
            try {
                JsonObject jsonObject = null;
                final Headers headers = he.getResponseHeaders();
                final String requestMethod = he.getRequestMethod().toUpperCase();
                switch (requestMethod) {
                    case METHOD_POST:
                        System.out.println("received users/login/POST from " +  he.getRemoteAddress().getHostName());
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
                        JsonReader jsonReader = Json.createReader(new StringReader(response.toString()));
                        jsonObject = jsonReader.readObject();

                        if (DATABASE.loginUser(jsonObject.getString("username"), jsonObject.getString("password"))) {
                            he.sendResponseHeaders(STATUS_OK, 0);
                        } else {
                            he.sendResponseHeaders(304, -1);
                        }
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
        }
    }
}
