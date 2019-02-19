/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/account/XanbooQuota.java,v $
 * $Id: XanbooQuota.java,v 1.7 2007/03/08 13:15:32 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.account;

/**
 * <p>Represents a Quota object in Xanboo system.</p>
 * <p>
 * A quota allows you to specify limits on a user's usage of certain aspects of the Xanboo system, including disk storage and number of devices installed.
 * </p>
 * <p>
 *    Important properties of the XanbooQuota object are:
 *      <ul>
 *          <li><b>quotaId</b><br>
 *              Specifies the entity to which the quota applies, one of XanbooQuota constants DEVICE, DISK, GATEWAY or USER
 *          </li>
 *          <li><b>quotaClass</b><br>
 *              For device quotas, specifies the 4-character class of the device type to which the quota applies.<br>
 *              Note that if a device quota uses a major class ID, it specifies a quota global to that entire class. For example, a device quota for class '0200' indicates a limit on the total number of cameras, regardless of whether they are wired (0201), wireless (0202) etc.
 *              For notification quotas, specified the profile type id to which the quota applies.
 *          </li>
 *          <li><b>quotaValue</b><br>
 *              Specifies the quota value itself, the limit to impose on this entity. The constant XanbooQuota.UNLIMITED may be used to impose no quota limit.
 *          </li>
 *          <li><b>currentValue</b><br>
 *              Indicates the account's current usage of this entity.
 *          </li>
 *          <li><b>resetPeriod</b><br>
 *              Specifies how often the usage counter for this quota will be reset to zero. A zero
 *              period value will never reset the counter. Value must be between 0 and 31.
 *          </li>
 *      </ul>
 * </p>
 * <p>
 * 
 * If you are familiar with the device class concept, you may have noticed that as well as being specified with a quotaId of XanbooQuota.GATEWAY,
 * a gateway quota can also be specified with a quotaId of XanbooQuota.DEVICE, and a quotaClass of '0000'. Note that the SDK will accept either specification for updateQuota, 
 * but will always return a gateway quota specified with a quotaId of XanbooQuota.GATEWAY and a quotaClass of '0000'.
 */
public class XanbooQuota  implements java.io.Serializable {
    private static final long serialVersionUID = 743675336640668272L;

    /** Quota reference id constant for User Quotas */
    public static final int USER=0;

    /** Quota reference id constant for Disk Quotas */
    public static final int DISK=1;
        
    /** Quota reference id constant for Gateway Quotas */
    public static final int GATEWAY=2;
    
    /** Quota reference id constant for Device Quotas. quotaClass specifies device class
     *  to apply quota on.
     */
    public static final int DEVICE=3;

    /** Quota reference id constant for Notification Quotas. quotaClass specifies notification
     *  profile type to apply quota on.
     */
    public static final int NOTIFICATION=4;
    
    /** Quota value constant used to specify unlimited quota value */
    public static final int UNLIMITED=-1;
    
    /** Holds the account id for the quota. */
    private long accountId;

    /** Holds the quota reference id. Possible values are XanbooQuota.USER, XanbooQuota.DISK and XanbooQuota.GATEWAY */
    private int quotaId;
   
    /** Holds the quota value. */
    private long quotaValue;
    
    /** Holds the current usage counter value. */
    private long currentValue;
    
    /** Holds the optional quota class property. This is used to hold class ids for device quotas
        and profiletype ids for notification quotas
     */
    private String quotaClass;
    
    /** Holds the day of the month, when the quota usage counter will be reset to 0.
        A 0 value will not reset (default).
     */
    private int resetPeriod;
    
    
    /** Default constructor: Creates a new XanbooQuota object */
    public XanbooQuota() {
        this.accountId=-1;
        this.quotaId=-1;
        this.quotaValue=-1;
        this.currentValue=-1;
        this.resetPeriod=0;
    }

    
    /** Creates a new XanbooQuota object from given parameters.
     * @param accountId Identifies the account to which the quota refers.
     * @param quotaId quota reference id.
     * @param quotaClass Device class ID for DEVICE type quota
     * @param quotaValue current quota value
     * @param currentValue current quota usage value 
     * @param resetPeriod the day of the month to reset quota usage counter. O: no reset
     * @deprecated
     */
    public XanbooQuota( long accountId, int quotaId, String quotaClass, int quotaValue, int currentValue, int resetPeriod ) {
        this.accountId = accountId<0 ? 0 : accountId;
        this.quotaClass = quotaClass;
        this.quotaValue = quotaValue<0 ? 0 : quotaValue;
        this.currentValue = currentValue<0 ? 0 : currentValue;
        this.quotaId = (quotaId<0 || quotaId>4) ? XanbooQuota.USER : quotaId;
        this.resetPeriod = (resetPeriod<0 || resetPeriod>31) ? 0 : resetPeriod;
    }
    
