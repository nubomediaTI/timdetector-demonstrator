/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tilab.ca.jrpcwrap.exception;

/**
 * Class that implements this interface define the error codes of the application.
 * The error code has the following structure:
 * - code (Integer) : internal error code of the application. use the first 3 numbers as the correspondent
 *                    http error code and then a sequential number.
 *        			  Example: UNHANDLED ERROR -> 500 (internal server error) + 1 (first error described) => 5001
 * - status code: the correspondent http error
 * - message : default error message
 * 
 * Reserved internal error codes: 
 * - 5001 -> unhandled error: thrown when a unhandled exception is catched
 * - 4041 -> method not found: thrown when a non existent method is called
 * @author kurento
 *
 */
public interface ErrorCodes {
    
    public static final ErrorCodes UNHANDLED_ERROR = new ErrorCodes() {

        @Override
        public int getCode() {
            return 5001;
        }

        @Override
        public StatusCode getStatus() {
            return StatusCode.INTERNAL_SERVER_ERROR;
        }

        @Override
        public String getMessage() {
            return "unexpected server error";
        }
    };
    
    public static final ErrorCodes METHOD_NOT_FOUND_ERROR = new ErrorCodes() {

        @Override
        public int getCode() {
            return 4041;
        }

        @Override
        public StatusCode getStatus() {
            return StatusCode.NOT_FOUND;
        }

        @Override
        public String getMessage() {
            return "called a non existent method";
        }
    };
    
    int getCode();

    StatusCode getStatus();

    String getMessage();
}
