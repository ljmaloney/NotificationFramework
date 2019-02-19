/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.notification;

import java.util.HashMap;

/**
 *
 * @author lm899p
 */
public class XanbooResponseMap extends HashMap<String,Object>
{
    public static final String RESPONSE_CODE = "RESPONSE_CODE";
    public static final String SUCCESS = "SUCCESS";
    public static final String DESTINATIONS = "DESTINATIONS";
    
    public XanbooResponseMap()
    {
        super();
    }
    
    public Integer getResponseCode()
    {
        if ( containsKey(RESPONSE_CODE))
            return (Integer)super.get(RESPONSE_CODE);
        return 0;
    }
    public void setResponseCode(Integer code)
    {
        super.put(RESPONSE_CODE,code);
    }
    
    public Boolean getSuccess()
    {
        if ( containsKey(SUCCESS))
            return (Boolean)super.get(SUCCESS);
        return Boolean.FALSE;
    }
    public void setSuccess(Boolean success)
    {
        super.put(SUCCESS, success);
    }
    public String getDestinations()
    {
        if ( containsKey(DESTINATIONS))
            return (String)super.get(DESTINATIONS);
        return null;
    }
    public void setDestinations(String dest)
    {
        super.put(DESTINATIONS,dest);
    }
    
    public Object get(String key)
    {
        if ( key.equals(RESPONSE_CODE))
            return getResponseCode();
        if ( key.equals(SUCCESS))
            return getSuccess();
        if ( key.equals(DESTINATIONS))
            return getDestinations();
        
        return super.get(key);
    }
    @Override
    public Object put(String key,Object obj)
    {
        if ( key.equals(RESPONSE_CODE)|| key.equals(SUCCESS)|| key.equals(DESTINATIONS))
            throw new RuntimeException("Attempting to set value for "+RESPONSE_CODE+" or "+SUCCESS+" or "+DESTINATIONS+". Please use appropriate method");
        
        return super.put(key,obj);
    }
    
    @Override
    public String toString()
    {
        return "XanbooResponseMap["+super.toString()+"]";
    }
}
