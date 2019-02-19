/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/account/XanbooAccount.java,v $
 * $Id: XanbooAccount.java,v 1.8 2010/10/08 18:24:59 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.account;

import java.util.ArrayList;
import java.util.List;

import com.xanboo.core.sdk.contact.XanbooContact;
import com.xanboo.core.sdk.util.XanbooResultSet;

// TODO: Auto-generated Javadoc
/**
 * Class to represent Xanboo Subscriptions.
 */
public class XanbooSubscription implements java.io.Serializable {
    private static final long serialVersionUID = 4921681098988230182L;
    
    /** The Constant DEFAULT_SUBS_FLAGS. */
    public static final int     DEFAULT_SUBS_FLAGS          =    3+4+0+0+0;   // =23  ==> 3(11:active) + 4(01:3g+BB profile) + 0(0:promon disabled) + 0(0:concierge disabled) + 0(0:video ver. disabled):
    
    /** The Constant DEFAULT_SUBS_FLAGS_DOMESTIC. */
    public static final int     DEFAULT_SUBS_FLAGS_DOMESTIC =    3+4+16+0+0;   // =23  ==> 3(11:active) + 4(01:3g+BB profile) + 16(1:promon enabled) + 0(0:concierge disabled) + 0(0:video ver. disabled):
    
    /** Holds the owner account id for the subscription. */
    private long accountId;
    
    /** Holds the owner external account id (if available). */
    private String extAccountId;
    
    /** Holds the unique subscription identifier (e.g. CTN). */
    private String subsId;

    /** Holds a unique subscription hardware identifier (e.g. IMEI, MAC, SerialNo). If one not set during subscription creation, the hwid is set same as subsId as a temporary
     hardware identifier */
    private String hwId;
    
    /** the gateway guid assigned to the subscription hardware (if available). */
    private String gguid;
    
    private String subscriptionClass = "DLSEC";
    
    /** A positive integer value for service subscription flags (see controller descriptor oid 40 definition). Will be set, if >-1. */
    private int subsFlags;

    /** A positive integer bitmask value for service subscription flags. Bits with 1 in the mask will cause the corresponding bit in the subscription
     * flag to be set/updated with the value passed in the subs_flag parameter. If not specified or -1, the flags will be set as specified in subsFlags.*/
    private int subsFlagsMask;

    /** A CSV list of feature keywords enabled for the subscription (see controller descriptor oid 41 definition). */
    private String subsFeatures;
    
    
    /** Default subscription name/label. */
    private String label;
    
    /** Default timezone name (Olson) to be assigned to the associated gateway. */
    private String tzone;

    /** Holds the subscription business market code. */
    private String bMarket;    

    /** Holds the subscription business submarket code. */
    private String bSubMarket;    
    
    
    /** Holds the subscription name/address details. */
    private XanbooContact subsInfo;

    /** Holds the subscription disarm pin. */
    private String disarmPin;
    
    /** Holds the subscription duress pin. */
    private String duressPin;

    /** Holds the subscription alarm passcode. */
    private String alarmCode;
    
    /** the date the subscription was created in iso formatted string. */
    private String dateCreated;
    
    /** the date the subscription was cancelled in iso formatted string, if applicable. */
    private String dateCancelled;

    /** the terms and conditions acceptance flag (0/1) for the subscription, if applicable. */
    private int tcFlag;

    /** an optional professional monitoring alarm delay flag (0/1) for the subscription, if applicable. */
    private int alarmDelay;
    
    /** an optional install type ('P' - Professional Install, 'S' - Self Install. */
    private String installType;
    
    
    /** Optional, an array of XanbooNotificationProfile objects to be used as emergency contacts for the subscription. Max 5 entries allowed. */
    private XanbooNotificationProfile[] nProfiles;

    
    
