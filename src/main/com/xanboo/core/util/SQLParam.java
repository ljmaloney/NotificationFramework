/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/util/SQLParam.java,v $
 * $Id: SQLParam.java,v 1.4 2003/04/30 22:10:47 rking Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */


package com.xanboo.core.util;

import java.sql.Types;

/**
 * Class to represent parameter types for stored procedure calls. It provides
 * a generic mechanism to pass DAO class method parameters to stored procedures.
 */
public class SQLParam implements java.io.Serializable {
    private static final long serialVersionUID = -3054970854934670992L;

    public static int IN_OUT = 2;
    
    private Object param;
    private int  paramType;
    private boolean isOutputParameter;
    private int retType; //0 - IN , 1= OUT, 2 = INOUT

    /** 
     * Constructor
     * Creates a default INPUT NULL SQL parameter
     */
    public SQLParam() {
        this.isOutputParameter=false;
        this.retType=0; // input parameter.
        this.param=null;
        this.paramType=Types.NULL;
    }

    /**
     * Constructor
     * Creates a SQL parameter with specified type. isOutput specifies if it
     * is an INPUT or OUTPUT parameter.
     * @param param A SQL parameter
     * @param paramType An integer that represent the param object type
     * @param isOutput A boolean flag, true if the parameter is an output parameter, or false if it is an input parameter
     */
    public SQLParam(Object param, int paramType, boolean isOutput) {
        this.param=param;
        this.paramType=paramType;
        this.isOutputParameter=isOutput;
    }
    
    
    /**
     * Constructor
     * Creates a SQL parameter with specified type. retType specifies if it
     * is an INPUT, OUTPUT or INPUTOUTPUT parameter.
     * @param param A SQL parameter
     * @param paramType An integer that represent the param object type
     * @param retType  int value designate as 0 for IN,  1 for OUT, 2 for INOUT
     */
    public SQLParam(Object param, int paramType, int retType) {
        this.param=param;
        this.paramType=paramType;
        this.retType=retType;
    }

    /**
     * Constructor
     * Creates an INPUT SQL parameter with specified type.
     * @param param A SQL parameter
     * @param paramType An integer that represent the param object type
     */
    public SQLParam(Object param, int paramType) {
        this.param=param;
        this.paramType=paramType;
        this.isOutputParameter=false;
    }

    /**
     * Constructor
     * Creates an INPUT VARCHAR SQL parameter.
     * @param param A SQL parameter that is used to create the new INPUT VARCHAR SQL parameter
     */
    public SQLParam(Object param) {
        if ( param == null ) {
            this.param=null;
            this.paramType=Types.NULL;            
        } else {
            this.param=param.toString();
            this.paramType=Types.VARCHAR;
        }
        this.isOutputParameter=false;
    }
    
    // getters
    /**
     * Gets the parameter type of this SQLParam object
     */
    public int getParamType () { return paramType; }
    /**
     * Gets the SQL parameter object
     */
    public Object getParam () { return param; }
    /**
     * Gets the flag that indicates if this SQLParam is an INPUT or an OUTPUT parameter.
     * Returns true if it is an OUTPUT parameter 
     */
    public boolean isOutput () { return isOutputParameter ? true : false; }

    // setters
    /**
     * Sets the parameter type of this SQLParam object
     */
    public void setParamType (int pType) { this.paramType= pType;  }
    /**
     * Sets the SQL parameter object
     */
    public void setParam (Object obj) { this.param=obj;  }
    /**
     * Sets the flag that indicates if this SQLParam is an INPUT or an OUTPUT parameter.
     * Set it to true if it is an OUTPUT parameter or false for an INPUT parameter
     */
    public void setOutput (boolean isOutput) { this.isOutputParameter = isOutput; }

	/**
	 * @return the retType
	 */
	public int getRetType() { return retType; }

	/**
	 * @param retType the retType to set
	 */
	public void setRetType(int retType) { 	this.retType = retType; }
    
}