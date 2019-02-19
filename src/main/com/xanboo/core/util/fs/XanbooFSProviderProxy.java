/*
 * Copyright 2015 AT&T Digital Life
 */

package com.xanboo.core.util.fs;

import com.xanboo.core.util.*;
import com.xanboo.core.security.*;

import com.xanboo.core.model.XanbooItem;
import java.util.Calendar;
import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * A Singleton to proxy all FS operations
*/
public class XanbooFSProviderProxy {
    private static XanbooFSProviderProxy instance = null;
    public static final String ENCRYPTED_FILE_SUFFIX = ".enc";
    
    private XanbooFSProviderProxy() {}
    
    /**
     * Singleton instance creation/returning call
     *
     * @return an instance of the instantiated XanbooFSInterface class 
     */
    public static XanbooFSProviderProxy getInstance() {
        if ( instance == null ) {
            instance = new XanbooFSProviderProxy();
        }
        return instance;
    }

    
    
    public boolean checkDir(String providerId, String dirPath, boolean create) {
        try {
            XanbooFSInterface xfs = FSProviderCache.getInstance().getProviderInstanceById(providerId);
            return xfs.checkDir(dirPath, create);
            
        }catch(XanbooException xe) {
        }
        return false;
    }

    
    public boolean mkDir(String providerId, String dirPath) {
        try {
            XanbooFSInterface xfs = FSProviderCache.getInstance().getProviderInstanceById(providerId);
            return xfs.mkDir(dirPath);
            
        }catch(XanbooException xe) {
        }
        return false;
    }

    
    public boolean rmDir(String providerId, String dirPath, boolean recursive) {
        try {
            XanbooFSInterface xfs = FSProviderCache.getInstance().getProviderInstanceById(providerId);
            return xfs.rmDir(dirPath, recursive);
            
        }catch(XanbooException xe) {
        }
        return false;
    }
    
    public String[] listDir(String providerId, String dirPath) {
        try {
            XanbooFSInterface xfs = FSProviderCache.getInstance().getProviderInstanceById(providerId);
            return xfs.listDir(dirPath);
            
        }catch(XanbooException xe) {
        }
        return null;
        
    }   
    
    
    
    public boolean storeFile(String providerId, File srcFile, String destFilePath, boolean createDirs, boolean encryptionOverride) throws XanbooException {
        try {
            byte[] bytes = DefaultNFSProvider.getFileBytes(srcFile);
            return storeFile(providerId, bytes, destFilePath, createDirs, encryptionOverride);
        }catch(XanbooException xe) {
            throw xe;
        }catch(Exception e) {
            throw new XanbooException(10025, "Failed to store item file. EXCEPTION reading file bytes:" + e.getMessage());
        }
    } 
    
    
    public boolean storeFile(String providerId, byte[] bytes, String destFilePath, boolean createDirs, boolean encryptionOverride) throws XanbooException {
        try {
            XanbooFSInterface xfs = FSProviderCache.getInstance().getProviderInstanceById(providerId);

            boolean encryptionEnabled = false;
            if(encryptionOverride && GlobalNames.ITEM_ENCRYPTION_ENABLED && XanbooEncryptionProviderFactory.isEncryptionProviderAvailable()) {
                encryptionEnabled = true;
            }
            
            
            if(encryptionEnabled) {
                XanbooEncryptionProvider encryptor = XanbooEncryptionProviderFactory.getProvider();
                if(encryptor==null) {
                    throw new XanbooException(10020, "Failed to encrypt item file. No Provider found.");
                }

                try {
                    bytes = encryptor.encrypt(bytes);
                }catch(Exception e) {
                    throw new XanbooException(10022, "Failed to encrypt item file. EXCEPTION while encrypting: " + e.getMessage());
                }
                
                destFilePath += ENCRYPTED_FILE_SUFFIX;
            }
            
            if(!xfs.storeFile(bytes, destFilePath, createDirs)) {
                throw new XanbooException(10027, "Failed to store item file.");
            }

            //return indicates whether file was stored encrypted or not
            if(encryptionEnabled) {
                return true;
            }else {
                return false;
            }
            
        }catch(XanbooException xe) {
            throw xe;
        }catch(Exception e) {
            throw new XanbooException(20018, e);
        }
    }

    
    public boolean removeFile(String providerId, String filePath) {
        try {
            XanbooFSInterface xfs = FSProviderCache.getInstance().getProviderInstanceById(providerId);
            return xfs.removeFile(filePath);
            
        }catch(XanbooException xe) {

        }
        return false;
        
    }

