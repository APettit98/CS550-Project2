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
            connectToDatabase();
        } catch (Exception e) {
            System.out.println("Failed to connect to database");
            return;
        }
        try {
            runStartupScript();
        } catch (Exception e) {
            System.out.println("Failed to run startup script: " + e);
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

    public static void runStartupScript() throws FileNotFoundException {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter path to sql script file: ");
        String path = sc.nextLine();

        ScriptRunner sr = new ScriptRunner(con);
        sr.setLogWriter(null);
        sr.runScript(new FileReader(path));
        System.out.println("Script executed successfully.");
    }

    public static void viewTableContents() throws SQLException {
        Scanner sc = new Scanner(System.in);

        System.out.print("View PUBLICATIONS table? (Yes/No): ");
        String viewPublications = sc.nextLine();

        if (viewPublications.toLowerCase().startsWith("y")) {
            String publicationQuery = "SELECT * FROM PUBLICATIONS";
            ResultSet rs = stmt.executeQuery(publicationQuery);
            System.out.println("\n------------------------------------------------------------");
            while (rs.next()) {
                System.out.println("PUBLICATION ID: " + rs.getInt("PUBLICATIONID"));
                System.out.println("TITLE         : " + rs.getString("TITLE"));
                System.out.println("YEAR          : " + rs.getInt("YEAR"));
                System.out.println("TYPE          : " + rs.getString("TYPE"));
                System.out.println("SUMMARY       : " + rs.getString("SUMMARY"));
                System.out.println("------------------------------------------------------------");
            }
            rs.close();
        }

        System.out.print("View AUTHORS table? (Yes/No): ");
        String viewAuthors = sc.nextLine();

        if (viewAuthors.toLowerCase().startsWith("y")) {
            String authorQuery = "SELECT * FROM AUTHORS";
            ResultSet rs = stmt.executeQuery(authorQuery);
            System.out.println("\n------------------------------------------------------------");
            while (rs.next()) {
                System.out.println("PUBLICATION ID: " + rs.getInt("PUBLICATIONID"));
                System.out.println("AUTHOR        : " + rs.getString("AUTHOR"));
                System.out.println("------------------------------------------------------------");
            }
            rs.close();
        }
    }

    public static void searchByPublicationId() throws SQLException {
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
        if (rs.next()) {
            System.out.println("PUBLICATION ID   : " + rs.getInt("PUBLICATIONID"));
            System.out.println("TITLE            : " + rs.getString("TITLE"));
            System.out.println("YEAR             : " + rs.getInt("YEAR"));
            System.out.println("TYPE             : " + rs.getString("TYPE"));
            System.out.println("SUMMARY          : " + rs.getString("SUMMARY"));
            System.out.println("NUMBER OF AUTHORS: " + rs.getInt("NUM_AUTHORS"));
            System.out.println("------------------------------------------------------------");
        } else {
            System.out.println("No result found for PUBLICATIONID: " + publicationId);
        }

        rs.close();
        pstmt.close();
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
                    viewTableContents();
                    break;
                case 2:
                    System.out.println("Search by PUBLICATIONID");
                    searchByPublicationId();
                    break;
                case 3:
                    searchByAttributes();
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