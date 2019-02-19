/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/AbstractSDKManagerEJB.java,v $
 * $Id: AbstractSDKManagerEJB.java,v 1.1 2011/07/01 16:11:31 levent Exp $
 *
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Calendar;

import com.xanboo.core.model.XanbooItem;
import com.xanboo.core.util.GlobalNames;
import com.xanboo.core.util.ImageProcessor;
import com.xanboo.core.util.XanbooException;
import com.xanboo.core.util.XanbooUtil;
import com.xanboo.core.util.fs.AbstractFSProvider;
import com.xanboo.core.util.fs.*;

/**
 * <p>
* Abstract Session Bean class for SDK Inbox and Folder Manager EJB implementations
  * </p>
 */

public abstract class AbstractSDKArchiveManagerEJB extends AbstractSDKManagerEJB {


    protected XanbooItem saveItemBytes(String domainId, long accountId, XanbooItem item) throws XanbooException{
        
        boolean itemEncrypted = false;
        
        //determine/set item filename and folder path to store, base on passed filename
        XanbooItem item2 = null;
        if(item.getName()!=null && item.getName().indexOf(".")>0)
            item2 = XanbooFSProviderProxy.generateItemFileName(item.getName());
        else
            item2 = XanbooFSProviderProxy.generateItemFileName(item.getItemFilename());
        
        item.setItemDirectory(item2.getItemDirectory());
        item.setItemFilename(item2.getItemFilename());
        
        //obtain the mount point to be used for the account and its handler FS provider id (prefix)
        FSMountPoint mp = FSProviderCache.getInstance().getMountPointForAccount(accountId);
        item.setMount( mp.getMountPath() );
        item.setMountPrefix(mp.getProviderId());
        
        item.setItemSize(item.getItemBytes().length);     
        item.setAccountId(accountId);
        item.setDomain(domainId);
        item.setThumbFilename("");
        item.setThumbSize(0);
        
        String destFilePath = XanbooFSProviderProxy.getDestinationFolder(item.getMountDir(), domainId, accountId, true) + "/" + item.getItemDirectory() + "/" + item.getItemFilename();

        try {
            itemEncrypted = XanbooFSProviderProxy.getInstance().storeFile(item.getMountPrefix(), item.getItemBytes(), destFilePath, true, true);

            // create a thumbnail, if necessary
            if(GlobalNames.ITEM_THUMB_ENABLED && item.getItemType()!=null && GlobalNames.ITEM_IMAGE_TYPES.indexOf(item.getItemType()) != -1) {

                File sourceFile = null;

                try {

                    //if a jpeg file, use internal thumbnail creator
                    if(item.getItemType().indexOf("jp")!=-1) {
                        // check thumb dir existance, create if necessary
                        String thumbDir = AbstractFSProvider.getBaseDir(item.getMountDir(), domainId, accountId, AbstractFSProvider.DIR_ACCOUNT_THUMB) + "/" + item.getItemDirectory();
                        XanbooFSProviderProxy.getInstance().checkDir(item.getMountPrefix(), thumbDir, true);

                        //using tmp/upload direct?ry to create the thumb. ANY ISSUES????????
                        int thumbSize = ImageProcessor.createThumbnail(item.getItemBytes(),
                                                       GlobalNames.ITEM_UPLOAD_DIRECTORY + "/" + item.getItemFilename() + ".thumb",
                                                       GlobalNames.ITEM_THUMB_WIDTH,
                                                       GlobalNames.ITEM_THUMB_HEIGHT,
                                                       90,    // 90% default quality
                                                       true);

                        if(thumbSize>-1) {
                            sourceFile = new File(GlobalNames.ITEM_UPLOAD_DIRECTORY + "/" + item.getItemFilename() + ".thumb");
                            destFilePath = thumbDir + "/" + item.getItemFilename();

                            
                            if(XanbooFSProviderProxy.getInstance().storeFile(item.getMountPrefix(), sourceFile, destFilePath, true, true)) {
                                //if encrypted, append encryption suffix
                                item.setThumbFilename(item.getItemFilename() + XanbooFSProviderProxy.ENCRYPTED_FILE_SUFFIX);
                            }else {
                                item.setThumbFilename(item.getItemFilename());
                            }
                            item.setThumbSize(thumbSize);

                        }
                    }

                }catch(Exception e) {  //ignore thumbnail errors
                   logger.error("[saveItemBytes()]: couldn't create thumbnail." + e.getMessage());
                }finally {
                    //remove tmp thumb file
                    if(sourceFile != null) sourceFile.delete();
                }

            }//end thumb creation

        }catch(Exception e) {
            if(logger.isDebugEnabled()) {
                logger.debug("[saveItemBytes()]: Exception: ", e);
            }else {
                logger.error("[saveItemBytes()]: " + e.getMessage());
            }

            throw new XanbooException(22023, "Failed to add item. Exception: " + e.getMessage());
        }

        //if encrypted, append encryption suffix - only items
        if(itemEncrypted) { 
            item.setItemFilename(item.getItemFilename() + XanbooFSProviderProxy.ENCRYPTED_FILE_SUFFIX);
        }
        
        return item;
    }    
        
}
