package com.ljm.notification;


import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.FileDataSource;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.logging.log4j.LogManager;

import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * Abstract class containing common methods for sending email notifications. 
 * Properties expected in the config HashMap are:<br/>
 * <li>DEBUG.EMAIL
 * <li>EMAIL.HOST.NAME
 * <li>EMAIL.JNDI.NAME
 * <li>PASSWORD
 * <li>TRANSPORT.PROTOCOL
 * <li>USER.NAME
 * <br/>
 * If the EMAIL.JNDI.NAME is present, the mail session is obtained using the JNDI call
 * @author lm899p
 */
public abstract class AbstractSMTPNotificationProvider extends AbstractNotificationProvider
{
    /*Key for debug email property*/
    public static final String DEBUG_EMAIL="DEBUG.EMAIL";
    /** Key for email host name **/
    public static final String EMAIL_HOST = "EMAIL.HOST.NAME";
    /** Key for email JNDI name **/
    public static final String EMAIL_JNDI = "EMAIL.JNDI.NAME";
    public static final String PASSWD = "PASSWORD";
    /** key for transport protocol **/
    public static final String TRANSPORT_PROTOCOL="TRANSPORT.PROTOCOL";
    public static final String EMAIL_USER = "USER.NAME";
    /** constant for content type of text **/
    public static final String CONTENT_TYP_TEXT = "text/plain";
    /** constant for content type of html **/
    public static final String CONTENT_TYP_HTML = "text/html";
    //instance variables used to obtain mail session
    private boolean debugEmail = false;
    //host name of email server
    private String emailHostName = null;
    //jndi name for email sessions
    private String emailJNDIName = null;
    private String password = null;
    //transport protocol
    private String transportProtocol = "smtp";
    private String username = null;
    /**
    * Initialize the provider
    * @param config 
    */
    @Override
    public void initialize(HashMap config)
    {
        super.initialize(config);
        LogManager.getLogger(getClass().getName()).info("[initialize()] - configuration properties "+config);
        debugEmail = new Boolean((String)config.get(DEBUG_EMAIL));
        if ( config.containsKey(EMAIL_JNDI))
        {
            emailJNDIName = (String)config.get(EMAIL_JNDI);
            LogManager.getLogger(getClass().getName()).info("[initialize()] - Provider intialized with - [emailJNDIName="+emailJNDIName+"]");
        }
        else
        {
            emailHostName = (String)config.get(EMAIL_HOST);
            password = (String)config.get(PASSWD);
            transportProtocol = (String)config.get(TRANSPORT_PROTOCOL);
            username = (String)config.get(EMAIL_USER);
            LogManager.getLogger(getClass().getName()).info("[initialize()] - Provider intialized with - [emailHostName="+emailHostName+",transportProtocol="+transportProtocol+",debugEmail="+debugEmail+"]");
        }

    }
    /**
    * Verify/validate that the destinations are correctly formatted email addresses.
    * @param destinations
    * @return 
    */
    @Override
    public boolean validateDestinations(List<NotificationDestination> destinations)
    {
        return true;
    }
    /**
    * Obtains a <code>javax.mail.Session</code> instance either from JNDI or properties. 
    * @return
    * @throws Exception 
    */
    protected Session getMailSession()throws Exception
    {
        //If provider configured to obtain session via JNDI
        if ( emailJNDIName != null )
        {
            InitialContext initCtx = new InitialContext();
            //Context context = (Context)initCtx.lookup("java:comp/env");
            Session mailSession = (Session)initCtx.lookup(emailJNDIName);
            if ( LogManager.getLogger(getClass().getName()).isDebugEnabled())
            	LogManager.getLogger(getClass().getName()).debug("[getMailSession()] - Got MailSession using JNDI "+emailJNDIName);
            return mailSession;
        }
        //provider configured to obtain session via properties
        Session mailSession = null;
        //create mail session properties object
        Properties props = System.getProperties();
        props.put("mail.transport.protocol", transportProtocol);
        props.put("mail.smtp.host",emailHostName);
        props.put("mail.debug",debugEmail+"");
        //attempt to obtain connection to email
        mailSession = Session.getDefaultInstance(props);
        if ( LogManager.getLogger(getClass().getName()).isDebugEnabled())
        	LogManager.getLogger(getClass().getName()).debug("[getMailSession()] - Got MailSession using mail.smtp.host="+emailHostName);
        return mailSession;
    }
    /**
    * Convert the list of <code>NotificationDestinations</code> into <code>InternetAddresses</code>
    * @param destinations
    * @return 
    */
    protected InternetAddress[] getInternetAddress(List<NotificationDestination> destinations)
    {
        InternetAddress[] addresses = new InternetAddress[destinations.size()];
        int i = 0;
        for ( NotificationDestination dest : destinations )
        {
            try
            {
                InternetAddress addr = new InternetAddress(dest.getDestinationAddress());
                addresses[i] = addr;
                i++;
            }
            catch(AddressException ae)
            {
                dest.setValidDestination(false);
                LogManager.getLogger(getClass().getName()).info("[getInternetAddress()] - Invalid destination address - "+dest.getDestinationAddress(),ae);
            }
        }
        return addresses;
    }
    /**
    * Method to create the <code>javax.activation.DataSource</code> instances from the list of 
    * attachment file names. 
    * @param attachmentFiles
    * @return 
    */
    protected List<javax.activation.DataSource> getAttachmentDataSource(List<String> attachmentFiles)
    {
        List<javax.activation.DataSource> fileList = new ArrayList<javax.activation.DataSource>();
        for ( String fileName : attachmentFiles)
            fileList.add(new FileDataSource(fileName));
        return fileList;
    }

