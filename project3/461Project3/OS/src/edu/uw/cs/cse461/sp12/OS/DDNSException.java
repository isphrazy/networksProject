package edu.uw.cs.cse461.sp12.OS;

@SuppressWarnings("serial")
public class DDNSException extends Exception {
	public String resulttype = "ddnsexception";
	public int exceptionnum;
	public String message;
	
	public DDNSException() { super(); }
	public DDNSException(String message) { super(message); }
	public DDNSException(String message, Throwable cause) { super(message, cause); }
	public DDNSException(Throwable cause) { super(cause); }
}

class DDNSNoSuchNameException extends DDNSException {
	public int exceptionnum = 1;
	public String message = "No such name exists for name ";
	
	public DDNSNoSuchNameException() { super(); }
	public DDNSNoSuchNameException(String message) { super(message); }
	public DDNSNoSuchNameException(String message, Throwable cause) { super(message, cause); }
	public DDNSNoSuchNameException(Throwable cause) { super(cause); }
}
	
class DDNSNoAddressException extends DDNSException {
	public int exceptionnum = 2;
	public String message = "No such address exists for name ";
	
	public DDNSNoAddressException() { super(); }
	public DDNSNoAddressException(String message) { super(message); }
	public DDNSNoAddressException(String message, Throwable cause) { super(message, cause); }
	public DDNSNoAddressException(Throwable cause) { super(cause); }
}
	
class DDNSAuthorizationException extends DDNSException {
	public int exceptionnum = 3;
	public String message = "Bad password for ";

	public DDNSAuthorizationException() { super(); }
	public DDNSAuthorizationException(String message) { super(message); }
	public DDNSAuthorizationException(String message, Throwable cause) { super(message, cause); }
	public DDNSAuthorizationException(Throwable cause) { super(cause); }
}
	
class DDNSRuntimeException extends DDNSException {
	public int exceptionnum = 4;
	public String message = "Sorry, runtimeException. I don't know what's wrong";
	
	public DDNSRuntimeException() { super(); }
	public DDNSRuntimeException(String message) { super(message); }
	public DDNSRuntimeException(String message, Throwable cause) { super(message, cause); }
	public DDNSRuntimeException(Throwable cause) { super(cause); }
}
	
class DDNSTTLExpiredException extends DDNSException {
	public int exceptionnum = 5;
	public String message = "ttl expired before we can resolve for ";

	public DDNSTTLExpiredException() { super(); }
	public DDNSTTLExpiredException(String message) { super(message); }
	public DDNSTTLExpiredException(String message, Throwable cause) { super(message, cause); }
	public DDNSTTLExpiredException(Throwable cause) { super(cause); }
}
	
class DDNSZoneException extends DDNSException {
	public int exceptionnum = 6;
	public String message = "This zone does not contain ";

	public DDNSZoneException() { super(); }
	public DDNSZoneException(String message) { super(message); }
	public DDNSZoneException(String message, Throwable cause) { super(message, cause); }
	public DDNSZoneException(Throwable cause) { super(cause); }
}