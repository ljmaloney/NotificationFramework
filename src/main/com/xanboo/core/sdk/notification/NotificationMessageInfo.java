/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.sdk.notification;

/**
 *
 * @author lm899p
 */
public class NotificationMessageInfo
{
    private String messageToken = null;
    private Long queueId = null;
    
    public NotificationMessageInfo(Long queueId,String token)
    {
        this.messageToken = token;
        this.queueId = queueId;
    }
    
    public String getMessageToken()
    {
        return this.messageToken;
    }
    
    public Long getQueueId()
    {
        return this.queueId;
    }
}
