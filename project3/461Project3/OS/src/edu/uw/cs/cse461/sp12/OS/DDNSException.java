package edu.uw.cs.cse461.sp12.OS;

/**
 * DDNSException and its subclasses are exceptions being used to disclose
 * useful information to the RPC clients.
 * 
 * @author Cheng Hao Chuang
 * @author Pingyang He
 * @project CSE461 12sp project 4 
 *
 */
@SuppressWarnings("serial")
public class DDNSException extends Exception {
	public String resulttype = "ddnsexception";
	public int exceptionnum;
	public String message;
	
	public DDNSException() { super(); }
	public DDNSException(String message) { super(message); }
	public DDNSException(String message, Throwable cause) { super(message, cause); }
	public DDNSException(Throwable cause) { super(cause); }
	
	public int getExceptionnum() {
		return exceptionnum;
	}
	
	public String getMessage() {
		return message;
	}

    /**
     * Thrown when the name doesn't exist in the namespace
     */
    public static class DDNSNoSuchNameException extends DDNSException {
    	public int exceptionnum = 1;
    	public String message = "No such name exists for name ";
    	
    	public DDNSNoSuchNameException() { super(); }
    	public DDNSNoSuchNameException(String message) { super(message); }
    	public DDNSNoSuchNameException(String message, Throwable cause) { super(message, cause); }
    	public DDNSNoSuchNameException(Throwable cause) { super(cause); }
    	
    	public int getExceptionnum() {
    		return exceptionnum;
    	}
    	
    	public String getMessage() {
    		return message;
    	}
    }
    	
    /**
     * The name resolved to a node, but there is no address currently associated
     * with that name. 
     */
    public static class DDNSNoAddressException extends DDNSException {
    	public int exceptionnum = 2;
    	public String message = "No such address exists for name ";
    	
    	public DDNSNoAddressException() { super(); }
    	public DDNSNoAddressException(String message) { super(message); }
    	public DDNSNoAddressException(String message, Throwable cause) { super(message, cause); }
    	public DDNSNoAddressException(Throwable cause) { super(cause); }
    	
    	public int getExceptionnum() {
    		return exceptionnum;
    	}
    	
    	public String getMessage() {
    		return message;
    	}
    }
    
    /**
     * Thrown when an operation requiring a password is requested, but the correct
     * password has not been supplied
     */
    public static class DDNSAuthorizationException extends DDNSException {
    	public int exceptionnum = 3;
    	public String message = "Bad password for ";
    
    	public DDNSAuthorizationException() { super(); }
    	public DDNSAuthorizationException(String message) { super(message); }
    	public DDNSAuthorizationException(String message, Throwable cause) { super(message, cause); }
    	public DDNSAuthorizationException(Throwable cause) { super(cause); }
    	
    	public int getExceptionnum() {
    		return exceptionnum;
    	}
    	
    	public String getMessage() {
    		return message;
    	}
    }
    
    /**
     * This is a catch-all exception class, used to report anything that goes wrong
     * not falling into one of the five other exception types. The message is 
     * intended to be useful to the caller in determining what went wrong
     */
    public static class DDNSRuntimeException extends DDNSException {
    	public int exceptionnum = 4;
    	public String message = "Sorry, runtimeException. I don't know what's wrong";
    	
    	public DDNSRuntimeException() { super(); }
    	public DDNSRuntimeException(String message) { super(message); }
    	public DDNSRuntimeException(String message, Throwable cause) { super(message, cause); }
    	public DDNSRuntimeException(Throwable cause) { super(cause); }
    	
    	public int getExceptionnum() {
    		return exceptionnum;
    	}
    	
    	public String getMessage() {
    		return message;
    	}
    }
    
    /**
     * Some resolution step limit used to deal with possible naming loops went to
     * zero before the name was resolved. The name may or may not exist, but in
     * any case couldn't be resolved
     */
    public static class DDNSTTLExpiredException extends DDNSException {
    	public int exceptionnum = 5;
    	public String message = "ttl expired before we can resolve for ";
    
    	public DDNSTTLExpiredException() { super(); }
    	public DDNSTTLExpiredException(String message) { super(message); }
    	public DDNSTTLExpiredException(String message, Throwable cause) { super(message, cause); }
    	public DDNSTTLExpiredException(Throwable cause) { super(cause); }
    	
    	public int getExceptionnum() {
    		return exceptionnum;
    	}
    	
    	public String getMessage() {
    		return message;
    	}
    }
    
    /**
     * The name server was asked to resolve a name not in its zone (the subtree
     * of the namespace rooted at the server's SOA name)
     */
    public static class DDNSZoneException extends DDNSException {
    	public int exceptionnum = 6;
    	public String message = "This zone does not contain ";
    
    	public DDNSZoneException() { super(); }
    	public DDNSZoneException(String message) { super(message); }
    	public DDNSZoneException(String message, Throwable cause) { super(message, cause); }
    	public DDNSZoneException(Throwable cause) { super(cause); }
    	
    	public int getExceptionnum() {
    		return exceptionnum;
    	}
    	
    	public String getMessage() {
    		return message;
    	}
    }
}