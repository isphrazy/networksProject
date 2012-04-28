package edu.uw.cs.cse461.sp12.OS;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import edu.uw.cs.cse461.sp12.util.TCPMessageHandler;

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
	private final String HOST_JSON = "cse461";
	
	private String mRemoteHost;
	private OutputStream mOs;
	private InputStream mIs;
	private int msgId;
	private TCPMessageHandler tcpHandler;
	
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
		msgId = 0;
		String respond = null;
		tcpHandler = new TCPMessageHandler(this);
		try {
			do{
				msgId ++;
				String handShakeMessage = createHandShakeJsonMessage();
//				System.out.println(handShakeMessage);
				tcpHandler.sendMessage(handShakeMessage);
				respond = tcpHandler.readMessageAsString();
			}while(!checkStatus(respond));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
	}
	
	
	/**
	 * Close this socket.
	 */
	@Override
	public void close() {
		//TODO: implement
		tcpHandler.discard();
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
//		System.out.println("start invoking");
		String outputStream = generateJsonMessage(service, method, userRequest);
//		System.out.println(outputStream);
		String respond = null;
		try {
			do{
				msgId ++;
				tcpHandler.sendMessage(outputStream);
				respond = tcpHandler.readMessageAsString();
			}while(!checkStatus(respond));
			return new JSONObject(respond).getJSONObject("value");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	//create json string based on the info given
	private String generateJsonMessage(String service, String method, JSONObject userRequest) {
		JSONObject messageJ = new JSONObject();
		try {
			messageJ.put("id", msgId);
			messageJ.put("app", service);
			messageJ.put("args", userRequest);
			messageJ.put("host", HOST_JSON);
			messageJ.put("type", "invoke");
			messageJ.put("method", method);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return messageJ.toString();
	}
	
	//create json string for handshanking
	private String createHandShakeJsonMessage() {
		JSONObject handShakeJ = new JSONObject();
		try {
			handShakeJ.put("id", msgId);
			handShakeJ.put("host", HOST_JSON);
			handShakeJ.put("action", "connect");
			handShakeJ.put("type", "control");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return handShakeJ.toString();
	}
	
	//check respond status
	private boolean checkStatus(String respond) throws JSONException{
		if(respond == null) return false;
		return new JSONObject(respond).getString("type").equals("OK");
	}
	
}
