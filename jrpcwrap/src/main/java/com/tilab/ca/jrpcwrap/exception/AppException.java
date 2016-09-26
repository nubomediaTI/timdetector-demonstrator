package com.tilab.ca.jrpcwrap.exception;


public class AppException extends RuntimeException{
    
    private static final long serialVersionUID = 1L;
	private final ErrorCodes errorCode;

    public AppException(ErrorCodes errorCode,String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AppException(ErrorCodes errorCode,String message,Throwable t) {
        super(message,t);
        this.errorCode = errorCode;
    }
    
    public AppException(ErrorCodes errorCode,Throwable t) {
        super(t);
        this.errorCode = errorCode;
    }
    
    
    public ErrorCodes getErrorCode() {
        return errorCode;
    }
    
}
