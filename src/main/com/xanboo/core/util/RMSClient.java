/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.util;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author rp385f
 */
public class RMSClient {
    //private static ClientManager manager = ClientManager.getInstance();

    private static Logger logger = LoggerFactory.getLogger(RMSClient.class.getName());
    private static final String DLC_ALREADY_EXISTS = "415";
    private static final String DLC_ALREADY_EXISTS_MSG = "already exists";
    
    public static void deleteDLC(String imei, String domainId, String rmsphEnabled) throws XanbooException {
    	if(rmsphEnabled.equalsIgnoreCase("rms") && testClassPath("com.att.dlife.rms.client.ClientManager")){  // RMS Client	     
	            try {
	                com.att.dlife.rms.client.ClientManager manager = com.att.dlife.rms.client.ClientManager.getInstance();
	                com.att.dlife.rms.client.util.ServiceResponse response = manager.geRMSClientService().deleteDLC(imei, Boolean.TRUE);
	                
	                if(response != null && response.getCode() != null && !response.getCode().equals("0") ){
	                    logger.warn("DeleteDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
	                }   
	                else{
	                    logger.info("DeleteDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
	                }
	            } catch (Exception e) {
	                throw new XanbooException(32000, "[deleteRMSDLC]:" + e.getMessage());
	            }	     
    	}
    	else
    	if(rmsphEnabled.equalsIgnoreCase("ph")){  // PH Client	     
            try {
                com.att.dlife.ph.client.ClientManager manager = com.att.dlife.ph.client.ClientManager.getInstance();
                com.att.dlife.ph.client.util.ServiceResponse response = manager.getPhClientService().deleteDLC(imei, domainId, Boolean.TRUE);
                
                if(response != null && response.getCode() != null && !response.getCode().equals("0") ){
                    logger.warn("DeleteDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
                }   
                else{
                    logger.info("DeleteDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
                }
            } catch (Exception e) {
                throw new XanbooException(32000, "[deletePHDLC]:" + e.getMessage());
            }	     
	}

    }
    public static void updateDLC(String imei,String typeValue,String type, String domainId, String rmsphEnabled) throws XanbooException {
        if (rmsphEnabled.equalsIgnoreCase("rms") && testClassPath("com.att.dlife.rms.client.ClientManager")) {
            try {
                com.att.dlife.rms.client.ClientManager manager = com.att.dlife.rms.client.ClientManager.getInstance();
                com.att.dlife.rms.client.util.ServiceResponse response = manager.geRMSClientService().updateDLC(imei,typeValue,type, Boolean.TRUE);
                
                if(response != null && response.getCode() != null && !response.getCode().equals("0") ){
                    logger.warn("updateDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
                }   
                else{
                    logger.info("updateDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
                }
            } catch (Exception e) {
                throw new XanbooException(32010, "[updateRMSDLC]:" + e.getMessage());
            }
        }else
        if (rmsphEnabled.equalsIgnoreCase("ph")) {
            try {
                com.att.dlife.ph.client.ClientManager manager = com.att.dlife.ph.client.ClientManager.getInstance();
                com.att.dlife.ph.client.util.ServiceResponse response = manager.getPhClientService().updateDLC(imei,typeValue,type, domainId,Boolean.TRUE);
                
                if(response != null && response.getCode() != null && !response.getCode().equals("0") ){
                    logger.warn("updateDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
                }   
                else{
                    logger.info("updateDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
                }
            } catch (Exception e) {
                throw new XanbooException(32010, "[updatePHDLC]:" + e.getMessage());
            }
        }

    }
    public static void replaceDLC(String oldImei,String newImei,String typeValue,String type, String domainId, String rmsphEnabled) throws XanbooException {
        if (rmsphEnabled.equalsIgnoreCase("rms") && testClassPath("com.att.dlife.rms.client.ClientManager")) {
            try {
                com.att.dlife.rms.client.ClientManager manager = com.att.dlife.rms.client.ClientManager.getInstance();
                com.att.dlife.rms.client.util.ServiceResponse response = manager.geRMSClientService().replaceDLC(oldImei,newImei,typeValue,type, Boolean.TRUE);
                
                if(response != null && response.getCode() != null && !response.getCode().equals("0") ){
                    logger.warn("ReplaceDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
                }   
                else{
                    logger.info("replaceDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
                }
            } catch (Exception e) {
                throw new XanbooException(32020, "[replaceRMSDLC]:" + e.getMessage());
            }
        }
        else
        	if (rmsphEnabled.equalsIgnoreCase("ph") ) {
                try {
                    com.att.dlife.ph.client.ClientManager manager = com.att.dlife.ph.client.ClientManager.getInstance();
                    com.att.dlife.ph.client.util.ServiceResponse response = manager.getPhClientService().replaceDLC(oldImei,newImei,typeValue,type, domainId, Boolean.TRUE);
                    
                    if(response != null && response.getCode() != null && !response.getCode().equals("0") ){
                        logger.warn("ReplaceDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
                    }   
                    else{
                        logger.info("replaceDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
                    }
                } catch (Exception e) {
                    throw new XanbooException(32020, "[replaceRMSDLC]:" + e.getMessage());
                }
            }

    } 
    public static void registerDLC(String imei, HashMap<String,String> groups,String domainId, String rmsphEnabled) throws XanbooException  {
    	if(rmsphEnabled.equalsIgnoreCase("rms")){  // RMS Client
	        try {
	            com.att.dlife.rms.client.ClientManager manager = com.att.dlife.rms.client.ClientManager.getInstance();
	            com.att.dlife.rms.client.util.ServiceResponse response = manager.geRMSClientService().registerDLC(imei, groups, domainId, Boolean.TRUE);
	            if(response != null && response.getCode() != null && !response.getCode().equals("0") ){
	                    logger.warn("registerDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
	                }   
	                else{
	                    logger.info("Success registerDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
	                }
	        } catch (Exception e) {
	                throw new XanbooException(32020, "[registerDLC]:" + e.getMessage());
	            }
    	}
    	else if(rmsphEnabled.equalsIgnoreCase("ph") && imei != null){     // Prime Home Client
    		try {
	            com.att.dlife.ph.client.ClientManager manager = com.att.dlife.ph.client.ClientManager.getInstance();
	            com.att.dlife.ph.client.util.ServiceResponse response = manager.getPhClientService().registerDLC(imei, groups, domainId, Boolean.TRUE);
	            if(response != null && response.getCode() != null && !response.getCode().equals("0") ){
	                    logger.warn("registerDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
	                }   
	                else{
	                    logger.info("Success registerDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
	                }
	        } catch (Exception e) {
	                throw new XanbooException(32020, "[registerDLC]:" + e.getMessage());
	            }	
    	}
    }
    
    public static void registerOrUpdateDLC(String imei, HashMap<String,String> groups,String domainId, String rmsphEnabled) throws XanbooException  {
    	if(rmsphEnabled.equalsIgnoreCase("rms")){  // RMS Client
	        try {
	            com.att.dlife.rms.client.ClientManager manager = com.att.dlife.rms.client.ClientManager.getInstance();
	            com.att.dlife.rms.client.util.ServiceResponse response = manager.geRMSClientService().registerDLC(imei, groups, domainId, Boolean.TRUE);
	            if(response != null && response.getCode() != null && !response.getCode().equals("0") ){
	                    logger.warn("registerOrUpdateDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
	                    //if the registration fails because of existing DLC, update the DLC
	                    String responseMessage = response.getMessage();
	                    if(response.getCode().equals(DLC_ALREADY_EXISTS)){
	                    	if(groups != null && groups.size() >0){
		                    	Map.Entry<String,String> groupEntry=groups.entrySet().iterator().next();
		                    	response = manager.geRMSClientService().updateDLC(imei,groupEntry.getValue(),groupEntry.getKey(), Boolean.TRUE);
		                    	if(response != null && response.getCode() != null && !response.getCode().equals("0") ){
		                    		logger.warn("updateDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
		                    	}   
		                    	else{
		                    		logger.info("Success updateDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
		                    	}
	                    	}
	                    }
	           }   
	           else{
	                    logger.info("Success registerDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
	          }
	        } catch (Exception e) {
	                throw new XanbooException(32020, "[registerDLC]:" + e.getMessage());
	            }
    	}
    	else if(rmsphEnabled.equalsIgnoreCase("ph")){    		
	        try {
	            com.att.dlife.ph.client.ClientManager manager = com.att.dlife.ph.client.ClientManager.getInstance();
	            com.att.dlife.ph.client.util.ServiceResponse response = manager.getPhClientService().registerDLC(imei, groups, domainId, Boolean.TRUE);
	            if(response != null && response.getCode() != null && !response.getCode().equals("0") ){
	                    logger.warn("registerOrUpdateDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
	                    //if the registration fails because of existing DLC, update the DLC	                    
	                    if(response.getCode().equals(DLC_ALREADY_EXISTS)){
	                    	if(groups != null && groups.size() >0){
		                    	Map.Entry<String,String> groupEntry=groups.entrySet().iterator().next();
		                    	response = manager.getPhClientService().updateDLC(imei,groupEntry.getValue(),groupEntry.getKey(), domainId, Boolean.TRUE);
		                    	if(response != null && response.getCode() != null && !response.getCode().equals("0") ){
		                    		logger.warn("updateDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
		                    	}   
		                    	else{
		                    		logger.info("Success updateDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
		                    	}
	                    	}
	                    }
	           }   
	           else{
	                    logger.info("Success registerDLC returned code " + response.getCode() + ". Message: " + response.getMessage());
	          }
	        } catch (Exception e) {
	                throw new XanbooException(32020, "[registerDLC]:" + e.getMessage());
	            }
    	
    	}
    }
    private static boolean testClassPath(String fullClassName) {
        boolean result = false;
        try {
            Class.forName(fullClassName);
            result = true;
        } catch (Throwable e) {
            //e.printStackTrace();
        }
        return result;
    }
}
