package cse461.snet;

import android.util.Log;
import edu.uw.cs.cse461.sp12.OS.OS;
import edu.uw.cs.cse461.sp12.util.DB461.DB461Exception;
import edu.uw.cs.cse461.sp12.util.DB461SQLite;
import edu.uw.cs.cse461.sp12.util.SNetDB461;
import edu.uw.cs.cse461.sp12.util.SNetDB461.CommunityRecord;

/**
 * wrapper class over SNetDB461
 * @author Pingyang He
 *
 */
public class MDb {

    private static SNetDB461 instance;
    
    private MDb(){}
    
    public static SNetDB461 getInstance() throws DB461Exception{
        if(instance == null){
            instance = new SNetDB461();
            String hostName = OS.config().getProperty("host.name");
            instance.registerMember("cse461.");
            instance.registerMember(hostName);
            
        }
        
        return instance;
    }
}
