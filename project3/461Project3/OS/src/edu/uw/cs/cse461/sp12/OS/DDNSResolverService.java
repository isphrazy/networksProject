package edu.uw.cs.cse461.sp12.OS;

public class DDNSResolverService extends RPCCallable{

	@Override
	public String servicename() {
		return "ddnsresolver";
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub	
	}
	
	public void register(DDNSFullName hostname, int port) {
		
	}
	
	public void unregister(DDNSFullName hostname) {
		
	}
	
	public DDNSRRecord resolve(String target) {
		return null;
	}
}
