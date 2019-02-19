/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.xanboo.core.notification;

import java.util.HashMap;
import java.util.Map;
import com.xanboo.core.util.LoggerFactory;
import com.xanboo.core.util.Logger;
import java.io.IOException;
import com.att.dlife.httplib.http.XanbooHttpResponse;

/**
 *
 * @author lm899p
 */
public abstract class AbstractJsonNotificationProvider extends AbstractRESTNotificationProvider
{
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String ACCEPT_TYPE_JSON = "application/json";

    /**
    * Initialization routines for this class.
    * @param config 
    */
    @Override
    public void initialize(HashMap config)
    {
        super.initialize(config);
        LoggerFactory.getLogger(getClass().getName()).info("[initialize()] Initialize JSON Provider");
    }

    @Override
    public String getAcceptHeader()
    {
        return ACCEPT_TYPE_JSON;
    }

    @Override
    public String getContentTypeHeader()
    {
        return CONTENT_TYPE_JSON;
    }

    @Override
    public Map<String,String> getRequestHeaders()
    {
        return null;
    }

    public abstract String createJSON(NotificationDestination destination,NotificationMessageInterface message);
    
    public abstract void processResponse(Integer responseCode,Boolean success,String content,NotificationMessageInterface message);

    @Override
    public void sendMessage(NotificationDestination destination,NotificationMessageInterface message)throws XanbooNotificationException
    {
        String jsonRequestData = createJSON(destination,message);
        
        Logger logger = LoggerFactory.getLogger(getClass().getName());
        if(logger.isDebugEnabled()) {
            logger.debug("[sendMessage()] JSON request data : "+jsonRequestData);
        }
    
        try 
        {
            XanbooHttpResponse response = sendPostRequest(null,jsonRequestData);

            if ( response.isSuccess() ) 
            {
                logger.debug("[sendMessage()] Successful response ("+response.getContent()+") sending message to "+destination.getDestinationAddress());
            }
            else 
            {
                logger.warn("[sendMessage()] Error response ("+response.getContent()+") sending message to "+destination.getDestinationAddress());
            }
            processResponse(response.getResponseCode(),response.isSuccess(),response.getContent(),message);
        }
        catch(XanbooNotificationException xe) 
        {
            throw xe;
        }
        catch(IOException ioe) 
        {
            logNotificationError(destination, message, "IOException sending notification. JSON Request="+jsonRequestData, ioe);
            throw new XanbooNotificationException("IOException sending notification. JSON Request="+jsonRequestData, ioe);
        }
        catch(Exception e) 
        {
            logNotificationError(destination, message, "Exception sending notification", e);
            throw new XanbooNotificationException("Exception sending notification", e);
        }
    }
}
