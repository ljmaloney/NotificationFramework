/*
 * $Source:  $
 * $Id:  $
 *
 * Copyright 2011 AT&T Digital Life
 *
 */

package com.xanboo.core.util;

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import javax.xml.datatype.XMLGregorianCalendar;
import com.att.dlife.csiclient.CSIResponse;
import com.att.dlife.csiclient.ClientManager;
import com.att.dlife.csiclient.AccountNotificationService;
import com.att.dlife.csiclient.DeviceNotificationService;
import com.att.dlife.csiclient.util.PropertiesManager;
import com.att.dlife.csiclient.util.TransactionType;
import com.att.dlife.csiclient.util.ApplicationException;
import com.att.dlife.csiclient.CSIClientService;
import com.att.dlife.csiclient.ws.*;
import com.att.dlife.csiclient.ws.InquirePortActivationStatusResponseInfo.StatusDetails.ActivationStatus;
import com.att.dlife.csiclient.ws.SendAccountNotificationRequestInfo.NotificationSelector.DigitalLife.AlarmDetails;
import com.att.dlife.csiclient.ws.SendAccountNotificationRequestInfo.NotificationSelector.DigitalLife.SystemCondition;

import java.util.*;



/**
 * Utility class to generate notifications thru AT&T CSI CLient Libraries
 * By default, library configuration is done thru a config file specified by
 * a JVM command line argument (E.g. "-Dconfig.path=/etc/csi-client.properties")
 */
public class SimpleACSIClient {

    private static final int MAX_RETRIES=3;

    private static Logger logger = LoggerFactory.getLogger(SimpleACSIClient.class.getName());

    private static ClientManager cmanager;

    private static final String DL_SOC_3GONLY = "DL3GONLY";
    
    public SimpleACSIClient() {}
    
    //an override to config file settings
    public SimpleACSIClient(Properties props) {
        PropertiesManager.configure(props);
    }


    public int send(String BAN, String gguid, String CTN, String toName, String[] toAddress,
            String tStamp, String eventSource, String eventDesc, String alarmCode, String unsubscribeURL,
            String eventId) {
        if(logger.isDebugEnabled()) {
            logger.debug("[send()]:");
        }

        if(toName==null || toName.trim().length()==0) toName="Digital Life Customer";
        if(CTN==null || CTN.trim().length() != 10) CTN="1234567890";    //use fake CTN
        if(BAN==null || BAN.trim().length() < 12 || !XanbooUtil.isValidString(BAN, "01234567890")) BAN="123456789012";  //use fake BAN

        logger.debug("[send()]: unsubscribeURL "+unsubscribeURL+" eventId "+eventId);
        if(alarmCode==null) {
            return sendEvent(BAN, gguid, CTN, toName, toAddress, tStamp, eventSource, eventDesc,
                    unsubscribeURL, eventId);
        }
           
        else
            return sendAlarm(BAN, gguid, CTN, toName, toAddress, tStamp, eventSource, eventDesc, alarmCode, unsubscribeURL);

    }
    
    
    private int sendEvent(String BAN, String gguid, String CTN, String toName, String[] toAddress,
            String tStamp, String eventSource, String eventDesc, String unsubscribeURL, String eventId) {
        if(logger.isDebugEnabled()) {
            logger.debug("[sendEvent()]:");
        }
        
        //incoming date is now in localized format "MM/DD/YY HH:MM am/pm"
        String ts = tStamp;
        int ix = tStamp.indexOf(" ");
        if(ix>-1) ts = tStamp.substring(0,ix) + " at " + tStamp.substring(ix+1);
        
	try {
            cmanager = ClientManager.getInstance();

	    List<String> emails = new ArrayList<String>();
	    List<String> phones = new ArrayList<String>();
            
            for(int i=0; i<toAddress.length; i++) {
                if(toAddress[i]==null) continue;
                if(toAddress[i].indexOf("@")!=-1)
                    emails.add(toAddress[i]);
                if(XanbooUtil.isValidString(toAddress[i], "01234567890")) 
                    phones.add(toAddress[i]);
            }

            SystemCondition cond = new SystemCondition();

            ///v52
	    ///cond.setEventDateTime(javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(ts));
	    ///cond.setEventName(eventSource);

            cond.setEventDateTime(ts);
            ////cond.setEventDescription(eventSource + " " + eventDesc);
            cond.setEventDescription(eventDesc);

            AccountNotificationService anservice = (AccountNotificationService) cmanager.getAccountNotificationService();

            SendAccountNotificationResponseInfo responseInfo = null;
            
            //send email notifs
            if(emails.size()>0) {
                if(logger.isDebugEnabled()) {
                    logger.debug("[sendEvent()]: Sending EMAIL for BAN=" + BAN + ", CTN=" + CTN + ", Name=" + toName + ", To=" + emails.toString() + ", TS=" + ts + ", DEV=" + eventSource + ", EVENT=" + eventDesc);
                }
                CSIResponse response  = anservice.sendNotification(CTN, emails, toName, BAN, "E", cond, unsubscribeURL, eventSource, eventId, false);
                responseInfo = (SendAccountNotificationResponseInfo)response.getResponse();
                if(responseInfo != null) {
                    logger.info("[sendEvent()]: ACSI response code=" + responseInfo.getResponse().getCode() + ",ACSI Conversation ID =" +response.getConversationId()+ ", msg=" + responseInfo.getResponse().getDescription());
                }
            }
            
            //send sms notifs
            if(phones.size()>0) {
                for(int i=0; i<phones.size(); i++) {
                    if(logger.isDebugEnabled()) {
                        logger.debug("[sendEvent()]: Sending SMS for BAN=" + BAN + ", CTN=" + CTN + ", Name=" + toName + ", To=" + phones.get(i) + ", TS=" + ts + ", DEV=" + eventSource + ", EVENT=" + eventDesc);
                    }
                    CSIResponse response =  anservice.sendNotification((String)phones.get(i), null, toName, BAN, "S", cond, null, null, eventId, false);
                    responseInfo = (SendAccountNotificationResponseInfo) response.getResponse();
                    if(responseInfo != null) {
                        logger.info("[sendEvent()]: ACSI response code=" + responseInfo.getResponse().getCode() + ",ACSI Conversation ID =" +response.getConversationId()+", msg=" + responseInfo.getResponse().getDescription());
                    }
                }
            }
            
            if(responseInfo!=null)
                return Integer.parseInt(responseInfo.getResponse().getCode());
            else
                return 0;

	}catch (ApplicationException ae) {
            if(logger.isDebugEnabled()) {
                logger.warn("[sendEvent()]: ACSI Conversation ID ="+ae.getConversationId()+"ACSI Application exception: ", ae);
            }else {
                logger.warn("[sendEvent()]: ACSI Conversation ID ="+ae.getConversationId()+"ACSI Application exception: "+ae.getMessage());
            }
        }
        catch (Exception e) {
	    if(logger.isDebugEnabled()) {
                logger.warn("[sendEvent()]: ACSI exception: ", e);
            }else {
                logger.warn("[sendEvent()]: ACSI exception: " + e.getMessage());
            }
	}

        return -99;
    }


