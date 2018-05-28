package server;

import beans.MyPlayer;
import beans.MyTable;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyServer {
    private static final int BACKLOG = 1;

    /**
     * HTTP HEADERS USABLE DEFINES
     */
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private static final String HEADER_ALLOW = "Allow";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    private static final int STATUS_OK = 200;
    private static final int STATUS_CREATED = 201;
    private static final int STATUS_ACCEPTED = 202;
    private static final int STATUS_METHOD_NOT_ALLOWED = 405;
    private static final int STATUS_METHOD_FORBIDDEN = 403;
    private static final int STATUS_CONFLICT = 409;
    private static final int STATUS_INTERNAL_ERROR = 500;

    private static final int NO_RESPONSE_LENGTH = -1;

    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String METHOD_PUT = "PUT";
    private static final String METHOD_OPTIONS = "OPTIONS";
    private static final String ALLOWED_METHODS = METHOD_GET + "," + METHOD_POST + "," + METHOD_OPTIONS;

    private static final String APPLICATION_JSON = "application/json";
    private static final String TEXT_PLAIN = "text/plain";

    /**
     * CONNECTION VARIABLES
     */
    private static HttpsServer server = null;
    private static SSLContext sslContext = null;
    private static MySSLConnectionFactory sslConnection = null;
    private static MyUtilities myUtilities = null;
    private static final String HOSTNAME = "localhost";
    private static BasicAuthenticator guestAuthenticator = null;
    private static BasicAuthenticator userAuthenticator = null;

    /**
     * SERVER VARIABLES
     */
    private static MyDatabase DATABASE = null;
    private int tableCounter = 1;
    private static int port = 8080;
    protected static ArrayList<MyTable> tables = new ArrayList<>();

    public MyServer(int port) throws IOException {
        this.port = port;
        server = HttpsServer.create(new InetSocketAddress(HOSTNAME, port), BACKLOG);

        DATABASE = new MyDatabase();
        DATABASE.createConnection();

        URL whatismyip = new URL("http://checkip.amazonaws.com");
        BufferedReader in = new BufferedReader(new InputStreamReader(
                whatismyip.openStream()));

        String outIP = in.readLine(); //you get the IP as a String

        sslConnection = new MySSLConnectionFactory(HOSTNAME, outIP);

        myUtilities = new MyUtilities();

        createAuthenticators();
        createContexts();

        //generateTables();
    }

    public void generateTables() {
        tables.add(new MyTable(tableCounter, false));
        tableCounter++;
        tables.add(new MyTable(tableCounter, false));
        tableCounter++;
        tables.add(new MyTable(tableCounter, false));
        tableCounter++;
    }

    public void start() {
        server.setExecutor(null);
        server.start();
        System.out.println("Server running on port " + port);
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
        sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(keyManagerFactory.getKeyManagers(),trustManagerFactory.getTrustManagers(),null);

        //https://docs.oracle.com/javase/7/docs/jre/api/net/httpserver/spec/com/sun/net/httpserver/HttpsConfigurator.html
        server.setHttpsConfigurator (new HttpsConfigurator(sslContext) {
            public void configure (HttpsParameters params) {
                SSLContext c = sslContext;
                // get the default parameters
                SSLParameters sslparams = c.getDefaultSSLParameters();
                sslparams.setNeedClientAuth(true);
                params.setSSLParameters(sslparams);

            }
        });
    }

    public void createContexts() {
        HttpContext rootDynamic = server.createContext("/tables/", new RootHandler());
        rootDynamic.setAuthenticator(userAuthenticator);

        HttpContext hc1 = server.createContext("/users/login", new LoginHandler());
        hc1.setAuthenticator(guestAuthenticator);

        HttpContext hc2 = server.createContext("/users/register", new RegisterHandler());
        hc2.setAuthenticator(guestAuthenticator);

        HttpContext hc3 = server.createContext("/tables/public", new PublicTablesHandler());
        hc3.setAuthenticator(userAuthenticator);

        HttpContext hc4 = server.createContext("/tables/private", new PrivateTablesHandler());
        hc4.setAuthenticator(userAuthenticator);

        HttpContext hc5 = server.createContext("/users/credits", new CreditsCheckerHandler());
        hc5.setAuthenticator(userAuthenticator);
    }

    public static void createAuthenticators() {

        guestAuthenticator = new BasicAuthenticator("basic") {
            @Override
            public boolean checkCredentials(String username, String password) {
                return username.equals("guest") && password.equals("guest");
            }
        };

        userAuthenticator = new BasicAuthenticator("user") {
            @Override
            public boolean checkCredentials(String username, String password) {
                return DATABASE.loginUser(username, password) != -1;
            }
        };
    }

    public class RootHandler implements HttpHandler {

        @Override

        public void handle(HttpExchange httpExchange) throws IOException {

            Pattern pattern = Pattern.compile("/tables/(\\d+)");

            Matcher matcher = pattern.matcher(httpExchange.getRequestURI().getPath());

            if (matcher.matches()) {

                try {
                    int tableId = Integer.parseInt(matcher.group(1));

                    /* Handle connection */
                    HttpsExchange he = (HttpsExchange) httpExchange;
                    try {
                        final Headers headers = he.getResponseHeaders();
                        final String requestMethod = he.getRequestMethod().toUpperCase();
                        switch (requestMethod) {
                            /* UPDATE CLIENT STATE */
                            case METHOD_GET:
                                System.out.println("received tables/" + tableId + "/GET from " + he.getRemoteAddress().getHostName());
                                headers.set(HEADER_ALLOW, ALLOWED_METHODS);
                                headers.set(HEADER_CONTENT_TYPE, TEXT_PLAIN);
                                JsonObject request = MyUtilities.DecodeQuery(he.getRequestURI());

                                boolean validUser = false;

                                for (MyTable table:
                                     tables) {
                                    if (table.getId() == tableId) {
                                        for (MyPlayer player:
                                             table.getPlayers()) {
                                            // Check player in table and valid token
                                            if (player.getUsername().equals(request.getString("username")) &&
                                                    player.getToken().equals(request.getString("token"))) {
                                                validUser = true;

                                                // Start round if stopped table
                                                if (!table.hasStarted()) {
                                                    System.err.println("waiting: " + table.isWaiting());
                                                    System.err.println("finished: " + table.hasFinished());
                                                    System.err.println("ENTER?: " + (table.hasFinished() && !table.isWaiting()));

                                                    if (!table.isWaiting() && (table.hasFinished() || !table.hasStarted())) {
                                                        table.setDealerFinished(true);
                                                    }
                                                    table.startRound();
                                                }

                                                JsonObject tableInfo = table.getJSONInfo();
                                                MyUtilities.sendJSONExchange(he, tableInfo, STATUS_OK);
                                            }
                                        }
                                    }
                                }

                                if (!validUser) {
                                    JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
                                    JsonObject jsonObject = jsonObjectBuilder.add("reason", "not valid at this table").build();
                                    MyUtilities.sendJSONExchange(he, jsonObject, STATUS_ACCEPTED);
                                }

                                break;
                            /* ADD PLAYER */
                            case METHOD_POST:
                                System.out.println("received tables/" + tableId + "/POST from " + he.getRemoteAddress().getHostName());
                                headers.set(HEADER_ALLOW, ALLOWED_METHODS);
                                headers.set(HEADER_CONTENT_TYPE, APPLICATION_JSON);
                                //myUtilities.DecodeQuery(he.getRequestURI(), results);
                                JsonObject request2 = MyUtilities.readJSONExchange(he);

                                MyPlayer player = new MyPlayer();
                                player.setUsername(request2.getString("username"));
                                String token = MyUtilities.sha256(player.getUsername() + "sha2Tkn");
                                player.setToken(token);
                                player.setBet(0);

                                boolean success = false;
                                boolean exists = false;
                                String reason = "Internal Error";
                                for (MyTable table :
                                        tables) {
                                    if (table.getId() == tableId) {
                                        exists = true;
                                        if (!table.isFull()) {

                                            if (table.isPrivate()) {
                                                if (table.getPassword().equals(request2.getString("password"))) {
                                                    if (table.isInTable(player.getUsername()))
                                                        reason = "Already in Table";
                                                    else {
                                                        success = true;
                                                        System.out.println(player.getUsername() + " added to table #" + tableId);
                                                        table.addPlayer(player);
                                                    }
                                                } else {
                                                    reason = "Wrong Table Password";
                                                }
                                            } else {
                                                if (table.isInTable(player.getUsername())) {
                                                    reason = "Already in Table";
                                                } else {
                                                    success = true;
                                                    System.out.println(player.getUsername() + " added to table #" + tableId);
                                                    table.addPlayer(player);
                                                }
                                            }
                                        } else {
                                            reason = "Table is Full";
                                        }
                                    }
                                }

                                JsonObjectBuilder jsonObjectBuilder2 = Json.createObjectBuilder();

                                if (success) {
                                    jsonObjectBuilder2
                                            .add("token", token);
                                    JsonObject jsonObject2 = jsonObjectBuilder2.build();
                                    MyUtilities.sendJSONExchange(he, jsonObject2, STATUS_CREATED);
                                } else {
                                    if (!exists)
                                        reason = "That Table does not exist";
                                    jsonObjectBuilder2
                                            .add("reason", reason);
                                    MyUtilities.sendJSONExchange(he, jsonObjectBuilder2.build(), STATUS_ACCEPTED);
                                }
                                break;
                            /* MAKE A PLAY */
                            case METHOD_PUT:
                                System.out.println("received tables/" + tableId + "/PUT from " + he.getRemoteAddress().getHostName());
                                headers.set(HEADER_ALLOW, ALLOWED_METHODS);
                                headers.set(HEADER_CONTENT_TYPE, TEXT_PLAIN);

                                JsonObject requestPut = MyUtilities.readJSONExchange(he);

                                boolean validUserPut = false;
                                boolean tableExists = false;

                                for (MyTable table:
                                        tables) {
                                    if (table.getId() == tableId) {
                                        tableExists = true;
                                        for (MyPlayer playerPut:
                                                table.getPlayers()) {
                                            // Check player in table and valid token
                                            if (playerPut.getUsername().equals(requestPut.getString("username")) &&
                                                    playerPut.getToken().equals(requestPut.getString("token"))) {
                                                validUserPut = true;

                                                String command = requestPut.getJsonArray("command").get(0).toString();
                                                String commandValue = requestPut.getJsonArray("command").get(1).toString();

                                                if (command.equals("move")) {
                                                    if (commandValue.equals("hit")) {
                                                        table.playHit();
                                                    } else if (commandValue.equals("stand")) {
                                                        table.nextPlayer();
                                                    } else if (commandValue.equals("walk")) {
                                                        table.removePlayer(playerPut);
                                                    }
                                                } else if (command.equals("bet")) {
                                                    playerPut.setBet(Integer.valueOf(commandValue));
                                                }
                                                MyUtilities.sendJSONExchange(he, table.getJSONInfo(), STATUS_OK);
                                            }
                                        }
                                    }
                                }

                                if (!tableExists) {
                                    JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
                                    JsonObject jsonObject = jsonObjectBuilder.add("reason", "no such table").build();
                                    MyUtilities.sendJSONExchange(he, jsonObject, STATUS_ACCEPTED);
                                    return;
                                }

                                if (!validUserPut) {
                                    JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
                                    JsonObject jsonObject = jsonObjectBuilder.add("reason", "not valid at this table").build();
                                    MyUtilities.sendJSONExchange(he, jsonObject, STATUS_ACCEPTED);
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
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        he.close();
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            } else {

            }

        }
    }

    public class PublicTablesHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            HttpsExchange he = (HttpsExchange) httpExchange;
            try {
                final Headers headers = he.getResponseHeaders();
                final String requestMethod = he.getRequestMethod().toUpperCase();
                switch (requestMethod) {
                    case METHOD_GET:
                        System.out.println("received tables/public/GET from " +  he.getRemoteAddress().getHostName());
                        headers.set(HEADER_ALLOW, ALLOWED_METHODS);
                        headers.set(HEADER_CONTENT_TYPE, APPLICATION_JSON);
                        //myUtilities.DecodeQuery(he.getRequestURI(), results);
                        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
                        JsonArrayBuilder jsonArrayTable = Json.createArrayBuilder();
                        JsonArrayBuilder jsonArrayIds = Json.createArrayBuilder();
                        for (MyTable table:
                                tables) {
                            if (!table.isPrivate()) {
                                jsonArrayTable.add(Json.createObjectBuilder().add("id", table.getId()).add("players", table.getPlayers().size()).build());
                                jsonArrayIds.add(table.getId());
                            }
                        }
                        jsonObjectBuilder.add("public tables", jsonArrayTable.build());
                        jsonObjectBuilder.add("table ids", jsonArrayIds.build());
                        JsonObject jsonObject = jsonObjectBuilder.build();
                        MyUtilities.sendJSONExchange(he, jsonObject, STATUS_OK);
                        break;
                    case METHOD_POST:
                        System.out.println("received tables/public/POST from " +  he.getRemoteAddress().getHostName());
                        headers.set(HEADER_ALLOW, ALLOWED_METHODS);
                        headers.set(HEADER_CONTENT_TYPE, APPLICATION_JSON);
                        //myUtilities.DecodeQuery(he.getRequestURI(), results);

                        MyTable publicTable = new MyTable(tableCounter, false);
                        tableCounter++;
                        tables.add(publicTable);
                        //create response
                        JsonObjectBuilder jsonObjectBuilder2 = Json.createObjectBuilder()
                                .add("table id", publicTable.getId());
                        JsonObject jsonObject2 = jsonObjectBuilder2.build();
                        MyUtilities.sendJSONExchange(he, jsonObject2, STATUS_CREATED);
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
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                he.close();
            }
        }
    }

    public class PrivateTablesHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            HttpsExchange he = (HttpsExchange) httpExchange;
            try {
                final Headers headers = he.getResponseHeaders();
                final String requestMethod = he.getRequestMethod().toUpperCase();
                switch (requestMethod) {
                    case METHOD_GET:
                        System.out.println("received tables/private/GET from " +  he.getRemoteAddress().getHostName());
                        headers.set(HEADER_ALLOW, ALLOWED_METHODS);
                        headers.set(HEADER_CONTENT_TYPE, APPLICATION_JSON);
                        //myUtilities.DecodeQuery(he.getRequestURI(), results);
                        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
                        JsonArrayBuilder jsonArrayTable = Json.createArrayBuilder();
                        JsonArrayBuilder jsonArrayIds = Json.createArrayBuilder();
                        for (MyTable table:
                                tables) {
                            if (!table.isPrivate()) {
                                jsonArrayTable.add(Json.createObjectBuilder().add("id", table.getId()).add("players", table.getPlayers().size()).build());
                                jsonArrayIds.add(table.getId());
                            }
                        }
                        jsonObjectBuilder.add("public tables", jsonArrayTable.build());
                        jsonObjectBuilder.add("table ids", jsonArrayIds.build());
                        JsonObject jsonObject = jsonObjectBuilder.build();
                        MyUtilities.sendJSONExchange(he, jsonObject, STATUS_OK);
                        break;
                    case METHOD_POST:
                        System.out.println("received tables/private/POST from " +  he.getRemoteAddress().getHostName());
                        headers.set(HEADER_ALLOW, ALLOWED_METHODS);
                        headers.set(HEADER_CONTENT_TYPE, APPLICATION_JSON);
                        //myUtilities.DecodeQuery(he.getRequestURI(), results);
                        JsonObject request = MyUtilities.readJSONExchange(he);
                        MyTable publicTable = new MyTable(tableCounter, true);
                        tableCounter++;
                        publicTable.setPassword(request.getString("password"));
                        tables.add(publicTable);
                        //create response
                        JsonObjectBuilder jsonObjectBuilder2 = Json.createObjectBuilder()
                                .add("table id", publicTable.getId());
                        JsonObject jsonObject2 = jsonObjectBuilder2.build();
                        MyUtilities.sendJSONExchange(he, jsonObject2, STATUS_CREATED);
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
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                he.close();
            }
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
                    case METHOD_POST:
                        System.out.println("received users/register/POST from " +  he.getRemoteAddress().getHostName());
                        headers.set(HEADER_ALLOW, ALLOWED_METHODS);
                        headers.set(HEADER_CONTENT_TYPE, APPLICATION_JSON);

                        JsonObject jsonObject1 = MyUtilities.readJSONExchange(he);

                        byte[] salt = MyPasswords.getNextSalt();
                        byte[] password = MyPasswords.hash(jsonObject1.getString("password").toCharArray(), salt);

                        int result =  DATABASE.insertUser(jsonObject1.getString("username"), password, salt);

                        switch (result) {
                            case 0:
                                // registered successfully
                                he.sendResponseHeaders(STATUS_CREATED, NO_RESPONSE_LENGTH);
                                break;
                            case 1:
                                // username exists
                                he.sendResponseHeaders(STATUS_CONFLICT, NO_RESPONSE_LENGTH);
                                break;
                            default:
                                // database error
                                he.sendResponseHeaders(STATUS_INTERNAL_ERROR, NO_RESPONSE_LENGTH);
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

    public class CreditsCheckerHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            HttpsExchange he = (HttpsExchange) httpExchange;
            try {
                final Headers headers = he.getResponseHeaders();
                final String requestMethod = he.getRequestMethod().toUpperCase();
                switch (requestMethod) {
                    case METHOD_GET:
                        System.out.println("received users/credits/GET from " +  he.getRemoteAddress().getHostName());
                        headers.set(HEADER_ALLOW, ALLOWED_METHODS);
                        headers.set(HEADER_CONTENT_TYPE, TEXT_PLAIN);
                        System.out.println(he.getRequestURI());
                        JsonObject request = MyUtilities.DecodeQuery(he.getRequestURI());
                        System.out.println(request);

                        boolean valid = DATABASE.checkCredits(request.getString("username"), Integer.valueOf(request.getString("credits")));

                        if (valid) {
                            he.sendResponseHeaders(STATUS_OK, NO_RESPONSE_LENGTH);
                        } else {
                            he.sendResponseHeaders(STATUS_INTERNAL_ERROR, NO_RESPONSE_LENGTH);
                        }

                        break;
                    case METHOD_POST:
                        System.out.println("received users/credits/POST from " +  he.getRemoteAddress().getHostName());
                        headers.set(HEADER_ALLOW, ALLOWED_METHODS);
                        headers.set(HEADER_CONTENT_TYPE, APPLICATION_JSON);

                        JsonObject requestPost = MyUtilities.readJSONExchange(he);

                        //create response

                        if (DATABASE.addCredits(requestPost.getString("username"),
                                                    Integer.valueOf(requestPost.getString("credits")))) {
                            he.sendResponseHeaders(STATUS_CREATED, NO_RESPONSE_LENGTH);
                        } else {
                            he.sendResponseHeaders(STATUS_INTERNAL_ERROR, NO_RESPONSE_LENGTH);
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
            } catch (Exception e) {
                e.printStackTrace();
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
                final Headers headers = he.getResponseHeaders();
                final String requestMethod = he.getRequestMethod().toUpperCase();
                switch (requestMethod) {
                    case METHOD_POST:
                        System.out.println("received users/login/POST from " +  he.getRemoteAddress().getHostName());
                        headers.set(HEADER_ALLOW, ALLOWED_METHODS);
                        headers.set(HEADER_CONTENT_TYPE, APPLICATION_JSON);

                        JsonObject jsonObject = myUtilities.readJSONExchange(he);
                        System.out.println("JSON: " + jsonObject);

                        JsonObject response;

                        int credits = DATABASE.loginUser(jsonObject.getString("username"), jsonObject.getString("password")) ;
                        System.out.println("credits: "+ credits);
                        if (credits != -1) {
                            response = Json.createObjectBuilder()
                                    .add("credits", credits)
                                    .build();
                            myUtilities.sendJSONExchange(he, response, STATUS_OK);
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

        protected ArrayList<MyTable> getTables() {
            return tables;
        }
    }
}
