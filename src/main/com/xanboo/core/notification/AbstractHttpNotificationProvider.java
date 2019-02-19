/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
*/
package com.xanboo.core.notification;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Iterator;
import com.xanboo.core.util.LoggerFactory;
import com.att.dlife.httplib.http.XanbooHttpConnector;
import com.att.dlife.httplib.http.XanbooHttpResponse;

/**
* Abstract class to provide a base for the implementation of notification providers using the HTTP protocols. 
* 
* @author Luther J Maloney
* @since October 2013
*/
public abstract class AbstractHttpNotificationProvider extends AbstractNotificationProvider
{
    //** the http url used to send the notification **/
    private String httpUrl = null;
    
    @Override
    public void initialize(HashMap configMap)
    {
        //retrieve the HTTP URL from either PROFILE_SUFFIX column or HttpUrl parameter in the 
        //config map
        super.initialize(configMap);
        httpUrl = (String)configMap.get(PROFILE_SUFFIX);
        if ( httpUrl == null )
            httpUrl = (String)configMap.get("HttpUrl");
        LoggerFactory.getLogger(getClass().getName()).info("[initialize()] - Provider initialized with [httpUrl="+httpUrl+"]");
    }

    public String getUrl()
    {
        return this.httpUrl;
    }
    /**
    * Encodes the parameter as UTF-8 format. 
    * @param message
    * @return 
    */
    @Override
    public String encodeMessage(String message)
    {
        try
        {
            return URLEncoder.encode(message, "UTF-8");
        }
        catch (UnsupportedEncodingException ex)
        {
            ex.printStackTrace();
            return null;
        }
    }

    /**
    * This method generates the HTTP query string using key=value pairs from the Hashmap, for example<br>
    * paramName=value&paramName2=value2&paramName3=value3<br>
    * Also encodes the values for the parameters in UTF-8 format. 
    * @param queryProperties
    * @return 
    */
    public String generateQueryString(HashMap queryProperties)
    {
        StringBuilder str = new StringBuilder();
        Iterator keyIterator = queryProperties.keySet().iterator();
        while ( keyIterator.hasNext() )
        {
            String key = (String)keyIterator.next();
            String value= queryProperties.get(key).toString();
            if ( str.length() > 0)
                str.append("&");
            str.append(encodeMessage(key));
            str.append("=");
            str.append(encodeMessage(value));
        }
        return str.toString();
    }

    protected XanbooHttpResponse sendGetRequest(String queryString) throws MalformedURLException, IOException, Exception
    {	
        LoggerFactory.getLogger(getClass().getName()).debug("Send notification GET request "+httpUrl);
        LoggerFactory.getLogger(getClass().getName()).debug("[sendGetRequest()] - query string is "+queryString);
        XanbooHttpConnector xanbooHttpConnector = new XanbooHttpConnector();
        XanbooHttpResponse resp = null;

        long startTime = System.currentTimeMillis();
        resp = xanbooHttpConnector.get(httpUrl, queryString, true, true);
        long stopTime = System.currentTimeMillis();
        if ( LoggerFactory.getLogger(getClass().getName()).isDebugEnabled())
            LoggerFactory.getLogger(getClass().getName()).debug("Elapsed time to send GET request to "+httpUrl+" is "+(stopTime-startTime)+" milliseconds");

        return resp;
    }

    protected XanbooHttpResponse sendPostRequest(String queryString)throws MalformedURLException, IOException, Exception
    {
        if ( LoggerFactory.getLogger(getClass().getName()).isDebugEnabled())
        {
            LoggerFactory.getLogger(getClass().getName()).debug("Send notification POST request "+httpUrl);	
            LoggerFactory.getLogger(getClass().getName()).debug("[sendPostRequest()] - query string is "+queryString);
        }
        XanbooHttpConnector xanbooHttpConnector = new XanbooHttpConnector();
        XanbooHttpResponse resp = null;

        long startTime = System.currentTimeMillis();
        resp = xanbooHttpConnector.post(httpUrl, queryString, true, true);

        long stopTime = System.currentTimeMillis();
        
        if ( LoggerFactory.getLogger(getClass().getName()).isDebugEnabled())
            LoggerFactory.getLogger(getClass().getName()).debug("Elapsed time to send POST request to "+httpUrl+" is "+(stopTime-startTime)+" milliseconds");

        return resp;
    }
}