    /** Creates a new XanbooQuota object from given parameters.
     * @param accountId Identifies the account to which the quota refers.
     * @param quotaId quota reference id.
     * @param quotaClass Device class ID for DEVICE type quota
     * @param quotaValue current quota value
     * @param currentValue current quota usage value 
     * @param resetPeriod the day of the month to reset quota usage counter. O: no reset
     */
    public XanbooQuota( long accountId, int quotaId, String quotaClass, long quotaValue, long currentValue, int resetPeriod ) {
        this.accountId = accountId<0 ? 0 : accountId;
        this.quotaClass = quotaClass;
        this.quotaValue = quotaValue<0 ? 0 : quotaValue;
        this.currentValue = currentValue<0 ? 0 : currentValue;
        this.quotaId = (quotaId<0 || quotaId>4) ? XanbooQuota.USER : quotaId;
        this.resetPeriod = (resetPeriod<0 || resetPeriod>31) ? 0 : resetPeriod;
    }
    
    
    /** Creates a new XanbooQuota object from given parameters.
     * @param accountId Identifies the account to which the quota refers.
     * @param quotaId quota reference id.
     * @param quotaClass Device class ID for DEVICE type quota
     * @param quotaValue current quota value
     * @param currentValue current quota usage value 
     * @deprecated
     */
    public XanbooQuota( long accountId, int quotaId, String quotaClass, int quotaValue, int currentValue ) {
        this.accountId = accountId;
        this.quotaClass = quotaClass;
        this.quotaValue = quotaValue;
        this.currentValue = currentValue;
        this.quotaId = quotaId;
    }

    /** Creates a new XanbooQuota object from given parameters.
     * @param accountId Identifies the account to which the quota refers.
     * @param quotaId quota reference id.
     * @param quotaClass Device class ID for DEVICE type quota
     * @param quotaValue current quota value
     * @param currentValue current quota usage value 
     */
    public XanbooQuota( long accountId, int quotaId, String quotaClass, long quotaValue, long currentValue ) {
        this.accountId = accountId;
        this.quotaClass = quotaClass;
        this.quotaValue = quotaValue;
        this.currentValue = currentValue;
        this.quotaId = quotaId;
    }

    /** Creates a new XanbooQuota object from given parameters.
     * @param accountId Identifies the account to which the quota refers.
     * @param quotaId quota reference id.
     * @param quotaClass Device class ID for DEVICE type quota
     * @param quotaValue current quota value
     * @param currentValue current quota usage value 
     * @deprecated
     */
    public XanbooQuota( int quotaId, String quotaClass, int quotaValue, int currentValue ) {
        this.quotaId = quotaId;
        this.quotaClass = quotaClass;
        this.quotaValue = quotaValue;
        this.currentValue = currentValue;
    }
    
    /** Creates a new XanbooQuota object from given parameters.
     * @param accountId Identifies the account to which the quota refers.
     * @param quotaId quota reference id.
     * @param quotaClass Device class ID for DEVICE type quota
     * @param quotaValue current quota value
     * @param currentValue current quota usage value 
     */
    public XanbooQuota( int quotaId, String quotaClass, long quotaValue, long currentValue ) {
        this.quotaId = quotaId;
        this.quotaClass = quotaClass;
        this.quotaValue = quotaValue;
        this.currentValue = currentValue;
    }
    
    /** Creates a new XanbooQuota object from given parameters.
     * @param accountId Identifies the account to which the quota refers.
     * @param quotaId quota reference id.
     * @param quotaValue current quota value
     * @param currentValue current quota usage value 
     * @deprecated
     */
    public XanbooQuota(long accountId, int quotaId, int quotaValue, int currentValue) {
        this.accountId=accountId;
        this.quotaId=quotaId;
        this.quotaValue=quotaValue;
        this.currentValue=currentValue;
    }
    
    /** Creates a new XanbooQuota object from given parameters.
     * @param accountId Identifies the account to which the quota refers.
     * @param quotaId quota reference id.
     * @param quotaValue current quota value
     * @param currentValue current quota usage value 
     */
    public XanbooQuota(long accountId, int quotaId, long quotaValue, long currentValue) {
        this.accountId=accountId;
        this.quotaId=quotaId;
        this.quotaValue=quotaValue;
        this.currentValue=currentValue;
    }
    
    /** Creates a new XanbooQuota object from given parameters.
     * @param quotaId quota reference id.
     * @param quotaValue current quota value
     * @param currentValue current quota usage value 
     * @deprecated
     */
    public XanbooQuota(int quotaId, int quotaValue, int currentValue) {
        this.quotaId=quotaId;
        this.quotaValue=quotaValue;
        this.currentValue=currentValue;
    }

