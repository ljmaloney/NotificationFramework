/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/gateway/rules/XanbooRuleDeviceStateGuard.java,v $
 * $Id: XanbooRuleDeviceStateGuard.java,v 1.7 2005/01/06 17:23:03 rking Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.gateway.rules;

import com.xanboo.core.util.XanbooException;

/**
 * XanbooRuleStateGuard class defines a device state condition for a rule's execution.<br>
 * E.g.  Check for 'is camera_1 armed'.
 * <br><br>
 * A XanbooRuleStateGuard object includes the following properties:
 * <ul>
 *  <li><b>Device GUID</b><br>
 *      Identifies the device whose state to be checked.
 *  </li>
 *  <li><b>Managed Object Id</b><br>
 *      Identifies the managed object to be checked.
 *  </li>
 *  <li><b>Operator</b><br>
 *      A comparison operator of one of the constants defined in this class. The operator is used to
 *      compare the current managed object value with the constant value provided in the 'value' attribute.
 *  </li>
 *  <li><b>Value</b><br>
 *      A constant value the managed object can take, which will be compared to the current managed object
 *      value using the specified operator.
 *  </li>
 * </ul>
 *
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleGuard XanbooRuleGuard
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleStateGuard XanbooRuleStateGuard
 */
public class XanbooRuleDeviceStateGuard extends XanbooRuleStateGuard {
    
    private static final String[] OPERATORS = { ".EQ.", ".GT.", ".LT.", ".GE.", ".LE.", ".NE." };
    
    /** Operator for 'equality' comparisons */
    public static final int OPERATOR_EQ = 0;
    /** Operator for 'greater than' comparisons */
    public static final int OPERATOR_GT = 1;
    /** Operator for 'less than' comparisons */
    public static final int OPERATOR_LT = 2;
    /** Operator for 'greater or equal' comparisons */
    public static final int OPERATOR_GE = 3;
    /** Operator for 'less or equal' comparisons */
    public static final int OPERATOR_LE = 4;
    /** Operator for 'not equal' comparisons */
    public static final int OPERATOR_NE = 5;
    
    /**
     * Holds value of property deviceGUID.
     */
    private String deviceGUID;
    
    /**
     * Holds value of property mobjectId.
     */
    private String mobjectId;
    
    /**
     * Holds value of property operator.
     */
    private int op;
    
    /**
     * Holds value of property value.
     */
    private String value;
    
    /** Creates a new instance of XanbooRuleStateGuard with given parameters
     *
     * @param deviceGUID The Id of the device, whose state will be be checked
     * @param mobjectID Identifies the managed object, whose current value we want to compare
     * @param operator operator for value comparison. Must be of one of the constants defined in this class.
     * @param value The value to compare using the specified operator
     */
    public XanbooRuleDeviceStateGuard(String deviceGUID, String mobjectId, int operator, String value) {
        super();
        this.deviceGUID = deviceGUID;
        this.mobjectId = mobjectId;
        this.op = operator;
        this.value = value;
    }
    
    
    /** Creates a new instance of XanbooRuleStateGuard.
     * <br>This method is used internally and should be avoided by SDK developers !!!
     */
    protected XanbooRuleDeviceStateGuard(String guard) {
        super();
        int p1 = guard.indexOf( "." );
        int p2 = guard.indexOf( ".", p1+1 );
        int p3 = guard.indexOf( ".", p2+1 );
        this.deviceGUID = guard.substring( 0, p1 );
        this.mobjectId = guard.substring( p1+2, p2 );
        op = getOperatorIndex( guard.substring( p2, p3+1 ) );
        this.value = guard.substring( p3+2, guard.length()-1 );
    }
    
    private static final int getOperatorIndex( String op ) {
        int indx = -1;
        for ( int i=0; i<OPERATORS.length; i++ ) {
            if ( OPERATORS[i].equals( op ) ) {
                indx = i;
            }
        }
        return indx;
    }
    
    /**
     * Method to return internal string representation of this guard object.
     * <br>This method is used internally and should be avoided by SDK developers !!!
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        
        boolean subGuards = ( this.guardArray != null && this.guardArray.size() > 0 );

        sb.append( super.toString() );
        
        if ( subGuards ) {
            sb.append( "(" );
        }
        
        sb.append( this.deviceGUID );
        sb.append( ".o" );
        sb.append( this.mobjectId );
        sb.append( op == -1 ? ".EQ." : OPERATORS[ op ] );
        sb.append( "'" );
        sb.append( this.value );
        sb.append( "'" );
        
        if ( subGuards ) {
            for ( int i=0; i<this.guardArray.size(); i++ ) {
                XanbooRuleGuard g = (XanbooRuleGuard) guardArray.get( i );
                sb.append( g.toString() );
            }
            
            sb.append( ")" );
        }
        
        return sb.toString();
    }

    /**
     * Validates this guard for correct format and completeness
     * 
     * @throws XanbooException if an error was found
     */    
    public void validate() throws XanbooException {
        if (    this.deviceGUID == null ||
        this.mobjectId == null ||
        this.value == null ||
        this.op >= OPERATORS.length ||
        this.op < 0 ) {
            throw new XanbooException( 29055 );
        }
    }

    
    /**
     * Returns target device Id for the guard.
     * @return deviceGUID target device Id for the guard.
     */
    public String getDeviceGUID() {
        return this.deviceGUID;
    }
    
    /**
     * Returns target managed object Id for the guard.
     * @return mobjectId target managed object Id for the guard.
     */
    public String getMobjectId() {
        return this.mobjectId;
    }
    
    /**
     * Returns the arithmetic operator for the guard comparison.
     * @return operator configured comparison operator
     */
    public int getArithOp() {
        return this.op;
    }
    
    /**
     * Returns the comparison value for the guard.
     * @return value comparison value
     */
    public String getValue() {
        return this.value;
    }
    
    /**
     * Sets target device Id for the guard.
     * @param deviceGUID The Id of the device, whose state will be be checked
     */
    public void setDeviceGUID( String deviceGUID ) {
        this.deviceGUID = deviceGUID;
    }
    
    /**
     * Sets target mobject Id for the guard.
     * @param mobjectID Identifies the managed object, whose current value we want to compare
     */
    public void setMobjectId( String mobjectId ) {
        this.mobjectId = mobjectId;
    }
    
    /**
     * Sets arithmetic operator for guard comparison.
     * @param operator operator for value comparison. Must be of one of the constants defined in this class.
     */
    public void setArithOp( int operator ) {
        this.op = operator;
    }
    
    /**
     * Sets comparison value.
     * @param value The value to compare using the specified operator
     */
    public void setValue( String value ) {
        this.value = value;
    } 
    
}
