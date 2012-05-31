package cse461.snet;

import edu.uw.cs.cse461.sp12.util.DB461.DB461Exception;
import edu.uw.cs.cse461.sp12.util.DB461SQLite;
import edu.uw.cs.cse461.sp12.util.SNetDB461;

/**
 * wrapper class over DB461SQLite
 * @author Pingyang He
 *
 */
public class MDb {

    private static SNetDB461 instance;
    
    private MDb(){}
    
    public static SNetDB461 getInstance() throws DB461Exception{
        if(instance == null){
            instance = new SNetDB461();
        }
        
        return instance;
    }
}
