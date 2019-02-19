/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.notification.provider;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.mail.*;
import javax.mail.internet.*;
import com.xanboo.core.util.LoggerFactory;
import com.xanboo.core.notification.AbstractSMTPNotificationProvider;
import com.xanboo.core.notification.NotificationDestination;
import com.xanboo.core.notification.NotificationMessageInterface;
import com.xanboo.core.notification.XanbooNotificationException;
import com.xanboo.core.util.Logger;
/**
 * Default SMTP provider for sending notification to a phone number via email address(es)
 * 
 * @author Luther J maloney
 * @since October 2013
 */
public class XanbooSMTPSmsNotificationProvider extends AbstractSMTPNotificationProvider
{
    private Logger logger = LoggerFactory.getLogger(getClass().getName());
    /**
    * Constructor
    */
    public XanbooSMTPSmsNotificationProvider()
    {

    }

    @Override
    public void initialize(HashMap config)
    {
        super.initialize(config);
        logger.info("[initialize()] Initialize SMS over SMTP");
    }
    /**
    * Override validate to truncate the SMS message based on the MAXLEN field of the PROFILETYPE_REF table
    * @param message 
    */
    @Override
    public void validate(NotificationMessageInterface message)
    {
        //if MAX_LEN is defined, check the message length
        Integer maxLen = new Integer((String)getConfigMap().get(PROFILE_MAXLEN));
        if ( maxLen != null && !maxLen.equals(-1) && !maxLen.equals(0))
        {
            //a valid maxLen field 
            int maxLength = maxLen.intValue();
            if ( message.getMessage().length() > maxLength )
            {
                //truncate
                logger.warn("[validate()] - Message text (length="+message.getMessage().length()+") longer than "+maxLen+", truncate");
                logger.warn("[validate()] - Message to be truncated is "+message.getMessage());
                String newMessage = message.getMessage().substring(0,maxLength);
                message.setMessage(newMessage);
            }
        }
    }
    /**
    * Sends a message to an SMS/phone number via email address
    * @param destination
    * @param message 
    */
    @Override
    public void sendMessage(NotificationDestination destination,NotificationMessageInterface message)throws XanbooNotificationException
    {
        try
        {
            if ( logger.isDebugEnabled() )
            {
                logger.debug("[sendMessage()] - message parameters "+message);
                logger.debug("[sendMessage()] - destination is "+destination);
            }
            List<NotificationDestination> destList = new ArrayList<NotificationDestination>();
            destList.add(destination);
            //SMS destinations do not include a suffix by default ... 
            //append suffix for provider if not already present
            if ( destination.getDestinationAddress().indexOf("@") < 0 )
                destination.setDestinationAddress(destination.getDestinationAddress()+this.getProfileSuffix());
            //create InternetAddress instances 
            InternetAddress[] toAddr = this.getInternetAddress(destList);
            InternetAddress fromAddr = new InternetAddress(message.getFromAddress());
            sendEmail(message.getSubject(), message.getMessage(),toAddr,fromAddr);
            logger.info("[sendMessage()]: SMS over SMTP notification sent to "+destination.getDestinationAddress());
        }
        catch(XanbooNotificationException ne)
        {
            throw ne;
        }
        catch(AddressException ae)
        {
            logNotificationError(destination,message, "Invalid notification destination (email address)", ae);
            logger.warn("[sendMessage()] - Invalid notification destination (email address)", ae);
            throw new XanbooNotificationException("Invalid notification email address "+destination,ae);
        }
        catch(java.security.NoSuchProviderException nspe)
        {
            logNotificationError(destination,message,"NoSuchProviderException",nspe);
            logger.error("[sendMessage()] - NoSuchProviderException occured when sending notification", nspe);
            throw new XanbooNotificationException("NoSuchProviderException occured when sending notification",nspe);
        }
        catch(MessagingException me)
        {
            logNotificationError(destination,message,"MessageException",me);
            logger.error("[sendMessage()] - MessagingException occured when sending notification", me);
            throw new XanbooNotificationException("MessagingException occured when sending notification",me);
        }
        catch(Exception ex)
        {
            logNotificationError(destination,message,"Exception",ex);
            logger.error("[sendMessage()] - An exception occured when sending notification", ex);
            throw new XanbooNotificationException("An exception occured when sending notification",ex);
        }
    }
}
