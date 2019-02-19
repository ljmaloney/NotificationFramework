/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/util/Logger.java,v $
 * $Id: Logger.java,v 1.4 2003/03/26 21:54:02 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */


package com.xanboo.core.util;

import org.apache.log4j.*;

/**
 * Wraps all Log4J logginf calls for application components
 */
public class Logger {

    private Category cat;

    // The name of this wrapper class
    private static String FQCN = Logger.class.getName();


    /** 
     * Creates a Category for a given name
     * @param name name reference for this category to create
     */
    public Logger(String categoryName) {
        cat = Category.getInstance(categoryName);
    }
    

    /** 
     * Logs a debug message
     * @param msg the message to be logged
     */
    public void debug(String msg) {
        cat.log(FQCN, Priority.DEBUG, msg, null);
    }

    /**
     * Logs a debug message with an exception
     * @param msg the message to be logged
     * @param t a Throwable
     */
    public void debug(String msg, Throwable t) {
        cat.log(FQCN, Priority.DEBUG, msg, t );
        //t.printStackTrace();
    }

    /** 
     * Logs an error message
     * @param msg the message to be logged
     */
    public void error(String msg) {
        cat.log(FQCN, Priority.ERROR, msg, null);
    }

    /**
     * Logs an error message with an exception
     * @param msg the message to be logged
     * @param t a Throwable
     */
    public void error(String msg, Throwable t) {
        cat.log(FQCN, Priority.ERROR, msg, t);
        //if(isDebugEnabled()) {
        //    t.printStackTrace();
        //}
    }

    /** 
     * Logs a warning message
     * @param msg the message to be logged
     */
    public void warn(String msg) {
        cat.log(FQCN, Priority.WARN, msg, null);
    }

    /**
     * Logs a warning message with an exception
     * @param msg the message to be logged
     * @param t a Throwable
     */
    public void warn(String msg, Throwable t) {
        cat.log(FQCN, Priority.WARN, msg, t);
    }

    /** 
     * Logs an info message
     * @param msg the message to be logged
     */
    public void info(String msg) {
        cat.log(FQCN, Priority.INFO, msg, null);
    }

    /**
     * Logs an info message with an exception
     * @param msg the message to be logged
     * @param t a Throwable
     */
    public void info(String msg, Throwable t) {
        cat.log(FQCN, Priority.INFO, msg, t);
    }

    /** is debugging enabled (use this before debug() )
     * @return Wrapper
     */
    public boolean isDebugEnabled() {
        return cat.isDebugEnabled();
    }
}
