package com.tilab.ca.call_on_detect.exception;

import com.tilab.ca.jrpcwrap.exception.ErrorCodes;
import com.tilab.ca.jrpcwrap.exception.StatusCode;


public enum InternalErrorCodes implements ErrorCodes{

	USER_NOT_AUTHENTICATED(4011,"no active session for user",StatusCode.UNAUTHORIZED);
    
    private final int code;
    private final StatusCode status;
    private final String message;
    
    private InternalErrorCodes(int code,String message,StatusCode status){
        this.code = code;
        this.message = message;
        this.status = status;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public StatusCode getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
    
}