    /**
     * Default constructor: Creates new XanbooAccount.
     */
    public XanbooSubscription() {
        this.accountId=-1L;
        this.extAccountId=null;
        this.subsId=null;
        this.hwId=null;
        this.subsFlags=-1;
        this.subsFlagsMask=-1;
        this.subsFeatures=null;
        this.subsInfo=null;
        this.gguid=null;
    }
    
    
     /**
      * Creates a new Xanboo Subscription from given parameters.
      *
      * @param accountId    account id
      * @param extAccountId an external account id for the account
      */
    public XanbooSubscription(long accountId, String extAccountId) {
        this.accountId=accountId;
        this.extAccountId=extAccountId;
        this.subsId=null;
        this.hwId=null;
        this.subsFlags=-1;
        this.subsFlagsMask=-1;
        this.subsFeatures=null;
        this.subsInfo=null;
        this.gguid=null;
    }
    
     /**
      * Creates a new Xanboo Subscription from a XanbooResultSet object and given row.
      *
      * @param xrs a XanbooResulSet object containing subscription query results from an SDK call
      * @param row row number to construct the object from. Must be a >=0 value and cannot be bigger than resulset size
      * @throws InstantiationException the instantiation exception
      */
    public XanbooSubscription(XanbooResultSet xrs, int row) throws InstantiationException {
        if(xrs==null || row<0 || row>=xrs.size()) {
            throw new InstantiationException("Invalid resultset or row");
        }

        this.accountId=xrs.getElementLong(row, "ACCOUNT_ID");
        this.extAccountId=xrs.getElementString(row, "EXT_ACCOUNT_ID");
        this.subsId=xrs.getElementString(row, "SUBS_ID");
        this.hwId=xrs.getElementString(row, "HWID");
        this.gguid=xrs.getElementString(row, "GATEWAY_GUID");
        this.subsFlags=xrs.getElementInteger(row, "SUBS_FLAGS");
        this.subsFeatures=xrs.getElementString(row, "SUBS_FEATURES");
        this.label=xrs.getElementString(row, "LABEL");
        this.tzone=xrs.getElementString(row, "TZNAME");
        this.bMarket=xrs.getElementString(row, "BMARKET");
        this.bSubMarket=xrs.getElementString(row, "BSUBMARKET");
        this.disarmPin=xrs.getElementString(row, "MASTER_PIN");
        this.duressPin=xrs.getElementString(row, "MASTER_DURESS");
        this.alarmCode=xrs.getElementString(row, "ALARMPASS");
        this.dateCreated=xrs.getElementString(row, "DATE_CREATED");
        this.dateCancelled=xrs.getElementString(row, "DATE_CANCELLED");
        this.tcFlag=xrs.getElementInteger(row, "TC_FLAG");
        this.alarmDelay=xrs.getElementInteger(row, "ALARM_DELAY");
        this.installType=xrs.getElementString(row, "INSTALL_TYPE");
        this.subscriptionClass=xrs.getElementString(row, "SUBS_TYPE");
        if ( subscriptionClass == null || subscriptionClass.equalsIgnoreCase(""))
            subscriptionClass = "DLSEC";

        XanbooContact info = new XanbooContact();
        info.setAccountId(this.accountId);
        info.setAddress1(xrs.getElementString(row, "ADDRESS1"));
        info.setAddress2(xrs.getElementString(row, "ADDRESS2"));
        info.setLastName(xrs.getElementString(row, "LASTNAME"));
        info.setFirstName(xrs.getElementString(row, "FIRSTNAME"));
        info.setCity(xrs.getElementString(row, "CITY"));
        info.setState(xrs.getElementString(row, "STATE"));
        info.setCountry(xrs.getElementString(row, "COUNTRY"));
        info.setZip(xrs.getElementString(row, "ZIP"));
        info.setZip4(xrs.getElementString(row, "ZIP4"));
        this.setSubsInfo(info);

        this.subsFlagsMask=-1;
    }
    
    
    /** Gets the account id associated with the subscription.
     * @return account id value.
     */
    public long getAccountId() {
        return this.accountId;
    }
    
