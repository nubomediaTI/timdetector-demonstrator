
package com.tilab.ca.jrpcwrap.exception;


public enum StatusCode {
    
    OK(200),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    NOT_FOUND(404),
    INTERNAL_SERVER_ERROR(500);
    
    
    private final int statusCode;
    
    private StatusCode(int statusCode){
        this.statusCode = statusCode;
    }
    
    public int intValue(){
        return statusCode;
    }
    
    @Override
    public String toString(){
        return String.valueOf(statusCode);
    }
}
