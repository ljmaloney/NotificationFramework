/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/gateway/rules/XanbooRuleTimeStateGuard.java,v $
 * $Id: XanbooRuleTimeStateGuard.java,v 1.17 2005/11/23 21:18:36 rking Exp $
 *
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.gateway.rules;

import com.xanboo.core.util.XanbooException;

/**
 * A XanbooRuleTimeRangeGuard defines a time range condition for a rule's execution.
 * E.g. from 9am to 5pm every weekday
 * <br><br>
 * XanbooRuleTimeRangeGuard includes a 'from time', and a 'to time' attributes to specify
 * the start and end of the time range. Both time specifier are represented by {@link com.xanboo.core.sdk.gateway.rules.XanbooRuleTimeEventGuard XanbooRuleTimeEventGuard} objects.
 *
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleGuard XanbooRuleGuard
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleStateGuard XanbooRuleStateGuard
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleTimeEventGuard XanbooRuleTimeEventGuard
 */
public class XanbooRuleTimeStateGuard extends XanbooRuleStateGuard {
    
    private XanbooRuleTimeEventGuard t1;
    private XanbooRuleTimeEventGuard t2;
    private static final String TO_SEPARATOR = ".TO.";
    
    private static final String[] RANGE_KEYWORDS = { ".sun", ".mon", ".tue", ".wed", ".thu", ".fri", ".sat", ".everyday", ".weekday", ".weekend", ".morning", ".evening", ".daytime", ".nighttime", ".summer", ".winter", ".spring", ".fall" };
    
    /**
     * Time range constant specifies each and every day.
     */
    public static final int EVERYDAY  =  7;
    
    /**
     * Time range constant specifies weekdays, monday thru friday.
     */
    public static final int WEEKDAY   =  8;
    
    /**
     * Time range constant specifies weekends, saturday and sunday.
     */
    public static final int WEEKEND   =  9;
    
    /**
     * Time range constant specifies mornings.
     */
    public static final int MORNING   =  10;
    
    /**
     * Time range constant specifies evenings.
     */
    public static final int EVENING   =  11;
    
    /**
     * Time range constant specifies day time.
     */
    public static final int DAYTIME   =  12;
    
    /**
     * Time range constant specifies nighttime.
     */
    public static final int NIGHTTIME =  13;
    
    /**
     * Time range constant specifies during the summer season.
     */
    public static final int SUMMER    =  14;
    
    /**
     * Time range constant specifies during the winter season.
     */
    public static final int WINTER    =  15;
    
    /**
     * Time range constant specifies during spring season.
     */
    public static final int SPRING    = 16;
    
    /**
     * Time range constant specifies during the autumn season.
     */
    public static final int FALL      = 17;
    
    /**
     * Time range constant specifies Sunday
     */
    public static final int SUNDAY     = 0;
    
    /**
     * Time range constant specifies Monday
     */
    public static final int MONDAY     = 1;
    
    /**
     * Time range constant specifies Tuesday
     */
    public static final int TUESDAY    = 2;
    
    /**
     * Time range constant specifies Wednesday
     */
    public static final int WEDNESDAY  = 3;
    
    /**
     * Time range constant specifies Thursday
     */
    public static final int THURSDAY   = 4;
    
    /**
     * Time range constant specifies Friday
     */
    public static final int FRIDAY     = 5;
    
    /**
     * Time range constant specifies Saturday
     */
    public static final int SATURDAY   = 6;
    
    private int timeRange = -1;
    

    
    /*public static void main( String[] args ) {
        
        
        String gS = "TIME.K.mon";
        XanbooRuleTimeStateGuard g = new XanbooRuleTimeStateGuard( gS );
        
        System.err.println( "FROM: " + gS );
        System.err.println( "  TO: " + g.toString() );
        
    }//*
    
    /** Creates a new instance of XanbooRuleTimeRange from an internal string representation.
     * <br>This method is used internally and should be avoided by SDK developers !!!
     */
    protected XanbooRuleTimeStateGuard(String guard) {
        super();
        int p1 = guard.indexOf(TO_SEPARATOR);
        if ( p1 == -1 ) {
            for ( int i=0; i<RANGE_KEYWORDS.length; i++ ) {
                if ( guard.toUpperCase().equals( ("TIME.K" + RANGE_KEYWORDS[i]).toUpperCase()) ) {
                    this.timeRange = i;
                    break;
                }
            }
        } else {
            t1 = new XanbooRuleTimeEventGuard( guard.substring(0, p1) );
            t2 = new XanbooRuleTimeEventGuard( guard.substring(p1 + 4) );
            t1.setLogicalOp( XanbooRuleGuard.OPERATOR_NONE );
            t2.setLogicalOp( XanbooRuleGuard.OPERATOR_NONE );
        }
    }
    
