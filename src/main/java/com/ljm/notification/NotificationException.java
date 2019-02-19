/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ljm.notification;

/**
 *
 * @author lm899p
 */
public class NotificationException extends Exception
{
    private boolean retry = true;
    
    public NotificationException(String msg)
    {
        super(msg);
    }
	
    public NotificationException(String msg, Throwable t)
    {
        super(msg,t);
    }
    
    public NotificationException(String msg,Throwable t,Boolean retry)
    {
        this(msg,t);
        this.retry = retry;
    }
    
    public boolean getRetry(){return retry;}
}
