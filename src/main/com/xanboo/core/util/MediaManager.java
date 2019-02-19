package com.xanboo.core.util;

/**
 *
 * @author ra352k
 */

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import com.xanboo.core.model.XanbooBinaryContent;
import com.xanboo.core.model.XanbooItem;
import com.xanboo.core.sdk.inbox.InboxManager;
import com.xanboo.core.sdk.folder.FolderManager;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.fs.AbstractFSProvider;
import com.xanboo.core.util.fs.FSProviderCache;
import com.xanboo.core.util.fs.XanbooFSProviderProxy;

public class MediaManager {

    private InboxManager inboxManager=null;
    private FolderManager folderManager=null;
    private EjbProxy proxy = null;
    private static Properties props=null;

    static {
        props = new Properties();

        try {
            String propertiesFileLocation = System.getProperty("mediamanager.config");
            FileInputStream in = new FileInputStream(propertiesFileLocation);
            props.load(in);
            in.close();
        } catch (Exception e) {            
        }
    }
    
    public MediaManager() throws XanbooException {
        try {
            proxy = new EjbProxy((String) props.get("jndi.context.factory"), (String) props.get("jndi.provider.url"));
            inboxManager = (InboxManager) proxy.getObj(GlobalNames.EJB_INBOX_MANAGER);            

            if (inboxManager == null) {
                throw new Exception("InboxManager initialization error");
            }

            folderManager= (FolderManager) proxy.getObj(GlobalNames.EJB_FOLDER_MANAGER);
            
            if (folderManager == null) {
                throw new Exception("FolderManager initialization error");
            }
            
            FSProviderCache fsProviderCache = FSProviderCache.getInstance();
            fsProviderCache.initialize((String) props.get("fs.provider.csv"));
        } catch (Exception e) {
            throw new XanbooException(1001, e.getMessage());
        }
    }

    /**
     * Gets the binary content of a XanbooItem.
     * getItemBinary will first retreive the XanbooItem from the InboxManager.  If the item's
     * mount point is the media service mount point, retrieve the binary content from
     * the Media Service using XanbooFSProviderProxy, otherwise retrieve it from the InboxManager.
     * @param xap
     * @param inboxItemId
     * @param isThumb
     * @return InputStream of binary content for the specified inboxItemId
     * @throws Exception
     */
    public InputStream getItemBinary(XanbooPrincipal xap, long inboxItemId, boolean isThumb, boolean isFolderItem) throws Exception {
        XanbooItem item = null;
        
        if (isFolderItem) 
            item = folderManager.getItem(xap, inboxItemId);
        else
            item = inboxManager.getItem(xap, inboxItemId);

        String itemMount = item.getMount();

        if (itemMount != null && itemMount.equals(GlobalNames.MEDIA_SERVICE_MOUNTPOINT)) {   
            String itemPath = AbstractFSProvider.getBaseDir(item.getMountDir(), xap.getDomain(),
                    item.getAccountId(),
                    (isThumb ? AbstractFSProvider.DIR_ACCOUNT_THUMB : AbstractFSProvider.DIR_ACCOUNT_ITEM)) + "/" +
                    item.getItemDirectory() +
                    (item.getItemDirectory().endsWith("/") ? "" : "/") +
                    (isThumb ? item.getThumbFilename() : item.getItemFilename());

            return XanbooFSProviderProxy.getInstance().getFileAsStream(item.getMountPrefix(), itemPath);
        } else {
            XanbooBinaryContent xbc = inboxManager.getItemBinary(xap, itemMount,
                    item.getItemDirectory(), (isThumb ? item.getThumbFilename() : item.getItemFilename()), isThumb);
            
            return new ByteArrayInputStream(xbc.getBinaryContent());
        }
    }
}
