package cse461.snet;

import edu.uw.cs.cse461.sp12.util.DB461.DB461Exception;
import edu.uw.cs.cse461.sp12.util.DB461SQLite;

/**
 * wrapper class over DB461SQLite
 * @author Pingyang He
 *
 */
public class MDb {

    private static DB461SQLite instance;
    
    private MDb(){}
    
    public static DB461SQLite getInstance(String dbName) throws DB461Exception{
        if(instance == null){
            instance = new DB461SQLite(dbName);
        }
        
        return instance;
    }
}
