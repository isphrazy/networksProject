package cse461.snet.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Sends/receives a message over an established TCP connection.
 * To be a message means the unit of write/read is demarcated in some way.
 * In this implementation, that's done by prefixing the data with a 4-byte
 * length field.
 * <p>
 * Note used in Project 1.
 *  
 * @author zahorjan
 *
 */
public class TCPMessageHandler {
	private static final String TAG="TCPMessageHandler";
	
	protected Socket mSock;
	protected InputStream mIS;
	protected OutputStream mOS;
	
	//--------------------------------------------------------------------------------------
	// helper routines
	//--------------------------------------------------------------------------------------

	/**
	 * We need an "on the wire" format for a binary integer.
	 * This method encodes into that format, which is little endian
	 * (low order bits of int are in element [0] of byte array, etc.).
	 * @param i
	 * @return A byte[4] encoding the integer argument.
	 */
	protected static byte[] intToByte(int i) {
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN);
		b.putInt(i);
		byte buf[] = b.array();
		return buf;
	}
	
	/**
	 * We need an "on the wire" format for a binary integer.
	 * This method decodes from that format, which is little endian
	 * (low order bits of int are in element [0] of byte array, etc.).
	 * @param buf
	 * @return 
	 */
	protected static int byteToInt(byte buf[]) {
		ByteBuffer b = ByteBuffer.wrap(buf);
		b.order(ByteOrder.LITTLE_ENDIAN);
		int result = b.getInt();
		return result;
	}

	/**
	 * Constructor, associating this TCPMessageHandler with a connected socket.
	 * @param sock
	 * @throws IOException
	 */
	public TCPMessageHandler(Socket sock) throws IOException {
		if ( sock == null) throw new RuntimeException("TCPMessageHandler constructor: socket argument is null");
		if ( !sock.isConnected() ) throw new RuntimeException("TCPMessageHandler constructor: socket argument isn't in connected state");
		mSock = sock;
		mIS = sock.getInputStream();
		mOS = sock.getOutputStream();
	}
	
	/**
	 * Closes the underlying socket and associated streams.  The TCPMessageHandler object is 
	 * unusable after execution of this method.
	 */
	public void discard() {
		try {
			if ( mIS != null ) mIS.close();
			mIS = null;
			if ( mOS != null ) mOS.close();
			mOS = null;
			if ( mSock != null ) mSock.close();
			mSock = null;
		} catch (Exception e) {}
	}
	
	//--------------------------------------------------------------------------------------
	// send routines
	//--------------------------------------------------------------------------------------
	
	public void sendMessage(byte[] buf) throws IOException {
		mOS.write( intToByte(buf.length) );
		mOS.write( buf );
		mOS.flush();
	}
	
	public void sendMessage(String str) throws IOException {
		sendMessage(str.getBytes());
	}
	
	public void sendMesssage(JSONArray jsArray) throws IOException {
		sendMessage(jsArray.toString());
	}
	
	public void sendMessage(JSONObject jsObject) throws IOException {
		sendMessage(jsObject.toString());
	}
	
	//--------------------------------------------------------------------------------------
	// read routines
	//--------------------------------------------------------------------------------------
	
	public byte[] readMessageAsBytes() throws IOException {
		byte[] lengthBytes = readFromStream(4);  // 
		int length = byteToInt(lengthBytes);
		byte[] buf = readFromStream(length);
		return buf;
	}
	
	public String readMessageAsString() throws IOException {
		byte[] buf = readMessageAsBytes();
		return new String(buf);
	}
	
	public JSONArray readMessageAsJSONArray() throws IOException, JSONException {
		String jsonStr = readMessageAsString();
		return new JSONArray(jsonStr);
	}
	
	public JSONObject readMessageAsJSONObject() throws IOException, JSONException {
		return new JSONObject( readMessageAsString() );
	}
	
	/**
	 * Helper routine that reads a particular number of bytes from connection.
	 * @param nBytes Number of bytes to read.  Must be greater than zero.
	 * @return Throws EOFException if EOF reached; throws an IOException if less than nBytes read or nBytes is zero. Otherwise returns
	 * a <code>byte[nBytes]</code> array of received data.
	 * @throws IOException
	 */
	protected byte[] readFromStream(int nBytes) throws IOException {
		// debug ----------
		if ( nBytes == 0 ) {
			Log.e(TAG, "readFromStream: length 0 read requested"); // place to set breakpoint
			throw new IOException("TCPMessageHandler.readFromStream: read of zero bytes requested");
		}
		// debug ----------
		byte[] buf = new byte[nBytes];
		int totalRead = 0;
		try {
			while ( totalRead < nBytes) {
				int nRead = mIS.read(buf, totalRead, nBytes-totalRead);
				if ( nRead < 0 ) {
					if ( totalRead == 0 ) throw new EOFException("EOF reached");
					throw new IOException("TCPMessageHandler.readFromStream: EOF reached after " + totalRead + " bytes, but " + nBytes + " requested");
				}
				// debug ----------
				if ( nRead == 0 ) {
					Log.e(TAG, "readFromStream: read returned 0"); // place to set breakpoint -- under what conditions does this happen?
					throw new IOException("Read zero bytes!");  // don't go into infinite loop!
				}
				// debug ----------
				totalRead += nRead;
			}
		} catch (java.net.SocketTimeoutException e) {
			// timed out.  May have a partial message, but that's worthless...
			throw new IOException("Timed out after reading " + totalRead + " bytes");
		}
		return buf;
	}
	
}
