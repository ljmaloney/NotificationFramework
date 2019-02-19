package com.ljm.notification;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
/**
 *
 * @author Luther Maloney
 * @since October 2013
 */
public abstract class AbstractJMSNotificationProvider extends AbstractNotificationProvider
{
    private String jmsType = "";
    
    @Override
    public void initialize(HashMap config)
    {
        super.initialize(config);
        LogManager.getLogger(getClass().getName()).info("[initialize()]Initialize provider");
    }
}
