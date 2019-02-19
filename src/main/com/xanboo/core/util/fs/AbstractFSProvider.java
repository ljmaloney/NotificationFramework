/*
 * Copyright 2015 AT&T Digital Life
 */

package com.xanboo.core.util.fs;

import com.xanboo.core.util.GlobalNames;


/**
 * Abstract Base class for media/file server storage provider implementations.
 * A provider implementation must extend this class
 * 
 */
abstract public class AbstractFSProvider {
    
    public static final short DIR_ACCOUNT_BASE     = 0;
    public static final short DIR_ACCOUNT_ITEM     = 1;
    public static final short DIR_ACCOUNT_THUMB    = 2;
    public static final short DIR_ACCOUNT_MOBJECT  = 3;
    
    public static final short DIR_SYSTEM_LOG        = 11;
    public static final short DIR_SYSTEM_DOWNLOAD   = 12;
    public static final short DIR_SYSTEM_MOBJECT    = 13;

    
    
    /**
     * Returns the directory segment for an accountid 
     * @param accountId
     *
     * @return relative account directory path
     */
    public static String getAccountDir(long accountId) {
        
        int seg = (int) accountId/10000;  // account directories are segmented into 10K blocks
        
        if(seg > 9) {
            return seg + "/" + accountId;
        }else {
            return "0" + seg + "/" + accountId;
        }
    }
    
    
    
    // get base directory to read/store account specific data or system data
    // e.g.  <mount_root>/<mount_point>/account/<domain>/<accountDir>/
    //           where accountDir is typically "<segment>/<accountId>"
    //     OR
    //       <mount_root>/<mount_point>/system/<system folder>
    public static String getBaseDir(String mount, String domain, Long accountId, Short itemType) {

        //For Media Service Items
        if (mount != null && GlobalNames.MEDIA_SERVICE_MOUNTPOINT.indexOf(mount) >0 &&
            domain!=null && domain.length()>0 && accountId!=null) {
            StringBuffer mediaSvcBaseDir = new StringBuffer()
                    .append("/")
                    .append(domain)
                    .append("/account/").append(accountId.toString());
            return mediaSvcBaseDir.toString();
        }

        //system folders are not dynamic, jusr return initialized values
        switch(itemType) {
            case DIR_SYSTEM_LOG:
                return GlobalNames.SYSTEM_LOG_DIRECTORY;
            case DIR_SYSTEM_DOWNLOAD:
                return GlobalNames.SYSTEM_DOWNLOAD_DIRECTORY;
            case DIR_SYSTEM_MOBJECT:
                return GlobalNames.SYSTEM_MOBJECT_DIRECTORY;
        }
        
        
        //account specific folder must be dynamically formed
        StringBuffer dir = new StringBuffer().append(GlobalNames.APP_MOUNT_ROOT);
        
        
        //if mount point passed is null or for account MOBJECTs, SYSTEM_MOUNT_DIR is defaulted
        if(mount==null || itemType==DIR_ACCOUNT_MOBJECT) {
            dir.append(GlobalNames.SYSTEM_MOUNT_DIR);
            
        //item and thumbnail files are stored under the mount point passed (determined from account id or item_mount column stored in inbox table)
        }else {
            dir.append(mount);
        }
        
        
        //append common domain and account subfolders
        dir.append(GlobalNames.ACCOUNT_DIRECTORY);
        if(domain!=null && domain.length()>0) dir.append("/").append(domain);
        if(accountId!=null) dir.append("/").append(getAccountDir(accountId));
        
        //append the specific sub folder
        switch(itemType) {
            case DIR_ACCOUNT_BASE:
                break;
            case DIR_ACCOUNT_ITEM:
                dir.append(GlobalNames.ITEM_DIRECTORY);
                break;
            case DIR_ACCOUNT_THUMB:
                dir.append(GlobalNames.ITEM_THUMB_DIRECTORY);
                break;
            case DIR_ACCOUNT_MOBJECT:
                dir.append(GlobalNames.MOBJECT_DIRECTORY);
                break;
        }
        
        return dir.toString();
    }
    
}
