/*
 * $Source:  $
 * $Id:  $
 *
 * Copyright 2013 AT&T Digital Life
 *
 */

package com.xanboo.core.util;

import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;
import com.xanboo.core.util.XanbooUtil;
import com.xanboo.core.sdk.account.XanbooNotificationProfile;
import com.xanboo.core.sdk.contact.XanbooContact;
import com.xanboo.core.sdk.account.XanbooSubscription;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import com.att.dlife.sbnmware.client.ClientManager;
import com.att.dlife.sbnmware.client.ws.Account;
import com.att.dlife.sbnmware.client.ws.Installation;
import com.att.dlife.sbnmware.client.ws.InstallationPart;
import com.att.dlife.sbnmware.client.ws.Device;
import com.att.dlife.sbnmware.client.ws.EmergencyContact;
import com.att.dlife.sbnmware.client.util.ApplicationException;
import com.att.dlife.sbnmware.client.util.ConnectionException;
import com.att.dlife.sbnmware.client.util.ConnectionRetryException;
import com.att.dlife.sbnmware.client.ws.KeyValue;
import com.att.dlife.sbnmware.client.ws.KeyValueArray;

/**
 * Utility class used for entity synchronization with SBN thru SBN Middleware Client libraty
 * By default, library configuration is done thru a config file specified by
 * a JVM command line argument (E.g. "-Dsbnmw.config=/etc/sbn-client.properties")
 */
public class SBNSynchronizer {

    private static final int MAX_RETRIES=3;

    private static Logger logger = LoggerFactory.getLogger(SBNSynchronizer.class.getName());

    private static ClientManager manager;

    public static final String MONSTATUS_PENDING   = "PEND";
    public static final String MONSTATUS_ACTIVE    = "QAP";        //MON";
    public static final String MONSTATUS_SUSPENDED = "SUSP";
    public static final String MONSTATUS_CANCELLED = "CANX";
    public static final String MONSTATUS_MONITORED = "MON";
    public static final String MONSTATUS_NO_MONITORED = "NMON";

    private static final String DEVICE_FILTER_MAJOR_CLASS = "09,11,";   //dont sync these major class ids with SBN (e.g. 09: virtual devices)
    private static final String DEVICE_FILTER_FULL_CLASS  = "1100,";   //dont sync these full class ids with SBN (e.g. 1100: device groups)
    //private static final String EXTDEVICE_ALLOW_FULL_CLASS = "0908,0913,";   //allowed external service device classes to sync with SBN

    
    public SBNSynchronizer() {}
    

    /* adds a new shell account and installation record to the SBN database */
    public boolean newSubscription(XanbooSubscription xsub, String gguid)  {
        return newSubscription(xsub.getAccountId(), xsub.getExtAccountId(), gguid, xsub.getSubsId(), 
                xsub.getHwId(), xsub.getLabel(), xsub.getTzone(), xsub.getAlarmCode(), xsub.getSubsInfo(), xsub.getNotificationProfiles(), xsub.getSubsFlags(), 
                xsub.getSubsFeatures(), xsub.getSubscriptionClass());
    }
    
    
    /* adds a new shell account and installation record to the SBN database */
    public boolean newSubscription(long accountId, String extAccountId, String gguid, String subsId, String hwId, String label, String tzName, String alarmPass,
             XanbooContact subsInfo, XanbooNotificationProfile[] emergencyContacts, int dlFlag, String dlFeature, String subClass)  {
        if (logger.isDebugEnabled()) {
            logger.debug("[newSubscription()]:");
        }

        /* ******* DISABLING account/contract creation in SBN as it is no longer necessary

        //first create the account
	Account acct = new Account();
	acct.setSAcc(accountId);   //xanboo account id
	acct.setMisc2(extAccountId);                 //customer AT&T BAN
	acct.setName(subsInfo.getLastName());      
	acct.setFName(subsInfo.getFirstName());

        addUpdateAccount(acct, false);   //async retry disabled

        *************/

        //now add the installation record
	Installation inst = new Installation();
	inst.setSAcc(accountId);            //xanboo account id

	inst.setCtn(subsId);                //subscription id (ctn)
	inst.setImei(hwId);                 //imei
	//if(label!=null) inst.setTname(label);                //installation name

        //set installation timezone, if not null
        if(tzName!=null) inst.setTmzonprOlson(tzName);

        inst.setGguid(gguid);               //temporary gguid assigned
        inst.setIban(extAccountId);         //customer AT&T BAN

        inst.setFname(subsInfo.getFirstName());
        inst.setName(subsInfo.getLastName());
        
        if(subsInfo.getPhone()!=null && subsInfo.getPhone().trim().length()>0) {
            inst.setPhone1(subsInfo.getPhone().trim());
            //inst.setPhtxt1("0");
        }

        if(subsInfo.getCellPhone()!=null && subsInfo.getCellPhone().trim().length()>0) {
            inst.setPhone2(subsInfo.getCellPhone().trim());
            //inst.setPhtxt2("0");
        }
        

        try {   //extract numeric street number from street1 data
            int ix = subsInfo.getAddress1().indexOf(" ");
            if(ix>0 && ix<subsInfo.getAddress1().length()-1) {  //space found, extract the numeric street number before space
                int streetNo = Integer.parseInt(subsInfo.getAddress1().substring(0, ix));
                inst.setStreet1No1(""+streetNo);    //convert to string
                inst.setStreet1(subsInfo.getAddress1().substring(ix+1));
            }else { //no space, no street number
                inst.setStreet1(subsInfo.getAddress1());
                inst.setStreet1No1(null);
            }
        }catch(Exception ee) {  //in case of exception, no street number parsing
            inst.setStreet1(subsInfo.getAddress1());
            inst.setStreet1No1(null);
        }

        inst.setStreet2(subsInfo.getAddress2());
        inst.setCity(subsInfo.getCity());
        inst.setState(subsInfo.getState());
        inst.setZip(subsInfo.getZip());      // "-"+subsInfo.getZip4();

        inst.setCodeword(alarmPass);
        if(subClass!=null && subClass.equalsIgnoreCase(GlobalNames.DLLITE))
            inst.setSubtype("PSEC");    //subs type for dllite subs
        else
            inst.setSubtype("RESI");    //subs type default residential
        
       
        inst.setDlflag(String.format("%10s", Integer.toBinaryString(dlFlag)).replace(' ', '0'));        
        inst.setDlfeature(dlFeature);

        if(addUpdateInstallation(inst, false, false)) { //wont add emergency contacts yet!
        		
        	String strMonStatus = getSBNMonitoringType(subClass, dlFlag, dlFeature);        	
        	updateInstallationMonStatus(gguid, strMonStatus, false);
        	
           /* //if success, set mon status
            if(subClass==null || subClass.equalsIgnoreCase("DLSEC"))
                updateInstallationMonStatus(gguid, MONSTATUS_PENDING, false);   // for DLSEC, set to PEND - no need to retry, as default will be pending anyways
            else
                updateInstallationMonStatus(gguid, MONSTATUS_MONITORED, false);    // for DLLITE, set to MON
*/       
            //////update timezone properly  -- TEMPORARY until SBN ref data is baselined. W?ll move this to the add installation step above
            ////if(tzName!=null) updateInstallationTzone(gguid, tzName);
            
            if(emergencyContacts!=null) {
                for(int i=0; i<emergencyContacts.length; i++) {
                    ///if(emergencyContacts[i].isValidEmergencyContact()) {
                        emergencyContacts[i].setGguid(gguid);
                        ///addUpdateEmergencyContact(emergencyContacts[i]);
                    ///}
                }
                if (addUpdateEmergencyContact(emergencyContacts))   //use bulk call rather
                   activePlanUpdate(gguid, false);
            }

            return true;    //success
        }

        return false;   //failed
    }
    
   
    
