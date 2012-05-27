package cse461.snet.os;

import java.lang.reflect.Method;

import org.json.JSONObject;

/**
 * Base class for OS loaded services, like RPC and (Project 4) DDNS, and an inner
 * class (RPCCallableMethod) that makes it easy for those services to specify Java
 * methods to invoke when corresponding RPC calls come in.
 * <p> 
 * All objects that can receive RPC invocations must support THREE common methods:
 * <ul>
 * <li>A constructor taking no arguments
 * <li>  servicename() method returning their name.
 * <li>A shutdown() method
 * </ul>
 *
 * @author zahorjan
 *
 */
public abstract class RPCCallable {
		
	/**
	 * Returns the name used to identify the service in RPC calls.
	 * For example, the instance for the RPC service should return "rpc".
	 * @return
	 */
	public abstract String servicename();

	/**
	 * Called when the service should shut down.  If the service has started 
	 * any threads, its important that they be terminated - otherwise, the application
	 * as a whole won't terminate.  You can do any other cleanup that's required 
	 * as well.
	 */
	public abstract void shutdown();

	/**
	 * An object of this type represents an RPC callable method.  You'll have one object
	 * of this type for each method exposed by RPC.
	 * <p>
	 * The parameterized type, T, is the class exposing the method.
	 * <p>
	 * See the source in EchoService.java for an example of its use.
	 * @author zahorjan
	 *
	 * @param <T>
	 */
	protected static class RPCCallableMethod<T> {
		T service;
		Method method;
		/**
		 * Constructor.
		 * @param serviceObject The Java instance of the object that will field the RPC
		 * @param methodName The name of the Java method to invoke on that object, as a String
		 * @throws NoSuchMethodException
		 */
		RPCCallableMethod(T serviceObject, String methodName) throws NoSuchMethodException { 
			service = serviceObject; 
			Class<T> serviceClass = (Class<T>)service.getClass();
			method = serviceClass.getMethod(methodName, JSONObject.class);
		}
		/**
		 * This method is called to actually invoke the method that handles the RPC.
		 * @param args  The arguments to pass on this call
		 * @return The JSONObject returned by the RPC handling method of the service.
		 * @throws Exception
		 */
		public JSONObject handleCall(JSONObject args) throws Exception {
			return (JSONObject)method.invoke(service, args);
		}
	}
	
}
