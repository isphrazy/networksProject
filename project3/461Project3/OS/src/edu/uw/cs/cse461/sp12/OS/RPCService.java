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
		// Set some socket options.  
		// setReuseAddress lets you reuse a server port immediately after terminating
		// an application that has used it.  (Normally that port is unavailable for a while, for reasons we'll see
		// later in the course.
		// setSoTimeout causes a thread waiting for connections to timeout, instead of waiting forever, if no connection
		// is made before the timeout interval expires.  (You don't have to use 1/2 sec. for this value - choose your own.)
		mServerSocket.setReuseAddress(true); // allow port number to be reused immediately after close of this socket
//		mServerSocket.setSoTimeout(500); // well, we have to wake up every once and a while to check for program termination
		System.out.println(localIP());
		server = new Server(serverport, mServerSocket);
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
	
	class Server extends Thread {
		private ServerSocket server;
		private int port;
		
		public Server(int port, ServerSocket server) {
			this.port = port;
			this.server = server;
			
		}
		
		public void close() throws IOException {
			try {
				server.close();
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
		
		public void run() {
			try {
				while (true) {
					Socket incoming_socket = server.accept();
					RPCHandler rpcHandler = new RPCHandler(incoming_socket);
					rpcHandler.run();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	class RPCHandler extends Thread {
		private Socket socket;
		private TCPMessageHandler tcpHandler;
		private boolean connected;
		
		public RPCHandler(Socket socket) {
			this.socket = socket;
			this.connected = false;
			try {
				tcpHandler = new TCPMessageHandler(this.socket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void run() {
			try {
				while (true) {
					JSONObject request = tcpHandler.readMessageAsJSONObject();
					System.out.println(request.toString());
					msgId++;
					if (!connected) {
						if (numOfConnection >= maxNumOfConnection) {
							String errorResponse = createHandShakeJsonMessage(
									request.getInt("id"), "ERROR", "Max connections exceeded");
							System.out.println(errorResponse);
							tcpHandler.sendMessage(errorResponse);
							this.close();
						} else {
							String okReponse = createHandShakeJsonMessage(
									request.getInt("id"), "OK", "");
							System.out.println(okReponse);
							tcpHandler.sendMessage(okReponse);
							connected = true;
						}
					} else {
						int id = request.getInt("id");
						String app = request.getString("app");
						JSONObject args = request.getJSONObject("args");
						String method = request.getString("method");
						String type = request.getString("type");
						if (type.equals("invoke")){
							if (!services.containsKey(app)) {}
								// returns an error response
							if (!services.get(app).containsKey(method)) {}
								// returns an error response
							RPCCallableMethod serviceMethod = services.get(app).get(method);
							JSONObject returnedValue = serviceMethod.handleCall(args);
							String response = createResponseJSONMessage(id,returnedValue);
							tcpHandler.sendMessage(response);
							this.close();
							break;
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public void close() throws IOException {
			connected = false;
			numOfConnection--;
			tcpHandler.discard();
		}
		
		// create json string for accepting connection
		private String createHandShakeJsonMessage(int callid, String status, String msg) {
			JSONObject handShakeJ = new JSONObject();
			try {
				handShakeJ.put("id", msgId);
				handShakeJ.put("host", "");
				handShakeJ.put("callid", callid);
				handShakeJ.put("type", status);
				if (!msg.isEmpty())
					handShakeJ.put("msg", msg);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return handShakeJ.toString();
		}
		
		// create json string for accepting connection
		private String createResponseJSONMessage(int callid, JSONObject value) {
			JSONObject handShakeJ = new JSONObject();
			try {
				handShakeJ.put("id", msgId);
				handShakeJ.put("host", "");
				handShakeJ.put("callid", callid);
				handShakeJ.put("value", value);
				handShakeJ.put("type", "OK");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return handShakeJ.toString();
		}
	}
}
