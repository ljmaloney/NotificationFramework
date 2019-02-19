/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/gateway/rules/XanbooRuleTimeEventGuard.java,v $
 * $Id: XanbooRuleTimeEventGuard.java,v 1.9 2005/01/17 15:58:54 rking Exp $
 *
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.gateway.rules;

import com.xanboo.core.util.XanbooException;
import java.util.StringTokenizer;

/**
 * A XanbooRuleTimeEventGuard defines a specific, or repetetive time event at which a XanbooRuleGuard triggers,
 * which may be configured weekly or monthly.
 * <br><br> 
 * Time event guards define the following attributes. Notice some of these attributes apply only to weekly
 * or monthly time guards, or both:
 * <ul>
 *  <li><b>Minute</b> (Applies to both weekly and monthly guards)<br>
 *      Specifies the minutes past the hour ( 0..59 )
 *  </li>
 *  <li><b>Hour</b> (Applies to both weekly and monthly guards)<br>
 *      Specifies the hour of day in 24 hour format (0..23), or constant HOUR_ANY indicating any hour of the day.
 *  </li>
 *  <li><b>DayOfWeek</b> (Applies to only weekly guards)<br>
 *      Specifies the day of week as one of the DAY constants, or DAY_ANY constant indicating any day of the week.
 *  </li>
 *  <li><b>Date</b> (Applies to only monthly guards)<br>
 *      Specifies the day of the month, 1..31 depending on the month, or DATE_ANY constant indicating any day of the month.
 *  </li>
 *  <li><b>Month</b> (Applies to only monthly guards)<br>
 *      Specifies the month of the year as one of the MONTH constants, or MONTH_ANY constant, indicating any month of the year..
 *  </li>
 * </ul>
 *
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleGuard XanbooRuleGuard
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleEventGuard XanbooRuleEventGuard
 */
public class XanbooRuleTimeEventGuard extends XanbooRuleEventGuard {
    
    
    //public static final int EVENT_SUNSET/////
    private static final String[] KEYWORDS = { "TIME.K.sunset", "TIME.K.sunrise" };
    
    /** Time event constant specifying at the time the sun sets **/
    public static final int SUNSET = 0;
    
    /** Time event constant specifying at the time the sun rises **/
    public static final int SUNRISE = 1;
    
    /** Hour constant to indicate any hour of the day */
    public static final int HOUR_ANY=-1;

    /** Minute constant to indicate any minute of the day - valid only for repetitions */
    public static final int MINUTE_ANY=-1;
    
    /** Date constant to indicate any day of the month */
    public static final int DATE_ANY=-1;
    
    /** Day (dayOfWeek) constant for any day of the week */
    public static final int ANYDAY=-1;
    
    /** Day (dayOfWeek) constant for Sunday */
    public static final int SUNDAY    =    XanbooRuleTimeStateGuard.SUNDAY;
    /** Day (dayOfWeek) constant for Monday */
    public static final int MONDAY    =    XanbooRuleTimeStateGuard.MONDAY;
    /** Day (dayOfWeek) constant for Tuesday */
    public static final int TUESDAY   =    XanbooRuleTimeStateGuard.TUESDAY;
    /** Day (dayOfWeek) constant for Wednesday */
    public static final int WEDNESDAY =    XanbooRuleTimeStateGuard.WEDNESDAY;
    /** Day (dayOfWeek) constant for Thursday */
    public static final int THURSDAY  =    XanbooRuleTimeStateGuard.THURSDAY;
    /** Day (dayOfWeek) constant for Friday */
    public static final int FRIDAY    =    XanbooRuleTimeStateGuard.FRIDAY;
    /** Day (dayOfWeek) constant for Saturday */
    public static final int SATURDAY  =    XanbooRuleTimeStateGuard.SATURDAY;
    
    
    /** Month constant for any month of the year */
    public static final int MONTH_ANY=-1;
    /** Month constant for January */
    public static final int MONTH_JANUARY=1;
    /** Month constant for February */
    public static final int MONTH_FEBRUARY=2;
    /** Month constant for March */
    public static final int MONTH_MARCH=3;
    /** Month constant for April */
    public static final int MONTH_APRIL=4;
    /** Month constant for May */
    public static final int MONTH_MAY=5;
    /** Month constant for June */
    public static final int MONTH_JUNE=6;
    /** Month constant for July */
    public static final int MONTH_JULY=7;
    /** Month constant for August */
    public static final int MONTH_AUGUST=8;
    /** Month constant for September */
    public static final int MONTH_SEPTEMBER=9;
    /** Month constant for October */
    public static final int MONTH_OCTOBER=10;
    /** Month constant for November */
    public static final int MONTH_NOVEMBER=11;
    /** Month constant for December */
    public static final int MONTH_DECEMBER=12;

