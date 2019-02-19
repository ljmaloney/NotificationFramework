/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/util/mail/MailManager.java,v $
 * $Id: MailManager.java,v 1.6 2005/11/23 16:01:00 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.util.mail;

import java.rmi.*;


import com.xanboo.core.util.*;

/**
 * Remote Interface for the MailManagerEJB
 *
 */

public interface MailManager  
{
     /**
     * generates email messages.
     * @param from  email from address
     * @param to comma separated destination email addresses.
     * @param cc comma separated cc addresses. if there is no cc address, null or "" need to be passed.
     * @param bcc comma separated bcc addresses. if there is no bcc address, null or "" need to be passed.
     * @param subject email subject.
     * @param body email message body content.
     *
     * @throws XanbooException
     *
     * @deprecated  replaced by {@link #sendEmail(String, String, String, String, String, String, String[]) }
     *
     */
    public void sendEmail(String from, String to, String cc, String bcc, String subject, String body) throws RemoteException, XanbooException ;

     /**
     * generates email messages with attachments.
     * @param from  email from address
     * @param to comma separated destination email addresses.
     * @param cc comma separated cc addresses. if there is no cc address, null or "" need to be passed.
     * @param bcc comma separated bcc addresses. if there is no bcc address, null or "" need to be passed.
     * @param subject email subject.
     * @param body email message body content.
     * @param attachment array of filenames to attach. Full paths must be specified as filenames. If null or
      *       a zero-size array is passed, no attachment will be sent.
     *
     * @throws XanbooException
     */
    public void sendEmail(String from, String to, String cc, String bcc, String subject, String body, String[] attachment) throws RemoteException, XanbooException ;
    
    
     /**
     * generates notifications thru AT&T ACSI
     * @param extAccID customer external account id (e.g. ATT customer BAN)
     * @param to destination email address.
     * @param toInfo destination name (e.g. customer name)
     * @param timestamp
     * @param subject notification subject.
     * @param body notification message body content.
     *
     * @throws XanbooException
     */
    public int sendACSInotification(String extAccID, String to, String toInfo, String timestamp, String subject, String body) throws RemoteException, XanbooException;    
    
}