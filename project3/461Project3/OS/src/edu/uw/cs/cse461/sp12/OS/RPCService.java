package edu.uw.cs.cse461.sp12.OS;

import edu.uw.cs.cse461.sp12.util.TCPMessageHandler;
import java.net.ServerSocket;
import java.io.*;
import java.net.*;
import java.util.*;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Implements the side of RPC that receives remote invocation requests.
 * 
 * @author zahorjan
 *
 */
public class RPCService extends RPCCallable {
	// used with the android idiom Log.x, as in Log.v(TAG, "some debug log message")
	private static final String TAG="RPCService";
	private static final int maxNumOfConnection = 100;
	private static int numOfConnection;
	private static int msgId;
	private int rpcTimeout;
	private Server server;
	private Map<String, Map<String, RPCCallableMethod>> services;
	private ServerSocket mServerSocket;
	
	/**
	 * This method must be implemented by RPCCallable's.  
	 * "rpc" is the well-known name of this service.
	 */
	@Override
	public String servicename() {
		return "rpc";
	}
	
	/**
	 * Constructor.  Creates the Java ServerSocket and binds it to a port.
	 * If the config file specifies an rpc.serverport value, it should be bound to that port.
	 * Otherwise, you should specify port 0, meaning the operating system should choose a currently unused port.
	 * (The config file settings are available via the OS object.)
	 * <p>
	 * Once the port is created, a thread needs to be created to listen for connections on it.
	 * 
	 * @throws Exception
	 */
	RPCService() throws Exception {
		msgId = 0;
		numOfConnection = 0;
		services = new HashMap<String, Map<String, RPCCallableMethod>>();
		String configServerPort = OS.config().getProperty("rpc.serverport");
		int serverport = 0;
		if (!configServerPort.isEmpty())
			serverport = Integer.parseInt(OS.config().getProperty("rpc.serverport"));
		mServerSocket = new ServerSocket(serverport);
		
		String RPCTimeout= OS.config().getProperty("rpc.timeout");
		this.rpcTimeout = 5000;
		try {
			this.rpcTimeout = Integer.parseInt(RPCTimeout);
		} catch (Exception e) {
		}
		
		// Set some socket options.  
		// setReuseAddress lets you reuse a server port immediately after terminating
		// an application that has used it.  (Normally that port is unavailable for a while, for reasons we'll see
		// later in the course.
		// setSoTimeout causes a thread waiting for connections to timeout, instead of waiting forever, if no connection
		// is made before the timeout interval expires.  (You don't have to use 1/2 sec. for this value - choose your own.)
		mServerSocket.setReuseAddress(true); // allow port number to be reused immediately after close of this socket
		mServerSocket.setSoTimeout(10000); // well, we have to wake up every once and a while to check for program termination
		server = new Server(mServerSocket);
		server.start();
	}
	