    /**
    * Sends an email without any attachements
    * @param subject
    * @param content
    * @param toAddr
    * @param fromAddr
    * @throws NoSuchProviderException
    * @throws MessagingException
    * @throws Exception 
    */
    protected void sendEmail(String subject,String content,InternetAddress[] toAddr,InternetAddress fromAddr)
                                throws  NotificationException,NoSuchProviderException,MessagingException,Exception
    {
        sendEmail(subject,content,CONTENT_TYP_TEXT,toAddr,fromAddr,null);
    }
    /**
    * Sends an email with or without attachements
    * @param subject
    * @param content
    * @param contentType
    * @param toAddr
    * @param fromAddr
    * @param attachments
    * @throws NoSuchProviderException
    * @throws MessagingException
    * @throws Exception 
    */
    protected void sendEmail(String subject,String content,String contentType,InternetAddress[] toAddr,InternetAddress fromAddr,javax.activation.DataSource[] attachments)
                            throws NotificationException,NoSuchProviderException,MessagingException,Exception
    {
        Session mailSession = null;
        try
        {
            mailSession = getMailSession();
        }
        catch(Exception ex)
        {
            throw new NotificationException("Exception obtaining connection to email host "+super.getConfigMap(),ex);
        }
        Message msg = new MimeMessage(mailSession);
        msg.setFrom(fromAddr);
        msg.setRecipients(Message.RecipientType.TO,toAddr);
        if ( subject != null )
            msg.setSubject(subject);
        if ( attachments == null || attachments.length == 0 )
        {
            //msg.setText(getMessage());
            msg.setContent(content,contentType);
        }
        else	//attachments have been added to the message ... 
        {
            // create and fill the first message part (TEXT)
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setContent(content, contentType);
            // create the Multipart and add its parts to it
            Multipart mp = new MimeMultipart();
            mp.addBodyPart(textPart);	//the text message
            //add the attachments
            for ( javax.activation.DataSource ds : attachments )
            {
                // create the second message part
                MimeBodyPart attachPart = new MimeBodyPart();
                // attach the file to the message
                attachPart.setDataHandler(new javax.activation.DataHandler(ds));
                attachPart.setFileName(ds.getName());
                mp.addBodyPart(attachPart);	//the attachment
                if ( LogManager.getLogger(getClass().getName()).isDebugEnabled())
                	LogManager.getLogger(getClass().getName()).debug("[sendEmail] - Added attachment to email " +ds.getName());
            }
            // add the Multipart to the message
            msg.setContent(mp);
            // set the Date: header
            msg.setSentDate(new java.util.Date());
        }
        long startTime = System.currentTimeMillis();
        Transport transport = mailSession.getTransport(this.transportProtocol);
        transport.send(msg);
        long stopTime = System.currentTimeMillis();
        if ( LogManager.getLogger(getClass().getName()).isDebugEnabled())
        	LogManager.getLogger(getClass().getName()).debug("[sendEmail] - Elapsed time to send notification email : " + (stopTime-startTime)+" milliseconds");
    }
}