    /** Creates a new XanbooQuota object from given parameters.
     * @param quotaId quota reference id.
     * @param quotaValue current quota value
     * @param currentValue current quota usage value 
     */
    public XanbooQuota(int quotaId, long quotaValue, long currentValue) {
        this.quotaId=quotaId;
        this.quotaValue=quotaValue;
        this.currentValue=currentValue;
    }

    /** Gets user account id.
     * @return user account id.
     */
    public long getAccountId() {
        return this.accountId;
    }
    
    
    /** Gets the quota reference id.
     * @return quota reference id. Possible values are XanbooQuota.USER, XanbooQuota.DISK and XanbooQuota.GATEWAY.
     */
    public int getQuotaId() {
        return this.quotaId;
    }
    
    /** Sets the quota reference id.
     * @param quotaId quota reference id to set. Possible values are XanbooQuota.USER, XanbooQuota.DISK, and XanbooQuota.GATEWAY.
     */
    public void setQuotaId(int quotaId) {
        this.quotaId = quotaId;
    }

    /** Gets the current quota value.
     * @return quota value.
     * @deprecated
     */
    public int getQuotaValue() {
        return (int)this.quotaValue;
    }

    /** Gets the current quota value.
     * @return quota value.
     */
    public long getQuotaValueLong() {
        return this.quotaValue;
    }

    /** Gets the current quota usage value.
     * @return current quota usage.
     * @deprecated
     */
    public int getCurrentValue() {
        return (int)this.currentValue;
    }

    /** Gets the current quota usage value.
     * @return current quota usage.
     */
    public long getCurrentValueLong() {
        return this.currentValue;
    }

    /** Gets the current quota reset day of the month.
     * @return reset period.
     */
    public int getResetPeriod() {
        return this.resetPeriod;
    }

    /** Gets the device class ID attribute for device class quotas.
     * @return device class ID for device
     */
    public String getQuotaClass() {
        return this.quotaClass;
    }

    
    /** Sets user account id.
     * @param accountId user account id to set.
     */
    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }
       
    /** Sets the current quota value.
     * @param quotaValue new quota to set.
     * @deprecated
     */
    public void setQuotaValue(int quotaValue) {
        this.quotaValue = (long)quotaValue;
    }
    
    /** Sets the current quota value.
     * @param quotaValue new quota to set.
     */
    public void setQuotaValue(long quotaValue) {
        this.quotaValue = quotaValue;
    }
    
    /** Sets the current quota value.
     * @param quotaValue new quota to set.
     */
    public void setQuotaValueLong(long quotaValue) {
        this.quotaValue = quotaValue;
    }
       
    /** Sets the current quota usage value.
     * @param currentValue quota usage value to set.
     * @deprecated
     */
    public void setCurrentValue(int currentValue) {
        this.currentValue = (long)currentValue;
    }
    
    /** Sets the current quota usage value.
     * @param currentValue quota usage value to set.
     */
    public void setCurrentValue(long currentValue) {
        this.currentValue = currentValue;
    }

    /** Sets the current quota usage value.
     * @param currentValue quota usage value to set.
     */
    public void setCurrentValueLong(long currentValue) {
        this.currentValue = currentValue;
    }

    /** Sets the day of the month to reset quota usage counter.
     * @param period day of the month. 0 will not reset 
     */
    public void setResetPeriod(int resetPeriod) {
        this.resetPeriod = resetPeriod;
    }

    /** Sets the device class ID attribute for device class quotas.
     * @param quotaClass the class ID for this quota
     */
    public void setQuotaClass( String quotaClass ) {
        this.quotaClass = quotaClass;
    }
    
    /**
     * Generates a string represntation of the object.
     * @return A string containing the xml represntation.
     */
    public String toXML() {
        StringBuffer sb = new StringBuffer();
        sb.append( "<XanbooQuota>\n" );
        sb.append("     <accountId>").append(this.accountId).append("</accountId>\n");
        sb.append("     <quotaId>").append(this.quotaId).append("</quotaId>\n");
        sb.append("     <quotaClass>").append(this.quotaClass).append("</quotaClass>\n");
        sb.append("     <quotaValue>").append(this.quotaValue).append("</quotaValue>\n");
        sb.append("     <currentValue>").append(this.currentValue).append("</currentValue>\n");
        sb.append("     <resetPeriod>").append(this.resetPeriod).append("</resetPeriod>\n");
        sb.append( "</XanbooQuota>\n" );
        return sb.toString();
    }
    
}
