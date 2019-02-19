package com.xanboo.core.util.fs;

/**
 *
 */
public class FSMountPoint {
    String providerId;
    String mountPath;
    
    public static final char MOUNT_DELIMETER = '@';

    public FSMountPoint(String mountPathWithPrefix) {
        if(mountPathWithPrefix!=null && mountPathWithPrefix.length()>2 && mountPathWithPrefix.charAt(1)==MOUNT_DELIMETER) {     //if char 2 is thedelimeter, a provider id/index is specified/found --> return first digit as provider id
            this.providerId = mountPathWithPrefix.substring(0, 1);
            this.mountPath = mountPathWithPrefix.substring(2);
        }else {
            this.providerId = FSProviderCache.DEFAULT_PROVIDER_ID; //default
            this.mountPath = mountPathWithPrefix;
        }
    }
    
    public FSMountPoint(String providerId, String mountPath) {
        this.providerId = providerId;
        this.mountPath = mountPath;
    }

    public String getProviderId() {
        return this.providerId;
    }

    public String getMountPath() {
        return this.mountPath;
    }
    
    public String toString() {
        return "id=" + providerId + ", path=" + mountPath;
    }
       
}
