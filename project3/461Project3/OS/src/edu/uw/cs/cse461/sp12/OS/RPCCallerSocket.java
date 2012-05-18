package edu.uw.cs.cse461.sp12.OS;

import java.io.IOException;
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
		String RPCTimeout= OS.config().getProperty("rpc.timeout");
		int rpcTimeout = 5000;
		try {
			rpcTimeout = Integer.parseInt(RPCTimeout);
		} catch (Exception e) {
		}
		this.setSoTimeout(rpcTimeout);
		msgId = 0;
		tcpHandler = new TCPMessageHandler(this);
	}
	
	/**
	 * Close this socket.
	 */
	@Override
	public void close() {
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
		String respond = null;
		try {
			do{
				msgId++;
				String handShakeMessage = createHandShakeJsonMessage();
				tcpHandler.sendMessage(handShakeMessage);
				respond = tcpHandler.readMessageAsString();
				if (!checkResponse(respond, "handshake"))
					throw new IllegalArgumentException();
			} while(!checkStatus(respond, "OK"));
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			try {
				return new JSONObject().put("msg", "An error has occurd.");
			} catch (JSONException e1) {}
		} catch (IllegalArgumentException e) {
			try {
				return new JSONObject().put("msg", "An handshake error has occurd, connection is terminated");
			} catch (JSONException e1) {}
		}
		
		String outputStream = generateJsonMessage(service, method, userRequest);
		respond = null;
		
		try {
			msgId++;
			tcpHandler.sendMessage(outputStream);
			respond = tcpHandler.readMessageAsString();
			if (!checkResponse(respond, "invoke"))
				throw new IllegalArgumentException();
			if (checkStatus(respond, "ERROR")) {
				JSONObject res = new JSONObject(respond);
				return new JSONObject().put("msg", res.getString("message"));
			}
			return new JSONObject(respond).getJSONObject("value");
		} catch (IOException e) {
			try {
				return new JSONObject().put("msg", "An error has occurd");
			} catch (JSONException e1) {}		
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			try {
				return new JSONObject().put("msg", "An error has occurd, connection is terminated");
			} catch (JSONException e1) {}
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
	
	//create json string for handshaking
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
	private boolean checkStatus(String respond, String type) throws JSONException{
		if(respond == null) return false;
		return new JSONObject(respond).getString("type").equals(type);
	}
	
	// check if res follows the rpc protocol
	private boolean checkResponse(String res, String type) {
		JSONObject respond;
		try {
			respond = new JSONObject(res);
			if (type.equals("handshake")) {
				if (respond.has("id") && respond.has("type"))
					return true;
				else
					return false;
			} else {
				if (respond.has("id") && (respond.has("value") || respond.has("message")) && respond.has("type"))
					return true;
				else
					return false;
			}
		} catch (JSONException e) {
		}
		return false;
	}
}