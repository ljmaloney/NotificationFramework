/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/util/DAOFactory.java,v $
 * $Id: DAOFactory.java,v 1.4 2002/05/14 22:08:35 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.util;


/**
 * A Factory Class to create DAO implementation class instances for all DAOs
*/
public class DAOFactory {

    /**
     * Instantiates a particular DAO implementation class (Oracle, Sybase, etc).
     *
     * @return an instance of the requested DAO class 
     */
    public static Object getDAO() throws XanbooException {

        try {
            return Class.forName(GlobalNames.DAO_CLASS).newInstance();
        }catch (Exception se) {
            throw new XanbooException(20016, "Failed to instantiate a DAO object via DAOFactory");
        }
    }
    
}
