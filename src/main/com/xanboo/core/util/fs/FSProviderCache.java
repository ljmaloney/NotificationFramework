package com.xanboo.core.util.fs;


import java.util.ArrayList;
import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;
import com.xanboo.core.util.XanbooException;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache of FS provider class defs and instances
 * 
 */
public class FSProviderCache  {
    static final String DEFAULT_PROVIDER_ID = "0";
    
    private static FSProviderCache instance;
    
    private static Logger logger = LoggerFactory.getLogger(FSProviderCache.class.getName());
    private static ConcurrentHashMap<String, FSProviderCacheEntry> providerCache = new ConcurrentHashMap();     //key: provide id
    
    private static ConcurrentHashMap<String, FSMountPoint> mountPointCache = new ConcurrentHashMap();   //key:mount point path with prefix 
    private static ArrayList<FSMountPoint> mountPointPool = new ArrayList();   //List of mount point pool

    private static ConcurrentHashMap<String, FSMountPoint> mountPointCacheRO = new ConcurrentHashMap();   //key:mount point path, value=mount point object 
    
            
    private static Boolean isInitialized = false;
    

    private FSProviderCache() {}
    
    /**
     * Singleton instance creation/returning call
     *
     * @return an instance of the instantiated XanbooFSInterface class 
     */
    public static FSProviderCache getInstance() {
        if ( instance == null ) {
            instance = new FSProviderCache();
        }
        return instance;
    }
    
    
    public void initialize(String providerCSV, String mountPointCSV, String readOnlyMountPointCSV) {
        if(logger.isDebugEnabled()) {
            logger.debug("[initialize()]:");
        }

        if(!isInitialized) {
            synchronized(isInitialized) {
                if(!isInitialized) {
                    
                    // parse a CSV list of provided strings separated by , space or ;  and populate provider cache
                    // provider CSV format:    <1-digit-id>@<classname>,<1-digit-id>@<classname>.....
                    StringTokenizer st = new StringTokenizer(providerCSV,", ;");
                    while (st.hasMoreTokens()) {
                        String token = st.nextToken();
                        token = token.trim();
                        if(token.length()>0) {
                            FSProviderCacheEntry entry = new FSProviderCacheEntry(token, null);
                            providerCache.put(entry.getId(), entry);
                        }else {
                            logger.warn("[initialize()]:" + "Possible invalid token in FS provider CSV" + providerCSV);
                        }
                    }          
                    
                    
                    // parse a CSV list of mount point strings separated by , space or ;  and populate mount point cache
                    // mount point CSV format:    <1-digit-id>@<mount point path>,<1-digit-id>@<mount point path>.....
                    st = new StringTokenizer(mountPointCSV,", ;");
                    while (st.hasMoreTokens()) {
                        String token = st.nextToken();
                        token = token.trim();
                        if(token.length()>0) {
                            FSMountPoint  mpoint = new FSMountPoint(token);
                            mountPointPool.add(mpoint);
                        }else {
                            logger.warn("[initialize()]:" + "Possible invalid token in mount point pool CSV" + providerCSV);
                        }
                    }          

                    // parse a CSV list of read only mount point strings separated by , space or ;  and populate mount point read/only cache
                    // mount point CSV format:    <1-digit-id>@<mount point path>,<1-digit-id>@<mount point path>.....
                    st = new StringTokenizer(readOnlyMountPointCSV,", ;");
                    while (st.hasMoreTokens()) {
                        String token = st.nextToken();
                        token = token.trim();
                        if(token.length()>0) {
                            FSMountPoint  mpoint = new FSMountPoint(token);
                            mountPointCacheRO.put(token, mpoint);
                        }else {
                            logger.warn("[initialize()]:" + "Possible invalid token in mount point pool CSV" + providerCSV);
                        }
                    }          
                    
                    isInitialized = true;
                }
            }
        }
    }
    
