package edu.uw.cs.cse461.sp12.OS;

import java.io.IOException;
import java.net.Socket;

import org.json.JSONObject;

/**
 * Implements a Socket to use in sending remote RPC invocations.  (It must engage
 * in the RPC handshake before sending the invocation request.)
 * @author zahorjan
 *
 */
public class RPCCallerSocket extends Socket {
	// This variable is part of the android Log.x idiom, as in Log.v(TAG, "some debugging log message")
	// You can use Log.x in console apps as well.
	private static final String TAG = "RPCCallerSocket";
	
	private String mRemoteHost;
	
	/**
	 * Create a socket for sending RPC invocations, connecting it to the specified remote ip and port.
	 * @param hostname In Project 4, it's intended to be the string name of the remote system.  In Project 3, it's not terribly meaningful - repeat the ip.
	 * @param ip  Remote system IP address.
	 * @param port Remote RPC service's port.
	 * @throws IOException
	 */
	public RPCCallerSocket(String hostname, String ip, String port) throws IOException {
		super(ip, Integer.parseInt(port));

		mRemoteHost = hostname;

		// An rpc timeout value is specified in the config file.  You should use that one, not this literal.
		int rpcTimeout = 5000;  
		this.setSoTimeout(rpcTimeout);
		
		//TODO: implement
	}
	
	/**
	 * Close this socket.
	 */
	@Override
	public void close() {
		//TODO: implement
	}
	
	/**
	 * Returns the name of the remote host to which this socket is connected (as specified in the constructor call).
	 * Useful in Project 4.
	 */
	public String remotehost() {
		return mRemoteHost;
	}

	/**
	 * Causes a remote call to the service/method names by the arguments.
	 * @param service Name of the remote service (or application)
	 * @param method Method of that service to invoke
	 * @param userRequest Call arguments
	 * @return
	 */
	public JSONObject invoke(String service, String method, JSONObject userRequest) {
		//TODO: implement
		return null;
	}
	
}
