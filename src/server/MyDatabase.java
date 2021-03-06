package server;

import java.sql.*;

public class MyDatabase {
    private static String dbURL = "jdbc:derby:../db";
    // private static String dbURL = "jdbc:derby:/Users/igorsilveira/Documents/GitHub/sdis1718-blackjack/db";
    //private static String dbURL = "jdbc:derby:D:/Data/GitHub/sdis1718-blackjack/db";
    // jdbc Connection
    private static Connection conn = null;
    private static Statement stmt = null;

    public MyDatabase() {
    }

    public static void createConnection()
    {
        try
        {
            Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
            //Get a connection
            conn = DriverManager.getConnection(dbURL);
        }
        catch (Exception except)
        {
            except.printStackTrace();
        }
    }

    public static int insertUser(String username, byte[] password, byte[] hash)
    {
        try
        {
            PreparedStatement stmt = conn.prepareStatement("SELECT ID FROM USERS_TABLE WHERE USERNAME = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next())
                return 1;

            stmt = conn.prepareStatement("INSERT into  USERS_TABLE(USERNAME, PASSWORD, SALT) values (? ,?, ?)");
            stmt.setString(1, username);
            stmt.setBytes(2, password);
            stmt.setBytes(3, hash);
            int count = stmt.executeUpdate();
            if (count > 0)
                return 0;
        }
        catch (SQLException sqlExcept)
        {
            sqlExcept.printStackTrace();
        }
        return -1;
    }

    private static void selectUsers()
    {
        try
        {
            stmt = conn.createStatement();
            ResultSet results = stmt.executeQuery("select * from user");
            ResultSetMetaData rsmd = results.getMetaData();
            int numberCols = rsmd.getColumnCount();
            for (int i=1; i<=numberCols; i++)
            {
                //print Column Names
                System.out.print(rsmd.getColumnLabel(i)+"\t\t");
            }

            System.out.println("\n-------------------------------------------------");

            while(results.next())
            {
                int id = results.getInt(1);
                String restName = results.getString(2);
                String cityName = results.getString(3);
                System.out.println(id + "\t\t" + restName + "\t\t" + cityName);
            }
            results.close();
            stmt.close();
        }
        catch (SQLException sqlExcept)
        {
            sqlExcept.printStackTrace();
        }
    }

    public static void shutdown()
    {
        try
        {
            if (stmt != null)
            {
                stmt.close();
            }
            if (conn != null)
            {
                DriverManager.getConnection(dbURL + ";shutdown=true");
                conn.close();
            }
        }
        catch (SQLException sqlExcept)
        {

        }

    }

    public static boolean checkCredits(String username, int credits) {
        try {
            PreparedStatement statement = conn.prepareStatement("SELECT CREDITS FROM USERS_TABLE WHERE USERNAME = ?");
            statement.setString(1, username);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                // remove them
                int amount = rs.getInt(1);
                if (amount - credits < 0)
                    return false;
                else {
                    String newAmount = String.valueOf(amount - credits);
                    statement = conn.prepareStatement("UPDATE USERS_TABLE SET CREDITS = ? WHERE USERNAME = ?");
                    statement.setString(1, newAmount);
                    statement.setString(2, username);
                    int count = statement.executeUpdate();

                    return count > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        return false;
    }

    public static boolean addCredits(String username, int credits) {

        try {
            PreparedStatement statement = conn.prepareStatement("SELECT CREDITS FROM USERS_TABLE WHERE USERNAME = ?");
            statement.setString(1, username);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                // remove them
                int amount = rs.getInt(1);
                int newAmount = amount + credits;
                String toUpdate = String.valueOf(newAmount);
                statement = conn.prepareStatement("UPDATE USERS_TABLE SET CREDITS = ? WHERE USERNAME = ?");
                statement.setString(1, toUpdate);
                statement.setString(2, username);
                int count = statement.executeUpdate();

                return count > 0;

            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        return false;
    }

    public static int loginUser(String username, String password) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT PASSWORD, SALT, CREDITS FROM USERS_TABLE where username=?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next())
                if (MyPasswords.isExpectedPassword(password.toCharArray(), rs.getBytes(2), rs.getBytes(1)))
                    return rs.getInt(3);
        } catch (SQLException sqlExcept) {
            sqlExcept.printStackTrace();
            return -1;
        }
        return -1;
    }
}
