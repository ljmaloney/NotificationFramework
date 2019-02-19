/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.sdk.notification;

import java.util.ArrayList;
import java.util.List;

/**
 * An object representing a NotificationChain. <br/>
 * Possible values for quiteTimeType field are: <br/>
 * <ul>
 *  <li>"EVENT" means that the quietTime is the time to wait from the occurance of the event before sending the notification to a recipient
 *  <li>"INTERVAL" means the time to wait for acknowledgement from a recipient before sending to the next recipients in the chain
 * </ul>
 * @author lm899p
 */
public class NotificationChain implements java.io.Serializable
{
    private long accountId = -1l;
    private long chainId = -1l;
    private boolean sendToSBN = false;
    private boolean useAsDefault = false;
    private QuietTimeType quietTimeType = QuietTimeType.INTERVAL;
    private String description = "";
    
    private List<NotificationChainRecipient> recipients = new ArrayList<NotificationChainRecipient>();
    
    public NotificationChain()
    {
        
    }
    /**
     * 
     * @param accountId - The account id associated with the chain
     * @param sendToSBN -  flag to indicate if the chain includes SBN 
     * @param useAsDefault - specifies if the chain is default chain for adls
     * @param type - specifies how the quiet time is to be handled. This is either QuietTypeType.INTERVAL or QuietTimeType.EVENT. 
     * @param description 
     */
    public NotificationChain(long accountId,boolean sendToSBN,boolean useAsDefault,QuietTimeType type,String description)
    {
        this.accountId = accountId;
        this.sendToSBN = sendToSBN;
        this.useAsDefault = useAsDefault;
        this.quietTimeType = type;
        this.description = description;
    }
    /**
     * Constructor to create an instance of a NotificationChain object
     * @param chainId - The id number of the chain
     * @param accountId - The account associated with the chain
     * @param sendToSBN - flag if the chain includes SBN
     * @param useAsDefault - flag if this chain should be used as the default for ADL rules
     * @param quietTimeType - The string representation of how the quiet time is to be handled.
     * @param description - the name of the chain
     */
    protected NotificationChain(long chainId,long accountId,boolean sendToSBN,boolean useAsDefault,String quietTimeType,String description)
    {
        this.chainId = chainId;
        this.accountId = accountId;
        this.sendToSBN = sendToSBN;
        this.useAsDefault = useAsDefault;
        this.description = description;
        if ( quietTimeType.equals(QuietTimeType.EVENT.getDBValue()))
            this.quietTimeType = QuietTimeType.EVENT;
        else if ( quietTimeType.equals(QuietTimeType.INTERVAL.getDBValue()))
            this.quietTimeType = QuietTimeType.INTERVAL;
    }
    /** 
     * Returns the accountId associated with the notification chain
     * @return 
     */
    public long getAccountId()
    {
        return accountId;
    }
    /**
     * The account Id associated with the chain
     * @param id 
     */
    public void setAccountId(long id)
    {
        this.accountId = id;
    }
    /**
     * The notification chain's chainId
     * @return 
     */
    public long getChainId()
    {
        return this.chainId;
    }
    protected void setChainId(long id)
    {
        this.chainId = id;
    }
    /**
     * The description (name) of the notification chain
     * @return 
     */
    public String getDescription()
    {
        return this.description;
    }
    /**
     * Sets the name of the notification chain
     * @param description 
     */
    public void setDescription(String description)
    {
        this.description = description;
    }
    /**
     * The recipients of notification messages assigned to this chain
     * @return 
     */
    public List<NotificationChainRecipient> getRecipients()
    {
        return this.recipients;
    }
    /**
     * Adds a recipient to the notification chain
     * @param recipient - an instance of NotificationChainRecipient
     */
    public void addRecipient(NotificationChainRecipient recipient)
    {
        this.recipients.add(recipient);
    }
    /**
     * Sets the list of recipients as members of the notification chain
     * @param recipients 
     */
    public void setRecipients(List<NotificationChainRecipient> recipients)
    {
        this.recipients = recipients;
    }
    /**
     * Returns true if notifications send using this chain will also send to SBN if 
     * no recipient of the chain acknoweleges reciept of the message
     * @return 
     */
    public boolean sendToSBN()
    {
        return this.sendToSBN;
    }
    /**
     * Sets if the chain also includes sending to SBN as the last step (when a recipient 
     * does not acknowlege the message)
     * @param send 
     */
    public void setSendToSBN(Boolean send)
    {
        this.sendToSBN = send;
    }
    /**
     * Specifies this chain can be used as the default selection when creating ADLs
     * @return 
     */
    public boolean useAsDefault()
    {
        return this.useAsDefault;
    }
    /**
     * Setter
     * @param asDefault 
     */
    public void setUseAsDefault(Boolean asDefault)
    {
        this.useAsDefault = asDefault;
    }
    /**
     * Specifies how the waitTime (quietTime) for the recipient is being handled. 
     * @return 
     */
    public QuietTimeType getQuietTypeType()
    {
        return this.quietTimeType;
    }
    /**
     * Specify how the waitTime(quietTime) for the recipient is being handled.
     * @param type 
     */
    public void setQuietTimeType(QuietTimeType type)
    {
        this.quietTimeType = type;
    }
    protected void setQuietTypeType(String dbType)
    {
        if ( dbType.equals(QuietTimeType.EVENT.getDBValue()))
            quietTimeType = QuietTimeType.EVENT;
        else if ( dbType.equals(QuietTimeType.INTERVAL.getDBValue()))
            quietTimeType = QuietTimeType.INTERVAL;
    }
    
    @Override
    public String toString()
    {
        StringBuilder bldr = new StringBuilder();
        bldr.append("NotificationChain[");
        bldr.append("chainId=").append(chainId);
        bldr.append(",accountId=").append(accountId);
        bldr.append(",sendToSBN=").append(sendToSBN);
        bldr.append(",useAsDefault=").append(useAsDefault);
        bldr.append(",quietTimeType=").append(quietTimeType);
        bldr.append(",description=").append(description);
        bldr.append("]");
        return bldr.toString();
    }
    /**
     * enumeration for QuiteTimeType. 
     * "EVENT" means that the quietTime is the time to wait from the occurance of the event before sending the notification to a recipient
     * "INTERVAL" means the time to wait for acknowlegement from a recipient before sending to the next recipients in the chain
     */
    public enum QuietTimeType 
    {
        INTERVAL("interval"),EVENT("event");
        private String type = "";
        QuietTimeType(String type){this.type = type;}
        private String type(){return this.type;}
        public String getDBValue(){return type;}
    };
}
