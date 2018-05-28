package com;

import com.sun.net.httpserver.HttpsExchange;

import javax.json.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.*;

public class MyUtilities {

    public MyUtilities() {
    }

    /**
     * Decodes the query portion of the passed-in URI.
     *
     * @param encodedURI the URI containing the query to decode
     */
    public static JsonObject DecodeQuery(URI encodedURI) {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        Scanner scanner = new Scanner(encodedURI.getRawQuery());
        scanner.useDelimiter("&");
        try {
            while (scanner.hasNext()) {
                String param = scanner.next();
                String[] valuePair = param.split("=");
                String name, value;
                if (valuePair.length == 1) {
                    value = null;
                } else if (valuePair.length == 2) {
                    value = URLDecoder.decode(valuePair[1], "UTF-8");
                } else {
                    throw new IllegalArgumentException("query parameter invalid");
                }
                name = URLDecoder.decode(valuePair[0], "UTF-8");
                jsonObjectBuilder.add(name, value);
            }
        } catch (UnsupportedEncodingException e) {
            // This should never happen.

        }

        return jsonObjectBuilder.build();
    }

    public static String EncodeQuery(HashMap<String, String> toEncode) throws UnsupportedEncodingException {
        String result = "";
        Iterator it = toEncode.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            result += URLEncoder.encode(pair.getKey().toString(), "UTF-8").replace("+", "%20") + "="
                    + URLEncoder.encode(pair.getValue().toString(), "UTF-8").replace("+", "%20");
            if (it.hasNext())
                result += "&";
            it.remove();
        }
        return result;
    }

    public static void sendJSONConnection(HttpURLConnection connection, JsonObject jsonObject) throws IOException {
        connection.setRequestProperty("Content-Length",
                Integer.toString(jsonObject.toString().getBytes().length));
        JsonWriter jsonWriter = Json.createWriter(connection.getOutputStream());
        jsonWriter.writeObject(jsonObject);
        jsonWriter.close();
    }

    public static JsonObject readJSONConnection(HttpURLConnection connection) throws IOException {
        JsonReader jsonReader = Json.createReader(connection.getInputStream());
        JsonObject jsonObject = jsonReader.readObject();
        jsonReader.close();
        return jsonObject;
    }

    public static void sendJSONExchange(HttpsExchange he, JsonObject jsonObject, int status) throws IOException {
        he.sendResponseHeaders(status, jsonObject.toString().getBytes().length);
        he.getResponseBody().write(jsonObject.toString().getBytes(), 0, jsonObject.toString().length());
    }

    public static JsonObject readJSONExchange(HttpsExchange he) throws IOException {
        JsonReader jsonReader = Json.createReader(he.getRequestBody());
        JsonObject jsonObject = jsonReader.readObject();
        jsonReader.close();
        return jsonObject;
    }

    // int minValue may also be defined if necessary
    public static int askUserForNumberInput(Scanner scanner, String prompt, int maxValue) {
        System.out.println("-----------------------");
        System.out.println("Please choose an option\n");
        System.out.println(prompt);
        System.out.println("-----------------------");

        int value = -1;

        while (value < 1 || value > maxValue) {
            try {
                value = scanner.nextInt();
                if (value < 1 || value > maxValue) {
                    System.out.println("\n\n[ERROR] Invalid choice, please try again\n");
                    System.out.println("\n> ");
                    // java.util.InputMismatchException should also be caught
                    // to intercept non-numeric input
                    value = scanner.nextInt();
                }
                return value;
            } catch (InputMismatchException e) {
                System.err.println("Invalid input, try again");
                value = -1;
                scanner.next();
            }
        }
        return -1;
    }

    public static String sha256(String base) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }
}
