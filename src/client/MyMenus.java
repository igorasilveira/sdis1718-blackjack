package client;

import beans.MyPlayer;
import com.MyUtilities;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.Scanner;

public class MyMenus {

    private static final int MAX_TRIES = 3;
    private static Scanner scanner = null;

    public MyMenus() {
        scanner = new Scanner(System.in);
    }

    public static void printState(JsonObject gameState) {

        System.out.format("\n\n%15s%3s%12s%3s%50s\n\n", "PLAYER", " | ", "CARD SCORE", " | ", "CARDS");
        System.out.println("-----------------------------------------------------------------------------------------------");
        System.out.format("%15s%3s%12s%3s%50s\n\n", "DEALER", " | ",
                gameState.getBoolean("running") ? " " : gameState.getJsonObject("dealer").getInt("total"), " | ",
                gameState.getJsonObject("dealer").getJsonArray("cards").toString());

        JsonArray players = gameState.getJsonArray("players");

        for (int i = 0; i < players.size(); i++) {
            JsonObject object = players.getJsonObject(i);
            System.out.format("%15s%3s%12s%3s%50s\n",
                    object.getString("username") == MyClient.user.getUsername() ? "THIS IS YOU" : object.getString("username"),
                    " | ", object.getInt("total"),
                    " | ", object.getJsonArray("cards"));
        }

        System.out.println("-----------------------------------------------------------------------------------------------\n\n");
    }

    public static int chooseMove() {
        final int moveSelection = MyUtilities.askUserForNumberInput(scanner,
                "> 1 - Hit me\n> 2 - Stand\n",
                2);

        return moveSelection;
    }

    public void mainMenu() throws InterruptedException {

        final int mainMenuSelection = MyUtilities.askUserForNumberInput(scanner,
                "> 1 - login\n> 2 - register\n> 3 - exit", 3);
        switch (mainMenuSelection) {
            case 1:
                login();
                break;
            case 2:
                register();
                break;
            case 3:
                System.exit(1);
        }
    }
    public void tableMenu() throws InterruptedException {

        System.out.println("*****************************");
        System.out.println("You have " + MyClient.user.getCredits() +  " credits!");
        System.out.println("*****************************");

        final int tableMenuSelection = MyUtilities.askUserForNumberInput(scanner,
                "> 1 - Join Public Table\n" +
                        "> 2 - Join Private Table\n" +
                        "> 3 - Create Public Table\n" +
                        "> 4 - Create Private Table\n" +
                        "> 5 - Exit Application", 5);
        switch (tableMenuSelection) {
            case 1:
                if (searchTables())
                    MyClient.play();
                break;
            case 2:
                if (joinPrivate())
                    MyClient.play();
                break;
            case 3:
                if (createPublic())
                    MyClient.play();
                break;
            case 4:
                if (createPrivate())
                    MyClient.play();
                break;
            case 5:
                System.err.println("See you next time.");
                System.exit(0);
                break;
        }
        tableMenu();
    }

    private boolean joinPrivate() {
        boolean entered = false;
        System.out.print("Enter table ID >");
        try {
            Integer tableID = scanner.nextInt();
            System.out.print("Enter Password >");
            String password = MyUtilities.sha256(scanner.next());


            System.out.println("Sending request");
            entered = MyClient.enterTable(tableID, password);

            if (entered) {
                System.out.println("\n---- Joined table #" + tableID + " ----\n");
            } else {
                System.err.println("[ERROR] Redirecting.");
                tableMenu();
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid input, try again.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return entered;
    }

    private boolean createPrivate() {
        int created = -1;
        try {
            boolean isValid = false;
            String password;
            String confirmPassword;


            while (!isValid) {
                System.out.print("Enter Password >");
                password = scanner.next();
                System.out.print("Confirm Password >");
                confirmPassword = scanner.next();

                if (password.equals(confirmPassword)) {

                    if (password.length() > 5) {
                        password = MyUtilities.sha256(password);

                        int tries = 0;
                        boolean success = false;

                        while (tries < MAX_TRIES && !success) {
                            System.out.println("Sending request");
                            try {
                                created = MyClient.createPrivate(password);
                                success = true;
                                isValid = true;
                            } catch (Exception e) {
                                System.out.println("An error occurred trying to connect.");
                                tries++;
                            }
                        }

                        if (tries == MAX_TRIES) {
                            System.out.println("[ERROR] Redirecting...");
                            mainMenu();
                        }

                    } else
                        System.err.println("The password should be at least 6 characters long");

                } else
                    System.err.println("Passwords do not match");

                if (created != -1) {
                    isValid = true;
                    System.out.println("Table with ID #" + created + " Successfully! Now entering...");
                    if (!MyClient.enterTable(created, password)) {
                        System.err.println("Some error occurred while entering");
                        return false;
                    }
                    else
                        return true;
                } else {
                    System.err.println("An error has occurred, please try again.");
                    createPrivate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean createPublic() {
        int tableId = MyClient.createPublicTable();

        if (tableId != -1) {
            System.out.println("Entering table #" + tableId + "...");
            return MyClient.enterTable(tableId, "");
        }
        else
            System.out.println("Error creating table");
        return false;
    }

    private boolean searchTables() {
        int result = MyClient.listPublicTables();

        if (result == -1) {
            System.err.println("\nThere are no public tables online...");
        } else {
            if (!MyClient.enterTable(result, "")) {
                System.err.println("An error occurred while entering");
                return false;
            } else {
                System.out.println("\n---- Joined table #" + result + " ----\n");
                return true;
            }
        }
        return false;
    }

    private void login() throws InterruptedException {
        boolean logged = false;

        System.out.print("Enter Username >");
        String username = scanner.next();
        System.out.print("Enter Password >");
        String password = MyUtilities.sha256(scanner.next());

        MyPlayer loginUser = new MyPlayer();
        loginUser.setUsername(username);
        loginUser.setPassword(password);

        int tries = 0;
        boolean success = false;

        while (tries < MAX_TRIES && !success) {
            System.out.println("Sending request");
            logged = MyClient.loginUser(loginUser);
            System.out.println("return " + logged);
            if (logged) {
                MyClient.user.setUsername(username);
                MyClient.user.setPassword(password);
                success = true;
            }
            tries++;
        }

        if (tries == MAX_TRIES) {
            System.out.println("[ERROR] Redirecting...");
            mainMenu();
        }

        if (logged) {
            System.out.println("\n---- Logged in Successfully ----\n");
            tableMenu();
        } else {
            System.out.println("not logged in");
            login();
        }
    }

    public void register() throws InterruptedException {
        int registered = -1;
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
                        try {
                            registered = MyClient.createUser(username, password);
                            success = true;
                            isValid = true;
                        } catch (Exception e) {
                            System.out.println("An error occurred trying to connect.");
                            tries++;
                        }
                    }

                    if (tries == MAX_TRIES) {
                        System.out.println("[ERROR] Redirecting...");
                        mainMenu();
                    }

                } else
                    System.out.println("The password should be at least 8 characters long");

            } else
                System.out.println("Passwords do not match");

            if (registered == 0) {
                MyClient.user.setUsername(username);
                MyClient.user.setPassword(password);
                System.out.println("Registered Successfully!");
                tableMenu();
            }
            else if (registered == 1){
                System.out.println("That username is taken.");
                register();
            } else {
                System.out.println("An error as occured");
                register();
            }
        }
    }

}
