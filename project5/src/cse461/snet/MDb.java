package cse461.snet;

public class MDb {

    private static MDb instance;
    
    private MDb(){}
    
    public static MDb getInstance(){
        if(instance == null){
            instance = new MDb();
        }
        
        return instance;
    }
}
