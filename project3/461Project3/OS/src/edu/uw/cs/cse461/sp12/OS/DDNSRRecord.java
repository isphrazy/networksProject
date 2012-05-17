package edu.uw.cs.cse461.sp12.OS;

public class DDNSRRecord {
    public boolean success;
    public Integer lifetime;
    public String name;
    public String type;
    public String ip;
    public String portString;
    public int port;
    
    public DDNSRRecord(){
        type = "";
        ip = "";
        portString = "";
        port = 0;
        success = false;
    }
    
    public String toString(){
        
        return type;
    }
}
