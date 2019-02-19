/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ljm.notification.provider;

import com.ljm.notification.NotificationDestination;
import com.ljm.notification.NotificationMessageInterface;
import com.ljm.notification.NotificationException;
import com.ljm.notification.NotificationMessage;
import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;
import java.util.HashMap;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.naming.InitialContext;

/**
 * A SMTP notification provider that contains extended functionality to handle authentication to a mail server. 
 * 
 * @author lm899p
 */
public class SecureSMTPSmsNotificationProvider extends XanbooSMTPSmsNotificationProvider
{
    private Logger logger = null;
    private Properties mailSessionProps = null;
    private String username = null;
    private String mailFrom = null;
    private transient String password = null;
    private Authenticator passwordAuth = null;
    private boolean usePlus = false;
    
    public static final String TLS_ENABLE = "mail.smtp.starttls.enable";
    public static final String SMTP_AUTH = "mail.smtp.auth";
    public static final String MAIL_FROM = "mail.from";
    /**
    * Constructor
    */
    public SecureSMTPSmsNotificationProvider()
    {
        logger = LoggerFactory.getLogger(getClass().getName());
    }

    @Override
    public void initialize(HashMap config)
    {
        super.initialize(config);
        logger.info("[initialize()] Initialize Secure SMS over SMTP");
        mailSessionProps = new Properties();
        mailSessionProps.put("mail.transport.protocol", (config.containsKey(TRANSPORT_PROTOCOL) ? config.get(TRANSPORT_PROTOCOL) : "smtp"));
        mailSessionProps.put("mail.smtp.host",config.get(EMAIL_HOST));
        mailSessionProps.put("mail.debug",(config.containsKey(DEBUG_EMAIL) ? config.get(DEBUG_EMAIL) : "false"));
        
        if ( config.containsKey(TLS_ENABLE))
            mailSessionProps.put("mail.smtp.starttls.enable",config.get(TLS_ENABLE));
        if ( config.containsKey(SMTP_AUTH))
            mailSessionProps.put("mail.smtp.auth", config.get(SMTP_AUTH));
        
        password = (String)config.get(PASSWD);
        username = (String)config.get(EMAIL_USER);
        
        if ( config.containsKey(MAIL_FROM))
        {
            mailSessionProps.put("mail.from",config.get(MAIL_FROM));
            mailFrom = (String)config.get(MAIL_FROM);
        }
        else if ( username != null && !username.equalsIgnoreCase("") )
        {
            mailSessionProps.put("mail.from",username);
        }
        if ( !getConfigMap().containsKey(SMTP_AUTH) && username != null && !username.equalsIgnoreCase("") )
        {
            mailSessionProps.put("mail.user", username);
            mailSessionProps.put("mail.password", password);
        }
        
        if ( getConfigMap().containsKey(SMTP_AUTH) )
        {
            passwordAuth = new Authenticator() 
            {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() 
                {
                    return new PasswordAuthentication(username,password);
                }
            };
        }
        
        //prepend the "+" symbol to the phone number
        String usePStr = config.containsKey("useplusintl") ? (String)config.get("useplusintl") : "false";
        usePlus = new Boolean(usePStr);
    }
    
    /**
    * Obtains a <code>javax.mail.Session</code> instance either from JNDI or properties. 
    * @return
    * @throws Exception 
    */
    @Override
    protected Session getMailSession()throws Exception
    {
        //If provider configured to obtain session via JNDI
        String emailJNDIName = (String)getConfigMap().get(EMAIL_JNDI);
        if ( emailJNDIName != null )
        {
            InitialContext initCtx = new InitialContext();
            //Context context = (Context)initCtx.lookup("java:comp/env");
            Session mailSession = (Session)initCtx.lookup(emailJNDIName);
            LoggerFactory.getLogger(getClass().getName()).debug("[getMailSession()] - Got MailSession using JNDI "+emailJNDIName);
            return mailSession;
        }
        //provider configured to obtain session via properties
        Session mailSession = null;
        //attempt to obtain connection to email
        if ( LoggerFactory.getLogger(getClass().getName()).isDebugEnabled() )
            LoggerFactory.getLogger(getClass().getName()).debug("[getMailSession] - session properties : "+mailSessionProps);
        
        if ( !getConfigMap().containsKey(SMTP_AUTH) )
            mailSession = Session.getDefaultInstance(mailSessionProps);
        else 
            mailSession = Session.getInstance(mailSessionProps,passwordAuth);
        
        if ( LoggerFactory.getLogger(getClass().getName()).isDebugEnabled() )
            LoggerFactory.getLogger(getClass().getName()).debug("[getMailSession()] - Got MailSession using mail.smtp.host="+getConfigMap().get(EMAIL_HOST));
        return mailSession;
    }
    
    public void sendMessage(NotificationDestination destination,NotificationMessageInterface message)throws NotificationException
    {
        //code to override NOTFROM field on the DOMAINREF table
        //the field will only be overriden when "mail.from" is specified in the provider properties
        String destString = destination.getDestinationAddress().replace(" ","");
        if ( usePlus && !destString.startsWith("+"))
            destString = "+"+destString;
        destination.setDestinationAddress(destString);
        if ( message instanceof NotificationMessage && mailFrom != null && !mailFrom.equalsIgnoreCase(""))
        {
            ((NotificationMessage)message).setFromAddress(mailFrom);
        }
        super.sendMessage(destination, message);
    }
}
