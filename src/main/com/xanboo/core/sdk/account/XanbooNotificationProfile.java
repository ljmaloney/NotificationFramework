/*
 * $Source:  $
 * $Id:  $
 * 
 * Copyright 2011 AT&T Digital Life
 *
 */

package com.xanboo.core.sdk.account;

/**
 * Class to represent Xanboo Notification Profiles and Emergency Contacts
 */
public class XanbooNotificationProfile implements java.io.Serializable {
    private static final long serialVersionUID = 3928603773083003383L;


    /** Unique profile identifier. */
    private long profileId;


    /** Profile name/description */
    private String name;

    /** Required profile type for the first associated profile address. Type must be a valid PROFILETYPE_ID<br>
     *  returned by {@link com.xanboo.core.sdk.sysadmin.SysAdminManager#getProfileTypeList(XanbooAdminPrincipal xCaller)}  */
    private int profileType;

    /** Required first profile address */
    private String profileAddress;


    /** Optional Profile type for the second associated profile address. Type must be a valid PROFILETYPE_ID<br>
     *  returned by {@link com.xanboo.core.sdk.sysadmin.SysAdminManager#getProfileTypeList(XanbooAdminPrincipal xCaller)}  */
    private int profileType2;

    /** Optional second profile address. Typically set for emergency contacts. */
    private String profileAddress2;

    /** Optional profile type for the third associated profile address. Type must be a valid PROFILETYPE_ID<br>
     *  returned by {@link com.xanboo.core.sdk.sysadmin.SysAdminManager#getProfileTypeList(XanbooAdminPrincipal xCaller)}  */
    private int profileType3;

    /** Optional third profile address. Typically set for emergency contacts  */
    private String profileAddress3;


    /** Flag to indicate if the profile is an emergency contact or not */
    private boolean isEmergencyContact;
    
    /** Flag to indicate that if a notification should be sent to CSI when this notification profile is changed**/
    private boolean isSendNotification = false;

    /** gateway guid to be associated with the profile. Typically set for emergency contacts */
    private String gguid;

    /** notification order associated with the profile. Must be set to a >0 value for emergency contacts.
     * A >-1 may indicate the record is an emergency contact too. */
    private int order;

    /** an optional alarm confirmation codeword for emergency contact records */
    private String codeword;
    
    private final static int MAX_ACTION_PLANS = 10;
    private short[] actionPlanOrder = null;


    /** Constructs a new Notification Profile
     * @param isEmergencyContact    boolean to indicate if this profile is an emergency contact or not
     */
    public XanbooNotificationProfile(boolean isEmergencyContact) {
        this.profileId=-1L;
        this.isEmergencyContact=isEmergencyContact;

        this.profileType=-99;
        this.profileType2=-99;
        this.profileType3=-99;
        this.order=-1;
    }

    /** Constructs a new Notification Profile with an existing profile id
     * @param profileId    a unique identifier for an existing profile
     * @param isEmergencyContact    boolean to indicate if this profile is an emergency contact or not
     */
    public XanbooNotificationProfile(long profileId, boolean isEmergencyContact) {
        this.profileId=profileId;
        this.isEmergencyContact=isEmergencyContact;

        this.profileType=-99;
        this.profileType2=-99;
        this.profileType3=-99;
        this.order=-1;
    }

    /** Constructs a new Notification Profile from given parameters
     * @param isEmergencyContact    boolean to indicate if this profile is an emergency contact or not
     * @param name    an optional name/description for the profile
     * @param profileType the type of profile address/id to be created. Type must be a valid PROFILETYPE_ID<br>
     *  returned by {@link com.xanboo.core.sdk.sysadmin.SysAdminManager#getProfileTypeList(XanbooAdminPrincipal xCaller)}
     * @param profileAddress the address/id (email address, pager pin number, phone number, etc.) for the profile to be created.
     */
    public XanbooNotificationProfile(boolean isEmergencyContact, String name, int profileType, String profileAddress) {
        this.profileId=-1L;
        this.isEmergencyContact=isEmergencyContact;
        setName(name);
        setType(profileType);
        setAddress(profileAddress);

        this.profileType2=-99;
        this.profileType3=-99;
        this.order=-1;
    }

    
    /** Gets the id for the profile.
     * @return user id.
     */
    public long getProfileId() {
        return this.profileId;
    }
    
