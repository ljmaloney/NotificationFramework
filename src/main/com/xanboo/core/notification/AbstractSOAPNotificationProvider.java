/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.notification;

import java.util.HashMap;
import com.xanboo.core.util.LoggerFactory;
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
        LoggerFactory.getLogger(getClass().getName()).info("[intialize()] Initialize SOAP provider");
    }
	/**
	 * Method to use to sign the request
	 * @param message 
	 */
    public void signMessage(NotificationMessageInterface message)
    {
    }
}