    // subclass - DLITE - MON - irrespective of montype i.e PM, SM or NM
    // subclasss - DLSEC all combinations need to be checked
    private String getSBNMonitoringType(String subClass, int dlFlag, String keyHolderCsvString){
    	if (logger.isDebugEnabled()) {
            logger.debug("[getSBNMonitoringType()]: subClass=" + subClass + ", dlFlag=" + dlFlag + ", new keyHolderCsvString=" + keyHolderCsvString );
        }
    	
    	// for DLLITE, set to MON
    	if(subClass !=null && subClass.equalsIgnoreCase(GlobalNames.DLLITE)){
    		return MONSTATUS_MONITORED;
    	}
    	
    	String monStatus=null;
    	if(subClass==null || subClass.equalsIgnoreCase(GlobalNames.DLSEC)){
	    	
	    	Boolean addKeyHolder = false;
	    	if (keyHolderCsvString != null) {
	    		addKeyHolder = keyHolderCsvString.contains(GlobalNames.FEATURE_KEY_HOLDER);			
			}
	    	
	    	/* bit 4-5: monitoring type
              00 - No monitoring
              01 - Professional monitoring
              10 - Self monitoring
              11 - Professional monitoring pending
			*/
	    	boolean pos4 = XanbooUtil.isBitOn(dlFlag, 4);
			boolean pos5 = XanbooUtil.isBitOn(dlFlag,5);
			
			
	    	// if P - professional monitoring, then with or without keyholder SBN  status should be Pend
	    	if (!pos5 && pos4 ){
	    		monStatus =  MONSTATUS_PENDING;
	    	}
	    	
	    	// for  self monitoring
	    	if(pos5 && !pos4 ){
	    		if(addKeyHolder){
	    			monStatus =  MONSTATUS_PENDING;
	    		}
	    		else
	    		{
	    			monStatus = MONSTATUS_NO_MONITORED;
	    		}
	    	}
	    	// For NM
	    	if(!pos5 && !pos4 ){
	    		if(addKeyHolder){
	    			monStatus =  MONSTATUS_PENDING;
	    		}
	    		else
	    		{
	    			monStatus = MONSTATUS_NO_MONITORED;
	    		}
	    	}
    	}
    	if (logger.isDebugEnabled()) {
            logger.debug("[getSBNMonitoringType()]: Final monStatus=" + monStatus  );
        }
    	
    	return monStatus;
    }


    /* updates gguid for an existing installation (by imei or ctn) and activates it */
    public boolean newInstallationRegistered(String subsId, String hwId, String gguid, String catalogId, String dLabel, int dlFlag, String subsFeatures)  {
        if (logger.isDebugEnabled()) {
            logger.debug("[newInstallationRegistered()]: CTN=" + subsId + ", IMEI=" + hwId + ", new GGUID=" + gguid + ",  dlFlag=" + dlFlag + ", subsFeatures=" + subsFeatures);
        }

        if(updateInstallation(subsId, hwId, gguid, null, null, null, null, null,-1,null, null)) {
            
            //if success, add the DLC zone record
            newDeviceRegistered(gguid, "0", catalogId, dLabel);   //hardcoded gateway catalog id
            
            Boolean addKeyHolder = false;
	    	if (subsFeatures != null) {
	    		addKeyHolder = subsFeatures.contains(GlobalNames.FEATURE_KEY_HOLDER);			
			}
	    	
	    	/* bit 4-5: monitoring type
	            00 - No monitoring
	            01 - Professional monitoring
	            10 - Self monitoring
	            11 - Professional monitoring pending
			*/
	    	String monStatus = MONSTATUS_ACTIVE ;
	    	if(dlFlag != -1 ){
	    		boolean pos4 = XanbooUtil.isBitOn(dlFlag, 4);
	    		boolean pos5 = XanbooUtil.isBitOn(dlFlag,5);			
			
	    		// 	for  self monitoring
	    		if ( (pos5 && !pos4 ) || (!pos5 && !pos4) ) {
	    			if(!addKeyHolder){
	    				monStatus = MONSTATUS_NO_MONITORED;
	    			}
	    		}
	    	}
            //if success, set mon status
            updateInstallationMonStatus(gguid,monStatus , false);
            return true;    //success
        }

        return false;
    }


