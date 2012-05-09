package edu.uw.cs.cse461.sp12.OS;

import java.io.IOException;
import java.util.Properties;

import edu.uw.cs.cse461.sp12.util.Log;
import edu.uw.cs.cse461.sp12.util.NanoHTTPD;

/**
 * Implements a simple HTTP server.  The URI's are of form http://host:ip/<servicename>/....
 * It's up to the service to implement the HTTPProvider interface.
 * <p>
 * There's a small, technical issue that affects the implementation.  The service
 * loading code, in the OS, needs a constructor that takes no arguments.  We want to
 * extend NanoHTTPD, though, and its constructor requires a port number.  Thus, the 
 * public class contains an inner class that extends NanoHTTPD.
 * @author zahorjan
 *
 */
public class HTTPDService extends RPCCallable {
	private static final String TAG="DDNSServiceHTTPD";

	private NanoHTTPDService mNanoService;

	public interface HTTPProvider {
		// uriArray[0] is empty
		// uriArray[1] is service name (e.g., "ddns")
		// higher elements are some uri path specific to the service
		public String httpServe(String[] uriArray);
	}

	@Override
	public String servicename() { 
		return "httpd"; 
	}
	
	@Override
	public void shutdown() { 
		if ( mNanoService != null ) mNanoService.stop();
		mNanoService = null;
		try {
			DDNSResolverService resolver = (DDNSResolverService)OS.getService("ddnsresolver");
			if ( resolver == null ) Log.w(TAG, "No local resolver.  Can't unregister name www");
			else resolver.unregister(new DDNSFullName(OS.hostname() + ".www") );  
		} catch (Exception e) {
			Log.w(TAG, "ADVISORY: Caught exception while unregistering with parent:\n" + e.getMessage());
		}
	}

	private class NanoHTTPDService extends NanoHTTPD {
		/**
		 * Specify port 0 if you don't care what port the name server uses.
		 * @param port
		 * @throws IOException
		 */
		public NanoHTTPDService(int port) throws IOException {
			super(port,null); // nano won't like the null webroot, if it ever uses it, preventing inadvertently allowing access to local files via nano 
		}

		@Override
		public Response serve( String uri, String method, Properties header, Properties parms, Properties files ) {
			if ( uri == null ) return new Response( HTTP_NOTFOUND, MIME_HTML, HTTP_NOTFOUND);
			try {
				Log.i(TAG,  "method = '" + method + "'  uri=" + uri);

				String[] uriVec = uri.split("/");
				if ( uriVec.length < 1 ) return new Response( HTTP_NOTFOUND, MIME_HTML, HTTP_NOTFOUND);

				try {
					HTTPProvider service = (HTTPProvider)OS.getService(uriVec[1]);
					if ( service == null ) return new Response( HTTP_NOTFOUND, MIME_HTML, HTTP_NOTFOUND);
					String response = service.httpServe(uriVec);
					return new Response( HTTP_OK, MIME_PLAINTEXT, response );
				} catch (Exception e) {
					Log.e(TAG, "server response exception");
					e.printStackTrace();
					return new Response( HTTP_NOTFOUND, MIME_PLAINTEXT, e.getMessage());
				}

			} catch (Exception e) {
				Log.e(TAG, "server: " + e.getMessage());
				e.printStackTrace();
				return new Response( HTTP_INTERNALERROR, MIME_HTML, HTTP_INTERNALERROR + "<p><pre>" + e.getMessage() + "</pre>");
			}
		}
	}

	public HTTPDService() throws IOException {
		
		// Http.port default is set to 46111 <- Randomly selected
		int port = Integer.parseInt(OS.config().getProperty("httpd.port", "46111"));
		this.mNanoService = new NanoHTTPDService(port);
		port = mNanoService.localPort();

		// create a name entry for me, but dont' fail to start just because we can't register
		try {
			DDNSResolverService resolver = (DDNSResolverService)OS.getService("ddnsresolver");
			if ( resolver == null ) Log.w(TAG, "No local resolver.  Can't register name www");
			else resolver.register(new DDNSFullName(OS.hostname() + ".www"), port );  
		} catch (Exception e) {
			Log.w(TAG , "Couldn't register name: " + e.getMessage());
		}
		Log.i(TAG, "Service started on port " + mNanoService.localPort());
	}
}
