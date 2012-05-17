package edu.uw.cs.cse461.sp12.OS;

public class DDNSRRecord {
    public String type;
    public String ip;
    public String portString;
    public int port;
    public boolean initialized;
    
    public DDNSRRecord(){
        type = "";
        ip = "";
        portString = "";
        port = 0;
        initialized = false;
    }
    
    public String toString(){
        return type;
    }
}
