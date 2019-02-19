/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/gateway/rules/XanbooRule.java,v $
 * $Id: XanbooRule.java,v 1.49 2006/08/03 15:01:46 rking Exp $
 *
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.gateway.rules;

import com.xanboo.core.util.XanbooException;
import com.xanboo.core.util.XanbooUtil;
//import com.xanboo.testui.util.Utils;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * <p>
 * A XanbooRule object is representation of a Rule entry in a Gateway Rules Table. A Xanboo Rule consists of a sequence of actions that are to be executed by a Xanboo Gateway at a specified time of day
 * or whenever particular device events occur, or a logical combination of the two.
 * <br>
 * <br>
 * Each Gateway device maintains a Policy Rules Table, and each entry in this table is represented by a XanbooRule object.<br>
 * The following attributes are defined for a XanbooRule object:<br>
 *  &nbsp; <i>rule Index</i>: The absolute index number of the Rule within the Rules Table. Rule Indices start from 0.<br>
 *  &nbsp; <i>Status</i>: Current status of the Rule, which must be one of the status constants defined in this class.<br>
 *  &nbsp; <i>Name</i>: An optional alphanumeric name for the Rule.<br>
 *  &nbsp; <i>GuardList</i>: List of Events/Triggers and Conditions defined for the Rule.<br>
 *  &nbsp; <i>ActionList</i>: List of Actions to be executed (when the Guards evalute to true) for this Rule<br>
 * <br>
 * The Rule attributes given above are manipulated thru the associated getter and setter methods provided.
 *
 * <ul>
 * <li><b> ActionList </b>
 * <br>
 * When a Rule is executed, the actions associated with it are processed. Actions are represented by instances of {@link com.xanboo.core.sdk.gateway.rules.XanbooRuleAction XanbooRuleAction} class,
 * and are associated with a XanbooRule using its {@link com.xanboo.core.sdk.gateway.rules.XanbooRule#addRuleAction addRuleAction()} method.
 * </li>
 * <br>
 * <br>
 * <li><b> Guard List </b>
 * <br>
 * The Rule Guard List defines the conditions under which an action is executed. For a rule to execute, all of its guard conditions must evaluate to true. <br><br>
 * There are two types of Guards, one being the occurrance of some kind of device or time event (Trigger Guard), and the other being some sort of state condition (State Guard).
 * A non-null, valid rule guard must contain one and only one Event/Trigger Guard, and an optional number of additional state condition guards. 
 * <br><br>
 * All guards of a Xanboo Rule is 'ANDed' together during guard list evaluation. For example, a rule guard could define:<i> 'when motion detected on camera1 AND light1 is on'</i>.
 * In this case, <i>'motion detected on camera1'</i> is the Event/Trigger guard, and <i>'Light1 is on'</i> check is a state condition Guard of a device state.
  * <br><br>
 * A XanbooRule can be defined without a Guard, which disables auto-execution of the rule. In this case it is only executed 'on demand' thru GatewayManager {@link com.xanboo.core.sdk.gateway.GatewayManager#execRule execRule()}  method.
 * <br><br>
 * Rule Guards are represented by sub-classes of the abstract {@link com.xanboo.core.sdk.gateway.rules.XanbooRuleGuard XanbooRuleGuard} class and are associated with a XanbooRule
 * using its {@link com.xanboo.core.sdk.gateway.rules.XanbooRule#addRuleGuard addRuleGuard()}
* </ul>
 * <br>
 * Refer to the SDK Programmer's Guide for details on all Rules related topics.
 * </p>
 *
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleGuard XanbooRuleGuard
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleAction XanbooRuleAction
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleEventGuard XanbooRuleEventGuard
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleStateGuard XanbooRuleStateGuard
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleDeviceEventGuard XanbooRuleDeviceEventGuard
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleDeviceStateGuard XanbooRuleDeviceStateGuard
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleTimeEventGuard XanbooRuleTimeEventGuard
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleTimeStateGuard XanbooRuleTimeStateGuard
 * @see com.xanboo.core.sdk.gateway.GatewayManager GatewayManager
 */
public class XanbooRule implements java.io.Serializable, XanbooGuardContainer {
    
    
    /*public static void main( String[] args ) {
        
        try {
            System.err.println("[XanbooRule] main() ");

            //String[] s = {"(TIME.k.sunrise.AND.0.o1100.GT.'b'.OR.time.k.everyday).AND.(0.o1200.GT.'12'.OR.time.k.winter)"};
            //String[] s = { "(TIME.K.weekday).AND.TIME.K.sunrise" };
            String[] s = { "TIME.k.sunrise.AND.time.k.everyday.AND.time.k.winter",
                           "TIME.k.sunrise.AND.0.o1100.GT.'b'.OR.time.k.everyday.AND.(0.o1200.GT.'12'.OR.time.k.winter).OR.0.o1200.GT.'12'.OR.time.k.winter",
                           "TIME.k.sunrise.AND.0.o1100.GT.'b'.OR.time.k.everyday.AND.(0.o1200.GT.'12'.OR.time.k.winter.AND.time.k.everyday).OR.0.o1200.GT.'12'.OR.time.k.winter",
                           "(TIME.k.sunrise.AND.0.o1100.GT.'b'.OR.time.k.everyday).AND.0.o1200.GT.'12'.OR.time.k.winter",
                           //"(0.e1024.AND.0.o1100.EQ.'armed'.OR.AA00000002.o1100.EQ.'armed').AND.(TIME.W.1d.0h.0m.TO.TIME.W.1d.11h.15m)",
                           "(TIME.K.weekday).AND.TIME.K.sunrise" };
                         //* /
            boolean overall = true;
                         
            for ( int i=0; i<s.length; i++ ) {
                String guardString = s[i] ; //+ ".AND.TIME.W.*d.0h.0m.TO.TIME.W.*d.1h.45m";
                System.err.println("------TEST " + i + " : " + guardString);
                
                String actionString = "0.o1100='armed'";
                String name = "My Rule";

                XanbooRule rule = new XanbooRule( 0, STATUS_ACTIVE, name, guardString, actionString );
                
                XanbooRuleGuard[] guards = rule.getRuleGuardList();
                
                String newGuard = rule.toGuardString();
                String newAction = rule.toActionString();

                boolean guardMatch = guardString.toUpperCase().equals( newGuard.toUpperCase() );
                boolean actionMatch = actionString.toUpperCase().equals( newAction.toUpperCase() );
                
                System.err.println( "OLD: " + guardString );
                System.err.println( "NEW: " + newGuard );
                System.err.println( "Guard tests " + guardMatch + " Action tests " + actionMatch );
                System.err.println( "XanbooRule tests " + (guardMatch && actionMatch) );
                
                //if ( (overall = (guardMatch && actionMatch) ) != true ) break;                

            }
            
            System.err.println("OVERALL TEST: " + overall);
            
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        
    }//*/
    
    private static final String[] RULE_STATUS = { "",
    "PENDING",
    "ACTIVE",
    "SUSPENDED",
    "CONFLICT",
    "","","",
    "INVALID",
    "INVALID GUARD",
    "INVALID ACTIONS"
    };
    
    /** Status code indicating the rule has not yet been validated */
    public static final int STATUS_PENDING = 1;
    /** Status code indicating the rule is valid, and it will be executed when its guard evaluates to TRUE */
    public static final int STATUS_ACTIVE = 2;
    /** Status code indicating the rule is not in use, but can be re-activated on request */
    public static final int STATUS_SUSPENDED = 3;
    /** Status code indicating the rule is not in use, and conflicts with another ACTIVE rule in the table */
    public static final int STATUS_CONFLICT = 4;
    /** Status code indicating the rule's guard and action list are invalid, and it cannot be activated */
    public static final int STATUS_INVALID = 8;
    /** Status code indicating the rule's guard is invalid, and it cannot be activated */
    public static final int STATUS_INVALID_GUARD = 9;
    /** Status code indicating the rule's action list is invalid, and it cannot be activated */
    public static final int STATUS_INVALID_ACTIONS = 0xA;
    
    /** special device GUID to represent any device in rule guards */
    public static final String ANY_DEVICE = "_c*";
    
    /** special eventId to represent any event in rule guards */
    public static final String ANY_EVENT  = "*";
    
    
    //public static final String SEPARATOR = "|";
    
    private static final int MAX_ACTION_LENGTH=512;
    private static final int MAX_NAME_LENGTH=32;
    private static final int MAX_GUARD_LENGTH = MAX_ACTION_LENGTH - MAX_NAME_LENGTH - 2;

    private static final String ALLOWABLE_NAME_CHARS = "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz -_'";
    
    private int ruleIndex; //rule number
    private int status = STATUS_PENDING;
    private String name;
    private ArrayList guardArray;
    private ArrayList actionArray;
    private ArrayList notificationArray;
    
    //private boolean hasGuardTrigger; //used to track which (if any) of the configured guards is the single supported event
    private char accessRights;
    
    /** Creates a new blank Rule object with given rule index.
     *
     * @param ruleIndex Absolute rule index of the Rule. Indices start from 0.
     */
    public XanbooRule( int ruleIndex ) {
        this.ruleIndex = ruleIndex;
        this.status = STATUS_PENDING; //set status to pending
        this.name = "";
        //this.hasGuardTrigger = false;
        this.guardArray = new ArrayList();
        this.actionArray = new ArrayList();
    }

    /**
     * Creates a new Rule object with given parameters.
     * <br>This constructor is used internally and should be avoided by SDK developers !!!
     *
     * @param ruleIndex Absolute index of the Rule. Indices start from 0.
     * @param status One of the status constants defined in this class.
     * @param guardList String containing zero or more Guard conditions
     * @param actionList String containing one or more Device Actions
     *
     * @throws XanbooException if there was a problem with one of the supplied parameters.
     */
    public XanbooRule(int ruleIndex, int status, String name, String guard, String actionList, boolean isNameWritable, boolean isGuardWritable, boolean isActionWritable ) throws XanbooException {
        try {
            
            this.ruleIndex = ruleIndex;
            this.status = status == 0 ? STATUS_PENDING : status;
            this.guardArray = new ArrayList();
            this.actionArray = new ArrayList();            
            
            try {
                setGuard( guard );
            } catch ( XanbooException xe ) {
                throw xe;
            } catch ( Exception e ) {
                status = STATUS_INVALID_GUARD;
            }
            
            try {
                setAction( actionList );
            } catch ( Exception e ) {
                //error parsing actions, flag as invalid
                if ( status == STATUS_INVALID_GUARD ) {
                    //guard already flagged as invalid, so flag both as invalid
                    status = STATUS_INVALID;
                } else {
                    //just flag actions as invalid
                    status = STATUS_INVALID_ACTIONS;
                }
            }
            
            this.setName( name );
            
            if ( isNameWritable ) {
                accessRights = 0;
            } else if ( !isGuardWritable && !isActionWritable ) {
                accessRights = '&';
            } else if ( isGuardWritable && isActionWritable ) {
                accessRights = '%';
            } else if ( !isGuardWritable ) {
                accessRights = '~';
            }
            
        } catch ( XanbooException xe ) {
            throw xe;
        } catch ( Exception e ) {
            e.printStackTrace();
            throw new XanbooException( 29000, "Could not create rule" );
        }
        

    }
    
    /**
     * Creates a new Rule object with given parameters.
     * <br>This constructor is used internally and should be avoided by SDK developers !!!
     *
     * @param ruleIndex Absolute index of the Rule. Indices start from 0.
     * @param status One of the status constants defined in this class.
     * @param guardList String containing zero or more Guard conditions
     * @param actionList String containing one or more Device Actions
     *
     * @throws XanbooException if there was a problem with one of the supplied parameters.
     */
    public XanbooRule(int ruleIndex, int status, String name, String guard, String actionList) throws XanbooException {
        try {
            
            this.ruleIndex = ruleIndex;
            this.status = status == 0 ? STATUS_PENDING : status;
            this.guardArray = new ArrayList();
            this.actionArray = new ArrayList();            
            
            try {
                setGuard( guard );
            } catch ( XanbooException xe ) {
                throw xe;
            } catch ( Exception e ) {
                status = STATUS_INVALID_GUARD;
            }
            
            try {
                setAction( actionList );
            } catch ( Exception e ) {
                //error parsing actions, flag as invalid
                if ( status == STATUS_INVALID_GUARD ) {
                    //guard already flagged as invalid, so flag both as invalid
                    status = STATUS_INVALID;
                } else {
                    //just flag actions as invalid
                    status = STATUS_INVALID_ACTIONS;
                }
            }
            
            this.setName( name );
            
        } catch ( XanbooException xe ) {
            throw xe;
        } catch ( Exception e ) {
            e.printStackTrace();
            throw new XanbooException( 29000, "Could not create rule" );
        }
        
    }
    
    private void setGuard( String guard ) throws Exception {
        setGuard( guard, this );
    }
    
    private void setGuard( String guard, XanbooGuardContainer container ) throws Exception{
        
        boolean loop = true;
        int lastOperator = XanbooRuleGuard.OPERATOR_NONE;
        int operator = lastOperator;
        
        // Search for tokens that start a guard term
        String[] tokens = { ".AND.(", ".OR.(", ".AND.", ".OR.", "(" };
        
        while( loop ) {
            // Guard is defined between two of the above tokens.
            int token1Pos = guard.length(); // position of first token
            int token2Pos = token1Pos; // position of second token
            int token1 = -1; // token 1
            int token2 = -1; // token 2
            
            String gg = null;

            // Locate posistion of first token
            for ( int i=0; i<tokens.length; i++ ) {
                int pos = guard.indexOf( tokens[i] );
                if ( pos != -1 && pos < token1Pos ) {
                    token1Pos = pos;
                    token1 = i;
                }
            }
            
            // Locate position of second token, ignoring closed parenthesis
            for ( int i=0; i<tokens.length-1; i++ ) {
                int pos = guard.indexOf( tokens[i], token1Pos+1 );
                if ( pos != -1 && pos < token2Pos ) {
                    token2Pos = pos;
                    token2 = i;
                }
            }
                
            if ( token1 == 0 || token1 == 2 ) {
                lastOperator = XanbooRuleGuard.OPERATOR_AND;
            } else {
                lastOperator = XanbooRuleGuard.OPERATOR_OR;
            }            
            
            if ( token1Pos != 0 ) {
                token1 = -2;
            }
            
            switch ( token1 ) {
                case -2:
                    // Means term is first with no operator
                    gg = guard.substring( 0, token1Pos );
                    container.addRuleGuard( XanbooRuleGuard.createGuard( gg ), XanbooRuleGuard.OPERATOR_NONE );
                    guard = guard.substring( token1Pos );
                    break;
                case -1:
                    // Means no AND, OR or parenthesis present.
                    if ( guard.trim().length() > 0 ) {
                        container.addRuleGuard(XanbooRuleGuard.createGuard(guard.trim()), lastOperator);
                        gg = guard.trim();
                    }
                    loop = false; // end loop - no more terms
                    break;
                case 2:
                case 3:
                    // AND || OR condition
                    if ( token1Pos == 0 ) {
                        // String starts with .AND. or .OR.
                        gg = guard.substring( tokens[token1].length(), token2Pos );
                        guard = guard.substring( token2Pos );
                        XanbooRuleGuard g = XanbooRuleGuard.createGuard(gg);
                        container.addRuleGuard( g, lastOperator );
                    } else {
                        // First operator found after term is .AND. or .OR.
                        gg = guard.substring( 0, token1Pos );
                        guard = guard.substring( token1Pos );
                        XanbooRuleGuard g = XanbooRuleGuard.createGuard(gg);
                        container.addRuleGuard( g, lastOperator );
                    }
                    break;
                case 4:
                    // This means the next token is closed bracket
                case 0:
                case 1:
                    // Parenthesis - string starts with .AND.( or .OR.(
                    int rp = guard.indexOf(")"); //right parenthesis position
                    String subGuard = null;
                    
                    if ( token1Pos == 0 ) {
                        // Starting at far left of string
                        gg = guard.substring( tokens[token1].length(), token2Pos );
                        
                        // Strip right parenthesis if necessary
                        if ( gg.endsWith( ")" ) ) gg = gg.substring( 0, gg.length() -1 );
                        
                        if ( rp > token2Pos ) {
                            subGuard = guard.substring( token2Pos, rp ); // send with operator
                        }
                        guard = guard.substring( rp+1 );
                    } else {
                        gg = guard.substring( 0, token1Pos );
                        subGuard = guard.substring( token1Pos, rp ); // send with operator
                        guard = guard.substring( rp+1 );
                    }
                    
                    XanbooRuleGuard g = XanbooRuleGuard.createGuard(gg);
                    
                    container.addRuleGuard( g, lastOperator );
                    if ( subGuard != null ) setGuard( subGuard, g );
                    
                    break;
                    
            }

        }
        
    }    
    
    
    /* Sets action from intenal oid string value */
    private void setAction(String actionCSV) throws XanbooException {
        
        for(StringTokenizer st = new StringTokenizer( actionCSV, "," ); st.hasMoreTokens(); ) {
            String tkn = st.nextToken().trim();
            
            if( tkn.startsWith("_na='" ) ) {
                
                String addresses = tkn = tkn.substring( 5, tkn.length()-1 );
                for( StringTokenizer s = new StringTokenizer( addresses, ";" ); s.hasMoreTokens(); ) {
                    String addr = s.nextToken().trim();
                    this.addNotification( new XanbooRuleNotification(addr, true) );
                }
                
            } else if( tkn.startsWith("_n='" ) ) {

                String addresses = tkn = tkn.substring( 4, tkn.length()-1 );
                for( StringTokenizer s = new StringTokenizer( addresses, ";" ); s.hasMoreTokens(); ) {
                    String addr = s.nextToken().trim();
                    this.addNotification( new XanbooRuleNotification(addr, false) );
                }
                        
            } else {
                this.addRuleAction(new XanbooRuleAction(tkn));
            }
        }
    }
    
    
    /**
     * Returns the name of this rule
     *
     * @return The name of this rule.
     */
    public String getName() {
        return this.name;
    }
    
    /**
     *  Returns whether the name of this rule may be updated by the user
     */
    public boolean isNameWritable() {
        return ( this.accessRights == 0 );
    }
    
    /**
     *  Returns whether the guard of this rule may be updated by the user
     */
    public boolean isGuardWritable() {
        return ( this.accessRights == 0 || this.accessRights == '%' );
    }
        
    /**
     *  Returns whether the actions of this rule may be updated by the user
     */
    public boolean isActionWritable() {
        return ( this.accessRights == 0 || this.accessRights == '%' || this.accessRights == '~' );
    }
    
    /**
     * Sets the name of this rule
     *
     * @param name The new rule name.
     */
    public void setName( String name ) throws XanbooException {
        
        if ( !this.isNameWritable() ) {
            throw new XanbooException( 29050, "Rule Name is read only and cannot be changed." );
        }
        
        name = name.trim();
	if ( name.length() > 0 ) {
            switch( name.charAt( 0 ) ) {
                case '%':
                case '~':
                case '&':
                    accessRights = name.charAt( 0 );
                    name = name.substring( 1 );
            }
        }

        if ( !XanbooUtil.isValidString( name, ALLOWABLE_NAME_CHARS) )
            throw new XanbooException( 29005, "Invalid characters in rule name." );
        else if ( name.length() > MAX_NAME_LENGTH )
            throw new XanbooException( 29006, "Rule name too long. Max " + MAX_NAME_LENGTH + " chars" );
        
        this.name = name;
    }
    
    
    /**
     * Returns the status of this rule.
     * <br>
     *
     * @return The status of this rule corresponding to one of the constants defined in this class.
     *
     */
    public int getStatus() {
        return status;
    }
    
    /**
     * Sets the status of this rule.
     *
     * @param int the new status, corresponding to one of the status constants defined in this class.
     *
     */
    public void setStatus( int status ) {
        this.status = status;
    }
    
    /**
     * Returns the absolute rule index of the Rule within the Gateway Rule Table. Indices start from 0.
     *
     * @return The index of this rule.
     *
     */
    public int getRuleIndex() {
        return ruleIndex;
    }
    
    /**
     * Returns true if this Rule has an Event(Trigger) Guard configured. In order for a Rule to
     * be valid, one or more event guards must be specified.
     * <br>
     * @return true if an event trigger guard is defined.
     */
    public boolean hasGuardTrigger() {
        //return this.hasGuardTrigger;
        boolean has = false;
        for ( int i=0; i<this.guardArray.size(); i++ ) {
            XanbooRuleGuard g = (XanbooRuleGuard) this.guardArray.get( i );
            if ((has = g.isTrigger()) == true ) break;
        }
        return has;
    }

    /**
     * Returns true if this Rule has any State Guards configured.
     * <br>
     * @return true if an event trigger guard is defined.
     */
    public boolean hasGuardState() {
        boolean has = false;
        for ( int i=0; i<this.guardArray.size(); i++ ) {
            XanbooRuleGuard g = (XanbooRuleGuard) this.guardArray.get( i );
            if ((has = g.hasGuardState()) == true ) break;
        }
        return has;
    }
    
    public boolean hasTimeState() {
        boolean has = false;
        for ( int i=0; i<this.guardArray.size(); i++ ) {
            XanbooRuleGuard g = (XanbooRuleGuard) this.guardArray.get( i );
            if ((has = g.hasTimeState()) == true ) break;
        }
        return has;
    }
    public boolean hasDeviceState() {
        boolean has = false;
        for ( int i=0; i<this.guardArray.size(); i++ ) {
            XanbooRuleGuard g = (XanbooRuleGuard) this.guardArray.get( i );
            if ((has = g.hasDeviceState()) == true ) break;
        }
        return has;
    }
    
    /**
     * Returns an array of Actions defined for this Rule. In order for a Rule to
     * be valid, at least one Device Action must be specified.
     *
     * @return an array objects representing each action.
     */
    public XanbooRuleAction[] getRuleActionList() {
        if ( actionArray == null ) {
            return new XanbooRuleAction[0];
        } else {
            return (XanbooRuleAction[])actionArray.toArray(new XanbooRuleAction[actionArray.size()]);
        }
    }
    
    /**
     * Returns an array of Guards (if any) defined for this Rule.
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
    
    
    /**
     * Adds a new Device Action to this Rule.
     *
     * @param action the new Action definition to add
     *
     * @throws XanbooException if there is no space in the action string for this action or the action is read only.
     */
    public void addRuleAction( XanbooRuleAction action ) throws XanbooException {

        // check if action is writable
        if ( !isActionWritable() ) {
            throw new XanbooException( 29052, "Rule Action is read only and cannot be changed." );
        }
        
        // make sure we don't exceed the oid string max length
        if ( (this.toActionString().length() + action.toString().length()) > (MAX_ACTION_LENGTH-1) ) {
            throw new XanbooException( 29010, "Action string exceeds max length" );
        }
        
        // Dissalow duplicate actions.
        for ( int i=0; i<actionArray.size(); i++ ) {
            if ( action.equals( (XanbooRuleAction) actionArray.get( i ) ) ) {
                throw new XanbooException( 29090, "Identical rule action already exists." );
            }
        }
        
        this.actionArray.add( action );
    }

    /**
     * Adds a new Guard for this Rule.
     *
     * @param guard the new Guard definition to add
     *
     * @throws XanbooException if invalid or multiple event guards are specified, or the guard is read only.
     */
    public void addRuleGuard( XanbooRuleGuard g ) throws XanbooException {
        
        addRuleGuard( g, XanbooRuleGuard.OPERATOR_AND );

        /* - automatically select operator ?
        int op = XanbooRuleGuard.OPERATOR_AND;
        
        if( g instanceof XanbooRuleTimeStateGuard ) {
            XanbooRuleTimeStateGuard gg = (XanbooRuleTimeStateGuard) g;
            if ( (gg.isTimeRange() && this.hasTimeStateTimeRange()) || ( gg.isDateRange() && this.hasTimeStateDateRange()) ) {
                //op = OPERATOR_OR;
            }
        }
        
        addRuleGuard( g, op );
         **/
        
    }
    
    /**
     * Returns whether rule contains a time range/state guard.
     *
    public boolean hasTimeStateTimeRange() {
        for ( int i=0; i<this.guardArray.size(); i++ ) {
            XanbooRuleGuard g = (XanbooRuleGuard) this.guardArray.get( i );
            if ( g.hasTimeStateTimeRange() == true ) return true;
        }
        return false;
    }

    
    /**
     * Returns whether rule contains a date range/state guard.
     *
    public boolean hasTimeStateDateRange() {
        for ( int i=0; i<this.guardArray.size(); i++ ) {
            XanbooRuleGuard g = (XanbooRuleGuard) this.guardArray.get( i );
            if ( g.hasTimeStateDateRange() == true ) return true;
        }
        return false;
    }*/
    
    /**
     * Adds a new Guard for this Rule.
     *
     * @param guard the new Guard definition to add
     * @param operator one of XanbooRuleGuard AND or OR constants.
     *
     * @throws XanbooException if invalid or multiple event guards are specified, or the guard is read only.
     */
    public void addRuleGuard( XanbooRuleGuard guard, int operator ) throws XanbooException {
        
        if ( !isGuardWritable() ) {
            throw new XanbooException( 29051, "Rule Guard is read only and cannot be changed." );
        }
        
        // Limit length to ( MAX_GUARD_LENGTH - ".AND.".length() )
        if ( (this.toGuardString().length() + guard.toString().length()) > (MAX_GUARD_LENGTH-5) ) {
            throw new XanbooException( 29020, "Guard string exceeds max length"  );
        }
        
        // dissalow duplicate guards - TODO. this can change when parenthesis introduced.
        /* Parenthesis implemented - removing duplicate check.
         for ( int i=0; i<this.guardArray.size(); i++ ) {
            if ( guard.equals( (XanbooRuleGuard) guardArray.get( i ))) { // XanbooRuleGuard overrides equals.
                throw new XanbooException( 29092, "Identical rule guard condition already exists.");
            }
        }*/
        
        // Set the appropriate operator for the guard.
        if ( guardArray.size() > 0 ) {
            guard.setLogicalOp( operator );
        } else {
            guard.setLogicalOp( XanbooRuleGuard.OPERATOR_NONE );
        }
        
        guard.id = guardArray.size();
        this.guardArray.add( guard );
        //guard.nestLevel = 0; // top level guard
    }
    
    
    /**
     * Removes a Device Action from this rule
     *
     * @param actionIndex the index position of the Action to delete within the action array,
     * typically returned by a recent getActionArray() call.
     *
     * @throws XanbooException is the action is read only for this rule.
     */
    public void deleteRuleAction( int actionIndex ) throws XanbooException {
        if ( !isActionWritable() ) {
            throw new XanbooException( 29052, "Rule Action is read only and cannot be changed." );
        }
        
        if ( this.actionArray != null ) {
            this.actionArray.remove( actionIndex );
        }
    }
    
    /**
     * Removes a notification from this rule
     *
     * @param actionIndex the index position of the Notification to delete within the notification array,
     * typically returned by a recent getNotificationList() call.
     *
     * @throws XanbooException is the action is read only for this rule.
     */
    public void deleteNotification( int notificationIndex ) throws XanbooException {
        if ( !isActionWritable() ) {
            throw new XanbooException( 29052, "Rule Action is read only and cannot be changed." );
        }
        
        if ( this.notificationArray != null ) {
            this.notificationArray.remove( notificationIndex );
        }
    }
    
        
    
    
    /**
     * Removes a Guard from this rule
     *
     * @param guardIndex the index position of the Guard to delete within the Guard array,
     * typically returned by a recent getGuardArray() call.
     *
     * @throws XanbooException is the rule guard is read only for this rule.
     */
    public boolean deleteRuleGuard( int guardId ) throws XanbooException {
        if ( !isGuardWritable() ) {
            throw new XanbooException( 29051, "Rule Guard is read only and cannot be changed." );
        }

        boolean deleted = false;
        
        for ( int i=0; i<this.guardArray.size(); i++ ) {
            XanbooRuleGuard g = (XanbooRuleGuard) this.guardArray.get( i );
            if ( g.getId() == guardId ) {
                this.guardArray.remove( i );
                deleted = true;
                break;
            } else {
                deleted = g.deleteRuleGuard( guardId );
                if ( deleted ) break;
            }
        }
        
        return deleted;

    }
    
    
    /**
     * Method to return internal mobject oid string representation of a rule guard object.
     * <br>This method is used internally and should be avoided by SDK developers !!!
     */
    public String toGuardString() {
        if ( guardArray != null ) {
            StringBuffer sb = new StringBuffer();
            for ( int i=0, n=guardArray.size(); i<n; i++ ) {
                XanbooRuleGuard g = (XanbooRuleGuard) guardArray.get( i ) ;
                if ( g != null ) {
                    sb.append( g.toString() );
                }
            }
            return sb.toString();
        } else {
            return "";
        }
    }
    
    
    /**
     * Method to return internal mobject oid string representation of a rule action object.
     * <br>This method is used internally and should be avoided by SDK developers !!!
     */
    public String toActionString() {
        if ( actionArray != null ) {
            StringBuffer sb = new StringBuffer();
            
            boolean isEmpty=true;
            
            // Append device actions
            for ( int i=0, n=actionArray.size(); i<n; i++ ) {
                XanbooRuleAction xa = (XanbooRuleAction) actionArray.get( i ) ;
                isEmpty=false;
                if ( i != 0 ) {
                    sb.append(",");
                }
                sb.append( xa.toString() );
            }
            
            // append notifications with attachments ( _na )
            if( notificationArray != null ) {
                boolean first = true;
                for( int i=0; i<notificationArray.size(); i++ ) {
                    XanbooRuleNotification n = (XanbooRuleNotification)notificationArray.get(i);
                    if( n.isAttach() ) {
                        if( first ) {
                            if( !isEmpty ) sb.append(",");
                            sb.append( "_na='");
                            first = false; isEmpty=false;
                        } else {
                            sb.append( ";" );
                        }
                        sb.append(n.getType()).append("%").append(n.getAddress());
                    }
                }
                if( !first ) sb.append( "'" );
                
                // append notifications with no attachment ( _n )
                first = true;
                for( int i=0; i<notificationArray.size(); i++ ) {
                    XanbooRuleNotification n = (XanbooRuleNotification)notificationArray.get(i);
                    if( !n.isAttach() ) {
                        if( first ) {
                            if( !isEmpty ) sb.append(",");
                            sb.append( "_n='");
                            first = false;
                        } else {
                            sb.append( ";" );
                        }
                        sb.append(n.getType()).append("%").append(n.getAddress());
                    }
                }
                if( !first ) sb.append( "'" );
                
            }

            return sb.toString();
        } else {
            return "";
        }
    }
    
    
    /**
     * Validates this rule for correct format and completeness
     *
     * @throws XanbooException if an error was found in this rule
     */
    public void validate() throws XanbooException {
        
        if ( this.name == null || this.name.trim().length() < 1 ||
                !XanbooUtil.isValidString( this.name, ALLOWABLE_NAME_CHARS )) {
            throw new XanbooException( 29005, "Invalid rule name." );
        } else if ( this.name.length() > MAX_NAME_LENGTH ) {
            throw new XanbooException( 29006, "Rule name too long. Max " + MAX_NAME_LENGTH + " chars" );
        } else if ( this.toActionString().length() > MAX_ACTION_LENGTH ) {
            throw new XanbooException( 29010, "Action string exceeds max length" );
        } else if ( this.toGuardString().length() > MAX_GUARD_LENGTH ) {
            throw new XanbooException( 29020, "Guard string exceeds max length" );
        } else {
            //if ( this.guardArray.size() > 0 && !this.hasGuardTrigger() ) {
            //    throw new XanbooException( 29030, "No event/trigger guard specified" );
            //}
            if ( this.guardArray != null ) {
                for ( int i=0, n=this.guardArray.size(); i<n; i++ ) {
                    XanbooRuleGuard guard = (XanbooRuleGuard) this.guardArray.get(i);
                    guard.validate();
                }
            }
            
            if ( this.actionArray != null ) {
                for ( int i=0, n=this.actionArray.size(); i<n; i++ ) {
                    XanbooRuleAction action = (XanbooRuleAction) this.actionArray.get(i);
                    action.validate();
                }
            }
        }
    }
    
    /**
     * Sets up a notification for this rule.
     * Notification messages are sent when the rule is triggered (ie. when guard list evaluates to true).
     */
    public void addNotification( XanbooRuleNotification notification ) throws XanbooException{
        if( this.notificationArray == null ) this.notificationArray = new ArrayList();
        
        // Dissalow duplicate actions.
        for ( int i=0; i<notificationArray.size(); i++ ) {
            if ( notification.equals( (XanbooRuleNotification) notificationArray.get( i ) ) ) {
                throw new XanbooException( 29090, "Identical notification action already exists." );
            }
        }
        
        notificationArray.add( notification );
        
    }
    
    public XanbooRuleNotification[] getNotificationList() {
        if ( notificationArray == null ) {
            return new XanbooRuleNotification[0];
        } else {
            return (XanbooRuleNotification[])notificationArray.toArray(new XanbooRuleNotification[notificationArray.size()]);
        }
    }
    
}
