import java.io.*;
import java.sql.*;
import java.util.*;
import oracle.jdbc.driver.*;
import org.apache.ibatis.jdbc.ScriptRunner;

public class Student{
    static Connection con;
    static Statement stmt;

    public static void main(String argv[])
    {
        try {
            //Method to connect to database
            connectToDatabase();
        } catch (Exception e) {
            System.out.println("Failed to connect to database");
            return;
        }
        try {
            //Method to execute SQL script
            runStartupScript();
        } catch (Exception e) {
            System.out.println("Failed to run startup script: " + e);
        }
        try{
            //Launches interactive menu for the user
            getUserInput();
        } catch (Exception e) {
            System.out.println("Error encountered: " + e);
        }
    }

    /**
     * Method to connect to Oracle Database, based on the connection string provided
     * @throws Exception
     */
    public static void connectToDatabase() throws Exception
    {
        //JDBC url
        String driverPrefixURL="jdbc:oracle:thin:@";
        String jdbc_url="artemis.vsnet.gmu.edu:1521/vse18c.vsnet.gmu.edu";

        //Prompts the db username and password
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

        //Attempts to establish a connection to the Oracle database using user input
        try{
            System.out.println(driverPrefixURL+jdbc_url);
            con=DriverManager.getConnection(driverPrefixURL+jdbc_url, username, password);
            DatabaseMetaData dbmd=con.getMetaData();
            stmt=con.createStatement();

            //Once connection is successful, the metadata of the DB will display (if it exists)
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
        //Error message will display if the connection is not successful
        } catch( Exception e) {
            e.printStackTrace();
            throw e;
        }

    }
    /**
     Prompts the user to input the file path of a SQL script and executes it against the connected database.

     The method uses MyBatis's ScriptRunner to:
     - Read SQL commands from the specified file
     - Execute the script

     It assumes a valid database connection is already established (`con` is not null).
     * @throws FileNotFoundException
     **/
    public static void runStartupScript() throws FileNotFoundException {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter path to sql script file: ");
        String path = sc.nextLine();

        // Use ScriptRunner to execute the SQL script on the existing database connection
        ScriptRunner sr = new ScriptRunner(con);
        sr.setLogWriter(null);
        sr.runScript(new FileReader(path));
        System.out.println("Script executed successfully.");
    }

    /**
     * The following method will allow the user to view Publication and Author records
     * @throws SQLException
     */
    public static void viewTableContents() throws SQLException {
        /** Allows the user to determine if they would like to view records in the Publications table
            If the user enters 'Y' or 'Yes', the connection object will send the query statement
            to the DB server
         **/
        Scanner sc = new Scanner(System.in);
        String viewPublications;

        while (true) {
            System.out.print("View PUBLICATIONS table? (Y/N): ");
            viewPublications = sc.nextLine();

            if (viewPublications.equalsIgnoreCase("Y") ||
            viewPublications.equalsIgnoreCase("N")) {
                break;
            } else {
                System.out.print("Please enter either (Y/N) \n");
            }
        }

        if (viewPublications.toLowerCase().startsWith("y")) {
            String publicationQuery = "SELECT * FROM PUBLICATIONS";
            ResultSet rs = stmt.executeQuery(publicationQuery);
            System.out.println("\n------------------------------------------------------------");

            //Prints tuples in the Publications table
            while (rs.next()) {
                System.out.println("PUBLICATION ID: " + rs.getInt("PUBLICATIONID"));
                System.out.println("TITLE         : " + rs.getString("TITLE"));
                System.out.println("YEAR          : " + rs.getInt("YEAR"));
                System.out.println("TYPE          : " + rs.getString("TYPE"));
                System.out.println("SUMMARY       : " + rs.getString("SUMMARY"));
                System.out.println("------------------------------------------------------------");
            }
            //Closes the result set, after all records are returned
            rs.close();
        }

        /** Allows the user to determine if they would like to view records in the Authors table
         If the user enters 'Y' or 'Yes', the connection object will send the query statement
         to the DB server
         **/
        String viewAuthors;

        while (true) {
            System.out.print("View AUTHORS table? (Y/N): ");
            viewAuthors = sc.nextLine();

            if (viewAuthors.equalsIgnoreCase("Y") ||
                    viewAuthors.equalsIgnoreCase("N")) {
                break;
            } else {
                System.out.print("Please enter either (Y/N) \n");
            }
        }

        if (viewAuthors.toLowerCase().startsWith("y")) {
            String authorQuery = "SELECT * FROM AUTHORS";
            ResultSet rs = stmt.executeQuery(authorQuery);
            System.out.println("\n------------------------------------------------------------");

            //Prints tuples in the Authors table
            while (rs.next()) {
                System.out.println("PUBLICATION ID: " + rs.getInt("PUBLICATIONID"));
                System.out.println("AUTHOR        : " + rs.getString("AUTHOR"));
                System.out.println("------------------------------------------------------------");
            }
            //Closes the result set, after all records are returned
            rs.close();
        }
    }