    /**
     *
     * @param providerId
     * @param filePaths a JSON array of file paths
     * @return String of JSON arary containg file paths that were not removed
     */
    public List<String> removeFiles(String providerId, List<String> filePaths) {
        try {
            XanbooFSInterface xfs = FSProviderCache.getInstance().getProviderInstanceById(providerId);
            return xfs.removeFiles(filePaths);

        }catch(XanbooException xe) {

        }
        return null;

    }
    
    public byte[] getFileBytes(String providerId, String filePath) throws XanbooException {
        try {
            XanbooFSInterface xfs = FSProviderCache.getInstance().getProviderInstanceById(providerId);
            byte[] bytes = xfs.getFileBytes(filePath);

            //if encryption enabled, and filename ends with ".enc", decrypt
            if(filePath.trim().endsWith(ENCRYPTED_FILE_SUFFIX)) {
                XanbooEncryptionProvider encryptor = XanbooEncryptionProviderFactory.getProvider();
                if(encryptor==null) {
                    throw new XanbooException(10020, "Failed to decrypt item file. No Provider found.");
                }

                try {
                    bytes = encryptor.decrypt(bytes);
                }catch(Exception e) {
                    throw new XanbooException(10023, "Failed to decrypt item file. EXCEPTION while decrypting: " + e.getMessage());
                }
            }
            
            return bytes;
            
        }catch(XanbooException xe) {
            throw xe;
        }catch(Exception e) {
            throw new XanbooException(20018, e);
        }
    }
    
    public InputStream getFileAsStream(String providerId, String filePath) throws XanbooException {
        try {
            XanbooFSInterface xfs = FSProviderCache.getInstance().getProviderInstanceById(providerId);
           return xfs.getFileAsStream(filePath);
        }catch(XanbooException xe) {
            throw xe;
        }catch(Exception e) {
            throw new XanbooException(20018, e);
        }
    }
    
    //-------------------------- static methods ----------------------------------------------
    public static XanbooItem generateItemFileName(String attachmentFileName, String attachmentThumbFileName) {
        String newName = null;
        Calendar cal = Calendar.getInstance();
        
        int cYear  = cal.get(Calendar.YEAR);
        String cMonth = (((cal.get(Calendar.MONTH) + 1) < 10) ? "0" : "") + (cal.get(Calendar.MONTH)+1);
        String cDay   = ((cal.get(Calendar.DATE) < 10) ? "0" : "") + cal.get(Calendar.DATE);
        
        String subDir = cYear + "/" + cMonth + cDay;

        String uniqueFileName = XanbooUtil.createUniqueFileName(cal);
        if(attachmentFileName.lastIndexOf(".") != -1) {
            newName = "item_" + uniqueFileName + attachmentFileName.substring(attachmentFileName.lastIndexOf("."));
        }else {
            newName = "item_" + uniqueFileName ;
        }
        
        XanbooItem item = new XanbooItem();
        item.setItemDirectory(subDir);
        item.setItemFilename(newName);

        if (attachmentThumbFileName != null) {
            if (attachmentThumbFileName.lastIndexOf(".") != -1)
                item.setThumbFilename("thumb_" + uniqueFileName + attachmentThumbFileName.substring(attachmentThumbFileName.lastIndexOf(".")));
            else 
                item.setThumbFilename("thumb_" + uniqueFileName);
        }
        
        return item;
    }

    public static XanbooItem generateItemFileName(String attachmentFileName) {
        return generateItemFileName(attachmentFileName, null);
    }
    
    public static XanbooItem generateMobjectFileName(String gwyGUID, String deviceGUID, String mobjectId) {
        String newName = deviceGUID + "." + mobjectId +".0";

        XanbooItem item = new XanbooItem();
        item.setItemDirectory(gwyGUID);
        item.setItemFilename(newName);
        
        return item;
    }
    
    
    public static String getDestinationFolder(String mount, String domainId, long accountId, boolean isItemAttachment) {
        String destDir = null;
        
        if(isItemAttachment) {
            // form the directory path to save the item attachment: '<baseAccountDir>/item/YYYY/MMDD/item_xxxxxxxx'
            destDir = AbstractFSProvider.getBaseDir(mount, domainId, accountId, AbstractFSProvider.DIR_ACCOUNT_ITEM);
            
        }else {
            // form the directory path to save the mobject attachment: '<baseAccountDir>/mobject/gguid/dguid.oid.0'
            destDir = AbstractFSProvider.getBaseDir(mount, domainId, accountId, AbstractFSProvider.DIR_ACCOUNT_MOBJECT);
        }
        
        return destDir;
    }    
    
        
}
