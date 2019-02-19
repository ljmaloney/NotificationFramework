/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/util/mail/MailManagerEJB.java,v $
 * $Id: MailManagerEJB.java,v 1.16 2005/11/23 18:04:49 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.util.mail;

import java.io.*;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import java.rmi.*;

import javax.ejb.*;
import javax.naming.*;
import javax.mail.*;
import javax.activation.*;
import javax.annotation.PostConstruct;
import javax.mail.internet.*;

import java.util.*;

import com.xanboo.core.util.*;

/**
 * Session Bean implementation of <code>MailManager</code>. This bean acts as a wrapper class for
 * sending emails via a Java Mail Connection Factory reference.
 *
 */
@Remote (MailManager.class)
@Stateless (name="MailManager")
@TransactionManagement( TransactionManagementType.CONTAINER )
public class MailManagerEJB   {

    private SessionContext context;
    private Logger logger;
    private Session mailer;

    @PostConstruct
    public void init() throws CreateException {
        try {
            // create a logger instance
            logger=LoggerFactory.getLogger(this.getClass().getName());
            if(logger.isDebugEnabled()) {
                logger.debug("[ejbCreate()]:");
            }
            
            getMailer();
            
        }catch(NamingException ne) {
            throw new CreateException(ne.getLocalizedMessage());
        }
    }


    
    
    // gets a mailer instance from the container, if necessary
    private void getMailer() throws NamingException {

      if(mailer==null) {
          if(GlobalNames.MAIL_HOST!=null && GlobalNames.MAIL_HOST.length()>0) {      //SMTP Host
                // lookup a mail server session instance
                // InitialContext ic = new InitialContext();
                // mailer = (Session) ic.lookup(GlobalNames.MAILER);

                // set mail properties
                Properties props = System.getProperties();
                props.put("mail.transport.protocol", "smtp");
                props.put("mail.smtp.host", GlobalNames.MAIL_HOST);
                props.put("mail.debug", GlobalNames.MAIL_DEBUG);
                mailer = Session.getDefaultInstance(props, null);

          }else {
                logger.error("[getMailer()]: No mail host is configured. Related functions will NOT work!");
          }
      }
    }    
    

    //--------------- Business methods ------------------------------------------------------
   

    public void sendEmail(String from, String to, String cc, String bcc, String subject, String body) throws XanbooException {
        sendEmail(from, to, cc, bcc, subject, body, null);
    }
    
    
    public void sendEmail(String from, String to, String cc, String bcc, String subject, String body, String[] attachment) throws XanbooException {
        if(logger.isDebugEnabled()) {
           logger.debug("[sendEmail()]:");
        }
        
        try {
            getMailer();

            if(mailer==null) throw new XanbooException(11120, "No mail host is configured. Related functions will NOT work!");

            Message msg= new MimeMessage(mailer);
            msg.setFrom(new InternetAddress(from));

            InternetAddress[] tos = InternetAddress.parse(to);
            msg.setRecipients(Message.RecipientType.TO,tos);

            if(cc!=null && cc.length()>0)  {
                InternetAddress[] ccs = InternetAddress.parse(cc);
                msg.setRecipients(Message.RecipientType.CC,ccs);
            }

            if(bcc!=null && bcc.length()>0)  {
                InternetAddress[] bccs = InternetAddress.parse(bcc);
                msg.setRecipients(Message.RecipientType.BCC,bccs);
            }
            
            msg.setSubject(subject);
            msg.setSentDate(new Date());
            
            if(attachment==null || attachment.length==0) {
                msg.setText(body);
            }else {
                // create and fill the first part: message body
                MimeBodyPart p1 = new MimeBodyPart();
                p1.setText(body);

                // create the Multipart
                Multipart mp = new MimeMultipart();
                mp.addBodyPart(p1);
                
                // Now add the attachments one by one
                for(int i=0; i<attachment.length; i++) {
                    FileDataSource fl;
                    MimeBodyPart p2 = new MimeBodyPart();
                    p2.setDataHandler(new DataHandler(new FileDataSource(attachment[i])));
                    int ix = attachment[i].lastIndexOf(File.separatorChar);
                    if(ix>0)
                        p2.setFileName(attachment[i].substring(ix+1));
                    else
                        p2.setFileName(attachment[i]);
                    mp.addBodyPart(p2);
                }
                
                // add the Multipart to the message
                msg.setContent(mp);
            }
            
            // send it
            Transport.send(msg);
        }catch(XanbooException xe) {
            throw xe;
        }catch(MessagingException me) {
            throw new XanbooException(11110, me.getLocalizedMessage());
        }catch(NamingException ne) {
            throw new XanbooException(11110, ne.getLocalizedMessage());
        }catch(Exception e) {
            throw new XanbooException(11110, e.getLocalizedMessage());
        }
        
    }



    public static String encode(String in) {
        try {
            byte[] inBytes = in.getBytes();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            OutputStream os = MimeUtility.encode(bos, "base64");
            os.write(inBytes);
            os.close();
            return (String) bos.toString();
        }catch(Exception e) { return null; }
    }

    
    public static String decode(String in) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(in.getBytes());
            InputStream is =  MimeUtility.decode(bis, "base64");
            byte[] buffer = new byte[in.length()];
            int len = is.read(buffer);
            is.close();
            return new String(buffer, 0, len);
        }catch(Exception e) { return null; }
    }
    
    
    
    public int sendACSInotification(String extAccID, String toAddr, String toInfo, String timestamp, String subject, String body) throws XanbooException {
        if(logger.isDebugEnabled()) {
           logger.debug("[sendACSInotification()]:");
        }
    
        // strip all @CSI suffixes, and put addresses in array
        StringTokenizer st = new StringTokenizer(toAddr, ",; " );
        int cnt = st.countTokens();
        String[] newToAddr = new String[cnt];
        for(int i=0;i<cnt; i++) {
            String addr = ((String) st.nextToken()).trim();
            if(addr.length()>4 && addr.endsWith("@CSI")) 
                newToAddr[i] = addr.substring(0,addr.length()-4);
            else
                newToAddr[i] = null;
        }
        
        
        SimpleACSIClient acsi = new SimpleACSIClient();
        int rc = acsi.send(extAccID, null, null, toInfo, newToAddr, timestamp, subject, body, null, null, null);
        
        return rc;

    }
}
