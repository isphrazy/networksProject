package edu.uw.cs.cse461.sp12.OS;

@SuppressWarnings("serial")
public class DDNSException extends Exception {
	public DDNSException() { super(); }
	public DDNSException(String message) { super(message); }
	public DDNSException(String message, Throwable cause) { super(message, cause); }
	public DDNSException(Throwable cause) { super(cause); }

	public static class DDNSNoSuchNameException extends DDNSException {
		public DDNSNoSuchNameException() { super(); }
		public DDNSNoSuchNameException(String message) { super(message); }
		public DDNSNoSuchNameException(String message, Throwable cause) { super(message, cause); }
		public DDNSNoSuchNameException(Throwable cause) { super(cause); }
	}
	
	public static class DDNSNoAddressException extends DDNSException {
		public DDNSNoAddressException() { super(); }
		public DDNSNoAddressException(String message) { super(message); }
		public DDNSNoAddressException(String message, Throwable cause) { super(message, cause); }
		public DDNSNoAddressException(Throwable cause) { super(cause); }
	}
	
	public static class DDNSAuthorizationExceptions extends DDNSException {
		public DDNSAuthorizationExceptions() { super(); }
		public DDNSAuthorizationExceptions(String message) { super(message); }
		public DDNSAuthorizationExceptions(String message, Throwable cause) { super(message, cause); }
		public DDNSAuthorizationExceptions(Throwable cause) { super(cause); }
	}
	
	public static class DDNSRuntimeException extends DDNSException {
		public DDNSRuntimeException() { super(); }
		public DDNSRuntimeException(String message) { super(message); }
		public DDNSRuntimeException(String message, Throwable cause) { super(message, cause); }
		public DDNSRuntimeException(Throwable cause) { super(cause); }
	}
	
	public static class DDNSTTLExpiredException extends DDNSException {
		public DDNSTTLExpiredException() { super(); }
		public DDNSTTLExpiredException(String message) { super(message); }
		public DDNSTTLExpiredException(String message, Throwable cause) { super(message, cause); }
		public DDNSTTLExpiredException(Throwable cause) { super(cause); }
	}
	
	public static class DDNSZoneException extends DDNSException {
		public DDNSZoneException() { super(); }
		public DDNSZoneException(String message) { super(message); }
		public DDNSZoneException(String message, Throwable cause) { super(message, cause); }
		public DDNSZoneException(Throwable cause) { super(cause); }
	}
}