    private int sendAlarm(String BAN, String gguid, String CTN, String toName, String[] toAddress,
            String tStamp, String eventSource, String eventDesc, String alarmCode, String unsubscribeURL) {
        if(logger.isDebugEnabled()) {
            logger.debug("[sendAlarm()]:");
        }
        
        //incoming date is now in localized format "MM/DD/YY HH:MM am/pm ZZZ"
        int ix = tStamp.indexOf(" ");
        
        if(ix==-1 || tStamp.length()<14) {
            logger.warn("[sendAlarm()]: Invalid timestamp: " + tStamp);
            return -88;
        }
        
        String aDate = tStamp.substring(0,ix);
        String aTime = tStamp.substring(ix+1).toLowerCase();
        ix = aTime.indexOf("m ");
        if(ix!=-1) {    //strip timezone suffix ZZZ, if there is one
            aTime = aTime.substring(0, ix+1);
        }

        if(aTime.charAt(1)==':') aTime="0"+aTime;
        if(aTime.length()!=8 || aTime.charAt(2)!=':' || aTime.charAt(5)!=' ') {
            logger.warn("[sendAlarm()]: Invalid time value in timestamp: " + tStamp);
            return -77;
        }
        
	try {
            cmanager = ClientManager.getInstance();

	    List<String> emails = new ArrayList<String>();
	    List<String> phones = new ArrayList<String>();
            
            for(int i=0; i<toAddress.length; i++) {
                if(toAddress[i]==null) continue;
                if(toAddress[i].indexOf("@")!=-1)
                    emails.add(toAddress[i]);
                if(XanbooUtil.isValidString(toAddress[i], "01234567890")) 
                    phones.add(toAddress[i]);
            }

	    List<AlarmDetails> alarmDetails = new ArrayList<SendAccountNotificationRequestInfo.NotificationSelector.DigitalLife.AlarmDetails>();
	    AlarmDetails detail = new AlarmDetails();
            
	    ////detail.setEventName(eventSource + " " + eventDesc);
	    detail.setEventName(eventDesc);
	    
            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.US);
            
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'Z'");
	    sdf.setTimeZone(TimeZone.getTimeZone(CSIClientService.TIME_ZONE));
            //String ts = aDate + " " + aTime;
            String ts = sdf.format(df.parse(tStamp));
	    detail.setEventDate(javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar( ts ));
            
	    detail.setEventTime(aTime);
            detail.setEventNumber("00000000000");        //alarmCode);

            alarmDetails.add(detail);


            AccountNotificationService anservice = (AccountNotificationService) cmanager.getAccountNotificationService();

            SendAccountNotificationResponseInfo responseInfo = null;
            
            //send email notifs
            if(emails.size()>0) {
                if(logger.isDebugEnabled()) {
                    logger.debug("[sendAlarm()]: Sending EMAIL for BAN=" + BAN + ", CTN=" + CTN + ", Name=" + toName + ", To=" + emails.toString() + ", TS=" + tStamp + ", DEV=" + eventSource + ", EVENT=" + eventDesc);
                }
                 CSIResponse response= anservice.sendAlarmNotification(CTN, emails, toName, BAN, "E", alarmDetails, unsubscribeURL, false);
                 responseInfo = (SendAccountNotificationResponseInfo)response.getResponse();
                if(responseInfo != null) {
                    logger.info("[sendAlarm()]: ACSI response code=" + responseInfo.getResponse().getCode() + ",ACSI Conversation ID =" +response.getConversationId()+", msg=" + responseInfo.getResponse().getDescription());
                }
                
                //now sync with SBN, if enabled - end-devices only!
                if(GlobalNames.MODULE_SBN_ENABLED && gguid!=null) {
                    SBNSynchronizer sbn = new SBNSynchronizer();
                    if(!sbn.addAlarmLogEntry(gguid, "EMAIL sent to:" + emails.toString() + "  --> for alarm: '" + detail.getEventName().replace('\n', ' ') + " on " + tStamp + "'")) {
                        logger.info("[sendAlarm()]: Failed to add customer alarm notification to SBN alarm logs");
                    }
                }
            }
            
            //send sms notifs
            if(phones.size()>0) {
                for(int i=0; i<phones.size(); i++) {
                    if(logger.isDebugEnabled()) {
                        logger.debug("[sendAlarm()]: Sending SMS for BAN=" + BAN + ", CTN=" + CTN + ", Name=" + toName + ", To=" + phones.get(i) + ", TS=" + tStamp + ", DEV=" + eventSource + ", EVENT=" + eventDesc);
                    }
                    CSIResponse response  = anservice.sendAlarmNotification((String)phones.get(i), null, toName, BAN, "S", alarmDetails, null, false);
                    responseInfo = (SendAccountNotificationResponseInfo)response.getResponse();
                    if(responseInfo != null) {
                        logger.info("[sendAlarm()]: ACSI response code=" + responseInfo.getResponse().getCode() + ",ACSI Conversation ID =" +response.getConversationId()+", msg=" + responseInfo.getResponse().getDescription());
                    }
                }
                
                //now sync with SBN, if enabled - end-devices only!
                if(GlobalNames.MODULE_SBN_ENABLED && gguid!=null) {
                    SBNSynchronizer sbn = new SBNSynchronizer();
                    if(!sbn.addAlarmLogEntry(gguid, "SMS sent to:" + phones.toString() + "  --> for alarm: '" + detail.getEventName().replace('\n', ' ') + " on " + tStamp + "'")) {
                        logger.info("[sendAlarm()]: Failed to add customer alarm notification to SBN alarm logs");
                    }
                }
                
            }
            
            if(responseInfo!=null)
                return Integer.parseInt(responseInfo.getResponse().getCode());
            else
                return 0;
        }catch (ApplicationException ae) {
	    if(logger.isDebugEnabled()) {
                logger.debug("[sendAlarm()]: Failed to send EMAIL for BAN=" + BAN + ", CTN=" + CTN + ", Name=" + toName + ", To=" + toAddress[0] + ", TS=" + tStamp + ", DEV=" + eventSource + ", EVENT=" + eventDesc + ", ADATE=" + aDate + ", ATIME=" + aTime+", ACSI Conversation ID =" +ae.getConversationId());
                logger.warn("[sendAlarm()]: ACSI Conversation ID =" +ae.getConversationId()+"ACSI exception: ", ae);
            }else {
                logger.warn("[sendAlarm()]: ACSI Conversation ID =" +ae.getConversationId()+" ACSI exception: " + ae.getMessage());
            }
	}catch (Exception e) {
	    if(logger.isDebugEnabled()) {
                logger.debug("[sendAlarm()]: Failed to send EMAIL for BAN=" + BAN + ", CTN=" + CTN + ", Name=" + toName + ", To=" + toAddress[0] + ", TS=" + tStamp + ", DEV=" + eventSource + ", EVENT=" + eventDesc + ", ADATE=" + aDate + ", ATIME=" + aTime);
                logger.warn("[sendAlarm()]: ACSI exception: ", e);
            }else {
                logger.warn("[sendAlarm()]: ACSI exception: " + e.getMessage());
            }
	}

