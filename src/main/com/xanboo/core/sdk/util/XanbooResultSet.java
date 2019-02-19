/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/util/XanbooResultSet.java,v $
 * $Id: XanbooResultSet.java,v 1.8 2003/04/04 17:42:07 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.util;

import java.util.*;
import java.text.SimpleDateFormat;

/**
 *<P>This class is returned by all SDK query methods that might retrieve one or more records (resultset) from the database.<br>
 *  The <code>XanbooResultSet</code> is implemented as an <code>ArrayList</code> of <code>HashMap</code> objects. 
 *  Each <code>HashMap</code> object holds key/value pairs of type String that correspond to the query specific database fields and values.</P>
 *
 * See <a href="{@docroot}/tutorial.html#Example-XanbooResultSet">XanbooResultSet SDK Example</a>
 *
 * @see java.util.ArrayList
 * @see java.util.HashMap
 */
public class XanbooResultSet extends java.util.ArrayList {
    
    public static final int UNDEFINED = -999;

    private int tSize=0;
    private SimpleDateFormat sdf=null;
    
    /** Creates a new XanbooResultSet object */
    public XanbooResultSet() { }

    
    /**
     * Sets the total number of records for the resultset. 
     *
     * @param size the total size/number of records for the resultset.
     */
    public void setSize(int size) { tSize=size; }
    
    
    /**
     * Returns the total number of records for the whole query. If the query is a paged query, this
     * method is used to return the total query result count 
     *
     * @return the total size/number of records
     */
    public int getSize() { return  tSize;  }
    
    
    
    /**
     * Convenience method to retrieve a specific row attribute value of the XanbooResultSet.
     *
     * @param row requested XanbooResultSet row number 
     * @param attrib requested attribute name of the specified row
     *
     * @return a String object reference to the requested row/attribute value, null if value not found.
     */
    public String getElementString(int row, String attrib) {
        if(row<0 || row>=this.size()) return null;
        
        try {
            HashMap rec = (HashMap) this.get(row);
            return (String) rec.get(attrib);
        }catch(Exception e) {
            return null;
        }
        
    }

    
    
    /**
     * Convenience method to retrieve a specific row attribute (of type int) value of the XanbooResultSet.
     *
     * @param row requested XanbooResultSet row number 
     * @param attrib requested attribute name of the specified row
     *
     * @return an integer value. XanbooResultSet.UNDEFINED if value not found or the value is an invalid integer.
     */
    public int getElementInteger(int row, String attrib) {
        try {
            return Integer.parseInt(getElementString(row, attrib));
        }catch(Exception e) {
            return UNDEFINED;
        }
    }
    

    /**
     * Convenience method to retrieve a specific row attribute (of type long) value of the XanbooResultSet.
     *
     * @param row requested XanbooResultSet row number 
     * @param attrib requested attribute name of the specified row
     *
     * @return a long value. XanbooResultSet.UNDEFINED if value not found or the value is an invalid integer.
     */
    public long getElementLong(int row, String attrib) {
        try {
            return Long.parseLong(getElementString(row, attrib));
        }catch(Exception e) {
            return UNDEFINED;
        }
    }

    
    /**
     * Convenience method to retrieve a specific row attribute (of type double) value of the XanbooResultSet.
     *
     * @param row requested XanbooResultSet row number 
     * @param attrib requested attribute name of the specified row
     *
     * @return a double value. XanbooResultSet.UNDEFINED if value not found or the value is an invalid integer.
     */
    public double getElementDouble(int row, String attrib) {
        try {
            return Double.parseDouble(getElementString(row, attrib));
        }catch(Exception e) {
            return UNDEFINED;
        }
    }

    
    
    /**
     * Convenience method to retrieve a specific row attribute (of type Date) value of the XanbooResultSet.
     *
     * @param row requested XanbooResultSet row number 
     * @param attrib requested attribute name of the specified row
     *
     * @return a reference to a Date object containing the attribute value, null if value not found. The Date
     *         value is assumed to be in GMT timezone.
     */
    public Date getElementDate(int row, String attrib) {
        try {
            if(sdf==null) {
                sdf = new SimpleDateFormat("yyyy-MM-dd H:mm:ss");
                sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            }
            return sdf.parse(getElementString(row, attrib));
        }catch(Exception e) {
            return null;
        }
    }
    
    
    
    /**
     *  Converts the XanbooResultSet query results into XML representation.
     *
     * @return <code>String</code> - XML result
     */
    public String toXML() {
        StringBuffer xml = new StringBuffer();        

        if(this.size()==0) return "";

        HashMap row = (HashMap) this.get(0);
        
        Set keys = row.keySet();
        Object[] skeys = keys.toArray();
        Arrays.sort(skeys);
        
        // Set up XML declaration and root element
        xml.append("<XanbooRS nrows=\"");
        xml.append(this.size());
        xml.append("\" ");
        xml.append(" trows=\"");
        xml.append(this.getSize());
        xml.append("\">\n");

        try {
            for(int i=0; i<this.size(); i++) {
                row = (HashMap) this.get(i);
                xml.append("  <row id=\"").append(i+1).append("\">\n");
                for(int j=0; j<skeys.length; j++) {
                    String key = (String) skeys[j];
                    xml.append("    <").append(key).append(">");
                    xml.append(row.get(key).toString());
                    xml.append("</").append(key).append(">\n");
                }
                xml.append("  </row>\n");
            }
        }catch(Exception e) {
            
        }

        xml.append("</XanbooRS>\n");

        return xml.toString();
    }
    
    /**
     * Returns an XML Representation of just the column names that exist in the results.
     *
     * @param rs <code>ResultSet</code> to use as input.
     * @return <code>String</code> - XML result
     */
    public String getColumnNames() {
        StringBuffer xml = new StringBuffer();
        
        if(this.size()==0) return "";
        
        xml.append("<XanbooRSColumnNames>");
        HashMap row = (HashMap) this.get(0);
        Set keys = row.keySet();
        Object[] skeys = keys.toArray();
        Arrays.sort(skeys);
        for (int i=0; i<skeys.length; i++) {
            xml.append("    <COLUMN>").append( (String)skeys[i] ).append("</COLUMN>\n");
        }
        xml.append("</XanbooRSColumnNames>");
        return xml.toString();
    }


}
