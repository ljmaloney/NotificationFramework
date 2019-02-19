/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/gateway/rules/XanbooRuleGuard.java,v $
 * $Id: XanbooRuleGuard.java,v 1.27 2005/01/27 16:29:41 rking Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.gateway.rules;

import com.xanboo.core.util.XanbooException;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Abstract base class to represent event and state condition guard definitions for a {@link com.xanboo.core.sdk.gateway.rules.XanbooRule XanbooRule} 
 * to be automatically executed by the Gateway Rules Engine.
 * <br>
 * <br>
 * There are two basic types of guards - one being an event (or a 'trigger'), and the other being a state condition, which
 * are both represented by abstract sub-classes of XanbooRuleGuard class - {@link com.xanboo.core.sdk.gateway.rules.XanbooRuleEventGuard XanbooRuleEventGuard} and
 * {@link com.xanboo.core.sdk.gateway.rules.XanbooRuleStateGuard XanbooRuleStateGuard}. 
 * <br>
 * <br>
 * Current guard implementation classes are:
 * <ul>
 * <li> <b>{@link com.xanboo.core.sdk.gateway.rules.XanbooRuleDeviceEventGuard XanbooRuleDeviceEventGuard}</b> <br>
 * Defines a specific device event trigger guard(for example, 'when motion detected on camera_1').
 * </li>
 * <li> <b>{@link com.xanboo.core.sdk.gateway.rules.XanbooRuleTimeEventGuard XanbooRuleTimeEventGuard}</b> <br>
 * Defines a repetetive time event trigger guard (for example, 'at 9:00am every day').
 * </li>
 * <li> <b>{@link com.xanboo.core.sdk.gateway.rules.XanbooRuleDeviceStateGuard XanbooRuleDeviceStateGuard}</b> <br>
 * Defines a device state condition guard for a rule to be executed (for example 'is camera_1 armed').
 * </li>
 * <li> <b>{@link com.xanboo.core.sdk.gateway.rules.XanbooRuleDeviceStateGuard XanbooRuleDeviceStateGuard}</b> <br>
 * Defines a time range condition guard for a rule to be executed (for example, from 9am to 5pm every weekday).
 * </li>
 * </ul>
 *
 *  <br>
 * Note that not all managed objects defined by a device are allowed to be used in rule guards. The 'public' attribute of mobject definitions within the descriptors
 * determine if a mobject may be referenced from a guard or not. 
 * <br>
 * <br>
 * Refer to the SDK Programmer's Guide for details on all Rules and Guard related topics.<BR>
 *
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRule XanbooRule
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleEventGuard XanbooRuleEventGuard
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleStateGuard XanbooRuleStateGuard
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleDeviceEventGuard XanbooRuleDeviceEventGuard
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleDeviceStateGuard XanbooRuleDeviceStateGuard
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleTimeEventGuard XanbooRuleTimeEventGuard
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleTimeStateGuard XanbooRuleTimeStateGuard
 */
public abstract class XanbooRuleGuard implements java.io.Serializable, XanbooGuardContainer {
    
    public static final int OPERATOR_NONE=-1;
    public static final int OPERATOR_AND=0;
    public static final int OPERATOR_OR=1;
    
    protected boolean isTrigger;
    private int logicalOp;
    
    protected ArrayList guardArray;
    
    protected int id;
    //protected int nestLevel;
    
    /** Creates a new instance of XanbooRuleGuard object */
    public XanbooRuleGuard() {
        this.logicalOp = OPERATOR_AND;
    }
    
    /**
     * Method to return a XanbooGuard object from an internal mobject oid string representation.
     * <br>This method is used internally and should be avoided by SDK developers.
     **/
    protected static XanbooRuleGuard createGuard( String guardTerm ) throws XanbooException {
        try {
            
            String gtu = guardTerm.toUpperCase();
            if ( gtu.indexOf( ".TO." ) != -1 ) {
                // Time range - format TIME.something.TO.TIME.something
                return new XanbooRuleTimeStateGuard( guardTerm );
            } else if ( gtu.startsWith( "TIME.K" ) ) {
                if ( gtu.equals( "TIME.K.SUNRISE" ) || gtu.equals( "TIME.K.SUNSET" ) ) {
                    // Time event keywords for sunset and sunrise.
                    return new XanbooRuleTimeEventGuard( guardTerm );
                } else {
                    // Time range keywords
                    return new XanbooRuleTimeStateGuard( guardTerm );
                }
            } else if ( gtu.indexOf( "TIME." ) != -1 ) {
                // time event
                return new XanbooRuleTimeEventGuard( guardTerm );
            } else if ( (gtu.indexOf( ".!E" ) != -1 || gtu.indexOf( ".E" ) != -1 ) && gtu.indexOf( "'" ) == -1 ) {
                // device event
                return new XanbooRuleDeviceEventGuard( guardTerm );
            } else if ( gtu.indexOf( "'" ) != -1 ) {
                // device / mobject state
                return new XanbooRuleDeviceStateGuard( guardTerm );
            } else {
                // unknown guard ?
                throw new XanbooException( 29055, "Invalid guard specification term:" + guardTerm );
            }
        } catch ( Exception e ) {
            throw new XanbooException( 29055, "Invalid guard spec:" + guardTerm );
        }
    }

    
    /**
     * Method to return internal mobject oid string representation of a rule guard object.
     */
    //protected abstract String toGuardString();
    
    /**
     * Confirms the validity of this guard object.
     * <br>
     * @throws XanbooException if the guard is not found to be valid.
     */
    public abstract void validate() throws XanbooException;

