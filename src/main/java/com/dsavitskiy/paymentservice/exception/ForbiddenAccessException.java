package com.dsavitskiy.paymentservice.exception;

public class ForbiddenAccessException extends RuntimeException{
    public ForbiddenAccessException(String message){
        super(message);
    }
}
