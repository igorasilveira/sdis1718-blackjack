package client;

import com.MyUtilities;
import sun.misc.BASE64Encoder;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class MyClient {

    public static void main(String[] args) {
        HashMap<String, String> parametersMap = new HashMap<>();
        parametersMap.put("test", "name");
        parametersMap.put("another", "pass asda asd");
        System.out.println("Map size: " + parametersMap.size());
        //String x = executeGet("http://localhost:8080/func1", parametersMap);
        //System.out.println("GET SENT\nresponse: " + x);
        String x = executePut("http://localhost:8080/func1", parametersMap);
        //String x = executePost("http://localhost:8080/func1", parametersMap);
        //System.out.println("POST SENT");
        //System.out.println("reponse: " +  x);


    }

    public static String executeGet(String targetURL, HashMap parameters) {
        HttpURLConnection connection = null;

        try {
            //Create connection

            String urlParameters = MyUtilities.EncodeQuery(parameters);
            URL url = new URL(targetURL + "?" + urlParameters);
            connection = (HttpURLConnection) url.openConnection();
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

    public static String executePost(String targetURL, HashMap parameters) {
        HttpURLConnection connection = null;

        try {
            //Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
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

    public static String executePut(String targetURL, HashMap parameters) {
        HttpURLConnection connection = null;

        try {
            //Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
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
