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

            keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream("keys/client/keystoreClient"),"sdisClient".toCharArray());

            trustStore = KeyStore.getInstance("JKS");
            trustStore.load(new FileInputStream("keys/truststore"),"truststore".toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, "sdisClient".toCharArray());

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