        return -99;
    }
    
    public int sendCancelNotification(String BAN, String CTN, String toName, String toAddress, java.util.Date cancelByDate, String subsType) {
	        if(logger.isDebugEnabled()) {
	            logger.debug("[sendCancelNotification()]:");
	        }
	
	        if(toAddress.indexOf("@")==-1) return -88;
	
	        if(toName==null || toName.trim().length()==0) toName="Digital Life Customer";
	        if(CTN==null || CTN.trim().length() != 10) CTN="1234567890";    //use fake CTN
	        if(BAN==null || BAN.trim().length() < 12 || !XanbooUtil.isValidString(BAN, "01234567890")) BAN="123456789012";      //use fake BAN
	        if(cancelByDate==null) {
	            Calendar cal = Calendar.getInstance();
	            cal.add(Calendar.DAY_OF_MONTH, 60);
	            cancelByDate = cal.getTime();
	        }
	
	        logger.debug("[sendCancelNotification()]: Sending Cancellation notification for BAN=" + BAN + ", CTN=" + CTN + ", Name=" + toName + ", To=" + toAddress + ", cancelBy=" + cancelByDate.toString() + ", subsType=" + subsType);
	
		try {
	            cmanager = ClientManager.getInstance();
			    List<String> emails = new ArrayList<String>();
			    emails.add(toAddress);
	            AccountNotificationService anservice = (AccountNotificationService) cmanager.getAccountNotificationService();
	
		        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'Z'");
	            sdf.setTimeZone(TimeZone.getTimeZone("EST5EDT"));
	
	            SendAccountNotificationResponseInfo responseInfo;
	            
	            CSIResponse response  = null;
	            if(XanbooUtil.isNotEmpty(subsType)) {
	            	 response  = anservice.sendAccountCancelNotification(CTN, emails, toName, BAN, null,
                             javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(sdf.format(cancelByDate)), false, TransactionType.DL_PS_ACC_CANCEL);
	            } else {
	            	 response  = anservice.sendAccountCancelNotification(CTN, emails, toName, BAN, null,
                             javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(sdf.format(cancelByDate)), false);
	            }
	            
	            responseInfo =(SendAccountNotificationResponseInfo)response.getResponse();
			    if(responseInfo != null) {
			    	logger.info("[sendCancelNotification()]: ACSI response code=" + responseInfo.getResponse().getCode() +",ACSI Conversation ID =" +response.getConversationId()+ ", msg=" + responseInfo.getResponse().getDescription());
			    }
	
	            return Integer.parseInt(responseInfo.getResponse().getCode());
	
		}catch (ApplicationException ae) {
		    if(logger.isDebugEnabled()) {
	                logger.warn("[sendCancelNotification()]: ACSI Conversation ID =" +ae.getConversationId()+" ACSI exception: ", ae);
	            }else {
	                logger.warn("[sendCancelNotification()]: ACSI Conversation ID =" +ae.getConversationId()+" ACSI exception: " + ae.getMessage());
	            }
		}catch (Exception e) {
		    if(logger.isDebugEnabled()) {
	                logger.warn("[sendCancelNotification()]: ACSI exception: ", e);
	            }else {
	                logger.warn("[sendCancelNotification()]: ACSI exception: " + e.getMessage());
	            }
		}
        return -99;
    }
    
    /**
     * 
     * @param billingAcctNbr
     * @param notificationDate
     * @param isSuccess
     * @param deviceId
     * @param templateTypeId
     * @return 
     */
    public int sendConfirmationNotification(String billingAcctNbr,java.util.Date notificationDate, Boolean isSuccess,String deviceId,String templateTypeId)
    {
        if(logger.isDebugEnabled()) 
        {
            logger.debug("[sendConfirmationNotification()]: billingAcctNbr="+billingAcctNbr+", notificationDate="+notificationDate+", isSuccess="
                            +isSuccess+", deviceId="+deviceId+", templateTypeId="+templateTypeId);
        }

        if ( billingAcctNbr == null ) return -98;
        if ( isSuccess == null ) isSuccess = false;
        if ( deviceId == null ) deviceId = "";
        if (templateTypeId == null || templateTypeId.equalsIgnoreCase("")) return -97;
        if ( notificationDate == null ) notificationDate = new java.util.Date();

        try 
        {
            cmanager = ClientManager.getInstance();
            AccountNotificationService anservice = (AccountNotificationService) cmanager.getAccountNotificationService();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("EST5EDT"));
            XMLGregorianCalendar xmlCal = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(sdf.format(notificationDate));

            CSIResponse response  = anservice.sendConfirmationNotification(billingAcctNbr,xmlCal,isSuccess, deviceId, templateTypeId, false);
            
            SendConfirmationNotificationResponseInfo responseInfo =(SendConfirmationNotificationResponseInfo)response.getResponse();
            
            if(responseInfo != null) 
            {
                logger.info("[sendConfirmationNotification()]: ACSI response code=" + responseInfo.getResponse().getCode() +",ACSI Conversation ID =" +response.getConversationId()+ ", msg=" + responseInfo.getResponse().getDescription());
            }

            return Integer.parseInt(responseInfo.getResponse().getCode());

        }
        catch (ApplicationException ae) 
        {
            if(logger.isDebugEnabled()) 
                logger.warn("[sendConfirmationNotification()]: ACSI Conversation ID =" +ae.getConversationId()+" ACSI exception: ", ae);
            else 
                logger.warn("[sendConfirmationNotification()]: ACSI Conversation ID =" +ae.getConversationId()+" ACSI exception: " + ae.getMessage());
        }
        catch (Exception e) 
        {
            if(logger.isDebugEnabled()) 
            {
                logger.warn("[sendConfirmationNotification()]: ACSI exception: ", e);
            }
            else 
            {
                logger.warn("[sendConfirmationNotification()]: ACSI exception: " + e.getMessage());
            }
        }
        return -99;
    }
    
    
    public int sendCancelNotification(String BAN, String CTN, String toName, String toAddress, java.util.Date cancelByDate) {
       
    	return sendCancelNotification(BAN, CTN, toName, toAddress, cancelByDate, null);
    }


    /* 
     *  DLDP 2728 Changes -vp889x
     *  -1: invalid arguments
     * -2: subscription found. CTN/IMEI mismatch
     * -3: subscription found. No IMEI found
     * -8: subscription not found
     * -9: exception locating subscription
     *
     *  0: subscription ok, w/o 3G only SOC
     *  1: subscription ok, with 3G only SOC
     */
    
    
  private int get3GInfo( InquireSubscriberProfileResponseInfo responseInfo , String CTN, String IMEI , String conversationId){
    
	    if(CTN==null || CTN.length()==0) return -1;
        if(IMEI==null || IMEI.length()==0) return -1;

    	
    	try{
    		
    		
    	    if(responseInfo != null && responseInfo.getSubscriber()!=null) {
    			InquireSubscriberProfileResponseInfo.Subscriber subs = responseInfo.getSubscriber();
    	                logger.info("[verifySubscription()]: CTN=" + CTN+",IMEI="+IMEI+",ACSI Conversation ID =" +conversationId);
    	                if(subs==null) {
    	                    logger.info("[verifySubscription()]: No subscription found for CTN=" + CTN+",ACSI Conversation ID =" +conversationId);
    	                    return -8;
    	                }
    	                
    	                InquireSubscriberProfileResponseInfo.Subscriber.DeviceInformation dInfo = subs.getDeviceInformation();
    	                
    	                if(dInfo==null || dInfo.getDevice()==null || dInfo.getDevice().isEmpty() || dInfo.getDevice().get(0).getIMEI()==null) {
    	                    logger.warn("[verifySubscription()]: Device/IMEI not found for subscription CTN=" + CTN+",ACSI Conversation ID =" +conversationId);
    	                    return -3;  //IMEI not found, return -3
    	                }
    	                
    	                String subsImei = subs.getDeviceInformation().getDevice().get(0).getIMEI();
    	                if(subsImei==null || subsImei.trim().length()==0) {
    	                    logger.warn("[verifySubscription()]: Device/IMEI not found for subscription CTN=" + CTN+",ACSI Conversation ID =" +conversationId);
    	                    return -3;  //IMEI not found, return -3
    	                }
    	                
    	                if(logger.isDebugEnabled()) logger.debug("Found Subscription IMEI='" + subsImei + "' for subscription CTN=" + CTN+",ACSI Conversation ID =" +conversationId);

    	                if(!subsImei.trim().equals(IMEI)) {
    	                    logger.warn("[verifySubscription()]: Mismatched IMEI for subscription CTN=" + CTN+",ACSI Conversation ID =" +conversationId);
    	                    return -2;  //IMEI mismatch, return -2
    	                }

    	                /* list bolt-on SOCs */
    	                List<OfferingsAdditionalDetailsInfo> boltOns = subs.getAdditionalOfferings();
    	                if(boltOns==null) return 0;
    	                
    	                for (OfferingsAdditionalDetailsInfo boltOn : boltOns) {
    	                    if(boltOn.getOfferingCode().equalsIgnoreCase(DL_SOC_3GONLY)) return 1;
    	                    //System.out.println("SOC code=" + boltOn.getOfferingCode() + ",  desc= " + boltOn.getOfferingDescription());
    	                }

    	                return 0;

    		    }else {
    	                logger.warn("[get3GInfo()]: No subscription found for CTN=" + CTN);
    	                return -8;
    	            }
    		}catch (Exception e) {
    	            if(logger.isDebugEnabled()) {
    	                logger.error("[get3GInfo()]: ACSI exception CTN="+CTN, e);
    	            }else {
    	                logger.error("[get3GInfo()]: ACSI exception CTN="+CTN + ", exception:" + e.getMessage());
    	            }
    		}
    	  return -9;
  }
  
    	
 /**  DLDP 2728 Changes -vp889x
  * T&C Indicator Meaning DL Core Converted value
  FALSE = T&C do not need accepting (TC_FLAG = 0), TRUE = T&C need accepting (TC_FLAG = 1)
  B B2B Status FALSE
  C CBO Accepted FALSE
  E Affiliate accepted FALSE
  I IVR Status (System X) FALSE
  N Prepaid/Reseller Status TRUE
  P Pending (regular activations) TRUE
  S Physical Signature (in store) FALSE
  T third party IVR accepted (applicable to rate plan changes only ) FALSE
  V Verbal (for rate plan changes with a commitment of 11 months or less only) FALSE
  W WEB Accepted (Used by DLOSF and DL APP Getting Started) FALSE
  Y Required, but pending (WLNP only) TRUE
  X Signature Capture Device(for OPUS) FALSE **/

  private int getTermsConditionStatus(
			TermsConditionsStatusInfo termsConditionStatusInfo) {
	  if(logger.isDebugEnabled()) {
          logger.debug("[getTermsConditionStatus()]: termsConditionStatusInfo=" + termsConditionStatusInfo);
      }
  if ( termsConditionStatusInfo == null ) return 1;
	  
	  if(TermsConditionsStatusInfo.B.equals(termsConditionStatusInfo)){
		  return 0;
	
	  }else if (TermsConditionsStatusInfo.C.equals(termsConditionStatusInfo)){
		  return 0;
	  }else if (TermsConditionsStatusInfo.E.equals(termsConditionStatusInfo)){
		  return 0;
	  }else if (TermsConditionsStatusInfo.I.equals(termsConditionStatusInfo)){
		  return 0;
	  }else if (TermsConditionsStatusInfo.N.equals(termsConditionStatusInfo)){
		  return 1;
	  }else if (TermsConditionsStatusInfo.P.equals(termsConditionStatusInfo)){
		  return 1;
	  }else if (TermsConditionsStatusInfo.S.equals(termsConditionStatusInfo)){
		  return 0;
	  }else if (TermsConditionsStatusInfo.T.equals(termsConditionStatusInfo)){
		  return 0;
	  }else if (TermsConditionsStatusInfo.V.equals(termsConditionStatusInfo)){
		  return 0;
	  }else if (TermsConditionsStatusInfo.W.equals(termsConditionStatusInfo)){
		  return 0;
	  }else if (TermsConditionsStatusInfo.Y.equals(termsConditionStatusInfo)){
		  return 1;
	  }else if (TermsConditionsStatusInfo.X.equals(termsConditionStatusInfo)){
		  return 0;
	  }
		return 1;
		
	}
  
    
  /**  DLDP 2728 Changes -vp889x
   * 
   * @param CTN
   * @param IMEI
   * @return int[0] is the 3GOnly Value 
   * 
   */
    public int[] verifySubscription(String CTN,String IMEI)
    {
        return verifySubscription(CTN,IMEI,new HashMap<String,Object>());
    }
    /**
     * 
     * @param CTN
     * @param IMEI
     * @param subsDataMap
     * @return 
     */
    public int[] verifySubscription(String CTN, String IMEI,HashMap<String,Object> subsDataMap) 
    {
        if(logger.isDebugEnabled()) 
        {
            logger.debug("[verifySubscription()]: ctn=" + (CTN==null ? "null" : CTN));
        }
        
       
        int is3GOnly = -9;
   
        
      
    
        try 
        {
            cmanager = ClientManager.getInstance();

            CSIResponse response = cmanager.getSubscriberProfileService().inquireSubscriberProfile(CTN, false);
            InquireSubscriberProfileResponseInfo responseInfo = (InquireSubscriberProfileResponseInfo)response.getResponse();
            is3GOnly =       get3GInfo(responseInfo , CTN , IMEI , response.getConversationId());
            
            
            
            InquireSubscriberProfileResponseInfo.Subscriber subs = responseInfo.getSubscriber();
      
      
  
            /** DLDP 3056 **/
            String subsClass = "";
            if ( subs.getSubscriberIndicators() != null && subs.getSubscriberIndicators().getSubscriptionClass() != null )
                subsClass = subs.getSubscriberIndicators().getSubscriptionClass();
            subsDataMap.put("SubscriptionClass", subsClass);
            subsDataMap.put("SOC_CODES",new ArrayList<String>()); //default value
            if ( logger.isDebugEnabled() )
                logger.debug("[verifySubscription() - subscriptionClass for ctn="+CTN+" and IMEI="+IMEI+" is "+subsClass);
            if ( subs.getAdditionalOfferings() != null )
            {
                List<OfferingsAdditionalDetailsInfo> addlOfferings = subs.getAdditionalOfferings();
                for ( OfferingsAdditionalDetailsInfo offering : addlOfferings )
                {
                    String socCode = offering.getOfferingCode();
                    if ( subsDataMap.containsKey("SOC_CODES") )
                    {
                        ((List)subsDataMap.get("SOC_CODES")).add(socCode);
                    }
                    else
                        subsDataMap.put("SOC_CODES",new ArrayList<String>().add(socCode));
                }
            }
            /** DLDP 3056 **/
        
        } 
        catch (ApplicationException e) 
        {
            if(logger.isDebugEnabled()) 
            {
                logger.error("[verifySubscription()]: ACSI application exception CTN="+CTN+", ACSI Conversation ID =" +e.getConversationId(), e);
            }
            else 
            {
                logger.error("[verifySubscription()]: ACSI application exception CTN="+"CTN ACSI Conversation ID =" +e.getConversationId()+ ", exception:" + e.getMessage());
            }
            is3GOnly = -8;
            
        } 
        catch (Exception e) 
        {
            if(logger.isDebugEnabled()) 
            {
                logger.error("[verifySubscription()]: ACSI exception CTN="+CTN, e);
            }
            else 
            {
                logger.error("[verifySubscription()]: ACSI exception CTN="+CTN + ", exception:" + e.getMessage());
            }
        }
  
	
        return new int[]{is3GOnly };
    }
    
    

	public int checkTermsConditionStatus(String CTN) {
		int termsConditionStatus;
		CSIResponse response;
		TermsConditionsStatusInfo tcsi = null;
		try {
			cmanager = ClientManager.getInstance();

			response = cmanager.getPortActivationService()
					.inquirePortActivationStatus(CTN, false);
			InquirePortActivationStatusResponseInfo ipasInfo = (InquirePortActivationStatusResponseInfo) response
					.getResponse();

			ActivationStatus statusDetail = ipasInfo.getStatusDetails()
					.getActivationStatus();
if(statusDetail != null && statusDetail.getContractCode() != null)
			tcsi = TermsConditionsStatusInfo.valueOf(statusDetail
					.getContractCode());
		} catch (Exception e) {
			// ignore

			logger.error(
					"[getTermsConditionStatus()]: inquirePortActivationStatus CTN="
							+ CTN, e);

			return -1;
		}

		termsConditionStatus = getTermsConditionStatus(tcsi);

		if (logger.isDebugEnabled()) {
			logger.debug("[DLDP2728 - getTermsConditionStatus()]:  CTN=" + CTN
					+ ", termsConditionStatus =" + termsConditionStatus);
		}
		return termsConditionStatus;
	}



	public String inquireSubscriptionStatus(String CTN) {
        if(logger.isDebugEnabled()) {
            logger.debug("[inquireSubscriptionStatus()]: ctn=" + (CTN==null ? "null" : CTN));
        }
        if(CTN==null || CTN.length()==0) return null;

	try {
            cmanager = ClientManager.getInstance();

            CSIResponse response = cmanager.getSubscriberProfileService().inquireSubscriberProfile(CTN, false);
            InquireSubscriberProfileResponseInfo responseInfo = (InquireSubscriberProfileResponseInfo)response.getResponse();
	    if(responseInfo != null && responseInfo.getSubscriber()!=null) {
		InquireSubscriberProfileResponseInfo.Subscriber subs = responseInfo.getSubscriber();
                
                InquireSubscriberProfileResponseInfo.Subscriber.DeviceInformation dInfo = subs.getDeviceInformation();
                String subsImei;
                logger.info("[inquireSubscriptionStatus()]:CTN=" + CTN+",ACSI Conversation ID=" +response.getConversationId());
                if(dInfo==null || dInfo.getDevice()==null || dInfo.getDevice().isEmpty() || dInfo.getDevice().get(0).getIMEI()==null) {
                    subsImei = "NONE";
                }else {
                    subsImei = dInfo.getDevice().get(0).getIMEI();
                }

                String subsStatus = subs.getSubscriberStatus().getSubscriberStatus().value();

                return subsStatus + "-" + subsImei;

	    }else {
                logger.info("[inquireSubscriptionStatus()]: No subscription found for CTN=" + CTN+",ACSI Conversation ID=" +response.getConversationId());
                return null;
            }
	} catch (ApplicationException e) {
            if(logger.isDebugEnabled()) {
                logger.error("[inquireSubscriptionStatus()]: ACSI inquire profile exception CTN="+CTN+", ACSI Conversation ID =" +e.getConversationId(), e);
            }else {
                logger.warn("[inquireSubscriptionStatus()]: No subscription found for CTN=" + CTN+", ACSI Conversation ID =" +e.getConversationId());
            }
	}catch (Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[inquireSubscriptionStatus()]: ACSI inquire profile exception CTN="+CTN, e);
            }else {
                logger.warn("[inquireSubscriptionStatus()]: No subscription found for CTN=" + CTN);
            }
	}

        return null;
    }


    public int activateSubscription(String CTN) throws Exception {
        if(logger.isDebugEnabled()) {
            logger.debug("[activateSubscription()]: ctn=" + (CTN==null ? "null" : CTN));
        }
        if(CTN==null || CTN.length()==0) return -1;

        return updateSubscriptionStatus(CTN, "R");
    }


    private int updateSubscriptionStatus(String CTN, String status) throws Exception {
        if(logger.isDebugEnabled()) {
            logger.debug("[updateSubscriptionStatus()]: ctn=" + (CTN==null ? "null" : CTN) + ", status:" + status);
        }

	try {
            cmanager = ClientManager.getInstance();
            CSIResponse response = cmanager.getSubscriberProfileService().updateSubscriberStatus(CTN, "CR", status, false);
            UpdateSubscriberStatusResponseInfo responseInfo = (UpdateSubscriberStatusResponseInfo) response.getResponse();
            int rc = Integer.parseInt(responseInfo.getResponse().getCode());
            if(rc!=0) {
                logger.warn("[updateSubscriptionStatus()]: ACSI response code=" + rc +",ACSI Conversation ID " +response.getConversationId()+ ", msg=" + responseInfo.getResponse().getDescription());
            }else {
                logger.info("[updateSubscriptionStatus()]: ACSI response code=" + rc + ",ACSI Conversation ID " +response.getConversationId()+", msg=" + responseInfo.getResponse().getDescription());
            }

            return rc;

	} catch (ApplicationException e) {
            if(logger.isDebugEnabled()) {
                logger.error("[updateSubscriptionStatus()]: ACSI exception CTN="+CTN+", ACSI Conversation ID =" +e.getConversationId(), e);
            }else {
                logger.error("[updateSubscriptionStatus()]: ACSI exception CTN="+CTN +", ACSI Conversation ID =" +e.getConversationId()+ ", exception:" + e.getMessage());
            }
	}catch (Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[updateSubscriptionStatus()]: ACSI exception CTN="+CTN, e);
            }else {
                logger.error("[updateSubscriptionStatus()]: ACSI exception CTN="+CTN + ", exception:" + e.getMessage());
            }
	}

        return -99;

    }
    
    
    public int newDeviceRegistered(String BAN, String CTN, String gguid, String dGuid, String catId, String insDate, String insId, String serialNo, String hwAddress, String sourceId) {
        if (logger.isDebugEnabled()) {
            logger.debug("[newDeviceRegistered()]: ctn=" + (CTN==null ? "null" : CTN) + ", gguid:" + gguid + ", dguid:" + dGuid + ", catalogId:" + catId + ", ts:" + insDate);
        }
        
	try {
            cmanager = ClientManager.getInstance();
            String classSubclassId = catId.substring(4, 10); 
           if(sourceId!=null && sourceId.equalsIgnoreCase("U")){
        	   classSubclassId=classSubclassId+"U";
           }

    	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone(CSIClientService.TIME_ZONE));  
            Date ts=null;
            if(insDate==null || insDate.length()!=19) {
                ts = new Date();
            }else {
                try {
                    ts = sdf.parse(insDate);
                }catch(Exception ee) { ts=new Date(); }
            }
 	    sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone(CSIClientService.TIME_ZONE));  
	    XMLGregorianCalendar installDate = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar( sdf.format(ts) );
            
            if(insId==null || insId.trim().length()==0) insId="C";
            String insType = (insId.equals("C") || insId.endsWith("C")) ? "S" : "P";
            String make = null;
            String model = null;
            //Change DLDP-SRD-2524: Enhance DL Core to send only self-install devices to Oracle via CSI SendDigitalLifeDeviceNotification interface.
            if(insType != null && insType.equals("S")){
	            DeviceNotificationService dnservice = (DeviceNotificationService) cmanager.getDeviceNotificationService();
	            Boolean thirdPartyInd = true;
	            if(sourceId == null || sourceId.trim().length()==0){
	            	thirdPartyInd = false;
	            }
	            CSIResponse response = dnservice.sendDigitalLifeAddNewDeviceNotification(BAN, CTN, dGuid, installDate, insId, insType, hwAddress, make, model, serialNo, classSubclassId, false, thirdPartyInd);
	            SendDigitalLifeDeviceNotificationResponseInfo responseInfo = (SendDigitalLifeDeviceNotificationResponseInfo)response.getResponse();
	            int rc = Integer.parseInt(responseInfo.getResponse().getCode());
	            if(rc!=0) {
	                logger.warn("[newDeviceRegistered()]: ACSI ADD DEVICE NOTIFICATION FAILED for: extAcc:" + BAN + ", subid:" + CTN + ", gguid:" +  gguid + ", dguid:" +  dGuid + ", catid:" +  catId + ", rc=" + rc + ", msg=" + responseInfo.getResponse().getDescription()+",ACSI Conversation ID =" +response.getConversationId());
	            }else {
	                logger.info("[newDeviceRegistered()]: SENT ACSI ADD DEVICE NOTIFICATION for: extAcc:" + BAN + ", subid:" + CTN + ", gguid:" +  gguid + ", dguid:" +  dGuid + ", catid:" +  catId +",ACSI Conversation ID =" +response.getConversationId());
	            }
	            return rc;
            }
            return -99;
            
            

	}catch (ApplicationException e) {
            if(logger.isDebugEnabled()) {
                logger.error("[newDeviceRegistered()]: ACSI ADD DEVICE NOTIFICATION EXCEPTION for: extAcc:" + BAN + ", subid:" + CTN + ", gguid:" +  gguid + ", dguid:" +  dGuid + ", catid:" +  catId+", ACSI Conversation ID =" +e.getConversationId(), e);
            }else {
                logger.error("[newDeviceRegistered()]: ACSI ADD DEVICE NOTIFICATION EXCEPTION for: extAcc:" + BAN + ", subid:" + CTN + ", gguid:" +  gguid + ", dguid:" +  dGuid + ", catid:" +  catId +", ACSI Conversation ID =" +e.getConversationId()+ ", exception:" + e.getMessage());
            }
	}catch (Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[newDeviceRegistered()]: ACSI ADD DEVICE NOTIFICATION EXCEPTION for: extAcc:" + BAN + ", subid:" + CTN + ", gguid:" +  gguid + ", dguid:" +  dGuid + ", catid:" +  catId, e);
            }else {
                logger.error("[newDeviceRegistered()]: ACSI ADD DEVICE NOTIFICATION EXCEPTION for: extAcc:" + BAN + ", subid:" + CTN + ", gguid:" +  gguid + ", dguid:" +  dGuid + ", catid:" +  catId + ", exception:" + e.getMessage());
            }
	}

        return -99;
    }
    
    
    public int deviceRemoved(String BAN, String CTN, String gguid, String dGuid, String delDate) {
        if (logger.isDebugEnabled()) {
            logger.debug("[deviceRemoved()]: ctn=" + (CTN==null ? "null" : CTN) + ", gguid:" + gguid + ", dguid:" + dGuid + ", ts:" + delDate);
        }

	try {
            cmanager = ClientManager.getInstance();
            
    	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone(CSIClientService.TIME_ZONE));  
            Date ts=null;
            if(delDate==null || delDate.length()!=19) {
                ts = new Date();
            }else {
                try {
                    ts = sdf.parse(delDate);
                }catch(Exception ee) { ts=new Date(); }
            }
 	    sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone(CSIClientService.TIME_ZONE));  
	    XMLGregorianCalendar deleteDate = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar( sdf.format(ts) );

            String insId="C";
            String insType = insId.equals("C") ? "S" : "P";
            String classSubclassId = "000000";
            //Change DLDP-SRD-2524: Enhance DL Core to send only self-install devices to Oracle via CSI SendDigitalLifeDeviceNotification interface.
            if(insType != null && insType.equals("S")){
	            DeviceNotificationService dnservice = (DeviceNotificationService) cmanager.getDeviceNotificationService();
	            CSIResponse response = dnservice.sendDigitalLifeDeleteDeviceNotification(BAN, CTN, dGuid, deleteDate, insId, insType, null, null, null, null, classSubclassId, false, false);
	            SendDigitalLifeDeviceNotificationResponseInfo responseInfo = (SendDigitalLifeDeviceNotificationResponseInfo)response.getResponse();
	            int rc = Integer.parseInt(responseInfo.getResponse().getCode());
	            if(rc!=0) {
	                logger.warn("[deviceRemoved()]: ACSI DELETE DEVICE NOTIFICATION FAILED for: extAcc:" + BAN + ", subid:" + CTN + ", gguid:" +  gguid + ", dguid:" +  dGuid + ", rc=" + rc + ", msg=" + responseInfo.getResponse().getDescription()+",ACSI Conversation ID =" +response.getConversationId());
	            }else {
	                logger.info("[deviceRemoved()]: SENT ACSI DELETE DEVICE NOTIFICATION for: extAcc:" + BAN + ", subid:" + CTN + ", gguid:" +  gguid + ", dguid:" +  dGuid +",ACSI Conversation ID =" +response.getConversationId());
	            }
	            return rc;
            }
	} catch (ApplicationException e) {
            if(logger.isDebugEnabled()) {
                logger.error("[deviceRemoved()]: ACSI DELETE DEVICE NOTIFICATION EXCEPTION for: extAcc:" + BAN + ", subid:" + CTN + ", gguid:" +  gguid + ", dguid:" +  dGuid +", ACSI Conversation ID =" +e.getConversationId(), e);
            }else {
                logger.error("[deviceRemoved()]: ACSI DELETE DEVICE NOTIFICATION EXCEPTION for: extAcc:" + BAN + ", subid:" + CTN + ", gguid:" +  gguid + ", dguid:" +  dGuid +", ACSI Conversation ID =" +e.getConversationId()+ ", exception:" + e.getMessage());
            }
	}catch (Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[deviceRemoved()]: ACSI DELETE DEVICE NOTIFICATION EXCEPTION for: extAcc:" + BAN + ", subid:" + CTN + ", gguid:" +  gguid + ", dguid:" +  dGuid , e);
            }else {
                logger.error("[deviceRemoved()]: ACSI DELETE DEVICE NOTIFICATION EXCEPTION for: extAcc:" + BAN + ", subid:" + CTN + ", gguid:" +  gguid + ", dguid:" +  dGuid + ", exception:" + e.getMessage());
            }
	}

        return -99;
        
    }
    

}
