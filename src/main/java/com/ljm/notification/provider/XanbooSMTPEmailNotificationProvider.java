/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ljm.notification.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.mail.*;
import javax.mail.internet.*;
import com.xanboo.core.util.LoggerFactory;
import com.ljm.notification.AbstractSMTPNotificationProvider;
import com.ljm.notification.NotificationDestination;
import com.ljm.notification.NotificationMessageInterface;
import com.ljm.notification.NotificationException;
import com.xanboo.core.util.Logger;
/**
 * Default SMTP (Simple Mail Transport Protocol) notification provider. This class sends notifications
 * to defined email addresses. Attachements can be included. 
 * 
 * @author Luther Maloney
 * @since October 2013
 */
public class XanbooSMTPEmailNotificationProvider extends AbstractSMTPNotificationProvider
{
    private boolean canHaveAttachments = true;
    private Logger logger = LoggerFactory.getLogger(getClass().getName());
    /**
    * Constructor.
    */
    public XanbooSMTPEmailNotificationProvider()
    {

    }
    /**
    * Initialize the provider
    * @param config 
    */
    @Override
    public void initialize(HashMap config)
    {
        super.initialize(config);
        //flag for attachements
        String attachment = config.get(PROFILE_ATTACH).toString();
        canHaveAttachments = attachment.equals("1") ? true : false;
        logger.info("[initialize()] - configure instance of XanbooSMTPEmailNotificationProvider, canHaveAttachments="+this.canHaveAttachments);
    }
    /**
    * Sending to a normal email address, returns true (can send html email)
    * @param contentType
    * @return 
    */
    @Override
    public boolean canAccept(String contentType)
    {
        return true;	
    }
    /**
    * No length validations when sending to email address
    * @param message 
    */
    @Override
    public void validate(NotificationMessageInterface message)
    {
        //no length validations for notifications sent to email address.
    }
    /**
    * Overrides <code>AbstractNotificationProvider.sendMessage()</code>. Email can handle a list of addresses. 
    * @param destinations
    * @param message 
    */
    @Override
    public void sendMessage(List<NotificationDestination> destinations,NotificationMessageInterface message)throws NotificationException
    {
        LoggerFactory.getLogger(getClass().getName()).debug("[sendMessage()] - message parameters "+message.toString());
        try
        {
            //create InternetAddress instances
            InternetAddress[] toAddr = this.getInternetAddress(destinations);
            InternetAddress fromAddr = new InternetAddress(message.getFromAddress());
            if ( !canHaveAttachments || (message.getAttachments() == null || message.getAttachments().isEmpty()) )
            {
                if ( logger.isDebugEnabled() )
                    logger.debug("[sendMessage()] - Send notification message with no attachement");
                sendEmail(message.getSubject(), message.getMessage(),message.getContentType(), toAddr, fromAddr,null);
            }
            else //if attachments are present and can be sent .. 
            {
                List<javax.activation.DataSource> fileDataSourceList = this.getAttachmentDataSource(message.getAttachments());
                javax.activation.DataSource[] fileDSArray = (javax.activation.DataSource[])fileDataSourceList.toArray(new javax.activation.DataSource[1]);
                if ( logger.isDebugEnabled())
                    logger.debug("[sendMessage()] - Send notification message with "+fileDSArray.length+" attachements");
                sendEmail(message.getSubject(),message.getMessage(),message.getContentType(),toAddr,fromAddr,fileDSArray);
            }
            StringBuffer destBuffer = new StringBuffer();
            for ( NotificationDestination dest : destinations )
            {
                destBuffer.append(dest.getDestinationAddress());
                destBuffer.append(";");
            }
            logger.info("[sendMessage()] Notification email message "+message.getSubject()+" sent to "+destBuffer.toString());
        }
        catch(NotificationException ne)
        {
            throw ne;
        }
        catch(AddressException ae)
        {
            logNotificationError(null,message, "Invalid notification destination (email address)", ae);
            logger.warn("[sendMessage()] - Invalid notification destination (email address)", ae);
            throw new NotificationException("Invalid notification email address",ae);
        }
        catch(java.security.NoSuchProviderException nspe)
        {
            logNotificationError(null,message,"NoSuchProviderException",nspe);
            logger.error("[sendMessage()] - NoSuchProviderException occured when sending notification", nspe);
            throw new NotificationException("NoSuchProviderException occured when sending notification",nspe);
        }
        catch(MessagingException me)
        {
            logNotificationError(null,message,"MessageException",me);
            logger.error("[sendMessage()] - MessagingException occured when sending notification", me);
            throw new NotificationException("MessagingException occured when sending notification",me);
        }
        catch(Exception ex)
        {
            logNotificationError(null,message,"Exception",ex);
            logger.error("[sendMessage()] - An exception occured when sending notification", ex);
            throw new NotificationException("An exception occured when sending notification",ex);
        }
    }
    /**
    * Implementation of <code>sendMessage</code> to call <code>sendMessage(List<NotificationDestination> destList,NotificationMessageInterface message)</code>
    * 
    * @param destination
    * @param message 
    */
    @Override
    public void sendMessage(NotificationDestination destination,NotificationMessageInterface message)throws NotificationException
    {
        //place the destination into the List
        List<NotificationDestination> destList = new ArrayList<NotificationDestination>();
        destList.add(destination);
        //send the message
        sendMessage(destList,message);
    }	
}