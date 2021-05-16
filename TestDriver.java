import designpatterns.resourcepool.*;
import designpatterns.database.*;
import java.sql.*;

public class TestDriver {
    private static ResourcePool pool;
    private static void runQuery(String query,Connection conn) throws Exception {
        PreparedStatement preparedStatement = conn.prepareStatement(query);
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }
    
    private static class userThread extends Thread {
        public void run () {
            DBConnection conn = null;
            Connection sqlConn = null;
            int numQueries=1; //1 for controlled run, 5 for practical run
            int sleepFactor=3;
            try {
                for(int i=0;i<numQueries;i++) {                                  
                    conn = (DBConnection) pool.getResource();                    
                    while(conn == null) {                       
                        pool.waitForResource();
                        conn = (DBConnection) pool.getResource();
                    }
                    sqlConn = conn.getSQLConnection();                                  
                    runQuery("insert into  designpatterns.user values (111,'demo')",sqlConn);
                    Thread.sleep((long) (Math.random() * 1000)*sleepFactor); 
                    pool.releaseResource(conn);                    
                }
            }            
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public static void main(String args[]) throws Exception { 
        //create a new pool       
        pool = new ResourcePool(11,50,new DBConnectionFactory()); 
        pool.displayPoolState();
        int threadNum=40;
        userThread useThread[] = new userThread[threadNum];
        for(int i=0;i<threadNum;i++) {
            useThread[i] = new userThread();
            useThread[i].start();
        }
    }
}