    /** 
     * Returns internal string representation of this guard.
     */
    public String toString() {
        switch( this.logicalOp ) {
            case OPERATOR_AND:
                return ".AND.";
            case OPERATOR_OR:
                return ".OR.";
            case OPERATOR_NONE:
                default:
                return "";
        }
    }
    
    /**
     * Returns whether this guard is an event/trigger or a state condition guard. 
     *
     * @return boolean true, if the guard is an event guard, which may trigger rule execution.
     */
    public boolean isTrigger() {
        boolean is = false;
        if (isTrigger) {
            is = true;
        } else {
            if ( guardArray != null ) {
                for ( int i=0; i<guardArray.size(); i++ ) {
                    XanbooRuleGuard g = (XanbooRuleGuard) guardArray.get( i );
                    if ( (is = g.isTrigger()) == true ) break; // exit loop if we find a trigger in a sub guard.
                }
            }
        }
        return is;
    }

    /**
     * Returns whether this guard is an event/trigger or a state condition guard. 
     *
     * @return boolean true, if the guard is an event guard, which may trigger rule execution.
     */
    public boolean hasGuardState() {
        boolean is = false;
        if (!isTrigger) {
            is = true;
        } else {
            if ( guardArray != null ) {
                for ( int i=0; i<guardArray.size(); i++ ) {
                    XanbooRuleGuard g = (XanbooRuleGuard) guardArray.get( i );
                    if ( (is = g.hasGuardState()) == true ) break; // exit loop if we find a trigger in a sub guard.
                }
            }
        }
        return is;
    }

    
    /**
     * Returns whether this guard is or contains a time state guard.
     *
     * @return boolean true, if the guard is an event guard, which may trigger rule execution.
     */
    public boolean hasTimeState() {
        boolean is = false;
        if (this instanceof XanbooRuleTimeStateGuard) {
            is = true;
        } else {
            if ( guardArray != null ) {
                for ( int i=0; i<guardArray.size(); i++ ) {
                    XanbooRuleGuard g = (XanbooRuleGuard) guardArray.get( i );
                    if ( (is = (g instanceof XanbooRuleTimeStateGuard)) == true ) break; // exit loop if we find a trigger in a sub guard.
                }
            }
        }
        return is;
    }

    
    /**
     * Returns whether this guard is or contains a device state guard. 
     *
     * @return boolean true, if the guard is an event guard, which may trigger rule execution.
     */
    public boolean hasDeviceState() {
        boolean is = false;
        if (this instanceof XanbooRuleDeviceStateGuard) {
            is = true;
        } else {
            if ( guardArray != null ) {
                for ( int i=0; i<guardArray.size(); i++ ) {
                    XanbooRuleGuard g = (XanbooRuleGuard) guardArray.get( i );
                    if ( (is = (g instanceof XanbooRuleDeviceStateGuard)) == true ) break; // exit loop if we find a trigger in a sub guard.
                }
            }
        }
        return is;
    }
    
    /**
     * Returns the operator that preceeds this guard string.
     * <br>
     * @return one of XanbooRuleGuard's AND or OR constants.
     */
    public int getLogicalOp() {
        return this.logicalOp;
    }
    
    /**
     * Sets the operator to preceed this guard condition
     * @param op The operator - one of XanbooRuleGuard AND or OR constants.
     */
    public void setLogicalOp( int op ) {
        this.logicalOp = op;
    }
    
    public boolean equals( XanbooRuleGuard g ) {
        return ( g.toString().equals( this.toString() ) );
    }
    
    /**
     * Adds a sub-rule to this rule guard.
     */
    public void addRuleGuard( XanbooRuleGuard g, int logicalOp ) throws XanbooException {
        //System.err.println("ADDING TO GUARD: " + g.toString() + " : " + operator);
        if ( this.guardArray == null ) {
            this.guardArray = new ArrayList();
        }
        
        g.setLogicalOp( logicalOp );
        g.id = ((this.id+1) * 100 ) + guardArray.size();
        guardArray.add( g );
        
    }
    
    /**
     * Returns the unique identifier for this guard that can be used to delete it with a call to XanbooRule.deleteRuleGuard()
     */
    public int getId() {
        return this.id ;
    }

    /**
     * Returns an array of sub-Guards (if any) defined for this Guard.
     *
     * @return an array of objects representing each guard condition.
     */
    public XanbooRuleGuard[] getRuleGuardList() {
        if ( guardArray == null ) {
            return new XanbooRuleGuard[0];
        } else {
            return (XanbooRuleGuard[])guardArray.toArray(new XanbooRuleGuard[guardArray.size()]);
        }
    }
    
    protected boolean deleteRuleGuard( int id ) {
        
        boolean guardDeleted = false;
     
        // if sub guards present
        if ( this.guardArray != null ) {
            
            // loop through sub guards
            for ( int i=0; i<this.guardArray.size(); i++ ) {
                XanbooRuleGuard g = (XanbooRuleGuard)guardArray.get( i );
                
                if ( g.getId() == id ) {
                    // if we get a match, delete it, and flag guardDeleted to abort loop
                    guardArray.remove( i );
                    guardDeleted = true;
                } else {
                    // otherwise no match for this sub-guard - attempt to delete it's sub-guards
                    guardDeleted = g.deleteRuleGuard( id );
                }
                
                // if a guard was deleted by either condition above, exit the loop.
                if ( guardDeleted ) break;
            }
        }
        
        return guardDeleted;
        
    }

}
