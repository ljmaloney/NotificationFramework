/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/util/LoggerFactory.java,v $
 * $Id: LoggerFactory.java,v 1.1.1.1 2002/05/06 19:17:02 rking Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.util;

import java.util.HashMap;

/**
 * A Factory Class to create Logger instances for all EJBs
 */
public class LoggerFactory {

    private static HashMap loggers=new HashMap();
    
    /**
     * Instantiates a particular Logger class for a given name.
     *
     * @param className the name that will be associated with the logger
     * @return an instance of the requested Logger
     */
    public static Logger getLogger(String name) {
        
        Logger logger=null;
        try {
            logger=(Logger) loggers.get(name);
        }catch(NullPointerException ne) {
            logger=null;
        }
        
        if(logger==null) {
            logger=new Logger(name);
            loggers.put(name, logger);
        }
            
        return logger;  
    }
    
    
    

}
