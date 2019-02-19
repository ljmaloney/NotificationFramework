/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/util/BaseHandlerDAO.java,v $
 * $Id: BaseHandlerDAO.java,v 1.5 2011/06/23 13:49:13 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.util;

import java.sql.*;
import javax.sql.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;


import com.xanboo.core.util.*;

/**
 * This abstract class implements the methods to obtain/close database connections for all Handler DAOs.
*/
public abstract class BaseHandlerDAO {

    protected DataSource ds=null;      // reference to a connection factory object for CoreDS

    private static  Logger logger=LoggerFactory.getLogger(BaseHandlerDAO.class.getName());
    /**
     * Locates and returns a Data Source connection factory instance associated with a JNDI name
     */
    protected void getDataSource(String dsJndiName) throws XanbooException {
    	
    	long startMS = System.currentTimeMillis();
    	
        if(ds==null) {
            try {
                InitialContext ic = new InitialContext();
                ds = (DataSource) ic.lookup(dsJndiName);
            }catch(NamingException ne) {
                ne.printStackTrace();
                throw new XanbooException(20010, ne.getLocalizedMessage());
            }
        }
        
        long endMS = System.currentTimeMillis();
        
        long trace = endMS-startMS;
        
        if(trace >GlobalNames.TRACE_TIME || logger.isDebugEnabled()   ){
        	
        	logger.info("ALERT : getDataSource : "+ dsJndiName +" : " + trace);
        }
    }
    
    /**
     * Returns a database connection instance from a pool defined by a JNDI DS name
     */
     public Connection getConnection(String dsJndiName) throws XanbooException {
        try {
        	 long startMS = System.currentTimeMillis();
        	
            if(ds==null) getDataSource(dsJndiName);
            Connection conn = ds.getConnection();
            if(conn.getAutoCommit()) {
                conn.setAutoCommit(false);      // SET DEFAULT AUTOCOMMIT TO FALSE !!!!!
            }
            long endMS = System.currentTimeMillis();
            
            long trace = endMS-startMS;
            
            if(trace >GlobalNames.TRACE_TIME || logger.isDebugEnabled()){
            	
            	logger.info("ALERT : getConnection : "+ dsJndiName +" : " + trace);
            }
            return conn;
        }catch(XanbooException xe) {
            xe.printStackTrace();
            throw xe;
        }catch(SQLException e) {
            e.printStackTrace();
            throw new XanbooException(20020, e.getLocalizedMessage());
        }
    }

    
    /**
     * Returns a database connection instance from the default COREDS pool 
     */
     public Connection getConnection() throws XanbooException {
        try {
            return getConnection(GlobalNames.COREDS);
        }catch(XanbooException xe) {
            throw xe;
        }
    }
    
    
    /* Returns the database connection instance to the pool w/ commit/rollback option */
    public void closeConnection(Connection dbConnection, boolean rollback) throws XanbooException {
    	
    	long startMS = System.currentTimeMillis();
        if(dbConnection==null) return;
        
        try {
            if(rollback) {
                dbConnection.rollback();
            }else {
                dbConnection.commit();
            }
        }catch(SQLException se) {
            //throw new XanbooException(20030, se.getLocalizedMessage());
        }finally {
            try {
                dbConnection.close();
                dbConnection=null;
            }catch(SQLException se) {
                throw new XanbooException(20030, se.getLocalizedMessage());
            }
        }
        
        long endMS = System.currentTimeMillis();
        
        long trace = endMS-startMS;
        
        if(trace >GlobalNames.TRACE_TIME || logger.isDebugEnabled()){
        	
        	logger.info("ALERT : closeConnection : "+ trace);
        }
    }

    
    /* Returns the database connection instance to the pool  */
    public void closeConnection(Connection dbConnection) throws XanbooException {
        closeConnection(dbConnection,false);
    }
    
}
