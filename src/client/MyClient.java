package client;

import beans.MyPlayer;
import com.MySSLConnectionFactory;
import com.MyUtilities;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import sun.misc.BASE64Encoder;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class MyClient {

    private final String hostname = "localhost";
    private static String hostIP;
    private static int hostPort = 8080;

    private Map<String, Object> properties = new HashMap<>(1);
    private static JsonWriterFactory writerFactory = null;

    private static SSLSocketFactory sslSocketFactory = null;
    private static Scanner scanner = new Scanner(System.in);

    private static MyMenus menus = null;

    private static MySSLConnectionFactory sslConnection = null;

    static MyPlayer user = null;
    private static int currentTable = -1;
    private static String currentPlayer = "";
    private static int state = -1;

    public MyClient(String hIP, int port) {
        hostIP = hIP;
        hostPort = port;

        sslConnection = new MySSLConnectionFactory(hostname, "");

        try {
            sslSocketFactory = sslSocketFactory();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        menus = new MyMenus();
        user = new MyPlayer();
        user.setUsername("guest");
        user.setPassword("guest");

        /* Prep JSON pretty factory */
        properties.put(JsonGenerator.PRETTY_PRINTING, true);
        writerFactory = Json.createWriterFactory(properties);
    }

    private static SSLSocketFactory sslSocketFactory() throws IOException, InterruptedException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
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

        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        return sslSocketFactory;//send over this
    }

    public static boolean checkCredits(int amount) {
        HttpsURLConnection connection = null;

        try {
            //Create connection
            HashMap<String, String> parameters = new HashMap<>();
            parameters.put("username", user.getUsername());
            parameters.put("credits", String.valueOf(amount));

            URL url = new URL(null, "https://" + hostIP + ":" + hostPort + "/users/credits?" + MyUtilities.EncodeQuery(parameters), new sun.net.www.protocol.https.Handler());
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(5000);
            connection.setSSLSocketFactory(sslSocketFactory);
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");

            String userpassword = user.getUsername() + ":" + user.getPassword();
            String encodedAuthorization = Base64.getEncoder().encodeToString(userpassword.getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic "+
                    encodedAuthorization);

            connection.setUseCaches(false);
            connection.setDoOutput(false);

            connection.connect();
            //Get Response
            if (connection.getResponseCode() == 200) {
                user.setCredits(user.getCredits() - amount);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Some error occurred.");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        connection.disconnect();
        return false;
    }

    public static boolean addCredits(int amount) {
        HttpsURLConnection connection = null;

        try {
            //Create connection

            URL url = new URL(null, "https://" + hostIP + ":" + hostPort + "/users/credits", new sun.net.www.protocol.https.Handler());
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setReadTimeout(5000);
            connection.setSSLSocketFactory(sslSocketFactory);
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setRequestProperty("Content-Type",
                    "application/json");

            String userpassword = user.getUsername() + ":" + user.getPassword();
            String encodedAuthorization = Base64.getEncoder().encodeToString(userpassword.getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic "+
                    encodedAuthorization);

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            MyUtilities.sendJSONConnection(connection,
                    Json.createObjectBuilder()
                        .add("username", user.getUsername())
                        .add("credits", String.valueOf(amount))
                        .build());

            connection.connect();
            //Get Response
            if (connection.getResponseCode() == 201) {
                user.setCredits(user.getCredits() + amount);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Some error occurred.");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        connection.disconnect();
        return false;
    }

    public static void play() throws InterruptedException {

        boolean hasPlayed = false;
        boolean placedBet = false;
        boolean initializing = false;
        int bet = 0;
        try {

            boolean playing = true;

            JsonObject gameState = getGameState();
            while (playing) {

                if (gameState.getBoolean("running")) {
                    placedBet = false;

                    if (!gameState.getString("playing").equals(user.getUsername()))
                        System.err.println(gameState.getString("playing") + " is playing..");
                    // was there a player change?
                    if (!gameState.getString("playing").equals(currentPlayer) || (gameState.getInt("state") != state) || currentPlayer.equals(user.getUsername())) {
                        currentPlayer = gameState.getString("playing");
                        state = gameState.getInt("state");
                        MyMenus.printState(gameState);
                    }

                    // is it my turn?
                    if (currentPlayer.equals(user.getUsername())) {
                        initializing = false;
                        int move = MyMenus.chooseMove();

                        hasPlayed = true;
                        switch (move) {
                            // HIT
                            case 1:
                                gameState = sendCommand("move", "hit");
                                break;
                            // STAND
                            case 2:
                                gameState = sendCommand("move", "stand");
                                break;
                            // WALK AWAY
                            case 3:
                                gameState = sendCommand("move", "walk");
                                playing = false;
                                break;
                        }
                    }

                } else {

                    if (gameState.getBoolean("waiting") && placedBet) {
                        System.err.println("Waiting on other players at table");
                    } else {

                        if (hasPlayed) {

                            MyMenus.printState(gameState);

                            System.out.println("WANT TO PLAY AGAIN? (Y/N)");

                            String response = scanner.next();
                            while (!response.equalsIgnoreCase("y") && !response.equalsIgnoreCase("n")) {
                                System.out.println("\nInvalid response. Try again.");
                                response = scanner.next();
                            }
                            if (response.equalsIgnoreCase("n")) {
                                System.out.println("\nCome back next time.");
                                playing = false;
                                sendCommand("move", "walk");
                            } else {
                                System.out.println("\nGreat! Let's get started.");
                                initializing = true;
                                placedBet = false;
                            }
                        }

                        if (playing && !placedBet) {

                            boolean invalid = true;

                            while (invalid) {
                                try {
                                    System.out.print("Please enter your bet for this run (you have " + user.getCredits() + ")> ");
                                    bet = scanner.nextInt();
                                    if (checkCredits(bet)) {
                                        sendCommand("bet", String.valueOf(bet));
                                        invalid = false;
                                    } else {
                                        System.err.println("[ERROR] Amount not available, try again.");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            placedBet = true;
                        }
                    }
                }

                Thread.sleep(2000);


                gameState = getGameState();

                if (!initializing && hasPlayed && playing) {

                    boolean isWon = false;
                    boolean isBusted = false;
                    boolean isDraw = false;
                    JsonArray winners = gameState.getJsonArray("winners");
                    JsonArray busted = gameState.getJsonArray("busted");
                    JsonArray draws = gameState.getJsonArray("draw");

                    for (int i = 0; i < winners.size(); i++) {
                        for (JsonValue value:
                                winners) {
                            if (value.toString().equals(user.getUsername())) {
                                isWon = true;
                                continue;
                            }
                        }
                    }

                    if (!isWon) {
                        for (JsonValue value:
                                busted) {
                            if (value.toString().equals(user.getUsername())) {
                                isBusted = true;
                                continue;
                            }
                        }
                    }
                    else {
                        System.out.println("YOU WON " + bet * 2 + " CREDITS! Please wait for the round to finish.");
                        addCredits(bet * 2);
                        placedBet = false;
                    }

                    if (isBusted) {
                        System.out.println("YOU LOST " + bet + " CREDITS! Better luck next time.");
                        placedBet = false;
                    } else {
                        for (JsonValue value:
                                draws) {
                            if (value.toString().equals(user.getUsername())) {
                                isDraw = true;
                                continue;
                            }
                        }
                    }

                    if (isDraw) {
                        System.out.println("IT'S A DRAW! Not bad... You saved your " + bet + "credits");
                        addCredits(bet);
                        placedBet = false;
                    }
                }

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static JsonObject sendCommand(String command, String value) {
        HttpsURLConnection connection = null;

        try {
            //Create connection

            URL url = new URL(null, "https://" + hostIP + ":" + hostPort + "/tables/" + currentTable, new sun.net.www.protocol.https.Handler());
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setReadTimeout(5000);
            connection.setSSLSocketFactory(sslSocketFactory);
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setRequestProperty("Content-Type",
                    "application/json");

            JsonObject request = Json.createObjectBuilder()
                    .add("username", user.getUsername())
                    .add("token", user.getToken())
                    .add("command", Json.createArrayBuilder()
                    .add(command)
                    .add(value)).build();

            String userpassword = user.getUsername() + ":" + user.getPassword();
            String encodedAuthorization = Base64.getEncoder().encodeToString(userpassword.getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic "+
                    encodedAuthorization);

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            // send
            MyUtilities.sendJSONConnection(connection, request);

            connection.connect();
            //Get Response
            if (connection.getResponseCode() == 200) {
                JsonObject response = MyUtilities.readJSONConnection(connection);
                connection.disconnect();
                return response;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Some error occurred.");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        connection.disconnect();
        return null;
    }

    private static JsonObject getGameState() {

        HttpsURLConnection connection = null;

        try {
            //Create connection
            HashMap<String, String> parameters = new HashMap<>();
            parameters.put("username", user.getUsername());
            parameters.put("token", user.getToken());

            URL url = new URL(null, "https://" + hostIP + ":" + hostPort + "/tables/" + currentTable + "?" + MyUtilities.EncodeQuery(parameters), new sun.net.www.protocol.https.Handler());
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(5000);
            connection.setSSLSocketFactory(sslSocketFactory);
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");

            String userpassword = user.getUsername() + ":" + user.getPassword();
            String encodedAuthorization = Base64.getEncoder().encodeToString(userpassword.getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic "+
                    encodedAuthorization);

            connection.setUseCaches(false);
            connection.setDoOutput(false);

            connection.connect();
            //Get Response
            if (connection.getResponseCode() == 200) {
                JsonObject response = MyUtilities.readJSONConnection(connection);
                connection.disconnect();
                return response;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Some error occurred.");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        connection.disconnect();
        return null;
    }


    public void run() throws InterruptedException {
        menus.mainMenu();
    }

    public static int listPublicTables() {
        HttpsURLConnection connection = null;

        try {
            //Create connection
            URL url = new URL(null, "https://" + hostIP + ":" + hostPort + "/tables/public", new sun.net.www.protocol.https.Handler());
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(5000);
            connection.setSSLSocketFactory(sslSocketFactory);
            connection.setRequestProperty("Content-Type",
                    "application/json");

            String userpassword = user.getUsername() + ":" + user.getPassword();
            String encodedAuthorization = Base64.getEncoder().encodeToString(userpassword.getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic "+
            encodedAuthorization);

            connection.setUseCaches(false);
            connection.setDoOutput(false);

            connection.connect();
            //Get Response
            if (connection.getResponseCode() == 200) {
                JsonObject jsonObject = MyUtilities.readJSONConnection(connection);
                JsonArray availableIds = jsonObject.getJsonArray("table ids");
                boolean isValid = false;
                int tableID = -1;

                if (!availableIds.isEmpty()) {
                    JsonArray jsonValues = jsonObject.getJsonArray("public tables");

                    System.out.format("%10s%3s%10s\n", "Table ID", " | ", "#Players");

                    for (int i = 0 ; i < jsonValues.size(); i++) {
                        JsonObject table = jsonValues.getJsonObject(i);
                        System.out.format("%10d%3s%10d\n",
                        table.getInt("id"),
                        " | ",
                        table.getInt("players"));
                    }


                    while (!isValid) {
                        System.out.println("\nChoose a table by ID to join > ");

                        try {
                            tableID = scanner.nextInt();

                            boolean idExist = false;
                            for (int i = 0; i < availableIds.size(); i++) {
                                if (availableIds.getInt(i) == tableID) {
                                    idExist = true;
                                    isValid = true;
                                }
                            }

                            if (!idExist) {
                                System.err.println("\n[ERROR] invalid table ID entered\n");
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("\n[ERROR] invalid number detected\n");
                            scanner.next();
                        } catch (InputMismatchException e1) {
                            System.err.println("\n[ERROR] invalid number detected\n");
                            scanner.next();
                        }
                    }

                    connection.disconnect();
                    return tableID;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Some error occurred.");
            return -1;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        connection.disconnect();
        return -1;
    }

    public static int createPublicTable() {
        HttpsURLConnection connection = null;

        try {
            //Create connection
            URL url = new URL(null, "https://" + hostIP + ":" + hostPort + "/tables/public/", new sun.net.www.protocol.https.Handler());
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setReadTimeout(5000);
            connection.setSSLSocketFactory(sslSocketFactory);
            connection.setRequestProperty("Content-Type",
                    "application/json");


            String userpassword = user.getUsername() + ":" + user.getPassword();
            String encodedAuthorization = Base64.getEncoder().encodeToString(userpassword.getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic "+
                    encodedAuthorization);
            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            connection.connect();

            //Get Response
            if (connection.getResponseCode() == 201) {
                JsonObject jsonObject = MyUtilities.readJSONConnection(connection);
                connection.disconnect();
                return jsonObject.getInt("table id");
            } else {
                System.err.println("Some error occurred");
                connection.disconnect();
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Some error occurred.");
            return -1;
        }
        finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static int createPrivate(String password) {
        HttpsURLConnection connection = null;

        try {
            //Create connection
            URL url = new URL(null, "https://" + hostIP + ":" + hostPort + "/tables/private/", new sun.net.www.protocol.https.Handler());
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setReadTimeout(5000);
            connection.setSSLSocketFactory(sslSocketFactory);
            connection.setRequestProperty("Content-Type",
                    "application/json");

            JsonObject request = Json.createObjectBuilder()
                    .add("password", password)
                    .build();

            String userpassword = user.getUsername() + ":" + user.getPassword();
            String encodedAuthorization = Base64.getEncoder().encodeToString(userpassword.getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic "+
                    encodedAuthorization);
            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            //Sed request
            MyUtilities.sendJSONConnection(connection, request);

            connection.connect();

            //Get Response
            if (connection.getResponseCode() == 201) {
                JsonObject jsonObject = MyUtilities.readJSONConnection(connection);
                connection.disconnect();
                return jsonObject.getInt("table id");
            } else {
                System.err.println("Some error occurred");
                connection.disconnect();
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Some error occurred.");
            return -1;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static boolean enterTable(int tableId, String password) {
        HttpsURLConnection connection = null;

        try {
            //Create connection
            URL url = new URL(null, "https://" + hostIP + ":" + hostPort + "/tables/" + tableId, new sun.net.www.protocol.https.Handler());
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setReadTimeout(5000);
            connection.setSSLSocketFactory(sslSocketFactory);
            connection.setRequestProperty("Content-Type",
                    "application/json");

            JsonObject jsonObject = Json.createObjectBuilder()
                    .add("username", user.getUsername())
                    .add("password", password)
                    .build();

            String userpassword = user.getUsername() + ":" + user.getPassword();
            String encodedAuthorization = Base64.getEncoder().encodeToString(userpassword.getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic "+
                    encodedAuthorization);
            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            //Send
            MyUtilities.sendJSONConnection(connection, jsonObject);

            connection.connect();

            //Get Response
            if (connection.getResponseCode() == 201) {
                JsonObject response1 = MyUtilities.readJSONConnection(connection);
                user.setToken(response1.getString("token"));
                System.out.println("You are now in the table.");
                currentTable = tableId;
                currentPlayer = "";
                state = -1;
                connection.disconnect();
                return true;
            } else if (connection.getResponseCode() == 202) {
                JsonObject response2 = MyUtilities.readJSONConnection(connection);
                System.err.println("[ERROR] Responded with reason : " + response2.getString("reason"));
                connection.disconnect();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Some error occurred.");
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        connection.disconnect();
        return false;
    }

    public static boolean loginUser(MyPlayer myUser) {
        HttpsURLConnection connection = null;

        try {
            //Create connection
            URL url = new URL(null, "https://" + hostIP + ":" + hostPort + "/users/login", new sun.net.www.protocol.https.Handler());
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setReadTimeout(5000);
            connection.setSSLSocketFactory(sslSocketFactory);
            connection.setRequestProperty("Content-Type",
                    "application/json");

            JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
            jsonObjectBuilder.add("username", myUser.getUsername());
            jsonObjectBuilder.add("password", myUser.getPassword());

            JsonObject jsonObject = jsonObjectBuilder.build();

            String userpassword = user.getUsername() + ":" + user.getPassword();
            String encodedAuthorization = Base64.getEncoder().encodeToString(userpassword.getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic "+
                    encodedAuthorization);
            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            //Send request
            MyUtilities.sendJSONConnection(connection, jsonObject);

            connection.connect();

            //Get Response
            if (connection.getResponseCode() == 200) {
                jsonObject = MyUtilities.readJSONConnection(connection);
                user.setCredits(jsonObject.getInt("credits"));
                connection.disconnect();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Some error occurred.");
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        connection.disconnect();
        return false;
    }

    public static int createUser(String username, String password) {

        HttpsURLConnection connection = null;
        try {
            //Create connection
            URL url = new URL(null, "https://" + hostIP + ":" + hostPort + "/users/register", new sun.net.www.protocol.https.Handler());
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setReadTimeout(5000);
            connection.setSSLSocketFactory(sslSocketFactory);
            connection.setRequestProperty("Content-Type",
                    "application/json");

            JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
            jsonObjectBuilder.add("username", username);
            jsonObjectBuilder.add("password", password);

            JsonObject jsonObject = jsonObjectBuilder.build();

            String userpassword = "guest" + ":" + "guest";
            String encodedAuthorization = Base64.getEncoder().encodeToString(userpassword.getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic "+
                    encodedAuthorization);

            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            //Send request
            MyUtilities.sendJSONConnection(connection, jsonObject);

            connection.connect();

            //Get Response
            if (connection.getResponseCode() == 201) {
                connection.disconnect();
                return 0;
            } else if (connection.getResponseCode() == 409) {
                connection.disconnect();
                return 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        connection.disconnect();
        return -1;
    }
}