    /* updates label/name for a given gateway/installation by gguid */
    public boolean updateInstallationLabel(String gguid, String label, String catalogId)  {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateInstallationLabel()]: gguid=" + gguid + ", label=" + label );
        }

        //return updateInstallation(gguid, null, label, null, null, null);
        updateDeviceLabel(gguid, "0", label, catalogId);
        
        return true;
    }

    /* updates location code for a given gateway/installation by gguid */
    public void updateInstallationLocation(String gguid, String location, String catalogId)  {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateInstallationLocation()]: gguid=" + gguid + ", loc=" + location );
        }

        updateDeviceLocation(gguid, "0", location, catalogId);
    }
    
    
    /* updates TZ for a given gateway/installation by gguid */
    public boolean updateInstallationTzone(String gguid, String tzone)  {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateInstallationTzone()]: gguid=" + gguid + ", tzone=" + tzone );
        }

        return updateInstallation(gguid, null, null, tzone, null, null);
    }


    /* updates alarm password for a given gateway/installation by ctn/imei */
    public boolean updateInstallationAlarmPassword(String subsId, String hwId, String alarmPass)  {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateInstallationAlarmPassword()]: (CTN=" + subsId + ", IMEI=" + hwId + "), change to: alarmpass=" + alarmPass);
        }

        return updateInstallation(subsId, hwId, null, null,  null, null, alarmPass, null,-1,null, null);
    }


    /* updates existing installation (by imei or ctn) */
    public boolean updateInstallation(String subsId, String hwId, String gguid, String newHwId, String label, String tzName, String alarmPass, XanbooContact subsInfo, int subs_flag, String subs_features, String subsClass)  {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateInstallation()]: (CTN=" + subsId + ", IMEI=" + hwId + "), change to: gguid=" + gguid + ", imei=" + newHwId + ", subs_flag=" + subs_flag  + ", subs_features=" + subs_features + ",subsClass="+ subsClass);
        }

        /* locate installation and get its SBN id */
        int sIns;
        int retryCount=0;
        
        while(true) {
        	if(subsClass!=null && subsClass.equalsIgnoreCase(GlobalNames.DLLITE)){
        		sIns = lookupInstallation(subsId, hwId, gguid);
        	}else{
        		sIns = lookupInstallation(subsId, hwId, null);
        	}
            if(sIns!=-2 || retryCount>3) break;       //try 4 times max, if return is app error return is 99!
            retryCount++;
            try { Thread.sleep(3000); }catch(InterruptedException ie) {}    //sleep 3secs
        }
        

        //check? ret code?
        if(sIns>6) {
            logger.debug("[updateInstallation()]: Found sIns=" + sIns + " for IMEI=" + hwId + ", CTN=" + subsId);
            //now add the installation record
            Installation inst = new Installation();
            inst.setSIns(new Integer(sIns));

            //update non-null values
            if(gguid!=null && gguid.trim().length()>0) inst.setGguid(gguid);                      //gguid
            if(newHwId!=null && newHwId.trim().length()>0) inst.setImei(newHwId);                 //new imei
            //if(label!=null && label.trim().length()>0) inst.setTname(label);                       //installation name
            if(tzName!=null && tzName.trim().length()>0) inst.setTmzonprOlson(tzName);                 //timezone
            if(alarmPass!=null && alarmPass.trim().length()>0) inst.setCodeword(alarmPass);

            if(subsInfo!=null) {
                if(subsInfo.getFirstName()!=null && subsInfo.getFirstName().trim().length()>0) inst.setFname(subsInfo.getFirstName());
                if(subsInfo.getLastName()!=null && subsInfo.getLastName().trim().length()>0) inst.setName(subsInfo.getLastName());

                if(subsInfo.getPhone()!=null && subsInfo.getPhone().trim().length()>0) {
                    inst.setPhone1(subsInfo.getPhone().trim());
                    //inst.setPhtxt1("0");
                }

                if(subsInfo.getCellPhone()!=null && subsInfo.getCellPhone().trim().length()>0) {
                    inst.setPhone2(subsInfo.getCellPhone().trim());
                    //inst.setPhtxt2("0");
                }
                
                if(subsInfo.getAddress1()!=null && subsInfo.getAddress1().trim().length()>0) {
                    try { //extract numeric street number from street1 data
                        int ix = subsInfo.getAddress1().indexOf(" ");
                        if(ix>0 && ix<subsInfo.getAddress1().length()-1) {  //space found, extract the numeric street number before space
                            int streetNo = Integer.parseInt(subsInfo.getAddress1().substring(0, ix));
                            inst.setStreet1No1(""+streetNo);    //convert to string
                            inst.setStreet1(subsInfo.getAddress1().substring(ix+1));
                        }else { //no space, no street number
                            inst.setStreet1(subsInfo.getAddress1());
                            inst.setStreet1No1(null);
                        }
                    }catch(Exception ee) {  //in case of exception, no street number parsing
                        inst.setStreet1(subsInfo.getAddress1());
                        inst.setStreet1No1(null);
                    }
                }
                if(subsInfo.getAddress2()!=null && subsInfo.getAddress2().trim().length()>0) inst.setStreet2(subsInfo.getAddress2());
                if(subsInfo.getCity()!=null && subsInfo.getCity().trim().length()>0) inst.setCity(subsInfo.getCity());
                if(subsInfo.getState()!=null && subsInfo.getState().trim().length()>0) inst.setState(subsInfo.getState());
                if(subsInfo.getZip()!=null && subsInfo.getZip().trim().length()>0) inst.setZip(subsInfo.getZip());     // "-"+subsInfo.getZip4();
             }
            
            
          
            if(subs_flag>-1 && subs_features != null) {
            	inst.setDlflag(String.format("%10s", Integer.toBinaryString(subs_flag)).replace(' ', '0'));
            	inst.setDlfeature(subs_features);
            }
            
            //now update the installation
            return addUpdateInstallation(inst, true, true);

        }else {
            logger.debug("[updateInstallation()]: Failed to locate sIns for IMEI=" + hwId + ", CTN=" + subsId);
        }

        return false;
    }


    private boolean isFilteredDevice(String catalogId, String dguid) {
        //for invalid cats, filter it - this should not happen normally
        if(catalogId==null || catalogId.length()<10) return false;
        if(catalogId.length() >=15) catalogId = catalogId.substring(1,14);
        
        //separate filtration for external service devices
        if(XanbooUtil.isExternalServiceDevice(dguid)) {
        	
        	return false; // Don't filter external service devices.
        	
        	/* String dClass = catalogId.substring(4,8);      //parse 4-digit class id of this device
            if(EXTDEVICE_ALLOW_FULL_CLASS.indexOf(dClass+",") >-1) {
                return false;    //it is in the allowed list, not filter
            }*/
        }
        
        //-- filters for native devices
        //check major class id needs to be fitered or not
         String dClass = catalogId.substring(4,6); //parse 2-digit major class id of this device
                
        if(DEVICE_FILTER_MAJOR_CLASS.length()>2 && DEVICE_FILTER_MAJOR_CLASS.indexOf(dClass+",") >-1) {
            return true;    //filtered by major class id if in the list
        }

        //check full class id needs to be fitered or not
         dClass = catalogId.substring(4,8);      //parse 4-digit class id of this device
        if(DEVICE_FILTER_FULL_CLASS.length()>0 && DEVICE_FILTER_FULL_CLASS.indexOf(dClass+",") >-1) {
            return true;    //filtered by class id if in the list
        }
        
        return false;  //not filter
    }

    /* adds a new device/zone for a given gguid */
    public boolean newDeviceRegistered(String gguid, String dguid, String catalogId, String dLabel)  {
        if (logger.isDebugEnabled()) {
            logger.debug("[newDeviceRegistered()]: GGUID=" + gguid + ", DGUID=" + dguid + ", catalog=" + catalogId+ ", label=" + dLabel);
        }
        
        if(catalogId==null || catalogId.length()==0) return true;   //ignoring
        if(catalogId.length() >=15) catalogId = catalogId.substring(1,14);
        if(isFilteredDevice(catalogId, dguid)) return true;    //dont sync, if filtered

        String dClass = catalogId.substring(4,10);   //6-digit major+sub class
        
	Device dev = new Device();

	dev.setGatewayGuid(gguid);       //gguid
        dev.setDeviceGuid(dguid);        //dguid
        //dev.setClas("AL");             //device class
        dev.setExtClass(dClass);
        dev.setLocId("0");
	//dev.setText(dLabel);             //device name
        dev.setPArea(dLabel);            //as of CD5, use parea for device name
        
        return addUpdateDevice(dev, true);
    }


    /* removes a device/zone from SBN */
    public boolean removeDevice(String gguid, String dguid, String catalogId, boolean retry)  {
        if (logger.isDebugEnabled()) {
            logger.debug("[removeDevice()]: gguid=" + gguid + ", dguid=" + dguid + ", catalog=" + catalogId);
        }

        if(catalogId!=null && catalogId.length()>10) {
            if(isFilteredDevice(catalogId,dguid)) return true;    //dont sync, if filtered
        }
        
        return deleteDevice(gguid, dguid, retry);
    }


    /* changes the name/label associated with a device/zone */
    public void updateDeviceLabel(String gguid, String dguid, String label, String catalogId)  {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateDeviceLabel()]: gguid=" + gguid + ", dguid=" + dguid + ", label=" + label + ", catalog=" + catalogId );
        }

        String dClass = null;
        if(catalogId!=null && catalogId.length()>10) {
            if(catalogId.length() >=15) catalogId = catalogId.substring(1,14);
            if(isFilteredDevice(catalogId,dguid)) return;    //dont sync, if filtered
            dClass = catalogId.substring(4,10);   //6-digit major+sub class
        }
        
	Device dev = new Device();

	dev.setGatewayGuid(gguid);       //gguid
        dev.setDeviceGuid(dguid);        //dguid
	//dev.setText(label);              //device name
	dev.setPArea(label);            //as of CD5, use parea for device name

        if(dClass!=null) dev.setExtClass(dClass);
        
        addUpdateDevice(dev, true);
    }
    

    /* changes the name/label associated with a device/zone */
    public void updateDeviceLocation(String gguid, String dguid, String loc, String catalogId)  {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateDeviceLocation()]: gguid=" + gguid + ", dguid=" + dguid + ", loc=" + loc + ", catalog=" + catalogId);
        }
        if(loc==null || loc.trim().length()==0 || loc.equals("null")) return;

        String dClass = null;
        if(catalogId!=null && catalogId.length()>10) {
            if(isFilteredDevice(catalogId,dguid)) return;    //dont sync, if filtered
            if(catalogId.length() >=15) catalogId = catalogId.substring(1,14);
            dClass = catalogId.substring(4,10);   //6-digit major+sub class
        }

        
        try {
            Device dev = new Device();

            dev.setGatewayGuid(gguid);       //gguid
            dev.setDeviceGuid(dguid);        //dguid
            dev.setLocId(loc);
            if(dClass!=null) dev.setExtClass(dClass);

            addUpdateDevice(dev, true);
        }catch(Exception e) {}

    }


    public boolean updateInstallationMonStatus(String gguid, String monStatus) {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateInstallationMonStatus()]: gguid=" + gguid + ", monStatus=" + monStatus );
        }

        return updateInstallationMonStatus(gguid, monStatus, true);
    }



    private EmergencyContact prepareEmergencyContact(XanbooNotificationProfile xnp) {

	EmergencyContact contact = new EmergencyContact();
	contact.setGguid(xnp.getGguid());
	//contact.setSIns(1);
	contact.setSeq(xnp.getOrder());
	contact.setName(xnp.getName());
	contact.setDebug(0);

        contact.setCodeword((xnp.getCodeword()==null ? "" : xnp.getCodeword()));     //set alarm password for the contact

	contact.setPhone1(xnp.getAddress());        //required voice number
        
        //DL profile types: 10=old, 11:email, 12:sms, 13:voice, 14:voice-LL
        if(xnp.getType()==12) 
            contact.setPhone1Type("SMS");     //set sms flag
        else if(xnp.getType()==14) 
            contact.setPhone1Type("LL");      //set landline flag
        else 
            contact.setPhone1Type("");        //set default phone type blank

        //optional second address
        if(xnp.hasValidProfileAddress2()) {
            if(xnp.getAddress2().indexOf("@")==-1) {    //if this is a phone number  
                contact.setPhone2(xnp.getAddress2());
                if(xnp.getType2()==12) 
                    contact.setPhone2Type("SMS");           //set sms flag
                else if(xnp.getType2()==14) 
                    contact.setPhone2Type("LL");            //set landline flag
                else 
                    contact.setPhone2Type("");              //set default phone type blank
                
            }else {                                    //email address
                contact.setEmail(xnp.getAddress2());
            }
            
        }else {
            contact.setPhone2("");
        }
        

        if(xnp.hasValidProfileAddress3()) {
            if(xnp.getAddress3().indexOf("@")==-1) {    //if this is a phone number  
                contact.setPhone3(xnp.getAddress3());
                if(xnp.getType3()==12) 
                    contact.setPhone3Type("SMS");           //set sms flag
                else if(xnp.getType3()==14) 
                    contact.setPhone3Type("LL");            //set landline flag
                else 
                    contact.setPhone3Type("");              //set default phone type blank
                
            }else {                                    //email address
                contact.setEmail(xnp.getAddress3());
            }
            
        }else {
            contact.setEmail("");
        }
        

        //if(xnp.getActionPlanContactOrder(0) != -1) contact.setAp0(xnp.getActionPlanContactOrder(0));
        if(xnp.getActionPlanContactOrder(1) != -1) contact.setAp1(xnp.getActionPlanContactOrder(1));
        if(xnp.getActionPlanContactOrder(2) != -1) contact.setAp2(xnp.getActionPlanContactOrder(2));
        if(xnp.getActionPlanContactOrder(3) != -1) contact.setAp3(xnp.getActionPlanContactOrder(3));
        if(xnp.getActionPlanContactOrder(4) != -1) contact.setAp4(xnp.getActionPlanContactOrder(4));
        if(xnp.getActionPlanContactOrder(5) != -1) contact.setAp5(xnp.getActionPlanContactOrder(5));
        if(xnp.getActionPlanContactOrder(6) != -1) contact.setAp6(xnp.getActionPlanContactOrder(6));
        if(xnp.getActionPlanContactOrder(7) != -1) contact.setAp7(xnp.getActionPlanContactOrder(7));
        if(xnp.getActionPlanContactOrder(8) != -1) contact.setAp8(xnp.getActionPlanContactOrder(8));
        if(xnp.getActionPlanContactOrder(9) != -1) contact.setAp9(xnp.getActionPlanContactOrder(9));
        
        return contact;
    }

    public boolean addUpdateEmergencyContact(XanbooNotificationProfile xnp) {
        if (logger.isDebugEnabled()) {
            logger.debug("[addUpdateEmergencyContact()]: gguid=" + xnp.getGguid() + ", seq=" + xnp.getOrder() + ", name=" + xnp.getName() );
        }
        
        return addOrUpdateEmergencyContact(prepareEmergencyContact(xnp), false);
    }
    
    
    public boolean addUpdateEmergencyContact(XanbooNotificationProfile[] xnp) {
        if (logger.isDebugEnabled()) {
            logger.debug("[addUpdateEmergencyContact()]: BULK");
        }
        ArrayList<EmergencyContact> contacts = new ArrayList();
        
        for(int i=0; i<xnp.length; i++) {
            contacts.add(prepareEmergencyContact(xnp[i]));
        }

        return addOrUpdateEmergencyContact(contacts, false);
    }
    
    
    public boolean deleteEmergencyContact(String gguid, int seq) {
        if (logger.isDebugEnabled()) {
            logger.debug("[deleteEmergencyContact()]: gguid=" + gguid + ", seq=" + seq );
        }

        return deleteEmergencyContact(gguid, null, seq, true);
    }
    
    
    
    /* enables/disables alamr delay timer for an installation */
    public boolean setInstallationAlarmDelay(String subsId, String hwId, int alarmDelay) {
        if(alarmDelay<0) return true;
        if (logger.isDebugEnabled()) {
            logger.debug("[setInstallationAlarmDelay()]: subsId=" + subsId + ", hwId=" + hwId + ", delay=" + alarmDelay );
        }
        
	try {
            manager =  ClientManager.getInstance();
	    int sIns = manager.getInstallationClientService().lookupInstallation(null, hwId, subsId, null);  

            if(sIns>6) {
                manager.getInstallationClientService().setAlarmDelay(sIns, (alarmDelay==0 ? "" : "X"), false);
                return true;
            }else {
                logger.debug("[setInstallationAlarmDelay()]: Failed to locate sIns for IMEI=" + hwId + ", CTN=" + subsId);
                return false;
            }

	}catch (ConnectionRetryException e) {
            logger.warn("[setInstallationAlarmDelay()]: SBN Middleware Web Service connection exception (Async RETRY scheduled!): " + e.getMessage());

        }catch (ConnectionException e) {
            if(logger.isDebugEnabled())
                logger.error("[setInstallationAlarmDelay()]: SBN Middleware Web Service connection error: ", e);
            else
                logger.error("[setInstallationAlarmDelay()]: SBN Middleware Web Service connection exception:" + e.getMessage());

	}catch (ApplicationException e) {
            logger.warn("[setInstallationAlarmDelay()]: SBN Middleware Web Service ATOM exception. errCode:" + e.getErrorCode() + ", errMessage:" + e.getMessage());
	}

        return false;
    }
    
    
    public String getTimezoneByZip(String zip) {
	try {
            manager =  ClientManager.getInstance();
	    com.att.dlife.sbnmware.client.ws.Timezone tz = manager.getMiscellaneousClientService().getTimezone(zip);
            return (tz==null ?  null : tz.getText());

	}catch (ConnectionRetryException e) {
            logger.warn("[getTimezoneByZip()]: SBN Middleware Web Service connection exception (Async RETRY scheduled!): " + e.getMessage());

        }catch (ConnectionException e) {
            if(logger.isDebugEnabled())
                logger.error("[getTimezoneByZip()]: SBN Middleware Web Service connection error: ", e);
            else
                logger.error("[getTimezoneByZip()]: SBN Middleware Web Service connection exception:" + e.getMessage());

	}catch (ApplicationException e) {
            logger.error("[getTimezoneByZip()]: SBN Middleware Web Service ATOM exception. errCode:" + e.getErrorCode() + ", errMessage:" + e.getMessage());
	}

        return null;
    }
    
    
    public LinkedHashMap getLocationCodes(int part) {
	try {
            manager =  ClientManager.getInstance();
            
	    List<KeyValueArray> result = null;
            if(part==-1)
                result = manager.getMiscellaneousClientService().getLocationCodes(null);
            else
                result = manager.getMiscellaneousClientService().getLocationCodes((short) part);
            
            if(result==null) return null;

            LinkedHashMap codes = null;
            
            String locid=null;
            String loctext=null;
	    for (int i = 0; i < result.size(); i++) {
		List<KeyValue> values = result.get(i).getValues();
                locid=null;
                loctext=null;
		for (KeyValue value : values) {
                    if(value.getKey().equalsIgnoreCase("locid")) locid=(String) value.getValue();
                    if(value.getKey().equalsIgnoreCase("text")) loctext=(String) value.getValue();
		    //System.out.println(value.getKey() + "=" + value.getValue()  + "  ");
		}
                
                //found a valid loc code/text entry, add to hashmap to be returned
                if(locid!=null && loctext!=null) {
                    if(codes==null) codes = new LinkedHashMap();
                    codes.put(locid, loctext);
                }
	    }

            if(codes==null || codes.size()==0) {
                logger.warn("[getLocationCodes()]: Location codes for part " + part + " returned no results from SBN");
            }
            
            //code "0" is reserved for unassigned values
            if(codes.get("0")==null) codes.put("0", "N/A");
            
            return codes;
            
	}catch (ConnectionRetryException e) {
            logger.warn("[getLocationCodes()]: SBN Middleware Web Service connection exception (Async RETRY scheduled!): " + e.getMessage());

        }catch (ConnectionException e) {
            if(logger.isDebugEnabled())
                logger.error("[getLocationCodes()]: SBN Middleware Web Service connection error: ", e);
            else
                logger.error("[getLocationCodes()]: SBN Middleware Web Service connection exception:" + e.getMessage());

	}catch (ApplicationException e) {
            logger.error("[getLocationCodes()]: SBN Middleware Web Service ATOM exception. errCode:" + e.getErrorCode() + ", errMessage:" + e.getMessage());
	}
        
        return null;
    }

    
    
    /*
     * Enables/Disables test mode.  If timestamp is null or empty, disables test mode.
     * Returns 0 on success or retry=true, -1 otherwise.
     */
    public int setTestMode(String gguid, String dguid, String timestamp, String timezone, String userid, boolean retry) {

        if(gguid==null || gguid.length()==0) return -1;

        if (logger.isDebugEnabled()) {
            logger.debug("[setTestMode()]: gguid=" + gguid + ", dguid=" + dguid + ", ts=" + timestamp + ", tz=" + timezone + ", userid=" + userid);
        }
        
	try {
            InstallationPart part = new InstallationPart();
            part.setGguid(gguid);
            if (dguid != null && dguid.trim().length() > 0 && !dguid.equals("0")) {
               part.setDeviceGuid(dguid);
            }

            if (timestamp == null || timestamp.trim().equals("")) {
                return disableTestMode(gguid, dguid, userid, retry);
            }

            String dlcLocalTimeStamp = getDlcLocalTimeStamp(timestamp, timezone);
            if (dlcLocalTimeStamp == null) {
                 return -1;
            }

            //Get date and time values from dlcLocalTimeStamp
            String [] strArr = dlcLocalTimeStamp.split(" ");
            part.setTdate(strArr[0]);
            part.setTtime(strArr[1]);
            part.setType(1);
            
            manager =  ClientManager.getInstance();
	    return manager.getInstallationClientService().addOrUpdateInstallationPartEnable(part, retry);
            
	}catch (ConnectionRetryException e) {
            logger.warn("[setTestMode()]: SBN Middleware Web Service connection exception (Async RETRY scheduled!): " + e.getMessage());

        }catch (ConnectionException e) {
            if(logger.isDebugEnabled())
                logger.error("[setTestMode()]: SBN Middleware Web Service connection error: ", e);
            else
                logger.error("[setTestMode()]: SBN Middleware Web Service connection exception:" + e.getMessage());

            }catch (ApplicationException e) {
            logger.error("[setTestMode()]: SBN Middleware Web Service ATOM exception. errCode:" + e.getErrorCode() + ", errMessage:" + e.getMessage());
	}
        
        if (retry) return 0;
        return -1;
    }

    
    public boolean addAlarmLogEntry(String gguid, String logEntry) {
        if (logger.isDebugEnabled()) {
            logger.debug("[addAlarmLogEntry()]: gguid=" + gguid + ", logmsg:" + logEntry );
        }
        
        return addAlarmLogEntry(gguid, null, null, logEntry, false);
    }
    
    public void generateAlarm(String gguid, String dguid, Integer eventType, String comment, String misc3) {
         if (logger.isDebugEnabled()) {
            logger.debug("[generateAlarm()]: gguid=" + gguid + ", dguid=" + dguid + ", eventType " + eventType + ", comment=" + comment + ", misc3 " + misc3 );
        }

	try {
            manager =  ClientManager.getInstance();
	    Object obj = manager.getDeviceClientService().generateAlarm(gguid, dguid, eventType, comment, misc3, true);
            if (obj == null) {
                logger.error("[generateAlarm()]: Alarm data was not returned from SBN for gguid=" + gguid + ", dguid=" + dguid + ", eventType " + eventType + ", comment=" + comment + ", misc3 " + misc3);
            }
	}catch (ConnectionRetryException e) {
            logger.warn("[generateAlarm()]: SBN Middleware Web Service connection exception (Async RETRY scheduled!): " + e.getMessage());
        }catch (ConnectionException e) {
            if(logger.isDebugEnabled())
                logger.error("[generateAlarm()]: SBN Middleware Web Service connection error: ", e);
            else
                logger.error("[generateAlarm()]: SBN Middleware Web Service connection exception:" + e.getMessage());
	}catch (ApplicationException e) {
            logger.warn("[generateAlarm()]: SBN Middleware Web Service ATOM exception. errCode:" + e.getErrorCode() + ", errMessage:" + e.getMessage());
	}
    }
    
    
    //DLDP 3043 -start -vp889x
    
     public boolean checkIfSubscriptionExists(String subsId, String hwId, String gguid){
    	 
    		
    	int val = lookupInstallation(subsId,hwId,gguid);
    	
    	 if(logger.isDebugEnabled()) {
    	     	logger.debug(val + ": val: checkIfSubscriptionExists : subsId :  " + subsId + " \n hwId : " + hwId + " \n gguid : " + gguid );	
    	 		}
    	
    	 if(val > -1 ) return true;
    	 
    	
    	 
		return false;
    	 
     }
     
 
    //DLDP 3043 -end -vp889x
    
    //////////////////////////////////////////////////// PRIVATE METHODS ////////////////////////////////////////////////////////////////////
    private int lookupInstallation(String subsId, String hwId, String gguid) {
	try {
            manager =  ClientManager.getInstance();
	    int sIns = manager.getInstallationClientService().lookupInstallation(gguid, hwId, subsId, null); //DEMO_GGUID, DEMO_IMEI, DEMO_CTN, DEMO_ACCOUNTID
            return sIns;

	}catch (ConnectionRetryException e) {
            logger.warn("[lookupInstallation()]: SBN Middleware Web Service connection exception (Async RETRY scheduled!): " + e.getMessage());

	}catch (ConnectionException e) {
            if(logger.isDebugEnabled())
                logger.error("[lookupInstallation()]: SBN Middleware Web Service connection error: ", e);
            else
                logger.error("[lookupInstallation()]: SBN Middleware Web Service connection exception:" + e.getMessage());

	}catch (ApplicationException e) {
            logger.error("[lookupInstallation()]: SBN Middleware Web Service ATOM exception. errCode:" + e.getErrorCode() + ", errMessage:" + e.getMessage());
            if(e.getErrorCode()==99) {
                return -2;
            }
	}

        return -1;
    }


    private boolean addUpdateAccount(Account acct, boolean isAdding) {
	try {
            manager =  ClientManager.getInstance();
            manager.getAccountClientService().addOrUpdateAccount(acct, (isAdding? true : false));        //retry enabled for add
            return true;

	}catch (ConnectionRetryException e) {
            logger.warn("[addUpdateAccount()]: SBN Middleware Web Service connection exception (Async RETRY scheduled!): " + e.getMessage());

	}catch (ConnectionException e) {
            if(logger.isDebugEnabled())
                logger.error("[addUpdateAccount()]: SBN Middleware Web Service connection error: ", e);
            else
                logger.error("[addUpdateAccount()]: SBN Middleware Web Service connection exception:" + e.getMessage());

	}catch (ApplicationException e) {
            logger.error("[addUpdateAccount()]: SBN Middleware Web Service ATOM exception. errCode:" + e.getErrorCode() + ", errMessage:" + e.getMessage());
	}

        return false;
    }


    private boolean addUpdateInstallation(Installation inst, boolean retry, boolean isUpdateInstallRequest) {
	try {
            manager =  ClientManager.getInstance();

            //if isUpdateInstallRequest and hasSubsInfoUpdates, this is an
            //update installation request with subsInfo changes.
            //Check to see what subsInfo changed
            //if the address changed, create the comm log entry
            //if any other subsInfo changed, call addOrUpdateInstallation
            if (isUpdateInstallRequest && hasSubsInfoUpdates(inst)) {
                Installation existingInstall = manager.getInstallationClientService().getInstallation(inst.getGguid(), inst.getSIns());
                if (existingInstall != null) {
                   if (isAddressChange(inst, existingInstall)) {
                       if (logger.isDebugEnabled()) {
                           logger.debug("[addUpdateInstallation()]: got address change request");
                       }
                       //create the comm log entry in SBN if there is an address change
                       manager.getInstallationClientService().createCommLogEntry(inst, 0, retry);
                   }
                   if (isInstallDataChange(inst, existingInstall)) {
                       //Non-address related fields changed. Clear the address fields
                       //and call addOrUpdateInstallation
                       inst.setStreet1No1(null); inst.setStreet1(null); inst.setStreet2(null);
                       inst.setCity(null); inst.setState(null); inst.setZip(null);
                       manager.getInstallationClientService().addOrUpdateInstallation(inst, retry);
                   }
                } else {
                    logger.warn(("[addUpdateInstallation()]:Could not get the existing installation from SBN for gGuid:"+inst.getGguid()+
                                 " sIns:"+inst.getSIns()+".  no action performed."));
                }
            } else {
                 manager.getInstallationClientService().addOrUpdateInstallation(inst, retry);
            }
            
            return true;

	}catch (ConnectionRetryException e) {
            logger.warn("[addUpdateInstallation()]: SBN Middleware Web Service connection exception (Async RETRY scheduled!): " + e.getMessage());

	}catch (ConnectionException e) {
            if(logger.isDebugEnabled())
                logger.error("[addUpdateInstallation()]: SBN Middleware Web Service connection error: ", e);
            else
                logger.error("[addUpdateInstallation()]: SBN Middleware Web Service connection exception:" + e.getMessage());

	}catch (ApplicationException e) {
            logger.error("[addUpdateInstallation()]: SBN Middleware Web Service ATOM exception. errCode:" + e.getErrorCode() + ", errMessage:" + e.getMessage() + ", subsid:" + inst.getCtn() + ", gguid:" + inst.getGguid());
	}

        return false;
    }

    private boolean hasSubsInfoUpdates(Installation inst) {

        String [] fields = new String [] {
            inst.getFname(), inst.getName(), inst.getPhone1(), inst.getPhone2(),
            inst.getStreet1No1(), inst.getStreet1(), inst.getStreet2(),
            inst.getCity(), inst.getState(), inst.getZip()
        };

        for (String field : fields) {
           if ( field != null)
               return true;
        }

        return false;
    }

    private boolean isAddressChange(Installation newInst, Installation oldInst) {

        String [] newFields = new String [] {
            newInst.getStreet1No1(), newInst.getStreet1(), newInst.getStreet2(),
            newInst.getCity(), newInst.getState(), newInst.getZip()
        };

        String [] oldFields = new String [] {
            oldInst.getStreet1No1(), oldInst.getStreet1(), oldInst.getStreet2(),
            oldInst.getCity(), oldInst.getState(), oldInst.getZip()
        };

        for (int i=0; i<newFields.length; i++) {
            if (isChanged(newFields[i], oldFields[i]))
                return true;
        }

        return false;
    }

    private boolean isInstallDataChange(Installation newInst, Installation oldInst) {

        //oldInst will not contain a codeword (no need to compare oldInst.codeword with
        //newInst.codeword).
        //If newInst.codeword exists, return true
        if (newInst.getCodeword() != null && newInst.getCodeword().trim().length() >0)
            return true;

        String [] newFields = new String [] {
            newInst.getGguid(), newInst.getImei(), newInst.getTmzonprOlson(),
            newInst.getFname(), newInst.getName(),
            newInst.getPhone1(), newInst.getPhone2(), newInst.getDlfeature(), newInst.getDlflag()
        };

        String [] oldFields = new String [] {
            oldInst.getGguid(), oldInst.getImei(), oldInst.getTmzonprOlson(),
            oldInst.getFname(), oldInst.getName(),
            oldInst.getPhone1(), oldInst.getPhone2(), oldInst.getDlfeature(), oldInst.getDlflag()
        };

         for (int i=0; i<newFields.length; i++) {
            if (isChanged(newFields[i], oldFields[i]))
                return true;
        }
        
        return false;
    }

    private boolean isChanged(String newValue, String oldValue) {
        if ( (oldValue == null || oldValue.trim().length() == 0) &&
                newValue != null && newValue.trim().length() >0) {
            return true;
        }

        if (newValue != null && oldValue != null) {
            if (!newValue.trim().equals(oldValue.trim()))
                return true;
        }
        return false;
    }

    /* updates existing installation (by gguid) */
    private boolean updateInstallation(String gguid, String newHwId, String label, String tzName, String alarmPass, XanbooContact subsInfo)  {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateInstallation()]: (GGUID=" + gguid + "), change to: imei=" + newHwId );
        }

        Installation inst = new Installation();
        inst.setGguid(gguid);

        //update non-null values
        if(newHwId!=null && newHwId.trim().length()>0) inst.setImei(newHwId);                 //new imei
        //if(label!=null && label.trim().length()>0) inst.setTname(label);                       //installation name

        if(tzName!=null && tzName.trim().length()>0) inst.setTmzonprOlson(tzName);                 //timezone
        if(alarmPass!=null && alarmPass.trim().length()>0) inst.setCodeword(alarmPass);

        if(subsInfo!=null) {
            if(subsInfo.getFirstName()!=null && subsInfo.getFirstName().trim().length()>0) inst.setFname(subsInfo.getFirstName());
            if(subsInfo.getLastName()!=null && subsInfo.getLastName().trim().length()>0) inst.setName(subsInfo.getLastName());

            if(subsInfo.getAddress1()!=null && subsInfo.getAddress1().trim().length()>0) {
                try { //extract numeric street number from street1 data
                    int ix = subsInfo.getAddress1().indexOf(" ");
                    if(ix>0 && ix<subsInfo.getAddress1().length()-1) {  //space found, extract the numeric street number before space
                        int streetNo = Integer.parseInt(subsInfo.getAddress1().substring(0, ix));
                        inst.setStreet1No1(""+streetNo);    //convert to string
                        inst.setStreet1(subsInfo.getAddress1().substring(ix+1));
                    }else { //no space, no street number
                        inst.setStreet1(subsInfo.getAddress1());
                        inst.setStreet1No1(null);
                    }
                }catch(Exception ee) {  //in case of exception, no street number parsing
                    inst.setStreet1(subsInfo.getAddress1());
                    inst.setStreet1No1(null);
                }
            }
            if(subsInfo.getAddress2()!=null && subsInfo.getAddress2().trim().length()>0) inst.setStreet2(subsInfo.getAddress2());
            if(subsInfo.getCity()!=null && subsInfo.getCity().trim().length()>0) inst.setCity(subsInfo.getCity());
            if(subsInfo.getState()!=null && subsInfo.getState().trim().length()>0) inst.setState(subsInfo.getState());
            if(subsInfo.getZip()!=null && subsInfo.getZip().trim().length()>0) inst.setZip(subsInfo.getZip());      // "-"+subsInfo.getZip4();
        }

        //now update the installation
        return addUpdateInstallation(inst, true, true);

    }


    private boolean updateInstallationMonStatus(String gguid, String monStatus, boolean retry) {
	try {
            manager =  ClientManager.getInstance();
            manager.getInstallationClientService().updateInstallationMonitoringStatus(gguid, null, monStatus, null, null, 0, retry );        //retry enabled for update mon status
            return true;

	}catch (ConnectionRetryException e) {
            logger.warn("[updateInstallationMonStatus()]: SBN Middleware Web Service connection exception (Async RETRY scheduled!): " + e.getMessage());

        }catch (ConnectionException e) {
            if(logger.isDebugEnabled())
                logger.error("[updateInstallationMonStatus()]: SBN Middleware Web Service connection error: ", e);
            else
                logger.error("[updateInstallationMonStatus()]: SBN Middleware Web Service connection exception:" + e.getMessage());

	}catch (ApplicationException e) {
            logger.error("[updateInstallationMonStatus()]: SBN Middleware Web Service ATOM exception. errCode:" + e.getErrorCode() + ", errMessage:" + e.getMessage() + ", gguid" + gguid + ", monstat:" + monStatus);
	}

        return false;
    }

    
    
    private boolean addUpdateDevice(Device dev, boolean isAdding) {
	try {
            manager =  ClientManager.getInstance();
	    manager.getDeviceClientService().addOrUpdateDevice(dev, null, (isAdding? true : false));        //retry enabled for add
            return true;

	}catch (ConnectionRetryException e) {
            logger.warn("[addUpdateDevice()]: SBN Middleware Web Service connection exception (Async RETRY scheduled!): " + e.getMessage());

	}catch (ConnectionException e) {
            if(logger.isDebugEnabled())
                logger.error("[addUpdateDevice()]: SBN Middleware Web Service connection error: ", e);
            else
                logger.error("[addUpdateDevice()]: SBN Middleware Web Service connection exception:" + e.getMessage());

	}catch (ApplicationException e) {
            logger.error("[addUpdateDevice()]: SBN Middleware Web Service ATOM exception. errCode:" + e.getErrorCode() + ", errMessage:" + e.getMessage() + ", gguid:" + dev.getGatewayGuid() + ", dguid:" + dev.getDeviceGuid() + (dev.getLocId()!=null ? ", locid:"+dev.getLocId(): ""));
	}

        return false;
    }


    private boolean deleteDevice(String gguid, String dguid, boolean retry) {
	try {
            manager =  ClientManager.getInstance();
	    manager.getDeviceClientService().removeDevice(gguid, dguid, null, null, 0, retry);
            return true;

	}catch (ConnectionRetryException e) {
            logger.warn("[deleteDevice()]: SBN Middleware Web Service connection exception (Async RETRY scheduled!): " + e.getMessage());

        }catch (ConnectionException e) {
            if(logger.isDebugEnabled())
                logger.error("[deleteDevice()]: SBN Middleware Web Service connection error: ", e);
            else
                logger.error("[deleteDevice()]: SBN Middleware Web Service connection exception:" + e.getMessage());

	}catch (ApplicationException e) {
            logger.warn("[deleteDevice()]: SBN Middleware Web Service ATOM exception. errCode:" + e.getErrorCode() + ", errMessage:" + e.getMessage() + ", gguid:" + gguid + ", dguid:" + dguid );
            return true;    //ignoring not found exceptions so that device can be deleted from Xanboo
	}

        return false;
    }

    /*
	EmergencyContact contact = new EmergencyContact();
	contact.setGguid("123456789012345");
	contact.setSIns(1);
	contact.setSeq(10);
	contact.setName("Test name 0");
	contact.setPhone1("000-000-0000");
	contact.setPhone2("000-000-0000");
        contact.setCodeword("1234");
	contact.setDebug(0);
*/

    private boolean addOrUpdateEmergencyContact(Object ec, boolean retry) {
	try {
            manager =  ClientManager.getInstance();
            if(ec instanceof EmergencyContact) {
                manager.getInstallationClientService().addOrUpdateEmergencyContact((EmergencyContact) ec, retry);
            }else if(ec instanceof ArrayList) {
                manager.getInstallationClientService().addOrUpdateEmergencyContacts((List)ec, retry);
            }
            return true;

	}catch (ConnectionRetryException e) {
            logger.warn("[addOrUpdateEmergencyContact()]: SBN Middleware Web Service connection exception (Async RETRY scheduled!): " + e.getMessage());

        }catch (ConnectionException e) {
            if(logger.isDebugEnabled())
                logger.error("[addOrUpdateEmergencyContact()]: SBN Middleware Web Service connection error: ", e);
            else
                logger.error("[addOrUpdateEmergencyContact()]: SBN Middleware Web Service connection exception:" + e.getMessage());

	}catch (ApplicationException e) {
            logger.error("[addOrUpdateEmergencyContact()]: SBN Middleware Web Service ATOM exception. errCode:" + e.getErrorCode() + ", errMessage:" + e.getMessage());
	}

        return false;
    }


    private boolean deleteEmergencyContact(String gguid, Integer sIns, int seq, boolean retry) {
	try {
            manager =  ClientManager.getInstance();
	    manager.getInstallationClientService().deleteEmergencyContact(gguid, sIns, seq, 0, retry);

            return true;

	}catch (ConnectionRetryException e) {
            logger.warn("[deleteEmergencyContact()]: SBN Middleware Web Service connection exception (Async RETRY scheduled!): " + e.getMessage());

        }catch (ConnectionException e) {
            if(logger.isDebugEnabled())
                logger.error("[deleteEmergencyContact()]: SBN Middleware Web Service connection error: ", e);
            else
                logger.error("[deleteEmergencyContact()]: SBN Middleware Web Service connection exception:" + e.getMessage());

	}catch (ApplicationException e) {
            //ignoring rc=1, rc=2, rc=3 errors, which will indicate record not in SBN anyways!
            if(e.getErrorCode()==1 || e.getErrorCode()==2 || e.getErrorCode()==3) return true;
            
            logger.error("[deleteEmergencyContact()]: SBN Middleware Web Service ATOM exception. errCode:" + e.getErrorCode() + ", errMessage:" + e.getMessage() + ", gguid" + gguid + ", seq:" + seq);
	}

        return false;
    }
    
    private boolean addAlarmLogEntry(String gguid, Integer sIns, Integer eventNumber, String logEntry, boolean retry) {
    
	try {
            manager =  ClientManager.getInstance();
	    manager.getMiscellaneousClientService().addOrUpdateAlarmLog(gguid, sIns, eventNumber, logEntry, retry);

            return true;

	}catch (ConnectionRetryException e) {
            logger.warn("[addAlarmLogEntry()]: SBN Middleware Web Service connection exception (Async RETRY scheduled!): " + e.getMessage());

        }catch (ConnectionException e) {
            if(logger.isDebugEnabled())
                logger.error("[addAlarmLogEntry()]: SBN Middleware Web Service connection error: ", e);
            else
                logger.error("[addAlarmLogEntry()]: SBN Middleware Web Service connection exception:" + e.getMessage());

	}catch (ApplicationException e) {
            //ignoring rc=1, rc=2, rc=3 errors, which will indicate record not in SBN anyways!
            ///if(e.getErrorCode()==1 || e.getErrorCode()==2 || e.getErrorCode()==3) return true;
            
            logger.error("[addAlarmLogEntry()]: SBN Middleware Web Service ATOM exception. errCode:" + e.getErrorCode() + ", errMessage:" + e.getMessage());
	}

        return false;
    }

    /*
     * Disables test mode
     */
    private int disableTestMode(String gguid, String dguid, String userid, boolean retry) {

	try {
            InstallationPart part = new InstallationPart();
            part.setGguid(gguid);
            if (dguid != null && dguid.trim().length() > 0 && !dguid.equals("0")) {
               part.setDeviceGuid(dguid);
            }
            part.setType(1);
            manager =  ClientManager.getInstance();
	    manager.getInstallationClientService().addOrUpdateInstallationPartDisable(part, retry);
            return 0;
	}catch (ConnectionRetryException e) {
            logger.warn("[disableTestMode()]: SBN Middleware Web Service connection exception (Async RETRY scheduled!): " + e.getMessage());

        }catch (ConnectionException e) {
            if(logger.isDebugEnabled())
                logger.error("[disableTestMode()]: SBN Middleware Web Service connection error: ", e);
            else
                logger.error("[disableTestMode()]: SBN Middleware Web Service connection exception:" + e.getMessage());

	}catch (ApplicationException e) {
            logger.error("[disableTestMode()]: SBN Middleware Web Service ATOM exception. errCode:" + e.getErrorCode() + ", errMessage:" + e.getMessage());
	}
        
        if(retry) return 0;
        return -1;
    }

    private boolean activePlanUpdate(String gguid, boolean retry) {
	try {
            manager =  ClientManager.getInstance();
            manager.getMiscellaneousClientService().accountPlanUpdate(gguid, retry );
            return true;

	}catch (ConnectionRetryException e) {
            logger.warn("[activePlanUpdate()]: SBN Middleware Web Service connection exception (Async RETRY scheduled!): " + e.getMessage());

        }catch (ConnectionException e) {
            if(logger.isDebugEnabled())
                logger.error("[activePlanUpdate()]: SBN Middleware Web Service connection error: ", e);
            else
                logger.error("[activePlanUpdate()]: SBN Middleware Web Service connection exception:" + e.getMessage());

	}catch (ApplicationException e) {
            logger.error("[activePlanUpdate()]: SBN Middleware Web Service ATOM exception. errCode:" + e.getErrorCode() + ", errMessage:" + e.getMessage() + ", gguid" + gguid);
	}

        return false;
    }

    public String getDlcLocalTimeStamp(final String gmtTimestamp, final String dlcTimeZone) {
        if(gmtTimestamp==null || gmtTimestamp.length()!=19) return null;
        
        try {
            SimpleDateFormat sdkFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdkFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date theDate = sdkFormat.parse(gmtTimestamp);

            SimpleDateFormat sbnFormat = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
            sbnFormat.setTimeZone(TimeZone.getTimeZone(dlcTimeZone));

            return sbnFormat.format(theDate).toString();
        } catch (Exception e) {
            logger.warn("[getDlcLocalTimeStamp()]: error for input timestamp "+
                    gmtTimestamp+", input timezone "+dlcTimeZone+". "+e);
            return null;
        }
    }

}
