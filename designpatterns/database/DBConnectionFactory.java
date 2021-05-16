package designpatterns.database;
import designpatterns.resourcepool.*;

/**
 * Inherits from ResourceFactory, and implements the createResource method.
 * <p> In this case, the resource would be a database connection.
 */
public class DBConnectionFactory implements ResourceFactory {
    /**
     * It calls the method which establishes the database connection.
     * @return A connection to the DB.
     */
    public Resource createResource() throws Exception {
        return new DBConnection();
    }   
}