    /**
     * The following method will allow the user to search by PublicationId
     * Upon entering a valid PublicationId, all attributes from the Publications table will return,
     * including the number of authors associated with the PublicationId.
     * @throws SQLException
     */
    public static void searchByPublicationId() throws SQLException {

        /** Allows the user to enter a PublicationId
         Upon entering a PublicationId, the connection object will send the query statement
         to the DB server
         **/
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter PUBLICATIONID: ");
        int publicationId;
        try {
            publicationId = Integer.parseInt(sc.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid Publication ID");
            return;
        }

        String searchPubIdQuery = """
        SELECT P.PUBLICATIONID, P.TITLE, P.YEAR, P.TYPE, P.SUMMARY, COUNT(A.AUTHOR) AS NUM_AUTHORS
        FROM PUBLICATIONS P LEFT JOIN AUTHORS A ON P.PUBLICATIONID = A.PUBLICATIONID
        WHERE P.PUBLICATIONID = ?
        GROUP BY P.PUBLICATIONID, P.TITLE, P.YEAR, P.TYPE, P.SUMMARY
        """;

        PreparedStatement pstmt = con.prepareStatement(searchPubIdQuery);
        pstmt.setInt(1, publicationId);
        ResultSet rs = pstmt.executeQuery();

        System.out.println("\n------------------------------------------------------------");

        //Prints records, based on the query statement that has been passed in by the Connection object
        if (rs.next()) {
            System.out.println("PUBLICATION ID   : " + rs.getInt("PUBLICATIONID"));
            System.out.println("TITLE            : " + rs.getString("TITLE"));
            System.out.println("YEAR             : " + rs.getInt("YEAR"));
            System.out.println("TYPE             : " + rs.getString("TYPE"));
            System.out.println("SUMMARY          : " + rs.getString("SUMMARY"));
            System.out.println("NUMBER OF AUTHORS: " + rs.getInt("NUM_AUTHORS"));
            System.out.println("------------------------------------------------------------");
        } else {
            //If the PublicationId does not exist, no records will return
            System.out.println("No result found for PUBLICATIONID: " + publicationId);
        }

        //Closes the result set and prepared statement, after all records are returned
        rs.close();
        pstmt.close();
    }

    /** This method handles user interaction through a command-line menu.
        It loops until the user chooses to exit and calls the corresponding methods based on user input.
     **/
    public static void getUserInput() throws SQLException {
        Scanner sc = new Scanner(System.in); // Scanner to read user input
        String input; // Holds raw input from user
        int intInput; // Parsed integer value from user input

        inputLoop: while (true) {
            printMenu(); // Display the main menu options
            input = sc.nextLine(); // Read user input as a string

            try {
                intInput = Integer.parseInt(input); // Try to parse input into an integer
            } catch (NumberFormatException e) {
                // If parsing fails, notify the user and continue the loop
                System.out.println("Please enter a valid menu option.");
                continue;
            }

            // Determine which action to take based on the parsed menu input
            switch (intInput) {
                case 1:
                    // Option 1: View contents of PUBLICATIONS and/or AUTHORS table
                    viewTableContents();
                    break;
                case 2:
                    // Option 2: Search for a publication by its unique PUBLICATIONID
                    searchByPublicationId();
                    break;
                case 3:
                    // Option 3: Search using one or more user-specified attributes
                    searchByAttributes();
                    break;
                case 4:
                    // Option 4: Exit program — close database resources and break loop
                    stmt.close();
                    con.close();
                    break inputLoop;
                default:
                    // If user input doesn't match any valid menu option
                    System.out.println("Please enter a valid menu option.");
                    break;
            }
        }
    }


    /** This method displays the main menu options to the user.
        It is called each time the program waits for user input.
     */
    public static void printMenu() {
        // Print a blank line followed by the menu options
        System.out.println("""
                
            Please enter a menu option
            1. View table contents
            2. Search by PUBLICATIONID
            3. Search by one or more attributes
            4. Exit""");

        // Prompt symbol to indicate the user should enter input
        System.out.print(">>>");
    }


    public static void searchByAttributes() throws SQLException {
        // If user wants to view authors, need to query authors table
        // For all other fields publications table is sufficient
        ArrayList<String> selectFields = new ArrayList<>();
        boolean queryPublications = false;
        boolean queryAuthors = false;

        // Get input and output fields from user
        Scanner sc = new Scanner(System.in);
        System.out.println("Input fields:");
        System.out.print("AUTHOR: ");
        String author = sc.nextLine();
        System.out.print("TITLE: ");
        String title = sc.nextLine();
        System.out.print("YEAR: ");
        String year = sc.nextLine();
        try {
            Integer.parseInt(year);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input - YEAR must be an integer.");
            return;
        }
        System.out.print("TYPE: ");
        String type = sc.nextLine();

        System.out.println("\nOutput fields: ");
        System.out.print("PUBLICATIONID (Yes/No): ");
        if (sc.nextLine().toLowerCase().startsWith("y")) {
            selectFields.add("p.publicationid");
            queryPublications = true;
        }
        System.out.print("AUTHOR (Yes/No): ");
        if (sc.nextLine().toLowerCase().startsWith("y")) {
            selectFields.add("a.author");
            queryAuthors = true;
        }
        System.out.print("TITLE (Yes/No): ");
        if (sc.nextLine().toLowerCase().startsWith("y")) {
            selectFields.add("p.title");
            queryPublications = true;
        }
        System.out.print("YEAR (Yes/No): ");
        if (sc.nextLine().toLowerCase().startsWith("y")) {
            selectFields.add("year");
            queryPublications = true;
        }
        System.out.print("TYPE (Yes/No): ");
        if (sc.nextLine().toLowerCase().startsWith("y")) {
            selectFields.add("p.type");
            queryPublications = true;
        }
        System.out.print("SUMMARY (Yes/No): ");
        if (sc.nextLine().toLowerCase().startsWith("y")) {
            selectFields.add("p.summary");
            queryPublications = true;
        }

        System.out.print("\nSorted by: ");
        String sortedBy = sc.nextLine();

        // If only querying for authors use that table
        // Otherwise start with publications and join authors
        String queryTable = "";
        if (queryAuthors && !queryPublications) {
            queryTable = "AUTHORS a";
        } else {
            queryTable = "PUBLICATIONS p";
        }

        String sql = "SELECT " + String.join(", ", selectFields) + " FROM " + queryTable;

        // Add join if querying from both tables
        if (queryAuthors && queryPublications) {
            sql = sql + " JOIN AUTHORS a ON p.publicationid = a.publicationid ";
        }

        // Add where statement if user added an input field
        if (!author.isEmpty() || !title.isEmpty() || !year.isEmpty() || !type.isEmpty()) {
            sql = sql + " WHERE ";
        }
        // Need to check if we've added a where clause yet
        Boolean addedWhere = false;

        if (!author.isEmpty()) {
            sql = sql + "a.AUTHOR LIKE '%" + author + "%' ";
            addedWhere = true;
        }

        if (!title.isEmpty()) {
            if (addedWhere) {
                sql = sql + " AND ";
            }
            sql = sql + "p.TITLE LIKE '%" + title + "%'";
            addedWhere = true;
        }

        if (!year.isEmpty()) {
            if (addedWhere) {
                sql = sql + " AND ";
            }
            sql = sql + "p.YEAR = '" + year + "'";
            addedWhere = true;
        }

        if (!type.isEmpty()) {
            if (addedWhere) {
                sql = sql + " AND ";
            }
            sql = sql + "p.TYPE = '" + type + "'";
        }

        if (!sortedBy.isEmpty()) {
            sql = sql + " ORDER BY " + sortedBy;
        }

        // Uncomment below to view constructed sql query
        // System.out.println(sql);

        stmt = con.prepareStatement(sql);
        ResultSet res = stmt.executeQuery(sql);

        if (!res.isBeforeFirst()) {
            System.out.println("\nQuery returned no results.");
            return;
        }

        System.out.println("\n------------------------------------------------------------");
        while (res.next()) {
            if (selectFields.contains("p.publicationid")) {
                System.out.println("PUBLICATION ID :" + res.getInt("publicationid"));
            }
            if (selectFields.contains("a.author")) {
                System.out.println("AUTHOR         :" + res.getString("author"));
            }
            if (selectFields.contains("p.title")) {
                System.out.println("TITLE          :" + res.getString("title"));
            }
            if (selectFields.contains("year")) {
                System.out.println("YEAR           :" + res.getString("year"));
            }
            if (selectFields.contains("p.type")) {
                System.out.println("TYPE           :" + res.getString("type"));
            }
            if (selectFields.contains("p.summary")) {
                System.out.println("SUMMARY        :" + res.getString("summary"));
            }
            System.out.println("------------------------------------------------------------");
        }
        res.close();
        stmt.close();

    }
}