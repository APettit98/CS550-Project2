import java.io.*;
import java.sql.*;
import java.util.*;
import oracle.jdbc.driver.*;

public class Student{
    static Connection con;
    static Statement stmt;

    public static void main(String argv[])
    {
        try {
            connectToDatabase();
        } catch (Exception e) {
            System.out.println("Failed to connect to database");
            return;
        }
        try{
            getUserInput();
        } catch (Exception e) {
            System.out.println("Error encountered: " + e);
        }
    }

    public static void connectToDatabase() throws Exception
    {
        String driverPrefixURL="jdbc:oracle:thin:@";
        String jdbc_url="artemis.vsnet.gmu.edu:1521/vse18c.vsnet.gmu.edu";

        Scanner sc = new Scanner(System.in);
        System.out.print("Enter db username: ");
        String username = sc.nextLine();
        System.out.print("Enter db password: ");
        String password = sc.nextLine();

        try{
            //Register Oracle driver
            DriverManager.registerDriver(new OracleDriver());
        } catch (Exception e) {
            System.out.println("Failed to load JDBC/ODBC driver.");
            throw e;
        }

        try{
            System.out.println(driverPrefixURL+jdbc_url);
            con=DriverManager.getConnection(driverPrefixURL+jdbc_url, username, password);
            DatabaseMetaData dbmd=con.getMetaData();
            stmt=con.createStatement();

            System.out.println("Connected.");

            if(dbmd==null){
                System.out.println("No database meta data");
            }
            else {
                System.out.println("Database Product Name: "+dbmd.getDatabaseProductName());
                System.out.println("Database Product Version: "+dbmd.getDatabaseProductVersion());
                System.out.println("Database Driver Name: "+dbmd.getDriverName());
                System.out.println("Database Driver Version: "+dbmd.getDriverVersion());
            }
        } catch( Exception e) {
            e.printStackTrace();
            throw e;
        }

    }

    public static void getUserInput() throws SQLException {
        Scanner sc = new Scanner(System.in);
        String input;
        int intInput;
        inputLoop: while (true) {
            printMenu();
            input = sc.nextLine();
            try {
                intInput = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid menu option.");
                continue;
            }

            switch(intInput) {
                case 1:
                    System.out.println("View table contents");
                    break;
                case 2:
                    System.out.println("Search by PUBLICATIONID");
                    break;
                case 3:
                    System.out.println("Search by one or more attributes");
                    break;
                case 4:
                    stmt.close();
                    con.close();
                    break inputLoop;
                default:
                    System.out.println("Please enter a valid menu option.");
                    break;
            }

        }

    }

    public static void printMenu() {
        System.out.println("""
                Please enter a menu option
                1. View table contents
                2. Search by PUBLICATIONID
                3. Search by one or more attributes
                4. Exit""");
        System.out.print(">>>");
    }
}