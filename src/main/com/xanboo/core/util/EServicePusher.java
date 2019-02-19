/*
 * $Source:  $
 * $Id:  $
 *
 * Copyright 2012 AT&T Digital Life
 *
 */


package com.xanboo.core.util;

import com.xanboo.core.util.GlobalNames;
import com.xanboo.core.util.LoggerFactory;
import com.xanboo.core.util.Logger;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;

/**
 * Utility class to handle eService posts
 */
public class EServicePusher  {
    private static Logger logger = LoggerFactory.getLogger(EServicePusher.class.getName());

    /**
     *  Constructor: Also starts the thread to perform the check
     */
    public EServicePusher() {
    }


    //posts state change requests to eService
    public static void post2StateListener(long accountId, String gguid, String dguid, String catId, String oid, String val, String cqId, String ack, String filePath) {
        if(gguid==null || dguid==null || oid==null) return;

        //post now
        //  e.g. http://svcdev.digitallife.att.com/messageRelay/notify?key=[GUID]&dev=[deviceID]&(oid/eid)=[MOID/EID]&value=[VALUE]

        String bContent=null;  //binary content, if available
        
        //if binary content specified and OID is in filter config and text content, post binary content as value parameter (urlencoded)
        if(filePath!=null && GlobalNames.ESERVICE_FILTER_OID_BINARY.indexOf(oid)!=-1 && val!=null && (val.indexOf("text")!=-1 || val.indexOf("json")!=-1)) {
            byte[] binaryContent = null;
            try {
                logger.debug("[post2StateListener()]: binary file:" + filePath);                
                binaryContent = XanbooUtil.getFileBytes(filePath);
                logger.debug("[post2StateListener()]: binary file size:" + binaryContent.length);                
                if(binaryContent==null || binaryContent.length==0) {    //no or zero content, pass "&content="
                    bContent = "";       //blank content
                }else if(binaryContent.length>(GlobalNames.ESERVICE_MAX_SIZE_BINARY*1024)) {        //content >max size, do not pass &content
                    bContent = null;
                }else {
                    bContent = new String(binaryContent, GlobalNames.getCharSet());
                }
                
            }catch(Exception ee) { 
                bContent = null;    //exceptions reading/encoding binary, do not pass &content
                if(logger.isDebugEnabled()) {
                    logger.warn("[post2StateListener()]: exception reading/encoding binary file. Exception: " + ee.getMessage(), ee);
                }else {
                    logger.warn("[post2StateListener()]: exception reading/encoding binary file. Exception: " + ee.getMessage());
                }
            }
        }
        
        String postData=null;
        try {
            postData = "acc=" + accountId + "&key=" + gguid + "&dev=" + dguid + "&MOID=" + oid;
            
            if(cqId!=null && cqId.trim().length()>0) postData += "&cqid=" + java.net.URLEncoder.encode(cqId, GlobalNames.getCharSet());
            if(ack!=null && ack.trim().length()>0) postData += "&ack=" + ack;
            if(catId!=null && catId.length()>9) {
                postData += "&deviceClassId=" + catId.substring(4,8) + "&deviceSubClassId=" + catId.substring(8,10) + "&vendorId=" + catId.substring(0,4);
            }
            
            postData += "&value=" + (val==null ? "" : java.net.URLEncoder.encode(val, GlobalNames.getCharSet()));
            postData += (bContent==null ? "" : "&content=" + java.net.URLEncoder.encode(bContent, GlobalNames.getCharSet()));
            
        }catch(Exception e) {
            logger.warn("[post2StateListener()]: exception encoding eService post data: " + e.getMessage());
            return;
        }

        post(postData);
    }


