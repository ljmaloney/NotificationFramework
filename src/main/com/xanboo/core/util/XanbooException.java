/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/util/XanbooException.java,v $
 * $Id: XanbooException.java,v 1.5 2005/09/19 16:07:40 rking Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.util;

import javax.ejb.*;

/**
 * XanbooException is an exception that extends the standard
 * RunTimeException Exception. This is thrown by all web tier
 * Core classes when an exception/error occurs
 */

public class XanbooException extends Exception {

    private int errorCode;
    private String errorMessage;
    private Throwable rootException = null;    
    
    /**
     * Constructor
     * @param errorCode   an integer code to identify the exception
     * @param errorMessage    a string that explains the exception occured
     */
    public XanbooException(int errorCode, String errorMessage) {
        super(errorCode + ":" + errorMessage);        
        this.errorCode=errorCode;
        this.errorMessage=errorMessage;
    }

    /**
     * Constructor
     * @param errCode   an integer code value to identify the exception
     */
    public XanbooException(int errorCode) {
        super(errorCode + ":");        
        this.errorCode=errorCode;
        this.errorMessage="";
    }

    
    /**
     * Constructor
     * @param errCode an integer code value to identify the exception
     * @param cause a throwable root cause exception
     */
    public XanbooException(int errorCode, Throwable rootException) {
        super(errorCode + ":");        
        this.errorCode=errorCode;
        this.errorMessage="";
        this.rootException = rootException;
    }
    
    
    
    /**
     * Returns the code associated with the exception
     */
    public int getCode() {
        return this.errorCode;
    }

    /**
     * Returns the message associated with the exception
     */
    public String getMessage() {
        return ("[" + this.errorCode + "] " + this.errorMessage);
    }
    
   /**
     * Returns the A string that contains both of the error code and error message associated with the exception
     */
    public String getErrorMessage() {
        return this.errorMessage;
    }
    
   /**
     * Prints this Throwable and its backtrace to the standard error stream.    
    */
    public void printStackTrace() {
        //System.err.println("XanbooException:" + this.getMessage());
        super.printStackTrace();
        if(rootException != null) {
          System.err.println("Root Exception:");
          rootException.printStackTrace();
        }
    }

    
   /**
     * Prints this Throwable and its backtrace to a print stream.    
    */
    public void printStackTrace(java.io.PrintStream ps) {
        //ps.println("XanbooException:" + this.getMessage());
        super.printStackTrace(ps);
        if(rootException != null) {
          ps.println("Root Exception:");
          rootException.printStackTrace();
        }
    }

    
   /**
     * Prints this Throwable and its backtrace to a print writer.    
    */
    public void printStackTrace(java.io.PrintWriter pw) {
        //pw.println("XanbooException:" + this.getMessage());
        super.printStackTrace(pw);
        if(rootException != null) {
          pw.println("Root Exception:");
          rootException.printStackTrace();
        }
    }
    
    
}

