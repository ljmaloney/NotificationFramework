/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ljm.notification;

/**
 * Class to encapsulate the attributes for a destination of a notification. The destination 
 * can be either an SMS message or an email address. 
 * 
 * @author Luther J Maloney
 * @since October 2013
 */
public class NotificationDestination
{
    public static final Integer PROFILE_NOT_SPECIFIED = -100;
    private String destinationAddress = null;
    private Integer profileTypeId = PROFILE_NOT_SPECIFIED;
    private String suffix = "";
    boolean phoneNumberFlag = false;
    boolean emailAddressFlag = false;
    boolean httpUrlFlag = false;
    boolean validDestination = true; //default to true, can be changed if invalid
    private String token; //token value from the opt_in_status table which uniquely indentifies a noification address
    /**
    * Default constructor
    */
    public NotificationDestination()
    {

    }
    /** 
    * Constructor.<br/>
    * Creates an instance of a <code>NotificationDestination</code> using the argument. The destination can 
    * be in the format of PROFILETYPE_ID%DESTINATION or DESTINATION@SUFFIX (legacy DLA). 
    * 
    * @param destination 
    */
    public NotificationDestination(String destination)
    {
        if ( destination.indexOf("%") > 0)	//test for the presence of the profile type / destination delimiter
        {
            String idStr = destination.substring(0,destination.indexOf("%"));
            this.profileTypeId = new Integer(idStr);
            this.destinationAddress = destination.substring((destination.indexOf("%")+1));
            //the value of these is determined by the provider profile type ... 
            phoneNumberFlag = false;
            emailAddressFlag = false;
        }
        else //delimiter does not exist, legacy DLA notification
        {
            this.profileTypeId = null;
            if(destination.indexOf('@')==-1 && destination.startsWith("http")) 
            {
                this.destinationAddress = destination;
                this.suffix = destination;
                this.httpUrlFlag = true;
            }
            else if (destination.endsWith("@CSI") || destination.indexOf("@CSI,")!=-1) 
            {
                suffix = "@CSI";
                destinationAddress = destination.substring(0,destination.indexOf("@CSI"));
            }
            else 
            {
                destinationAddress = destination;
                suffix = destination.substring((destination.indexOf("@")));
            }
        }
    }
    /**
    * Constructor
    * @param destination
    * @param profileTypeId
    * @param suffix 
    */
    public NotificationDestination(String destination,Integer profileTypeId,String suffix)
    {
        this.destinationAddress = destination;
        this.profileTypeId = profileTypeId;
        this.suffix = suffix;
    }

    public String getDestinationAddress()
    {
        return this.destinationAddress;
    }
    public void setDestinationAddress(String address)
    {
        this.destinationAddress = address;
    }
    public boolean isDestinationURL()
    {
        return httpUrlFlag;
    }
    public boolean isEmailAddress()
    {
        return this.emailAddressFlag;
    }
    public boolean isPhoneNumber()
    {
        return this.phoneNumberFlag;
    }
    public Integer getProfileTypeId()
    {
        return this.profileTypeId;
    }
    public void setProfileTypeId(Integer id)
    {
        this.profileTypeId = id;
    }
    public String getSuffix()
    {
        return this.suffix;
    }
    public void setSuffix(String suffix)
    {
        this.suffix = suffix;
    }
    public boolean isValidDestination()
    {
        return this.validDestination;
    }
    public void setValidDestination(boolean valid)
    {
        this.validDestination = valid;
    }
    public String getToken()
    {
        return token;
    }
    public void setToken(String token)
    {
        this.token = token;
    }

    @Override
    public boolean equals(Object o)
    {
        if ( o instanceof NotificationDestination )
        {
            NotificationDestination nd = (NotificationDestination)o;
            return (getDestinationAddress().equals(nd.getDestinationAddress()) && getProfileTypeId().equals(nd.getProfileTypeId()) && getSuffix().equals(nd.getSuffix()));
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 17 * hash + (this.destinationAddress != null ? this.destinationAddress.hashCode() : 0);
        hash = 17 * hash + (this.profileTypeId != null ? this.profileTypeId.hashCode() : 0);
        hash = 17 * hash + (this.suffix != null ? this.suffix.hashCode() : 0);
        return hash;
    }
    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        str.append("NotificationDestination[");
        str.append("destinationAddress=");
        str.append(this.destinationAddress);
        str.append(",suffix=");
        str.append(suffix);
        str.append(",profileTypeId=");
        str.append((profileTypeId != null ? profileTypeId : "null"));
        str.append("]");
        return str.toString();
    }
}
