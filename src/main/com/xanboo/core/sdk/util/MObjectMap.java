/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/util/MObjectMap.java,v $
 * $Id: MObjectMap.java,v 1.7 2003/12/12 20:46:35 guray Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.util;

//import com.xanboo.core.sdk.util.XanbooResultSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Set;

/**
 * <strong>MObjHandler</strong> is a convenience class that allows the retrieval of managed object values, pending values & labels from
 * a XanbooResultSet by manged object ID.
 */
public class MObjectMap {

    HashMap oidmap;
    
    /* Creates a new managed object handler which allows direct retrieval of managed object values by name.
     * @param mobjs A XanbooResultSet of managed objects
     */
    public MObjectMap( XanbooResultSet mobjs) {
        
        this.oidmap = new HashMap();
        for (int i=0; i < mobjs.size() ; i++) {
            this.oidmap.put( (String)((HashMap)mobjs.get(i)).get("MOBJECT_ID"), mobjs.get(i) );
        }
        
    }
    
    /* Returns the current value of a particular managed object by name;
     * @param mobjName The name of the managed object to reference.
    */
    public String getValue( String mobjName ) {
        try{
            return (String) ( (HashMap)oidmap.get(mobjName) ).get("VALUE") ;
        } catch ( NullPointerException ne ) {
            return null;
        }
    }
    
    /* Returns the pending value of a particular managed object by name;
     * @param mobjName The name of the managed object to reference.
    */
    public String getPending( String mobjName ) {
        try {
            return (String) ( (HashMap)oidmap.get(mobjName) ).get("PENDING_VALUE") ;
        } catch ( NullPointerException ne ) {
            return null;
        }
    }


    /* Returns a hashmap of values related to a particular managed object.
     * @param mobjName The name of the managed object to reference.
    */
    public HashMap get( String mobjName ) {
        try {
            return (HashMap) oidmap.get(mobjName);
        } catch ( NullPointerException ne ) {
            return null;
        }
    }
    
    
    /* Returns the type of a particular managed object by name;
     * @param mobjName The name of the managed object to reference.
    */
    public String getType( String mobjName ) {
        try {
            return (String) ( (HashMap)oidmap.get(mobjName) ).get("TYPE") ;
        } catch ( NullPointerException ne ) {
            return null;
        }
    }
        
    
    /* Returns the pending value of a particular managed object by name;
     * @param mobjName The name of the managed object to reference.
    */
    public String getLabel( String mobjName ) {
        try {
            return (String) ( (HashMap)oidmap.get(mobjName) ).get("LABEL") ;
        } catch ( NullPointerException ne ) {
            return null;
        }
    }

    /* Returns the access method of a particular managed object by name;
     * @param mobjName The name of the managed object to reference.
    */
    public String getAccess( String mobjName ) {
        try {
            return (String) ( (HashMap)oidmap.get(mobjName) ).get("ACCESS_METHOD") ;
        } catch ( NullPointerException ne ) {
            return null;
        }
    }
    
    /* Returns the access id of a particular managed object by name;
     * @param mobjName The name of the managed object to reference.
    */
    public String getAccessType( String mobjName ) {
        try {
            String accessId = (String) ( (HashMap)oidmap.get(mobjName) ).get("ACCESS_ID") ;
            return accessId;
        } catch ( NullPointerException ne ) {
            return null;
        }
    }

    /*
     * Returns the device actions published by this device.
     *<br>
     * Device actions are actions which may be configured to be trigged by the occurance of an event on this, or another device. <br>
     * For example, you may want to configure an image to be captured on camera 1 when a contact sensor gets triggered. <br>
     * This method returns a HashMap array of published actions. Columns in the HashMap are 'OID', 'VALUE' & 'LABEL'
     * 
     * @return HashMap of published actions, or null if there are none.
     */
    public HashMap[] getPublishedActions(  ) {
        
        String as = this.getValue( "40" );
        if (as == null || as.length() < 1 || (as.indexOf("=") == -1)) {
            return null;
        }
        
        ArrayList actions = new ArrayList();
        
        java.util.StringTokenizer st = new StringTokenizer( as, ",");

        while (st.hasMoreTokens()) {
                StringTokenizer st2 = new StringTokenizer( st.nextToken(), "=" );
                if ( st2.countTokens() == 2) {
                        HashMap action = new HashMap();
                        String oid = st2.nextToken();
                        action.put( "OID", oid );
                        action.put( "VALUE", st2.nextToken() );
                        action.put( "LABEL", this.getLabel( oid ) );
                        actions.add( action );
                }
        }
        
        return (HashMap[])actions.toArray(new HashMap[actions.size()]);        
        
    }
    
