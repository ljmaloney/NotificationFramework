package com.xanboo.core.sdk.energy;

import java.rmi.RemoteException;
import java.util.Date;



import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.XanbooException;

/**
 * Remote interface for EnergyManagerEJB. <br/>
 * <br/>
 * <h2>Glossary</h2>
 * <h3>Parameters</3>
 * <li>XanbooPrincipal - Required for all getUsageData and getUsageDataByType methods. Used to specify the account
 * <li>GGUID - The gateway GUID, specified to filter the results by a specific gateway. 
 * <li>DGUID - The device GUID or list of device GUIDs. This parameter is used to restrict the results to the specified device or devices. 
 *             When this parameter is used, the GGUID is also required.
 * <li>includeDetails - True | False. When true, all devices associated with the specified gateway & account are returned. When includeDetails=true 
 *                  and calling the getUsageDataByType method, the devices returned are filtered by the deviceType parameter.
 * <li>deviceType - Optional parameter. Used for filter usage data by specific device types. When present, only usage for the requested device 
 *             types are returned.
 * <li>timePeriod -  The time period to be returned. Identifies if (H)ourly, (D)aily, or (M)onthly usage data is being requested.
 * <li>startDate - The starting date of the date range. Required
 * <li>endDate - The ending date of the date range. Required
 * <li>startDate_2 - The starting date of second the date range for date range comparision. Optional parameter
 * <li>endDate_2 - The ending date of second the date range for date range comparision. Optional parameter
 * <br><br>
 * <h3>Results</h3>
 * <li>RECORD_TYPE - THe type of record, either (S)ummary or (D)etail
 * <li>GATEWAY_GUID - The gateway GUID. If this is missing and RECORD_TYPE is (S)ummary, then the record is a summary for the entire account
 * <li>DEVICE_GUID - The device GUID. If this value is a null and the RECORD_TYPE is (S)ummary, then this record is a summary for all devices. <br/>
 *                   If this value is "DEVICESUM" the record represents a summary record for the specified devices.
 * <li>DEVICE_TYPE_ID - The device type id. Only returned when device details are returned OR when calling getUsageDataByType
 * <li>TIME_PERIOD - The time period the records represent. The actual time period, (hour of day, day of month, month of year)
 * <li>UNITS - The units for the usage amount. If null, assume KwH
 * <li>CURRENT_PRICE - This field returns either the current price per unit as reported by the gateway/device or an average (usage_cost/usage_amount). 
 *                     For any summary record returned, this field is an average as determined by the summary of usage_cost divided by the summary of the
 *                     usage_amount. When querying for (D)aily or (M)onthly records, this field is also an average of the accumulated usage cost divided by 
 *                     the accumulated usage amount (for either the day or month). This field only contains the current price as reported by the gateway/device
 *                     when returning hourly records for the individual devices. 
 * <li>USAGE_AMOUNT - The amount of energy consumed
 * <li>USAGE_COST - The usage cost
 * <li>USAGE_DATE - The date of the usage (does not include timestamp!)
 * @author Luther J Maloney
 * @since December 2013
 */

public interface EnergyManager   
{
    /** Constant to indicate daily records are requested **/
    public static final String ENERGY_USAGE_DAILY = "D";
    /** Constant to indicate hourly records are requested **/
    public static final String ENERGY_USAGE_HOURLY = "H";
    /** Constant used to indicate monthly records are requested **/
    public static final String ENERGY_USAGE_MONTHLY = "M";
    
    /**
     * Method to return the list of device types
     * @param domainId - The domain identifier, from the DOMAIN_REF table.
     * @return
     * @throws RemoteException
     * @throws XanbooException 
     */
    public XanbooResultSet getDeviceTypes(String domainId)throws RemoteException, XanbooException;
    /**
     * Method returns summary usage data for the account or gateway (if GGUID is specified) for the parameters provided. If 
     * includeDetails is "true", all the matching devices for parameters are also returned.
     * @param principal REQUIRED - The <code>XanbooPrincipal</code> instance representing the account.
     * @param GGUID Optional. The Gateway GUID. If not present, usage summary usage data for the entire account is returned. 
     * @param includeDetails Required, either true or false. If true, the usage for the devices filtered by the account 
     *                      (and GGUID if provided) is returned in addition to the summary data for the account (or gateway).
     * @param timePeriod - Required, the time period of the records, one of (H)ourly, (D)aily, or (M)onthly
     * @param startDate_1 - Required, the start date of the date range
     * @param endDate_1 - Required, the end date of the date range
     * @param startDate_2 - Optional, the start date of the date comparison range
     * @param endDate_2 - Optional, the end date of the date comparison range. Note this parameter is required if startDate_2 is provided.
     * @return
     * @throws RemoteException
     * @throws XanbooException 
     */
    public XanbooResultSet getUsageData(XanbooPrincipal principal,String GGUID,boolean includeDetails,String timePeriod,
                                        Date startDate_1,Date endDate_1,Date startDate_2,Date endDate_2)throws RemoteException,XanbooException;
    /**
     * Method returns usage data for a one or more specified devices based upon the parameters provided. When the value returned in DEVICE_GUID is
     * <b>DEVICESUM</b>, the summary record (RECORD_TYPE=S) indicates a summary record for the selected/requested devices. 
     * @param principal REQUIRED - The <code>XanbooPrincipal</code> instance representing the account.
     * @param GGUID Required, the Gateway GUID. If not present, usage summary usage data for the entire account is returned. 
     * @param DGUID Required, the list of specific devices to be returned.  
     * @param timePeriod - Required, the time period of the records, one of (H)ourly, (D)aily, or (M)onthly
     * @param startDate_1 - Required, the start date of the date range
     * @param endDate_1 - Required, the end date of the date range
     * @param startDate_2 - Optional, the start date of the date comparison range
     * @param endDate_2 - Optional, the end date of the date comparison range. Note this parameter is required if startDate_2 is provided.
     * @return 
     * @throws RemoteException
     * @throws XanbooException 
     */
    public XanbooResultSet getUsageData(XanbooPrincipal principal,String GGUID,String[] DGUID,String timePeriod,
                                        Date startDate_1,Date endDate_1,Date startDate_2,Date endDate_2)throws RemoteException,XanbooException;
    /**
     * Method to return usage data aggregated by device type for two date ranges. 
     * @param principal - The <code>XanbooPrincipal</code> instance representing the account
     * @param GGUID - Optional. The Gateway GUID. If not present, usage summary usage data for all the devices types on the account is returned. 
     * @param deviceTypeId - Optional. Filter the results by the specified device type(s). 
     * @param includeDetails. Required, either true or false. If true, the device detail is returned with the summary record(s). The devices returned
     *                        are filtered by the deviceTypeId parameter
     * @param timePeriod - The time period of the records, one of (H)ourly, (D)aily, or (M)onthly
     * @param startDate_1 - Required, the start date of the date range
     * @param endDate_1 - Required, the end date of the date range
     * @param startDate_2 - Optional, the start date of the date comparison range
     * @param endDate_2 - Optional, the end date of the date comparison range. Note this parameter is required if startDate_2 is provided.
     * @return
     * @throws RemoteException
     * @throws XanbooException 
     */
    public XanbooResultSet getUsageDataByType(XanbooPrincipal principal,String GGUID,String[] deviceType,boolean includeDetails,
                                              String timePeriod,Date startDate_1,Date endDate_1,Date startDate_2,Date endDate_2) throws RemoteException,XanbooException;
}
