package client;

import beans.MyPlayer;
import com.MyUtilities;
import server.MyPasswords;
import sun.misc.BASE64Encoder;

import javax.json.*;
import javax.json.stream.JsonParser;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.Callable;

public class MyClient {
    private static final int MAX_TRIES = 3;
    private static String hostIP;
    private static int hostPort = 8080;

    public MyClient(String hIP, int port) {
        hostIP = hIP;
        hostPort = port;
    }

    public void run() throws UnsupportedEncodingException {
        HashMap<String, String> parametersMap = new HashMap<>();
        parametersMap.put("test", "name");
        parametersMap.put("another", "pass asda asd");
        System.out.println("Map size: " + parametersMap.size());
        //String x = executeGet("http://localhost:8080/func1", parametersMap);
        //System.out.println("GET SENT\nresponse: " + x);
        //String x = executePost("http://localhost:8080/users", parametersMap);
        //System.out.println("POST SENT");
        //System.out.println("reponse: " +  x);

        System.out.println("sending create user");
        System.out.println(createUser("username", "password"));
        System.out.println("sent");

    }

    /*public void run() throws UnsupportedEncodingException {
        mainMenu();
    }*/

    public void mainMenu() {

        try (Scanner scanner = new Scanner(System.in)) {
            final int mainMenuSelection = MyUtilities.askUserForNumberInput(scanner, "> 1 - login\n> 2 - register\n> 3 - exit", 3);
            switch (mainMenuSelection) {
                case 1:
                    login();
                    break;
                case 2:
                    register();
                    break;
                case 3:
                    return;
            }
        }
    }

    private void login() {
        boolean logged = false;
        try (Scanner scanner = new Scanner(System.in)) {
            String username = scanner.next();
            String password = MyUtilities.sha256(scanner.next());

            MyPlayer loginUser = new MyPlayer(username, password);

            int tries = 0;
            boolean success = false;

            while (tries < MAX_TRIES && !success) {
                System.out.println("Sending request");
                logged = loginUser(loginUser);
                success = true;
            }

            if (tries == MAX_TRIES) {
                mainMenu();
            }

        }

        if (logged) {
            System.out.println("logged in successfully");
        } else {
            System.out.println("not logged in");
            login();
        }
    }

    private boolean loginUser(MyPlayer myUser) {
        //TODO authenticate using general user info for access

        //TODO send request with myUser info for login attempt and override current on success

        return false;
    }

    public void register() {
        boolean registered = false;
        try (Scanner scanner = new Scanner(System.in)) {
            boolean isValid = false;
            String username;
            String password;
            String confirmPassword;


            while (!isValid) {
                System.out.print("Enter Username >");
                username = scanner.next();
                System.out.print("Enter Password >");
                password = scanner.next();
                System.out.print("Confirm Password >");
                confirmPassword = scanner.next();

                if (password.equals(confirmPassword)) {

                    if (password.length() > 7) {
                        password = MyUtilities.sha256(password);

                        int tries = 0;
                        boolean success = false;

                        while (tries < MAX_TRIES && !success) {
                            System.out.println("Sending request");
                            registered = createUser(username, password);
                            success = true;
                            isValid = true;
                        }

                        if (tries == MAX_TRIES) {
                            mainMenu();
                        }

                    } else
                        System.out.println("The password should be at least 8 characters long");

                } else
                    System.out.println("Passwords do not match");
            }

            if (registered)
                System.out.println("Registered Successfully!");
            else {
                System.out.println("An error occurred, try again!");
                register();
            }
        }
    }

    public boolean createUser(String username, String password) {

        HttpURLConnection connection = null;

        try {
            //Create connection
            URL url = new URL("http://" + hostIP + ":" + hostPort + "/users");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
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
                System.out.println("Received CREATED");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return false;
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

    public String executePost(String targetURL, HashMap parameters) {
        HttpURLConnection connection = null;

        try {
            //Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    "application/json");

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

    public String executePut(String targetURL, HashMap parameters) {
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