        /* Returns the timeStamp of a particular managed object by name;
     * @param mobjName The name of the managed object to reference.
    */
    public String getTimeStamp( String mobjName ) {
        try {
            return (String) ( (HashMap)oidmap.get(mobjName) ).get("TIMESTAMP") ;
        } catch ( NullPointerException ne ) {
            return null;
        }
    }

    private boolean isEqual( String oid, String value, String val ) {
        String type = this.getType( oid );
        boolean retVal = false;
        
        try {
            if ( value.equals( val ) ) {
                //They're equal!
                retVal = true;
            } else if ( type.startsWith( "enum:" ) ) {
                //We have an enumeration
                type = type.substring( 5 );
                if ( val.startsWith( "@" ) ) {
                    //We've been supplied an index - translate to enumerated value
                    int index = Integer.parseInt( val.substring(1) );
                    int count = 0;
                    for (java.util.StringTokenizer st = new java.util.StringTokenizer( type, ","); st.hasMoreTokens();) {
                        String token = st.nextToken();
                        if ( count == index ) {
                            val = token;
                        }
                        count++;
                    }
                }

                if ( value.startsWith( "@" ) ) {
                    //We're comparing to an index - translate to enumerated value
                    int index = Integer.parseInt( value.substring(1) );
                    int count = 0;
                    for (java.util.StringTokenizer st = new java.util.StringTokenizer( type, ","); st.hasMoreTokens();) {
                        String token = st.nextToken();
                        if ( count == index ) {
                            value = token;
                        }
                        count++;
                    }
                }

                if ( val.equals(value) ) {
                    retVal = true;
                }

            }
        } catch (Exception e) {
            //Just return false if we run into problems
        }
        
        return retVal;        
        
    }

    /* Specifies whether the current value of specified oid is equal to the supplied value.
     * <br>
     * This method is particularly useful for determining the value of enumeration oids.
     * @param oid The ID of the mangged object to query
     * @param val The value to be compared. In the case of enumerations, this may be either the text value or the index.
     * @return boolean true if the value is equal.
    */
    public boolean isValue( String oid, String val ) {
        return this.isEqual( oid, this.getValue( oid ), val );
        
    }
    
    /* Specifies whether the pending value of specified oid is equal to the supplied value.
     * <br>
     * This method is particularly useful for determining the value of enumeration oids.
     * @param oid The ID of the mangged object to query
     * @param val The value to be compared. In the case of enumerations, this may be either the text value or the index.
     * @return boolean true if the value is equal.
    */
    public boolean isPending( String oid, String val ) {
        return this.isEqual( oid, this.getPending( oid ), val );
        
    }    
    
    
    /* Returns a set containing all Managed Object IDs (OIDs) stored in this map.
     *
     * @return Set of oids.
     */
    public Set keySet() {
        return this.oidmap.keySet();
    }
    
    /* Converts an indexed value into an enumerated value */
    /*
     *
     * These methods are for retrieving the text value of managed object enumerations rather than the index.
     * I'm commenting them as after writing them, I realise that I need to put this code in the UI, not in here.
     * But, I'll keep them commented in here, just in case they become a requirement.
     * Or, we this can just be trashed if we so choose.
     *
    private String getEnum( String mobjName, String val ) {
        try {
            String type = this.getType( mobjName );
            type = type.substring( 5 );
            if ( val.startsWith( "@" ) ) {
                //We've been supplied an index - translate to enumerated value
                int index = Integer.parseInt( val.substring(1) );
                int count = 0;
                for (java.util.StringTokenizer st = new java.util.StringTokenizer( type, ","); st.hasMoreTokens();) {
                    String token = st.nextToken();
                    if ( count == index ) {
                        val = token;
                    }
                    count++;
                }
            }
            
            return val;
            
        } catch ( NullPointerException ne ) {
            return null;
        }
    }    
    
    /* Returns the pending value of an enumerated managed object.
     * Note that getPending may return an index. This method will aways return the text value.
     * @param mobjName The name of the managed object to reference.
    * /
    
    public String getEnumValue( String mobjName ) {
        return this.getEnum( mobjName, this.getValue( mobjName ) );
    }    
    
    /* Returns the pending value of an enumerated managed object.
     * Note that getPending may return an index. This method will aways return the text value.
     * @param mobjName The name of the managed object to reference.
    * /
    public String getEnumPending( String mobjName ) {
        return this.getEnum( mobjName, this.getPending( mobjName ) );
    }
    */
    
    /* Returns the public method of a particular managed object by name;
     * @param mobjName The name of the managed object to reference.
    */
    public String getPublic( String mobjName ) {
        try {
            return (String) ( (HashMap)oidmap.get(mobjName) ).get("ISPUBLIC") ;
        } catch ( NullPointerException ne ) {
            return null;
        }
    }
}