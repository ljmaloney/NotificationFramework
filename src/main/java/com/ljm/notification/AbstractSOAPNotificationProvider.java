package com.ljm.notification;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;

/**
 * Abstract class to contain any common code for sending a notification via SOAP
 * 
 * @author Luther Maloney
 * @since October 2013
 */
public abstract class AbstractSOAPNotificationProvider extends AbstractHttpNotificationProvider
{
    @Override
    public void initialize(HashMap config)
    {
        super.initialize(config);
        LogManager.getLogger(getClass().getName()).info("[intialize()] Initialize SOAP provider");
    }
	/**
	 * Method to use to sign the request
	 * @param message 
	 */
    public void signMessage(NotificationMessageInterface message)
    {
    }
}