    public static final int REPEAT_NONE = 0;
    
    private int timeKeyword = -1; // specifies a keyword constant.
    
    /**
     * Holds value of property minute.
     */
    private int minute;
    
    /**
     * Holds value of property hour.
     */
    private int hour;
    
    /**
     * Holds value of property day.
     */
    private int day;
    
    /**
     * Holds value of property date.
     */
    private int date;
    
    /**
     * Holds value of property month.
     */
    private int month;
    
    private int hourRepetition;
    private int minRepetition;
    
    private boolean isWeekSchedule = false;  // Only one boolean is supported, but this way we can support
    private boolean isMonthSchedule = false; // a combined time format that contains both;
    
    /*
    public XanbooRuleTimeEventGuard( int minute, int hour, int day, int date, int month ) {
        super();
        this.minute = minute;
        this.hour = hour;
        this.day = day;
        this.date = date;
        this.month = month;
        this.isWeekSchedule = true;
        this.isMonthSchedule = true;
    }*/
    
    /*public static void main( String[] args ) {
        
        try {
            
            System.err.println("XanbooRuleTimeEventGuard");

            String[] gs = { "TIME.K.SUNRISE", 
                            "TIME.W.*d.2h.0m", 
                            "TIME.W.*d.*h.0m", 
                            "TIME.W.*d.*2h.0m", 
                            "TIME.W.*d.2*4h.0m",
                            "TIME.W.*d.2h.2m", 
                            "TIME.W.*d.2h.*m", 
                            "TIME.W.*d.2h.*2m", 
                            "TIME.W.*d.2h.2*4m" 
            };

            for (int i=0; i<gs.length; i++ ) {

                System.err.println("OLD: " + gs[i] );
                XanbooRuleTimeEventGuard g = new XanbooRuleTimeEventGuard( gs[i] );
                g.setLogicalOp( XanbooRuleGuard.OPERATOR_NONE );
                System.err.println("NEW: " + g.toString());
                System.err.println("");

            }

            XanbooRuleTimeEventGuard g = new XanbooRuleTimeEventGuard( 10, 10, 10 );
            System.err.println( g.toString());
            g.setMinuteRepetition( 20 );
            System.err.println( g.toString() );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
                        
    }//*/
    
    /** Creates a time event guard to trigger at a time specified by a constant
     * @param timeEvent one of time range constants, sunrise or sunset
     */
    public XanbooRuleTimeEventGuard(int timeEvent) {
        super();
        this.timeKeyword = timeEvent;
        this.minRepetition = REPEAT_NONE;
        this.hourRepetition = REPEAT_NONE;
    }
    
    /** Returns a constant specifying the keyword (sunrise/sunset) specified by this time event. -1 if none.
     */
    public int getTimeConstant() {
        return this.timeKeyword;
    }
    
    
    /** Creates a weekly time event guard 
     * @param minute value between 0 and 59
     * @param hour value between 0 and 23, or constant value HOUR_ANY
     * @param day value specified as one of DAY constants
     */
    public XanbooRuleTimeEventGuard(int minute, int hour, int day) {
        super();
        this.minute = minute;
        this.hour = hour;
        this.day = day;
        this.month = MONTH_ANY;
        this.isWeekSchedule = true;
        this.minRepetition = REPEAT_NONE;
        this.hourRepetition = REPEAT_NONE;
    }
    
    
    /** Creates a monthly time event guard 
     * @param minute value between 0 and 59
     * @param hour value between 0 and 23, or constant value HOUR_ANY
     * @param date value between 1 and 31, or constant value DATE_ANY
     * @param month value specified as one of MONTH constants
     */
    public XanbooRuleTimeEventGuard(int minute, int hour, int date, int month) {
        super();
        this.minute = minute;
        this.hour = hour;
        this.date = date;
        this.month = month;
        this.isMonthSchedule = true;
        this.minRepetition = REPEAT_NONE;
        this.hourRepetition = REPEAT_NONE;
    }
    
