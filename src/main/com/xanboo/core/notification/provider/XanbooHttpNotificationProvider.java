/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.notification.provider;

import java.util.HashMap;

import com.xanboo.core.util.LoggerFactory;
import com.att.dlife.httplib.http.XanbooHttpResponse;
import com.xanboo.core.util.Logger;
import com.xanboo.core.notification.AbstractHttpNotificationProvider;
import com.xanboo.core.notification.NotificationDestination;
import com.xanboo.core.notification.NotificationMessageInterface;
import com.xanboo.core.notification.XanbooNotificationException;
import java.util.List;
/**
 *
 * @author Luther Maloney
 * @since October 2013
 */
public class XanbooHttpNotificationProvider extends AbstractHttpNotificationProvider
{
    private Logger logger = LoggerFactory.getLogger(getClass().getName());
    /**
    * Constructor
    */
    public XanbooHttpNotificationProvider()
    {

    }
    /**
    * Handle any custom initialization routines and call super class intialize method.
    * @param config 
    */
    @Override
    public void initialize(HashMap config)
    {
        super.initialize(config);
        logger.info("[initialize() ] Initialize ...");
    }
    /**
    * By default, all providers can accept multiple destinations
    * @return 
    */
    @Override
    public boolean canAcceptDestinationList()
    {
        return false;
    }
    @Override
    public boolean encodeParametersBeforeTemplate()
    {
        return true;
    }
    /**
    * Sends the notification message. 
    * 
    * @param destination
    * @param message 
    */
    @Override
    public void sendMessage(NotificationDestination destination,NotificationMessageInterface message) throws XanbooNotificationException
    {
        //default (current impl) ... the message from DLA already formatted in name=value&name=value&name=value
        try 
        {
            if ( logger.isDebugEnabled() )
            {
                logger.debug("[sendMessage()] - message parameters "+message);
                logger.debug("[sendMessage()] - destination is "+destination);
            }

            XanbooHttpResponse resp = null;
            if ( (destination.isDestinationURL() && destination.getDestinationAddress().indexOf('?') == -1) 
                    || (!destination.isDestinationURL() && getUrl().indexOf("?") == -1 ) ) 
                resp = super.sendPostRequest(message.getMessage());
            else 
                resp = super.sendGetRequest(message.getMessage());

            if(resp.isSuccess()) 
            {
                logger.info("[sendMessage()]HTTP Notification sent using url="+this.getUrl());
            }
            else 
            {
                logger.warn("[sendMessage()] - Error = "+resp.getResponseCode());
                throw new XanbooNotificationException("Error sending HTTP notification, errorcode="+resp.getResponseCode());
            }
        }
        catch(Exception e) 
        {
        logger.error("[sendMessage()] - Exception while sending HTTP notification ",e);
        throw new XanbooNotificationException("Exception while sending HTTP notification", e);
        }
    }

    @Override
    public void sendMessage(List<NotificationDestination> destinations,NotificationMessageInterface message) throws XanbooNotificationException 
    {
        throw new XanbooNotificationException("Provider can only accept one destingation per call");
    }

    @Override
    public void validate(NotificationMessageInterface message)
    {
        super.validate(message);
    }
}
