/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.sdk.notification;

/**
 * Object to contain information about a recipient on a notification chain. The UI is only responsible for 
 * populating the chainId, profileId, and waitTime fields. The setters for the remaining fields are marked 
 * protected, as these fields should only be populated by the EJB. 
 * <br/>
 * Valid destination types are:<br/>
 * <ul>
 *  <li>PHONE - the primary phone for the contact
 *  <li>PHONE_CELL - the cell phone for the contact
 *  <li>FAX - the fax number field for the contact
 *  <li>EMAIL - the email address
 * </ul>
 * If PHONE, PHONE_CELL, or FAX is selected, the SMS_PREFS for that number must be set to "true". 
 * @author lm899p
 */
public class NotificationChainRecipient implements java.io.Serializable
{
    private Long chainId = -1l;
    private Integer chainGrpNo = -1;
    private Integer chainSeqNo = -1;
    private DestinationType destination = null;
    private long contactId = -1l;
    private long userId = -1l;
    private Integer waitTime = 0;
    
    public NotificationChainRecipient()
    {
        
    }
    /**
     * Constructor
     * @param chainId - The notification chain
     * @param contactId - Identifies the contact used for the recipient
     * @param waitTimeMinutes - the time in minutes to wait from the notification event 
     *                          before sending the notification to this recipient.
     * @param destination - The destination for the recipient, specifies which SMS number or specifies the email address. 
     */
    public NotificationChainRecipient(Long chainId,Long contactId,Long userId,Integer waitTimeMinutes,DestinationType destination)
    {
        this.chainId = chainId;
        this.waitTime = waitTimeMinutes;
        this.contactId = contactId;
        this.userId = userId == null ? -1l : userId;
        this.destination = destination;
    }
    /**
     * Constructor
     * @param chainId
     * @param contactId
     * @param userId
     * @param waitTimeMinutes
     * @param destination 
     */
    protected NotificationChainRecipient(Long chainId,Long contactId,Long userId,Integer waitTimeMinutes,String destination)
    {
        this.chainId = chainId;
        this.waitTime = waitTimeMinutes;
        this.contactId = contactId;
        this.userId = userId == null ? -1l : userId;
        setDestination(destination);
    }
    
    public Integer getChainGrpNo()
    {
        return this.chainGrpNo;
    }
    protected void setChainGrpNo(Integer number)
    {
        this.chainGrpNo = number;
    }
    public Long getChainId()
    {
        return this.chainId;
    }
    public void setChainId(Long id)
    {
        this.chainId = id;
    }
    public Integer getChainSeqNo()
    {
        return this.chainSeqNo;
    }
    protected void setChainSeqNo(Integer seqNo)
    {
        this.chainSeqNo = seqNo;
    }
    public DestinationType getDestination()
    {
        return this.destination;
    }
    protected void setDestination(DestinationType destination)
    {
        this.destination = destination;
    }
    protected void setDestination(String dbType)
    {
        if ( dbType.equals(DestinationType.EMAIL.getDBValue()))
            destination = DestinationType.EMAIL;
        if ( dbType.equals(DestinationType.PHONE.getDBValue()))
            destination = DestinationType.PHONE;
        if ( dbType.equals(DestinationType.PHONE_CELL.getDBValue()))
            destination = DestinationType.PHONE_CELL;
        if ( dbType.equals(DestinationType.FAX.getDBValue()))
            destination = DestinationType.FAX;
    }
    public Long getContactId()
    {
        return this.contactId;
    }
    public void setContactId(Long id)
    {
        this.contactId = id;
    }
    public Long getUserId()
    {
        return this.userId;
    }
    public void setUserId(Long id)
    {
        this.userId = id;
    }
    public Integer getWaitTime()
    {
        return this.waitTime;
    }
    public void setWaitTime(Integer time)
    {
        this.waitTime = time;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 79 * hash + (this.chainId != null ? this.chainId.hashCode() : 0);
        hash = 79 * hash + (this.chainGrpNo != null ? this.chainGrpNo.hashCode() : 0);
        hash = 79 * hash + (this.chainSeqNo != null ? this.chainSeqNo.hashCode() : 0);
        hash = 79 * hash + (this.destination != null ? this.destination.hashCode() : 0);
        hash = 79 * hash + (int) (this.contactId ^ (this.contactId >>> 32));
        hash = 79 * hash + (int) (this.userId ^ (this.userId >>> 32));
        hash = 79 * hash + (this.waitTime != null ? this.waitTime.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final NotificationChainRecipient other = (NotificationChainRecipient) obj;
        if ( this.chainId != other.chainId && (this.chainId == null || !this.chainId.equals(other.chainId)) )
        {
            return false;
        }
        if ( this.chainGrpNo != other.chainGrpNo && (this.chainGrpNo == null || !this.chainGrpNo.equals(other.chainGrpNo)) )
        {
            return false;
        }
        if ( this.chainSeqNo != other.chainSeqNo && (this.chainSeqNo == null || !this.chainSeqNo.equals(other.chainSeqNo)) )
        {
            return false;
        }
        if ( this.destination != other.destination )
        {
            return false;
        }
        if ( this.contactId != other.contactId )
        {
            return false;
        }
        if ( this.userId != other.userId )
        {
            return false;
        }
        if ( this.waitTime != other.waitTime && (this.waitTime == null || !this.waitTime.equals(other.waitTime)) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        StringBuilder bldr = new StringBuilder();
        bldr.append("NotificationChainRecipient[");
        bldr.append("chainId=").append(chainId);
        bldr.append(",chainGrpNo=").append(chainGrpNo);
        bldr.append(",chainSeqNo=").append(chainSeqNo);
        bldr.append(",destination=").append(destination);
        bldr.append(",contactId=").append(contactId);
        bldr.append(",userId=").append(userId);
        bldr.append(",waitTime=").append(waitTime);
        bldr.append(']');
        return bldr.toString();
    }
    /**
     * Enumeration for the types of destinations. 
     */
    public enum DestinationType 
    {
        PHONE("PHONE"),PHONE_CELL("PHONE_CELL"),
        FAX("FAX"),EMAIL("EMAIL");
        private String type = "";
        DestinationType(String type){this.type = type;}
        private String type(){return this.type;}
        public String getDBValue(){return type;}
    };
}