	/**
	 * System is shutting down imminently.  Do any cleanup required.
	 */
	public void shutdown() {
		try {
			server.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Services and applications with RPC callable methods register them with the RPC service using this routine.
	 * Those methods are then invoked as callbacks when an remote RPC request for them arrives.
	 * @param serviceName  The name of the service.
	 * @param methodName  The external, well-known name of the service's method to call
	 * @param method The descriptor allowing invocation of the Java method implementing the call
	 * @throws Exception
	 */
	public synchronized void registerHandler(String serviceName, String methodName, RPCCallableMethod method) throws Exception {
		if (!services.containsKey(serviceName))
			services.put(serviceName, new HashMap<String, RPCCallableMethod>());
		if (!services.get(serviceName).containsKey(methodName))
			services.get(serviceName).put(methodName, method);
	}
	
	/**
	 * Returns the local IP address.
	 * @return
	 * @throws UnknownHostException
	 */
	public String localIP() throws UnknownHostException {
		return IPFinder.getInstance().getIp();
	}

	/**
	 * Returns the port to which the RPC ServerSocket is bound.
	 * @return
	 */
	public int localPort() {
		return mServerSocket.getLocalPort();
	}
	
	/* ------------------------- Inner Classes ------------------------- */
	
	/** 
	 * Server extends Thread and is used to accept connections from the web
	 * 
	 * @author Cheng Hao Chuang & Pingyang He
	 */
	class Server extends Thread {
		private ServerSocket server;
		private boolean running;
		
		/**
		 * Constructs a Server object. use server as the port to accept
		 * connections.
		 * 
		 * @param server
		 */
		public Server(ServerSocket server) {
			this.server = server;
			this.running = true;
		}
		
		/**
		 * Closes this server when the program terminates
		 * 
		 * @throws IOException
		 */
		public void close() throws IOException {
			try {
				running = false;
				server.close();
			} catch (IOException e) {
			}
		}
		
		/**
		 * Server waits for connection. When a connection is received it forks
		 * a new thread to handle the request. Server wakes up every once a while
		 * and if the program is still running it will go back and wait for connections
		 */
		public void run() {
			try {
				while (running) {
					Socket incoming_socket = server.accept();
					RPCHandler rpcHandler = new RPCHandler(incoming_socket);
					rpcHandler.start();
//					rpcHandler.run();
				}
			} catch (SocketTimeoutException e) {
				if (!server.isClosed())
					this.run();
			} catch (IOException e) {} 
		}
	}
	
	/**
	 * RPCHandler is a thread that handles the incoming connection
	 * It establishes the handshake and wait for any invocation request
	 *  
	 * @author Cheng Hao Chuang & Pingyang He
	 */
	class RPCHandler extends Thread {
		private Socket socket;
		private TCPMessageHandler tcpHandler;
		private boolean connected;
		private boolean running;
		
		/**
		 * Constructs a RPCHandler object
		 * Sets TCPMessageHandler to this socket 
		 * 
		 * @param socket
		 */
		public RPCHandler(Socket socket) {
			this.socket = socket;
			try {
				this.socket.setSoTimeout(rpcTimeout);
				this.connected = false;
				this.running = true;
				this.tcpHandler = new TCPMessageHandler(this.socket);
			} catch (IOException e) {}
		}
		
		/**
		 * Establishes the handshake and wait for any invocation message
		 * Server times out after rpctimeout
		 * 
		 * If the JSON message received is not compatible with the rpc protocol 
		 * or current number of connect exceeds the max number connection allowed, 
		 * an error JSON message will be returned.
		 * 
		 * Otherwise, a JSON message that followed the rpc protocol will be returned
		 */
		public void run() {
			try {
				while (running) {
					JSONObject request = tcpHandler.readMessageAsJSONObject();
					msgId++;
					if (!connected) {
						// check if the request follows the rpc protocol
						String type = checkRequest(request, "handshake");
						if (type.equals("ERROR")) {
							String errorResponse = createJsonMessage(
									msgId, type, "Incompatible protocol"); 
							tcpHandler.sendMessage(errorResponse);
							throw new IllegalArgumentException();
						}
						
						// check if we can allow any more connections
						if (numOfConnection >= maxNumOfConnection) {
							String errorResponse = createJsonMessage(
									request.getInt("id"), type, "Max connections exceeded");
							tcpHandler.sendMessage(errorResponse);
							throw new IllegalArgumentException();
						} else {
							String okReponse = createJsonMessage(
									request.getInt("id"), type, "");
							tcpHandler.sendMessage(okReponse);
							connected = true;
						}
					} else {
						// check if the request follows the rpc protocol
						String status = checkRequest(request, "invoke");
						if (status.equals("ERROR")) {
							String errorResponse = createJsonMessage(
									msgId, status, "Incompatible protocol"); 
							tcpHandler.sendMessage(errorResponse);
							throw new IllegalArgumentException();
						}
						
 						int id = request.getInt("id");
						String app = request.getString("app");
						JSONObject args = request.getJSONObject("args");
						String method = request.getString("method");
						String type = request.getString("type");
						
						// If service requested is not available or 
						if (type.equals("invoke")){
							if (!services.containsKey(app)) {
								String errorResponse = createErrorResponseJSONMessage(
										"No such service, connection is terminated", id, request);
								tcpHandler.sendMessage(errorResponse);
								throw new IllegalArgumentException();
							}
							if (!services.get(app).containsKey(method)) {
								String errorResponse = createErrorResponseJSONMessage(
										"The service has no such method, connection is terminated", id, request);
								tcpHandler.sendMessage(errorResponse);
								throw new IllegalArgumentException();
							}
							RPCCallableMethod serviceMethod = services.get(app).get(method);
							JSONObject returnedValue = serviceMethod.handleCall(args);
							String response = createResponseJSONMessage(id,returnedValue);
							tcpHandler.sendMessage(response);
							this.close();
							break;
						}
					}
				}
			} catch (Exception e) {
				this.close();
			}
		}
		
		/**
		 * Closes this socket when the work is done
		 */
		public void close() {
			try{
				connected = false;
				running = false;
				numOfConnection--;
				tcpHandler.discard();
			} catch (Exception e) {}
		}
		
		// create json string for accepting connection
		private String createJsonMessage(int callid, String status, String msg) {
			JSONObject handShakeJ = new JSONObject();
			try {
				handShakeJ.put("id", msgId);
				handShakeJ.put("host", "");
				handShakeJ.put("callid", callid);
				handShakeJ.put("type", status);
				if (!msg.isEmpty())
					handShakeJ.put("msg", msg);
			} catch (JSONException e) {}
			return handShakeJ.toString();
		}
		
		// create json string for accepting connection
		private String createResponseJSONMessage(int callid, JSONObject value) {
			JSONObject response = new JSONObject();
			try {
				response.put("id", msgId);
				response.put("host", "");
				response.put("callid", callid);
				response.put("value", value);
				response.put("type", "OK");
			} catch (JSONException e) {}
			return response.toString();
		}
		
		// create json error message for the request
		private String createErrorResponseJSONMessage(String msg, int callid, JSONObject callargs) {
			JSONObject errorResposne = new JSONObject();
			try {
				errorResposne.put("message", msg);
				errorResposne.put("id", msgId);
				errorResposne.put("host", "");
				errorResposne.put("callid", callid);
				errorResposne.put("type", "ERROR");
				errorResposne.put("callargs", callargs);
			} catch (JSONException e) {}
			return errorResposne.toString();
		}
		
		// check if request follows the rpc protocol
		private String checkRequest(JSONObject request, String type) {
			if (type.equals("handshake")) {
				if (request.has("id"))
					return "OK";
				else
					return "ERROR";
			} else {
				if (request.has("id") && request.has("app") && request.has("args") &&
						request.has("method") && request.has("type"))
					return "OK";
				else
					return "ERROR";
			}
		}
	}
}
