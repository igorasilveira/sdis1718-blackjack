package client;

import beans.MyPlayer;
import com.MyUtilities;

import java.util.Scanner;

public class MyMenus {

    private static final int MAX_TRIES = 3;
    private static Scanner scanner;

    public MyMenus() {
        scanner = new Scanner(System.in);
    }

    public void mainMenu() {

        final int mainMenuSelection = MyUtilities.askUserForNumberInput(scanner, "> 1 - login\n> 2 - register\n> 3 - exit", 3);
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
    public void tableMenu() {

        final int tableMenuSelection = MyUtilities.askUserForNumberInput(scanner, "> 1 - Join Public Tables\n" +
                "> 2 - Join Private Table\n> 3 - Create Public Table\n" +
                "> 4 - Create Private Table", 3);
        switch (tableMenuSelection) {
            case 1:
                searchTables();
                break;
            case 2:
                joinPrivate();
                break;
            case 3:
                createPublic();
                break;
            case 4:
                createPrivate();
                break;
        }
    }

    private void joinPrivate() {
    }

    private void createPrivate() {
    }

    private void createPublic() {
    }

    private void searchTables() {
    }

    private void login() {
        boolean logged = false;
        System.out.print("Enter Username >");
        String username = scanner.next();
        System.out.print("Enter Password >");
        String password = MyUtilities.sha256(scanner.next());

        MyPlayer loginUser = new MyPlayer(username, password);

        int tries = 0;
        boolean success = false;

        while (tries < MAX_TRIES && !success) {
            System.out.println("Sending request");
            logged = MyClient.loginUser(loginUser);
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
            System.out.println("logged in successfully");
            tableMenu();
        } else {
            System.out.println("not logged in");
            login();
        }
    }

    public void register() {
        int registered = -1;
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

}
