package com.example.querysence.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidSQLException extends RuntimeException{
    public InvalidSQLException(String message) {
        super(message);
    }
      public InvalidSQLException(String message,Throwable cause) {
        super(message,cause);
    }
    
}
