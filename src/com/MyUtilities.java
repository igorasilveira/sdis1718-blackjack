package com;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

public class MyUtilities {
    /**
     * Decodes the query portion of the passed-in URI.
     *
     * @param encodedURI the URI containing the query to decode
     * @param results a map containing all query parameters. Query parameters that do not have a
     *            value will map to a null string
     */
    static public void DecodeQuery(URI encodedURI, Map<String, String> results) {
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
                results.put(name, value);
            }
        } catch (UnsupportedEncodingException e) {
            // This should never happen.

        }
    }

    static public String EncodeQuery(HashMap<String, String> toEncode) throws UnsupportedEncodingException {
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

    static public String EncodeJSON(HashMap<String, String> toEncode) throws UnsupportedEncodingException {
        String result = "{";
        Iterator it = toEncode.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            result += "\"" + pair.getKey().toString() + "\" :"
                    + "\"" + pair.getValue().toString() + "\"";
            if (it.hasNext())
                result += ",";
            it.remove();
        }
        return result + "}";
    }

    // int minValue may also be defined if necessary
    public static int askUserForNumberInput(Scanner scanner, String prompt, int maxValue) {
        System.out.println("Please choose an option\n");
        System.out.println(prompt);
        System.out.println("-----------------------");
        int value = scanner.nextInt();
        while (value < 1 || value > maxValue) {
            System.out.println("\n\n[ERROR] Invalid choice, please try again\n");
            System.out.println("\n> ");
            // java.util.InputMismatchException should also be caught
            // to intercept non-numeric input
            value = scanner.nextInt();
        }
        return value;
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
