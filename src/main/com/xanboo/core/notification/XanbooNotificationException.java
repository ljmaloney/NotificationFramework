/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.notification;

/**
 *
 * @author lm899p
 */
public class XanbooNotificationException extends Exception
{
    private boolean retry = true;
    
    public XanbooNotificationException(String msg)
    {
        super(msg);
    }
	
    public XanbooNotificationException(String msg, Throwable t)
    {
        super(msg,t);
    }
    
    public XanbooNotificationException(String msg,Throwable t,Boolean retry)
    {
        this(msg,t);
        this.retry = retry;
    }
    
    public boolean getRetry(){return retry;}
}