    /** Sets the id for the profile.
     * @param userId user id to set.
     */
    public void setProfileId(long profileId) {
        this.profileId = profileId;
    }
    
    
    /** Gets profile name.
     * @return profile name.
     */
    public String getName() {
        return this.name;
    }
    
    /** Sets profile name.
     * @param name a name or description for the profile.
     */
    public void setName(String name) {
        this.name = (name!=null && name.trim().length()>0) ? name : null;
    }
    
    /** Checks if the profile is an emergency contact record or not.
     * @return boolean flag.
     */
    public boolean isEmergencyContact() {
        return this.isEmergencyContact;
    }
    
    /** Sets the emergency contact flag for the profile.
     * @param isEmergencyContact boolean value to set the  flag.
     */
    public void setEmergencyContact(boolean isEmergencyContact) {
        this.isEmergencyContact = isEmergencyContact;
    }

    /** Gets associated gateway guid.
     * @return gateway guid.
     */
    public String getGguid() {
        return this.gguid;
    }

    /** Sets associated gateway guid.
     * @param gguid a gateway guid.
     */
    public void setGguid(String gguid) {
        this.gguid = (gguid!=null && gguid.trim().length()>0) ? gguid : null;
    }

    /** Gets notification order.
     * @return order.
     */
    public int getOrder() {
        return this.order;
    }

    /** Sets associated notification order.
     * @param order notification order. For emergency contacts, an order value between 1 and 5 must be specified.
     */
    public void setOrder(int order) {
        this.order = order;
    }

    /** Gets profile type for the first profile address.
     * @return profile type. -99 indicates no profile type specified.
     */
    public int getType() {
        return this.profileType;
    }

    /** Sets profile type for the first profile address.
     * @param type a valid profile type.
     */
    public void setType(int profileType) {
        this.profileType = profileType;
    }


    /** Gets profile type for the second profile address.
     * @return profile type. -99 indicates no profile type specified.
     */
    public int getType2() {
        return this.profileType2;
    }

    /** Sets profile type for the second profile address.
     * @param type a valid profile type.
     */
    public void setType2(int profileType2) {
        this.profileType2 = profileType2;
    }


    /** Gets profile type for the third profile address.
     * @return profile type. -99 indicates no profile type specified.
     */
    public int getType3() {
        return this.profileType3;
    }

    /** Sets profile type for the third profile address.
     * @param type a valid profile type.
     */
    public void setType3(int profileType3) {
        this.profileType3 = profileType3;
    }


    /** Gets the first profile address.
     * @return profile address.
     */
    public String getAddress() {
        return this.profileAddress;
    }

    /** Sets the first profile address.
     * @param address a profile addres of relevant type.
     */
    public void setAddress(String profileAddress) {
        this.profileAddress = (profileAddress!=null ? profileAddress.trim() : null);
        if(this.profileAddress!=null && this.profileAddress.trim().length()==0) this.profileAddress=null;
    }


    /** Gets the second profile address.
     * @return profile address.
     */
    public String getAddress2() {
        return this.profileAddress2;
    }

    /** Sets the second profile address.
     * @param address a profile addres of relevant type.
     */
    public void setAddress2(String profileAddress) {
        this.profileAddress2 = (profileAddress!=null ? profileAddress.trim() : null);
        if(this.profileAddress2!=null && this.profileAddress2.trim().length()==0) this.profileAddress2=null;
    }

    /** Gets the third profile address.
     * @return profile address.
     */
    public String getAddress3() {
        return this.profileAddress3;
    }

    /** Sets the third profile address.
     * @param address a profile addres of relevant type.
     */
    public void setAddress3(String profileAddress) {
        this.profileAddress3 = (profileAddress!=null ? profileAddress.trim() : null);
        if(this.profileAddress3!=null && this.profileAddress3.trim().length()==0) this.profileAddress3=null;
    }

    /** Returns the codeword for the emergency contact record
     * @return codeword.
     */
    public String getCodeword() {
        return this.codeword;
    }

    /** Sets the codeword for the emergency contact record
     */
    public void setCodeword(String codeword) {
        this.codeword = codeword;;
    }


