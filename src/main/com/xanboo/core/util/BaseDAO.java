/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/util/BaseDAO.java,v $
 * $Id: BaseDAO.java,v 1.2 2011/06/23 13:49:13 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.util;

import java.sql.*;

import com.xanboo.core.util.*;
import com.xanboo.core.sdk.util.XanbooResultSet;

/**
 * This abstract class defines the required methods for all database specific DAO implementations.
*/
public abstract class BaseDAO {

    protected static final int FETCH_SIZE_MAX       = 100;
    
    /** 
     * Closes sql resultset 
     *
     * @param rs resultset instance to close
     */     
    protected void closeResultSet(ResultSet rs) throws XanbooException {
        try {
            if (rs != null) {
                rs.close();
            }
        }catch(SQLException se) {
            throw new XanbooException(20040, se.getLocalizedMessage());
        }
    }

    
    /**
     * Closes sql statement 
     * @param sql statement instance to close
     */     
    protected void closeStatement(Statement stmt) throws XanbooException {
        try {
            if (stmt != null) {
                stmt.close();
            }
        }catch(SQLException se) {
            throw new XanbooException(20050, se.getLocalizedMessage());
        }
    }
    

    /* abstract methods that must be implemented by DB specific DAO class */

    /* calls for insert/update stored procedures */
    public abstract void callSP(Connection conn, String spName, SQLParam[] args, boolean commitFlag) throws XanbooException;


    /* calls for query stored procedures */
    public XanbooResultSet callSP(Connection conn, String spName, SQLParam[] args) throws XanbooException {
        return callSP(conn, spName, args, 0, -1);
    }    

    public abstract XanbooResultSet callSP(Connection conn, String spName, SQLParam[] args, int startRow, int numRows) throws XanbooException;
    
    

}
