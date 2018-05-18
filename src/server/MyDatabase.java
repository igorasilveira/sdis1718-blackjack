package server;

import java.sql.*;

public class MyDatabase {
    private static String dbURL = "jdbc:derby:/Users/igorsilveira/Documents/GitHub/sdis1718-blackjack/db";
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

    public static boolean insertUser(String username, byte[] password, byte[] hash)
    {
        try
        {
            PreparedStatement stmt = conn.prepareStatement("INSERT into  USERS_TABLE(USERNAME, PASSWORD, SALT) values (? ,?, ?)");
            stmt.setString(1, username);
            stmt.setBytes(2, password);
            stmt.setBytes(3, hash);
            return stmt.execute();
        }
        catch (SQLException sqlExcept)
        {
            sqlExcept.printStackTrace();
        }
        return false;
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

    public static int loginUser(String username, String password) {
        int count = 0;
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT PASSWORD, SALT FROM USERS_TABLE where username=?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            System.out.println(rs);
            while (rs.next()){
                count += 1;
            }
        } catch (SQLException sqlExcept) {
            sqlExcept.printStackTrace();
            return -1;
        }
        return count;
    }
}
