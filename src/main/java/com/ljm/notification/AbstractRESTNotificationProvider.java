/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ljm.notification;

import java.util.*;

import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.net.*;
import com.att.dlife.httplib.http.*;
/**
 * Abstract class to contain any methods common to all REST clients
 * 
 * @author Luther Maloney
 * @since October 2013
 */
public abstract class AbstractRESTNotificationProvider extends AbstractHttpNotificationProvider
{
    private boolean removeNewline = false;
	/**
	 * Initialization routines for this class.
	 * @param config 
	 */
	@Override
	public void initialize(HashMap config)
	{
		super.initialize(config);
		LogManager.getLogger(getClass().getName()).info("[initialize()] Initialize REST Provider");
        String remNewLine = config.containsKey("removenewline") ? (String)config.get("removenewline") : "false";
        removeNewline = new Boolean(remNewLine);
	}
    
    public abstract String getAcceptHeader();
    
    public abstract String getContentTypeHeader();
    
    public abstract Map<String,String> getRequestHeaders();
    
    @Override
    public void validate(NotificationMessageInterface message)
    {
        super.validate(message);
        //if the above doesnt throw a runtime exception
        if ( removeNewline )
        {
            StringBuilder sb = new StringBuilder();
            char[] chars = message.getMessage().toCharArray();
            for ( int i = 0; i < chars.length; i++ )
            {
                if ( chars[i] == '\n' || chars[i] == '\r')
                {
                    sb.append(' ');
                    continue;
                }
                sb.append(chars[i]);
            }
            message.setMessage(sb.toString());
        }
    }
    
    protected XanbooHttpResponse sendGetRequest(String queryString) throws MalformedURLException, IOException, Exception
	{
        throw new RuntimeException("No implementation");
    }
    
    protected XanbooHttpResponse sendPostRequest(String queryString,String requestData)throws MalformedURLException, IOException, Exception
    {
    	LogManager.getLogger(getClass().getName()).debug("[sendPostRequest()] - send notification using "+this.getUrl()+" requestData="+requestData);
        
        XanbooHttpHeader<String,String> headers = new XanbooHttpHeader<String,String>();
        if ( getRequestHeaders() != null )
            headers.putAll(getRequestHeaders());
        headers.put(XanbooHttpHeader.CONTENT_TYPE,getContentTypeHeader());
        headers.put(XanbooHttpHeader.ACCEPT,getAcceptHeader());
        if ( LogManager.getLogger(getClass().getName()).isDebugEnabled())
        	LogManager.getLogger(getClass().getName()).debug("[sendPostRequest()] - http headers = "+headers.toString());
        XanbooHttpRESTConnector connector = new XanbooHttpRESTConnector();
        return connector.postRequest(this.getUrl(),headers,null, requestData,true);
    }
    
	/**
	 * @param message 
	 */
	public void signMessage(NotificationMessageInterface message)
	{
		
	}
    
    private String processResponse(InputStream is) throws IOException
    {
        InputStreamReader isr = new InputStreamReader(is);
        StringBuilder out = new StringBuilder();
        char[] chars = new char[1024];
        int cnt = -1;
        while ( (cnt = isr.read(chars,0,chars.length)) > -1 )
        {
            out.append(chars,0,cnt);
        }
        return out.toString();
    }
}