    /** Sets the account id associated with the subscription.
     * @param accountId account id value to set.
     */
    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }
    
    /** Gets the external account id associated with the subscription.
     * @return the external account id.
     */
    public String getExtAccountId() {
        return extAccountId;
    }
    
    /** Sets the external account id associated with the subscription.
     * @param extAccountId the external account id value.
     */
    public void setExtAccountId(String extAccountId) {
        if(extAccountId!=null) extAccountId=extAccountId.trim();
        this.extAccountId = extAccountId;
    }
    
    /** Gets the subscription identifier (e.g. CTN)
     * @return the subscription id.
     */
    public String getSubsId() {
        return subsId;
    }

    /** Sets the the subscription identifier (e.g. CTN)
     * @param subsId the subscription id value.
     */
    public void setSubsId(String subsId) {
        if(subsId!=null) subsId=subsId.trim();
        this.subsId = subsId;
    }

    /** Gets the subscription hardware identifier (e.g. IMEI)
     * @return the subscription hardware id.
     */
    public String getHwId() {
        return hwId;
    }

    /** Sets the the subscription hardware identifier (e.g. IMEI)
     * @param hwId the subscription hardware id value.
     */
    public void setHwId(String hwId) {
        if(hwId!=null) hwId=hwId.trim();
        this.hwId = hwId;
    }

    /**
     * Gets the gateway guid associated with the subscription hardware.
     *
     * @return the subscription hardware gguid.
     */
    public String getGguid() {
        return gguid;
    }
    public String getSubscriptionClass()
    {
        return this.subscriptionClass;
    }
    public void setSubscriptionClass(String subsClass)
    {
        this.subscriptionClass = subsClass;
    }
        
    /**
     * Gets the service subscription flags.
     *
     * @return the subscription flags.
     */
    public int getSubsFlags() {
        return subsFlags;
    }

    /**
     * Sets the subscription flags.
     *
     * @param subsFlags the subscription flags value.
     */
    public void setSubsFlags(int subsFlags) {
        this.subsFlags = subsFlags;
    }

    /**
     * Gets the service subscription flags.
     *
     * @return the subscription flags.
     */
    public int getSubsFlagsMask() {
        return subsFlagsMask;
    }

    /**
     * Sets the subscription flags.
     *
     * @param subsFlagsMask the new subs flags mask
     */
    public void setSubsFlagsMask(int subsFlagsMask) {
        this.subsFlagsMask = subsFlagsMask;
    }
    
    /** Gets the subscription features string
     * @return the subscription features CSV list.
     */
    public String getSubsFeatures() {
        return subsFeatures;
    }

    /** Sets the the subscription features
     * @param subsId the subscription features CSV list value.
     */
    public void setSubsFeatures(String subsFeatures) {
        if(subsFeatures!=null) subsFeatures=subsFeatures;
        this.subsFeatures = subsFeatures;
    }
    
    /** Gets the subscription name/label assigned (e.g. location name)
     * @return the subscription name/label.
     */
    public String getLabel() {
        return label;
    }

    /** Sets the subscription name/label assigned (e.g. location name)
     * @param label the subscription name/label value.
     */
    public void setLabel(String label) {
        this.label = label;
    }

    
    /**
     * Gets the subscription timezone.
     *
     * @return the subscription timezone.
     */
    public String getTzone() {
        return tzone;
    }

    /**
     * Sets the subscription timezone.
     *
     * @param tzone the subscription timezone.
     */
    public void setTzone(String tzone) {
        this.tzone = tzone;
    }

    /** Gets the subscription name/address/contact information.
     * @return a XanbooContact object, containing name/address/contact information for the subscription.
     */
    public XanbooContact getSubsInfo() {
        return subsInfo;
    }

    /** Sets the subscription name/address/contact information.
     * @param subsInfo a XanbooContact object, containing name/address/contact information for the subscription.
     */
    public void setSubsInfo(XanbooContact subsInfo) {
        this.subsInfo = subsInfo;
    }

    /** Gets notification profiles defined for the subscription (e.g. emergency contacts).
     * @return an array of XanbooNotificationProfile objects.
     */
    public XanbooNotificationProfile[] getNotificationProfiles() {
        return nProfiles;
    }

    /** Sets the notification profiles defined for the subscription (e.g. emergency contacts).
     * @param nProfiles an array of XanbooNotificationProfile objects.
     */
    public void setNotificationProfiles(XanbooNotificationProfile[] nProfiles) {
        this.nProfiles = nProfiles;
    }

    
    /** Gets the primary alarm passcode (codeword) for the subscription/installation (encrypted).
     * @return the alarm passcode value (encrypted).
     */
    public String getAlarmCode() {
        return alarmCode;
    }

    /** Sets the primary alarm passcode (codeword) for the subscription/installation.
     * @param alarmCode an alarm passcode value.
     */
    public void setAlarmCode(String alarmCode) {
        this.alarmCode = alarmCode;
    }

    /** Gets the primary disarm PIN for the subscription/installation (hashed).
     * @return the disarm pin value (hashed).
     */
    public String getDisarmPin() {
        return disarmPin;
    }

    /** Sets the primary disarm PIN for the subscription/installation (hashed).
     * @param disarmPin a disarm pin value (hashed).
     */
    public void setDisarmPin(String disarmPin) {
        this.disarmPin = disarmPin;
    }

    /** Gets the primary duress PIN for the subscription/installation (hashed).
     * @return the duress pin value (hashed).
     */
    public String getDuressPin() {
        return duressPin;
    }

    /** Sets the primary duress PIN for the subscription/installation (hashed).
     * @param duressPin a duress pin value (hashed).
     */
    public void setDuressPin(String duressPin) {
        this.duressPin = duressPin;
    }

    /**
     * Gets the service subscription market code.
     *
     * @return the subscription market code.
     */
    public String getbMarket() {
        return bMarket;
    }

    /**
     * Sets the service subscription market code.
     *
     * @param bMarket the subscription market code value.
     */
    public void setbMarket(String bMarket) {
        this.bMarket = bMarket;
    }

    /**
     * Gets the service subscription submarket code.
     *
     * @return the subscription submarket code.
     */
    public String getbSubMarket() {
        return bSubMarket;
    }

    /**
     * Sets the service subscription submarket code.
     *
     * @param bSubMarket the subscription submarket code value.
     */
    public void setbSubMarket(String bSubMarket) {
        this.bSubMarket = bSubMarket;
    }

    /**
     * Gets the service subscription terms and conditions flag.
     *
     * @return the subscription t&c flag (0/1 value)
     */
    public int getTcFlag() {
        return tcFlag;
    }

    /**
     * Sets the service subscription terms and conditions flag.
     *
     * @param tcFlag the subscription t&c flag (0/1 value)
     */
    public void setTcFlag(int tcFlag) {
        this.tcFlag = tcFlag;
    }
    
    /**
     * Gets the service subscription alarm delay.
     *
     * @return the subscription alarm delay (0/1 value)
     */
    public int getAlarmDelay() {
        return alarmDelay;
    }

    /**
     * Sets the service subscription alarm delay.
     *
     * @param alarmDelay the subscription alarm delay (0/1 value)
     */
    public void setAlarmDelay(int alarmDelay) {
        this.alarmDelay = alarmDelay;
    }

    
    
    /**
     * Gets the date created.
     *
     * @return the date created
     */
    public String getDateCreated() {
		return dateCreated;
	}


	/**
	 * Sets the date created.
	 *
	 * @param dateCreated the new date created
	 */
	public void setDateCreated(String dateCreated) {
		this.dateCreated = dateCreated;
	}


	/**
	 * Gets the date cancelled.
	 *
	 * @return the date cancelled
	 */
	public String getDateCancelled() {
		return dateCancelled;
	}


	/**
	 * Sets the date cancelled.
	 *
	 * @param dateCancelled the new date cancelled
	 */
	public void setDateCancelled(String dateCancelled) {
		this.dateCancelled = dateCancelled;
	}


	/**
	 * Sets the gguid.
	 *
	 * @param gguid the new gguid
	 */
	public void setGguid(String gguid) {
		this.gguid = gguid;
	}
	
	/**
	 * @return the installType
	 */
	public String getInstallType() {
		return installType;
	}

	/**
	 * @param installType the installType to set
	 */
	public void setInstallType(String installType) {
		this.installType = installType;
	}


	/** Checks if the subscription is valid to create a new subscription.
     * @return boolean to indicate the object contains valid data to create a new subscription.
     */
    public boolean isValidSubscriptionToCreate() {
        if(this.accountId<=0 || this.subsId==null || this.label==null || this.tzone==null || this.subsInfo==null || this.subsInfo.getLastName()==null || this.subsInfo.getFirstName()==null ||
            this.subsInfo.getAddress1()==null || this.subsInfo.getCity()==null || this.subsInfo.getZip()==null ||
             this.subsId.trim().length()==0 || this.label.trim().length()==0 || this.tzone.trim().length()==0 ||
              this.subsInfo.getLastName().trim().length()==0 || this.subsInfo.getFirstName().trim().length()==0) {
            return false;
        }
        
        //if no hw id specified, hw id will be set to subs id!!!
        if(this.hwId==null || this.hwId.trim().length()==0) {
            this.hwId=this.subsId;
        }
        
        
        /* //disable CTN/IMEI specific checks to make it generic
        if(subsId.length()>10 || hwId.length()>16) {    //allow 10digit ctn, and 16digit imei max
            throw new XanbooException(10050);
        }
        */
        
        return true;
    }

    /** Checks if the subscription is valid to update an existing subscription.
     * @return boolean to indicate the object contains valid data to update an existing subscription.
     */
    public boolean isValidSubscriptionToUpdate() {
        
    	//if no hw id specified, hw id will be set to subs id!!!
        if(this.hwId==null || this.hwId.trim().length()==0) {
            this.hwId=this.subsId;
        }
    	if(this.accountId<=0 || this.subsId==null || this.subsId.trim().length()==0) {
            return false;
        }
        
        return true;
        
    }

    
    /**
     * checks if subscription flags value is to be updated.
     *
     * @return true, if is subs flags to be updated
     */
    public boolean isSubsFlagsToBeUpdated() {
        if(subsFlags==-9997 || subsFlags==-9998 || subsFlags==-9999 || subsFlags==-9994 || subsFlags==-9995 || subsFlags>-1) return true;
        return false;
    }
    

    /**
     * checks if subscription status flags value is to be updated.
     *
     * @return true, if is subs flags to be updated
     */
    public boolean isSubsStatusToBeUpdated() {
        if(subsFlags==-9997 || subsFlags==-9998 || subsFlags==-9999 || subsFlags>-1) return true;
        return false;
    }

    
    /**
     * checks if subscription attributes indicate cancelled service or not.
     *
     * @return true, if is cancelled
     */
    public boolean isCancelled() {
        if(subsFlags==-9997) return true;
        if(subsFlags>0 && subsFlagsMask<=0 && (subsFlags & 3)==0) return true;     //check low 2 bits, ignore mask
        if(subsFlags>0 && subsFlagsMask>00 && (subsFlagsMask & 3)>0 && (subsFlags & 3)==0) return true;    // if mask has low 2 bits enabled, check low bits again
                
        return false;
    }

    /**
     * checks if subscription attributes indicate suspended service or not.
     *
     * @return true, if is suspended
     */
    public boolean isSuspended() {
        if(subsFlags==-9998) return true;
        if(subsFlags>0 && subsFlagsMask<=0 && ((subsFlags & 3)==1 || (subsFlags & 3)==2)) return true;     //check low 2 bits, ignore mask
        if(subsFlags>0 && subsFlagsMask>00 && (subsFlagsMask & 3)>0 && ((subsFlags & 3)==1 || (subsFlags & 3)==2)) return true;    // if mask has low 2 bits enabled, check low bits again
                
        return false;
    }

    /**
     * checks if subscription attributes indicate active service or not.
     *
     * @return true, if is active
     */
    public boolean isActive() {
        if(subsFlags==-9999) return true;
        if(subsFlags>0 && subsFlagsMask<=0 && (subsFlags & 3)==3) return true;     //check low 2 bits, ignore mask
        if(subsFlags>0 && subsFlagsMask>00 && (subsFlagsMask & 3)>0 && (subsFlags & 3)==3) return true;    // if mask has low 2 bits enabled, check low bits again
                
        return false;
    }
    
    /**
     * checks if subscription hwid set is temporary or not.
     *
     * @return true, if is temporary hw id
     * @throws IllegalArgumentException the illegal argument exception
     */
    public boolean isTemporaryHwId() throws IllegalArgumentException {
        if(subsId==null || subsId.trim().length()==0) throw new IllegalArgumentException("A subscription id required.");
        if(hwId==null || hwId.trim().length()==0) return true;
        if(hwId.equals(subsId)) return true;    //if same, return true
        return false;
    }

    /**
     * checks if provided subscription hwid set is temporary or not.
     *
     * @param subsId the subs id
     * @param hwId the hw id
     * @return true, if is temporary hw id
     * @throws IllegalArgumentException the illegal argument exception
     */
    public static boolean isTemporaryHwId(String subsId, String hwId)  throws IllegalArgumentException {
        if(subsId==null || subsId.trim().length()==0) throw new IllegalArgumentException("A subscription id required.");
        if(hwId==null || hwId.trim().length()==0) return true;
        if(hwId.equals(subsId)) return true;    //if same, return true
        return false;
    }
    //appeneds the feature id keyword to the end if the list (when not already present)
    public void appendFeatureId(String featureId)
    {
        if ( subsFeatures == null || subsFeatures.equalsIgnoreCase(""))
            subsFeatures = featureId;
        else if ( subsFeatures.indexOf(featureId) < 0 )
            subsFeatures = new StringBuilder(subsFeatures).append(",").append(featureId).toString();
    }
    
    /**
     * Method to convert CSV string to list if subscription features.
     * @param featuresCSV - comma separated features
     * @return - list of subscription features.
     */
    public static List<SubscriptionFeature> toFeature(String featuresCSV){
    	String[] strArray = featuresCSV.split(",");
    	List<SubscriptionFeature> lstSubFeat = new ArrayList<SubscriptionFeature>();
    	
    	for (String string : strArray) {
    		if(string != null && string.trim().equalsIgnoreCase("")){ continue; }
    		SubscriptionFeature subscriptionFeature = new SubscriptionFeature();
    		subscriptionFeature.setFeatureId(string);
    		lstSubFeat.add(subscriptionFeature);
		}
    	
    	return lstSubFeat;
    }
    
    /**
     * Method to convert list of features to CSV string
     * @param featureList - list of features.
     * @return - string of comma separated feature values.
     */
    public static String toFeature(List<SubscriptionFeature> featureList){
    	StringBuffer strBuffSubFea = new StringBuffer();
    	for (SubscriptionFeature subscriptionFeature : featureList) {
			strBuffSubFea.append(subscriptionFeature.getFeatureId() + ",");
		}
    	strBuffSubFea.setLength(strBuffSubFea.length()-1);
    	return strBuffSubFea.toString();
    }

    /**
     * Method to validate if a subscription instance has a specific feature enabled/opted-in or out.
     * @param featureKeyword - validate again available subscription features.
     * @return true if found otherwise false.
     */
    public boolean hasFeature(String featureKeyword){
    	return getSubsFeatures().contains(featureKeyword);
    }


	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString()
	{
	    final String TAB = "    ";
	    
	    String retValue = "";
	    
	    retValue = "XanbooSubscription ( "
	        + super.toString() + TAB
	        + "accountId = " + this.accountId + TAB
	        + "extAccountId = " + this.extAccountId + TAB
	        + "subsId = " + this.subsId + TAB
	        + "hwId = " + this.hwId + TAB
	        + "gguid = " + this.gguid + TAB
	        + "subsFlags = " + this.subsFlags + TAB
	        + "subsFlagsMask = " + this.subsFlagsMask + TAB
	        + "subsFeatures = " + this.subsFeatures + TAB
	        + "label = " + this.label + TAB
	        + "tzone = " + this.tzone + TAB
	        + "bMarket = " + this.bMarket + TAB
	        + "bSubMarket = " + this.bSubMarket + TAB
	        + "subsInfo = " + this.subsInfo + TAB
	        + "disarmPin = " + this.disarmPin + TAB
	        + "duressPin = " + this.duressPin + TAB
	        + "alarmCode = " + this.alarmCode + TAB
	        + "dateCreated = " + this.dateCreated + TAB
	        + "dateCancelled = " + this.dateCancelled + TAB
	        + "tcFlag = " + this.tcFlag + TAB
	        + "alarmDelay = " + this.alarmDelay + TAB
	        + "installType = " + this.installType + TAB
	        + "nProfiles = " + this.nProfiles + TAB
	        + " )";
	
	    return retValue;
	}
    
}