    /** Creates a new instance of XanbooRuleTimeEventGuard from internal oid string.
     * <br>This method is used internally and should be avoided by SDK developers !!!
     */
    protected XanbooRuleTimeEventGuard(String guard) {
        super();
        
        this.minRepetition = REPEAT_NONE;
        this.hourRepetition = REPEAT_NONE;

        for ( int i=0; i<KEYWORDS.length; i++ ) {
            if ( KEYWORDS[i].toUpperCase().equals( guard.toUpperCase() ) ) {
                this.timeKeyword = i;
                return;
            }
        }
        
        for ( StringTokenizer st = new StringTokenizer( guard, "." ); st.hasMoreTokens(); ) {
            String token = st.nextToken().trim();
            if ( token.equals( "TIME" ) ) {
                //nothing
            } else if ( token.equals( "W" ) ) {
                this.isWeekSchedule = true;
            } else if ( token.equals( "M" ) ) {
                this.isMonthSchedule = true;
            } else if ( token.endsWith("m")) {
                //minute = Integer.parseInt( token.substring( 0, token.length()-1 ) );
                String ss = token.substring( 0, token.length()-1 );
                int wcPos = token.indexOf('*'); // wildcard/asterix position
                if ( wcPos == -1 ) { // no wildcard
                    minute = Integer.parseInt( ss );
                    minRepetition = REPEAT_NONE;
                } else if ( ss.length() == 1 ) { // wildcard only (*)
                    minute = MINUTE_ANY;
                    minRepetition = REPEAT_NONE;
                } else { /// wildcard present
                    if ( wcPos > 0 ) { // start time (min) also present
                        minute = Integer.parseInt( ss.substring( 0, wcPos ) );
                    } else { // no start minute specified
                        minute = MINUTE_ANY;
                    }
                    if ( wcPos < (ss.length()-1) ) { // repetition specified
                        minRepetition =  Integer.parseInt( ss.substring( wcPos + 1 ));
                    } else { // no repetition
                        minRepetition = REPEAT_NONE; 
                    }
                }                
            } else if ( token.endsWith("h")) {
                String ss = token.substring( 0, token.length()-1 );
                int wcPos = token.indexOf('*'); // wildcard/asterix position
                if ( wcPos == -1 ) { // no wildcard
                    hour = Integer.parseInt( ss );
                    hourRepetition = REPEAT_NONE;
                } else if ( ss.length() == 1 ) { // wildcard only (*)
                    hour = HOUR_ANY;
                    hourRepetition = REPEAT_NONE;
                } else { /// wildcard present
                    if ( wcPos > 0 ) { // start time (hour) also present
                        hour = Integer.parseInt( ss.substring( 0, wcPos ) );
                    } else { // no start hour specified
                        hour = HOUR_ANY;
                    }
                    if ( wcPos < (ss.length()-1) ) { // repetition specified
                        hourRepetition =  Integer.parseInt( ss.substring( wcPos + 1 ));
                    } else { // no repetition
                        hourRepetition = REPEAT_NONE; 
                    }
                }
            } else if ( token.endsWith("d")) {
                if(token.indexOf('*')>-1)
                    day=-1;
                else
                    day = Integer.parseInt( token.substring( 0, token.length()-1 ) );
            } else if ( token.endsWith("D")) {
                if(token.indexOf('*')>-1)
                    date=-1;
                else
                    date = Integer.parseInt( token.substring( 0, token.length()-1 ) );
            } else if ( token.endsWith("M")) {
                if(token.indexOf('*')>-1)
                    month=-1;
                else
                    month = Integer.parseInt( token.substring( 0, token.length()-1 ) );
            }
        }
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
        
        if ( this.timeKeyword != -1 ) {
            // Keyword-based guard
            sb.append( KEYWORDS[this.timeKeyword] );
        } else {
            // Specific time specified event
            if ( !this.isWeekSchedule ) { //Monthly time
                sb.append( "TIME.M" );
            } else if ( !this.isMonthSchedule ) { //Weekly time
                sb.append( "TIME.W" );
            } else {
                sb.append( "TIME" ); //THIS IS NOT PART OF THE SPEC - should fail validation instead of this?
            }

            if ( this.isMonthSchedule ) {
                sb.append( "." ).append( formatVal( this.month ) ).append("M");
                sb.append( "." ).append( formatVal( this.date ) ).append("D");
            }

            if ( this.isWeekSchedule ) {
                sb.append( "." ).append( formatVal( this.day ) ).append("d");
            }

            sb.append( "." ).append( formatVal( this.hour ) );
            if ( this.hourRepetition != REPEAT_NONE ) {
                sb.append( this.hour == HOUR_ANY ? "" : "*" ).append( this.hourRepetition );
            }
            sb.append("h");
            
            sb.append( "." ).append( formatVal(this.minute) );
            if ( this.minRepetition != REPEAT_NONE ) {
                sb.append( this.minute == MINUTE_ANY ? "" : "*").append( this.minRepetition );
            }
            sb.append("m");

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
    
    private static String formatVal( int val ) {
        if ( val == -1 ) {
            return "*";
        } else {
            return Integer.toString( val );
        }
    }
    
    /**
     * Validates this guard for correct format and completeness.
     * 
     * @throws XanbooException if an error was found
     */    
    public void validate() throws XanbooException {
        if (  (this.minute < 0 || this.minute > 59) || (this.hour < -1 || this.hour > 23 ) ) {
            throw new XanbooException( 29060 );
        } else if ( this.isMonthSchedule ) {
            if ((this.date < -1 || this.date > 31 || this.date == 0) || (this.month < -1 || this.month > 12 || this.month == 0)) {
                throw new XanbooException( 29060 );
            }
        } else if ( this.isWeekSchedule ) {
            int[] days = { ANYDAY, SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY };
            boolean valid = false;
            for ( int i=0; i<days.length; i++ ) {
                if ( this.day == days[i] ) valid = true;
            }
            if ( !valid ) {
                throw new XanbooException( 29060 );
            }
        }
    }
    
    
    /**
     * Returns the minute value for the guard.
     * @return minute.
     */
    public int getMinute() {
        return this.minute;
    }
    
    /**
     * Returns the hour value for the guard.
     * @return minute.
     */
    public int getHour() {
        return this.hour;
    }
    
    /**
     * Returns the day of week value for the guard.
     * @return day of week.
     */
    public int getDay() {
        return this.day;
    }
    
    /**
     * Returns the date (day of month) value for the guard.
     * @return date day of month.
     */
    public int getDate() {
        return this.date;
    }
    
    /**
     * Returns the month value for the guard.
     * @return month.
     */
    public int getMonth() {
        return this.month;
    }
    
    /**
     * Sets the minute for the guard.
     * @param minute value between 0 and 59
     */
    public void setMinute( int minute ) {
        this.minute = minute;
    }
    
    /**
     * Sets the hour for the guard.
     * @param hour value between 0 and 23, or constant value HOUR_ANY
     */
    public void setHour( int hour ) {
        this.hour = hour;
    }
    
    /**
     * Sets the day of week for the guard.
     * @param day value specified as one of DAY constants
     */
    public void setDay( int day ) {
        this.isMonthSchedule = false;
        this.isWeekSchedule = true;
        this.day = day;
    }

    /**
     * Sets repetition interval for hour field (event repeats every x hours )
     */
    public void setHourRepetition( int rep ) throws XanbooException {
        if ( rep != REPEAT_NONE && (rep < 1 || rep > 12)) {
            throw new XanbooException( 29036, "Hourly repetition value out of range" );
        }
        this.hourRepetition = rep;
    }
    
    /**
     * Sets repetition interval for minute field (event repeats every x minutes )
     */
    public void setMinuteRepetition( int rep ) throws XanbooException {
        if ( rep != REPEAT_NONE && (rep < 10 || rep > 30) ) {
            throw new XanbooException( 29035, "Minute repetition value out of range" );
        }
        this.minRepetition = rep;
    }
    
    /**
     * Gets the hourly repetition interval for this guard.
     */
    public int getHourRepetition() {
        return this.hourRepetition;
    }
    
    /**
     * Gets the minute repetition interval for this guard.
     */
    public int getMinuteRepetition() {
        return this.minRepetition;
    }
    
    /**
     * Sets the date (day of month) for the guard.
     * @param date value between 1 and 31, or constant value DATE_ANY
     */
    public void setDate( int date ) {
        this.isMonthSchedule = true;
        this.isWeekSchedule = false;
        this.date = date;
    }
    
    /**
     * Sets the month for the guard.
     * @param month value specified as one of MONTH constants
     */
    public void setMonth( int month ) {
        this.isMonthSchedule = true;
        this.isWeekSchedule = false;
        this.month = month;
    }
    
    /**
     * Returns whether this is a monthly time event guard or not.
     * @return true if this is a monthly time event guard
     */
    public boolean isMonthly() {
        return this.isMonthSchedule;
    }
    
    /**
     * Returns whether this is a weekly time event guard or not.
     * @return true if this is a weekly time event guard
     */
    public boolean isWeekly() {
        return this.isWeekSchedule;
    }
    
}
