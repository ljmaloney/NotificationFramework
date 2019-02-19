/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ljm.notification;

import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author lm899p
 */
public class NotificationCustomMap extends HashMap<String,Object>
{
    public static final String URL = "url";
    public static final String RULE_ID = "ruleId";
    public static final String RULE_NAME = "ruleName";
    public static final String RULE_DEVICE_LIST = "ruleDeviceList";
    public static final String CUSTOM_DLA = "custDLA";
    public static final String ALARM_CODE = "alarmCode";
    public static final String RESPONSE_REQ = "responseReq";
    
    public NotificationCustomMap()
    {
        super();
    }
    public String get(String key)
    {
        if ( super.containsKey(key))
        {
            MapEntry entry = (MapEntry)super.get(key);
            return entry.value.toString();
        }
        return null;
    }
    
    @Override
    public Object get(Object key)
    {
        if ( super.containsKey(key))
        {
            MapEntry entry = (MapEntry)super.get(key);
            return entry.value;
        }  
        return null;
    }
    @Override
    public Object put(String key, Object val)
    {
       throw new RuntimeException("Please call the put(key, value, type) method");
    }
    /**
     * Adds a key / value pair to the Map and sets the datatype. The type field determines 
     * when quotes are needed / used in the generated JSON. 
     * @param key
     * @param val
     * @param type
     * @return 
     */
    public Object put(String key, Object val, DataType type)
    {
        MapEntry e = (MapEntry) super.put(key, new MapEntry(val,type));
        if ( e != null ) return e.value;
        return null;
    }
    /**
     * Creates a JSON formatted representation of the map. 
     * @return 
     */
    public String toJSON()
    {
        StringBuilder bldr = new StringBuilder();
        Set<String> keySet = super.keySet();
        for ( String key : keySet )
        {
            MapEntry entry = (MapEntry)super.get(key);
            
            if ( entry.value == null || entry.value.toString().equalsIgnoreCase(""))
                continue;
            
            if ( bldr.length() > 0 ) bldr.append(", ");
            
            bldr.append("\"").append(key).append("\":");
            switch ( entry.type )
            {
                case STRING : 
                case OTHER_USE_QUOTES : 
                    bldr.append("\"").append(entry.value.toString()).append("\"");
                    continue;
                default :
                    bldr.append(entry.value.toString());
            }
        }
        return bldr.toString();
    }
    
    private class MapEntry
    {
        Object value = null;
        DataType type = null;
        MapEntry(Object value, DataType type)
        {
            this.value=  value;
            this.type = type;
        }
    }
    
    public enum DataType
    {
        BOOLEAN,STRING, INTEGER, LONG, FLOAT, DOUBLE, OBJECT, OTHER_USE_QUOTES;
    }
}