    //posts Xanboo events to eService
    public static  void post2EventListener(long accountId, String gguid, String dguid, String catId, String eid, String msg, String srcDguid, String cqId, String ack,String customAttr, long itemId) {
        if(gguid==null || dguid==null || eid==null) return;

        ////final String EIDs = "1030,1035,1036,1050,660,661,2090,2091,2092,2093,1060,1061,1062,1065";      //allowed EIDs   -->moved to GlobalNames xancore config

        //if system event or one of allowed eids, continue posting
        if(GlobalNames.ESERVICE_FILTER_EID.indexOf(eid)==-1 && !gguid.equals("0")) return;

        //post now
        //  e.g. http://svcdev.digitallife.att.com/messageRelay/notify?key=[GUID]&dev=[deviceID]&(oid/eid)=[MOID/EID]&value=[VALUE]&sdev=[SRCDEV]

        String postData=null;
        try {
            postData = "acc=" + accountId + "&key=" + gguid + "&dev=" + dguid + (srcDguid!=null ? ("&sdev="+srcDguid) : "") + "&EID=" + eid + "&value=" + (msg==null ? "" : java.net.URLEncoder.encode(msg, "UTF-8"));
            if(cqId!=null && cqId.trim().length()>0) postData += "&cqid=" + java.net.URLEncoder.encode(cqId, "UTF-8");
            if(ack!=null && ack.trim().length()>0) postData += "&ack=" + ack;
            if(catId!=null && catId.length()>9) {
                postData += "&deviceClassId=" + catId.substring(4,8) + "&deviceSubClassId=" + catId.substring(8,10) + "&vendorId=" + catId.substring(0,4);
            }
            if(itemId>-1) postData += "&inboxid=" + itemId;
            if(customAttr != null && customAttr.length() > 0 ) postData += "&custom=" + customAttr;
        }catch(Exception e) {
            logger.warn("[post2EventListener()]: exception encoding eService post data: " + e.getMessage());
            return;
        }

        post(postData);
    }
    //a request with an abbreviated parameter list to support usage by 
    //soluton center project
    public static void postSendNotification(Long accountId,String gguid,String label,String badge)
    {
        if ( logger.isDebugEnabled() )
            logger.debug("[postSendNotification()] - accountId="+accountId+", gguid="+gguid+", label="+label);
        if ( (accountId == null || accountId <= 0 ) && (gguid == null || gguid.equalsIgnoreCase("")) )
            throw new NullPointerException("Neither accountId or gguid was specified, at least one of accountId or gguid is required");
        if ( label == null )
            throw new NullPointerException("The \'label\' field is required");;
        
        StringBuilder bldr = new StringBuilder();
        try
        {
            bldr.append("type=pushMessage");
            if ( accountId != null && accountId > 0 )
                bldr.append("&acc=").append(accountId);
            if ( gguid != null && !gguid.equalsIgnoreCase(""))
                bldr.append("&gguid=").append(java.net.URLEncoder.encode(gguid,"UTF-8"));
            bldr.append("&message=").append(java.net.URLEncoder.encode(label,"UTF-8"));
            if ( badge != null && badge.equalsIgnoreCase("") )
                bldr.append("&badge=").append(java.net.URLEncoder.encode(badge,"UTF-8"));
            post(bldr.toString());
        }
        catch (UnsupportedEncodingException ex)
        {
            logger.warn("=[postSendNotification()] - Error sending push notification, error="+ex.toString(),ex);
        }
    }
    
    //posts Xanboo events to eService
    public static  void post2EntityListener(long accountId, String gguid, String dguid, String catId, String label, String timestamp, boolean isDelete) {
        if(gguid==null || dguid==null ) return;

        //post now
        //  e.g. http://svcdev.digitallife.att.com/messageRelay/notify?key=[GUID]&deviceUid=[deviceID]&label=[label]&status=0&deviceClassId=[4-digit-cls]&deviceSubClassId=[2-digit-subclass]&catalogId=[CATID]&type=[discoveryEvent|deleteEvent]

        String postData=null;
        try {
            if(isDelete) {
                postData = "acc=" + accountId + "&key=" + gguid + "&dev=" + dguid + "&type=delete";
            }else {
                postData = "acc=" + accountId + "&key=" + gguid + "&dev=" + dguid + "&label=" + (label==null ? "" : java.net.URLEncoder.encode(label, "UTF-8")) + "&dateTimeOfDiscovery=" + (timestamp==null ? "" : java.net.URLEncoder.encode(timestamp, "UTF-8")) + "&value=4000&type=discoveryEvent";

                if(catId!=null && catId.length()>9) {
                    postData += "&deviceClassId=" + catId.substring(4,8) + "&deviceSubClassId=" + catId.substring(8,10) + "&vendorId=" + catId.substring(0,4);
                }
            }
        }catch(Exception e) {
            logger.warn("[post2EventListener()]: exception encoding eService post data: " + e.getMessage());
            return;
        }

        post(postData);
    }
    

    private static void post(String postData) {
        if(logger.isDebugEnabled()) {
            logger.debug("[post()]: posting to eService: " + GlobalNames.ESERVICE_URL + " QS:" + (postData.length()<500 ? postData : (postData.substring(0,200)+"..................."+postData.substring(postData.length()-200))));
        }
        
        SimpleHttpResponse res = SimpleHttpsClient.post(GlobalNames.ESERVICE_URL, postData, false);
        if(res==null || !res.isSuccess()) {
            logger.warn("[post()]: post to eService failed: " + GlobalNames.ESERVICE_URL + " QS:" + (postData.length()<500 ? postData : (postData.substring(0,200)+"..................."+postData.substring(postData.length()-200))));
        }else {
            if(logger.isDebugEnabled()) logger.debug("[post()]: successfully posted to eService: rc=" + res.getReturnCode());
        }
    }
    
}
