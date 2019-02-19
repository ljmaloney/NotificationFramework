/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.notification;

import java.util.HashMap;
import com.xanboo.core.util.LoggerFactory;
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
        LoggerFactory.getLogger(getClass().getName()).info("[initialize()]Initialize provider");
    }
}
