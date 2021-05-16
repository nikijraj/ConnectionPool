package designpatterns.database;

import java.sql.*;
import designpatterns.resourcepool.*;


/**
 * It inherits from the Resource interface.
 */
public class DBConnection implements Resource {

    private Connection conn;

    /**
     * This method connects to the database.
     */
    public DBConnection() {

        try {
            // load the MySQL driver, each DB has its own driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            // setup the connection with the DB
            this.conn = DriverManager.getConnection("jdbc:mysql://localhost/designpatterns?"+"user=demoUser&password=passwordsql");
        } 
        catch (Exception e) {
            ResourcePool.log(e.toString());
        }
    }
    

    
    /**
     * gets SQL connection
     * @return the Connection object for a MySQL DB
     */
    public Connection getSQLConnection() {
        return conn;
    }
    

    /**
     * It kills the database connection.
     */
    public void destroyResource() {
        try {
            conn.close();
        }        
        catch (Exception e) {
            ResourcePool.log(e.toString());
        }
    }

}