   /** Creates a new instance of XanbooRuleTimeRange.
     *
     * @param timeRange One of time range constance defined
     *
     */
    public XanbooRuleTimeStateGuard( int timeRange ) {
        this.timeRange = timeRange;
    }
    
   /** Creates a new instance of XanbooRuleTimeRange with given parameters.
     *
     * @param fromTime a XanbooRuleTimeEventGuard object to represent start of the time range
     * @param toTime a XanbooRuleTimeEventGuard object to represent end of the time range
     *
     * @throws XanbooException if the object cannot be constructed.
     */
    public XanbooRuleTimeStateGuard(XanbooRuleTimeEventGuard fromTime, XanbooRuleTimeEventGuard toTime) throws XanbooException{
        super();
        t1 = fromTime;
        t2 = toTime;
        t1.setLogicalOp( XanbooRuleGuard.OPERATOR_NONE );
        t2.setLogicalOp( XanbooRuleGuard.OPERATOR_NONE );
        this.validate();
    }
    
    
    /** 
     * Returns string representation of this guard.
     * <br>This method is used internally and should be avoided by SDK developers !!!
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();

        boolean subGuards = ( this.guardArray != null && this.guardArray.size() > 0 );
        
        sb.append( super.toString() );
        
        if ( subGuards ) {
            sb.append( "(" );
        }
        
        if ( this.timeRange != -1 ) {
            // time keyword based. Note that day of week keywords are SDK features, not currently part of the rules spec
            // so the strings must be generated appropriately.
            int day = -1;
            sb.append("TIME.K").append(RANGE_KEYWORDS[this.timeRange]);
            // Return string for specified keyword
        } else {
            try {
                t1.setHourRepetition( XanbooRuleTimeEventGuard.REPEAT_NONE );
                t1.setMinuteRepetition( XanbooRuleTimeEventGuard.REPEAT_NONE );
            } catch ( XanbooException ignore ) {}
            // append the event strings to form range string,
            sb.append( t1.toString() ).append(TO_SEPARATOR).append( t2.toString() );
        }
        
        if ( subGuards ) {
            for ( int i=0; i<this.guardArray.size(); i++ ) {
                XanbooRuleGuard g = (XanbooRuleGuard) guardArray.get( i );
                sb.append( g.toString() );
            }
            sb.append( ")" );
        }
                
        return sb.toString();
        
    }
    

    /*
     * Returns true if this guard specifies a time range
     *
    public boolean isTimeRange() {
        if( this.timeRange == MORNING || this.timeRange == DAYTIME || 
                this.timeRange == EVENING || this.timeRange == NIGHTTIME ) {
            return true;
        } else if ( t1 != null ) {
            if( t1.isWeekly() ) {
                if ( t1.getDay() == XanbooRuleTimeEventGuard.ANYDAY ) {
                    return true;
                }
            } else if ( t1.isMonthly() ) {
                if( t1.getDate() == XanbooRuleTimeEventGuard.DATE_ANY && t1.getMonth() == XanbooRuleTimeEventGuard.MONTH_ANY ) {
                    return true;
                }
            }
        }
        return false;
    }    
    
    /*
     * Returns true if this guard specifies a day/date range
     *
    public boolean isDateRange() {
        return !this.isTimeRange();
    }*/
    
    
    /**
     * Validates this guard for correct format and completeness
     * 
     * @throws XanbooException if an error was found
     */
    public void validate() throws XanbooException {
        try {
            // no need to validate if pre-defined time range constant (weekeday, evening, etc )
            if ( this.timeRange == -1 ) {
                t1.validate();
                t2.validate();
                if ( (t1.getHour() == -1 ^ t2.getHour() == -1) ||
                (t1.getDay() == -1 ^ t2.getDay() == -1) ||
                (t1.getDate() == -1 ^ t2.getDate() == -1) ||
                (t1.getMonth() == -1 ^ t2.getMonth() == -1) ) {
                    throw new XanbooException( 29070 );
                }
            }
        } catch ( XanbooException xe ) {
            xe.printStackTrace();
            throw new XanbooException( 29070 );
        }
    }

    public int getTimeRange() {
        return this.timeRange;
    }
    
    
    /**
     * Returns the start of the time range.
     *
     * @return fromTime start of the time range as a XanbooRuleTimeEventGuard object 
     */
    public XanbooRuleTimeEventGuard getFromTimeGuard() {
        return this.t1;
    }
    
    /**
     * Returns the end of the time range.
     *
     * @return toTime end of the time range window as a XanbooRuleTimeEventGuard object
     */
    public XanbooRuleTimeEventGuard getToTimeGuard() {
        return this.t2;
    }

    
    
}