    public void initialize(String providerCSV) {
        if(!isInitialized) {
            synchronized(isInitialized) {
                if(!isInitialized) {
                    // parse a CSV list of provided strings separated by , space or ;  and populate provider cache
                    // provider CSV format:    <1-digit-id>@<classname>,<1-digit-id>@<classname>.....
                    StringTokenizer st = new StringTokenizer(providerCSV,", ;");
                    while (st.hasMoreTokens()) {
                        String token = st.nextToken();
                        token = token.trim();
                        if(token.length()>0) {
                            FSProviderCacheEntry entry = new FSProviderCacheEntry(token, null);
                            providerCache.put(entry.getId(), entry);
                        }else {
                            logger.warn("[initialize()]:" + "Possible invalid token in FS provider CSV" + providerCSV);
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns an FS provider instance for a give provider id (index/prefix)
     * If id passed is null, default provider (NFS) will be returned
     */
    public XanbooFSInterface getProviderInstanceById(String id) throws XanbooException {
        if(id==null || id.trim().length()==0) id=DEFAULT_PROVIDER_ID;
        
        FSProviderCacheEntry provider = providerCache.get(id);
        if(provider==null) {
            throw new XanbooException(20016, "Failed to instantiate a FS Provider with id:" + id + ". Not found in cache");
        }

        if(provider.getInstance() == null) {
            try {
                XanbooFSInterface fs = (XanbooFSInterface) Class.forName(provider.getClassName()).newInstance();
                fs.initialize(provider.getProps());

                synchronized(provider) {
                    provider.setInstance(fs);
                }
            }catch (Exception se) {
                throw new XanbooException(20016, "Failed to instantiate a FS Provider with id:" + id + ". EXCEPTION:" + se.getMessage());
            }            
        }
        
        return provider.getInstance();
    }
    

    /**
     * Returns an FS provider instance for a given mount point path
     */
    public XanbooFSInterface getProviderInstanceByPath(String mountPointPath) throws XanbooException {
        return getProviderInstanceById( getMountPoint(mountPointPath).getProviderId() );
    }
    
    
    /**
     * Returns the mount point from cache associated with a mount path given
     * Mount points are specified as "<1-digit id/prefix>@<path>". 
     *      E.g.   0@/opt/app/xapp   or 1@/opt/app
     * 
     * @return an FSMountPoint object matching the mount path specified in cache
     */
    public FSMountPoint getMountPoint(String mountPointPath) throws XanbooException {
        
        FSMountPoint mpoint = mountPointCache.get(mountPointPath);
        
        if(mpoint==null) {
            mpoint = new FSMountPoint(mountPointPath);
            if(providerCache.get( mpoint.getProviderId() )== null) {    //invalid provider id;
                throw new XanbooException(20017, "Invalid provider id in mount point path:" + mpoint.toString());
            } 
            synchronized(mountPointCache) {
                mountPointCache.put(mountPointPath, mpoint);
            }
        }
        
        return mpoint;
    }    
    

    /**
     * Returns mount point assigned to a given account id to write
     * @param accountId
     * @return FSMountPoint object
     * @throws XanbooException 
     */
    public FSMountPoint getMountPointForAccount(long accountId) {
        
        //hash account id and determine corresponding mount point index based on pool size
        String strAccountId = accountId+"";
        int idx = (int) (strAccountId.hashCode() % mountPointPool.size());
        if(idx <0) idx = -1 * idx; // change
        return mountPointPool.get(idx);
    }    
    
    
    /**
     * Returns the read/only mount point from cache associated with a mount path given
     * Mount points are specified as "<1-digit id/prefix>@<path>". 
     *      E.g.   0@/opt/app/xapp   or 1@/opt/app
     * 
     * @return an FSMountPoint object matching the mount path specified in read/only cache
     */
    public FSMountPoint getReadOnlyMountPoint(String mountPointPath) throws XanbooException {
        if(mountPointPath==null) return null;
        
        FSMountPoint mpoint = mountPointCacheRO.get(mountPointPath);
        return mpoint;
    }    
    
    
    public String dump() {
        StringBuffer buf = new StringBuffer();
        
        buf.append("\n\nPROVIDERS:\n").append(providerCache.toString());
        buf.append("\n\nMOUNT POINTS:\n").append(mountPointPool.toString());
        
        return buf.toString();
    }
    
}