    /** Returns the contact order for the action plan associated with the emergency contact record
     * @param actionNo action plan number, from 1 to 9.
     * @return contact order.
     */
    public short getActionPlanContactOrder (int actionNo) {
        if(actionNo<1 || actionNo>=MAX_ACTION_PLANS) return -1;
        
        //lazy init the array
        if(this.actionPlanOrder==null) {
            actionPlanOrder = new short[MAX_ACTION_PLANS];  //10 defined, only 1-9 used
            for(int i=0;i<MAX_ACTION_PLANS;i++) {
                this.actionPlanOrder[i] = -1;
            }
        }
        
        return this.actionPlanOrder[actionNo];
    }

    /** Sets the contact order for an action plan associated with the emergency contact record
     * @param actionNo action plan number, from 1 to 9.
     * @param actionOrder action plan contact order starting from 0. -1 indicates uninitialized and not synced with SBN. Contact orders must be unique for action plans set
     */
    public void setActionPlanContactOrder (int actionNo, short actionOrder) {
        if(actionNo<1 || actionNo>=MAX_ACTION_PLANS || actionOrder<-1 || actionOrder> Short.MAX_VALUE) return;
        
        //lazy init the array
        if(this.actionPlanOrder==null) {
            actionPlanOrder = new short[MAX_ACTION_PLANS];  //10 defined, only 1-9 used
            for(int i=0;i<MAX_ACTION_PLANS;i++) {
                this.actionPlanOrder[i] = -1;
            }
        }
        
        this.actionPlanOrder[actionNo] = actionOrder;
    }
    
    /**
     * Returns true if a notification is sent when the emergency contact changes or is deleted.
     */
    public boolean isSendNotification()
    {
        return this.isSendNotification;
    }
    
    /**
     * Sets the flag used to determine if a notification is sent when changing the emergency contact.
     */
    public void setSendNotification(boolean sendNotification)
    {
        this.isSendNotification = sendNotification;
    }
    /** Validates if the profile is a valid emergency contact or not
     */
    public boolean isValidEmergencyContact() {
        if(!isEmergencyContact) return false;
        if(this.name==null || this.name.trim().length()==0) return false;
        if(this.order<1 || this.order>5) return false;  //must specify an order between 1-5 for emergency contacts
        
        return isValidNotificationProfile();
    }

    /** Validates if the profile is a valid notification profile or not
     */
    public boolean isValidNotificationProfile() {
        if(!hasValidProfileAddress()) return false;
        if(this.profileAddress2!=null && this.profileType2==-99) return false;
        if(this.profileAddress3!=null && this.profileType3==-99) return false;
        return true;
    }

    /** Validates if the first profile type/address pair is valid or not
     */
    public boolean hasValidProfileAddress() {
        if(this.profileType==-99) return false;
        if(this.profileAddress==null || this.profileAddress.trim().length()==0) return false;
        return true;
    }

    /** Validates if the second profile type/address pair is valid or not
     */
    public boolean hasValidProfileAddress2() {
        if(this.profileType2==-99) return false;
        if(this.profileAddress2==null || this.profileAddress2.trim().length()==0) return false;
        return true;
    }

    /** Validates if the third profile type/address pair is valid or not
     */
    public boolean hasValidProfileAddress3() {
        if(this.profileType3==-99) return false;
        if(this.profileAddress3==null || this.profileAddress3.trim().length()==0) return false;
        return true;
    }


    public void dump() {
        System.err.println("PROFILE ID  :" + this.profileId);
        System.err.println("NAME  :" + this.name);
        System.err.println("ISEMERGENCYCONTACT    :" + (this.isEmergencyContact ? "YES" : "NO"));
        System.err.println("GGUID  :" + this.gguid);
        System.err.println("ORDER  :" + this.order);
        System.err.println("PROFILE_TYPE    :" + this.profileType);
        System.err.println("PROFILE_ADDRESS    :" + this.profileAddress);
        System.err.println("PROFILE_TYPE2   :" + this.profileType2);
        System.err.println("PROFILE_ADDRESS2    :" + this.profileAddress2);
        System.err.println("PROFILE_TYPE3   :" + this.profileType3);
        System.err.println("PROFILE_ADDRESS3   :" + this.profileAddress3);
        System.err.println("CODEWORD   :" + this.codeword);
        System.err.print("ACTION PLAN CONTACT ORDERS :");
        if(this.actionPlanOrder!=null) {
            for(int i=1;i<MAX_ACTION_PLANS;i++) {
                System.err.print(this.actionPlanOrder[i]);
                if(i<MAX_ACTION_PLANS-1) System.err.print(", ");
            }
        }
        System.err.println(" ");
    }
    
}
