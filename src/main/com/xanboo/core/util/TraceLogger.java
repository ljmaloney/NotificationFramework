/*
 * Copyright 2014-2015 ATT Digital Life
 */

package com.xanboo.core.util;

/* 
 * Utility class that provides trace logging methods
 *
 */
public class TraceLogger {

    private static Logger logger = LoggerFactory.getLogger(TraceLogger.class.getName());
    

    private TraceLogger() {}

    /* Trace logs the given message */
    public static void log(String message) {
        logger.debug(message);
    }

    public static void log(String txId, String accountId, String gguid, String dguid, String className, String command, String oid, String val) {
        logger.debug("acc=" + accountId + "  gguid=" + gguid + "  dguid=" + dguid + "  class=" + className + "  cmd=" + command + "  oid=" + oid + "  txid=" + txId );
    }
    
}
