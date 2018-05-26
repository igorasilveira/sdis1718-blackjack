package client;

import beans.MyPlayer;
import com.MySSLConnectionFactory;
import com.MyUtilities;
import sun.misc.BASE64Encoder;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.HashMap;

public class MyClient {

    private final String hostname = "localhost";
    private static String hostIP;
    private static int hostPort = 8080;

    private static SSLSocketFactory sslSocketFactory = null;

    private static MyMenus menus = null;

    private static MySSLConnectionFactory sslConnection = null;

    static MyPlayer user = new MyPlayer("guest", "guest");

    public MyClient(String hIP, int port) {
        hostIP = hIP;
        hostPort = port;

        sslConnection = new MySSLConnectionFactory(hostname);

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
        sslContext = SSLContext.getInstance("TLSv1");
        sslContext.init(keyManagerFactory.getKeyManagers(),trustManagerFactory.getTrustManagers(),null);

        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        return sslSocketFactory;//send over this
    }

//    public void run() throws UnsupportedEncodingException {
//        HashMap<String, String> parametersMap = new HashMap<>();
//        parametersMap.put("test", "name");
//        parametersMap.put("another", "pass asda asd");
//        System.out.println("Map size: " + parametersMap.size());
//        //String x = executeGet("http://localhost:8080/func1", parametersMap);
//        //System.out.println("GET SENT\nresponse: " + x);
//        //String x = executePost("http://localhost:8080/users", parametersMap);
//        //System.out.println("POST SENT");
//        //System.out.println("reponse: " +  x);
//
//    }

    public void run() {
       menus.mainMenu();
    }

    public static boolean loginUser(MyPlayer myUser) {
        HttpsURLConnection connection = null;

        try {
            //Create connection
            URL url = new URL(null, "http://" + hostIP + ":" + hostPort + "/users/login", new sun.net.www.protocol.https.Handler());
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    "application/json");

            HashMap<String, String> parameters = new HashMap<>();
            parameters.put("username", myUser.getUsername());
            parameters.put("password", myUser.getPassword());

            BASE64Encoder enc = new sun.misc.BASE64Encoder();
            String userpassword = user.getUsername() + ":" + user.getPassword();
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
            if (connection.getResponseCode() == 200) {
                return true;
            }
        } catch (Exception e) {
//            e.printStackTrace();
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return false;
    }

    public static int createUser(String username, String password) {

        HttpsURLConnection connection = null;

        try {
            //Create connection
            URL url = new URL(null, "http://" + hostIP + ":" + hostPort + "/users/register", new sun.net.www.protocol.https.Handler());
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setSSLSocketFactory(sslSocketFactory);
            connection.setRequestProperty("Content-Type",
                    "application/json");

            HashMap<String, String> parameters = new HashMap<>();
            parameters.put("username", username);
            parameters.put("password", password);

            BASE64Encoder enc = new sun.misc.BASE64Encoder();
            String userpassword = "guest" + ":" + "guest";
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
            if (connection.getResponseCode() == 201) {
                return 0;
            } else if (connection.getResponseCode() == 409) {
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
        return -1;
    }

    public String executeGet(String targetURL, HashMap parameters) {
        HttpURLConnection connection = null;

        try {
            //Create connection

            String urlParameters = MyUtilities.EncodeQuery(parameters);
            URL url = new URL(targetURL + "?" + urlParameters);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");

            BASE64Encoder enc = new sun.misc.BASE64Encoder();
            String userpassword = "guest" + ":" + "guest";
            String encodedAuthorization = enc.encode( userpassword.getBytes() );
            connection.setRequestProperty("Authorization", "Basic "+
                    encodedAuthorization);

            connection.setUseCaches(false);
            connection.setDoOutput(false);

            if (connection.getResponseCode() == 200) {

                /* Get Server response to string */
                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                StringBuffer response = new StringBuffer(); // or StringBuffer if Java version 5+
                String line;
                while ((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\r');
                }
                rd.close();

                /* Create JSON object from the created string */
                JsonReader jsonReader = Json.createReader(new StringReader(response.toString()));
                JsonObject jsonObject = jsonReader.readObject();
                return jsonObject.toString();
            }

            return "ERROR";
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
