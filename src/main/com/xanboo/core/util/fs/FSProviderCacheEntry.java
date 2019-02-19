package com.xanboo.core.util.fs;

/**
 *
 */
class FSProviderCacheEntry {
    String id;
    String className;
    String props;
    XanbooFSInterface instance;

    public FSProviderCacheEntry(String providerWithIdPrefix, String props) {
        if(providerWithIdPrefix!=null && providerWithIdPrefix.length()>2 && providerWithIdPrefix.charAt(1)==FSMountPoint.MOUNT_DELIMETER) {     //char 2 is the delimeter, a provider id/index is specified/found --> return first digit as provider id
            this.id = providerWithIdPrefix.substring(0, 1);
            this.className = providerWithIdPrefix.substring(2);
        }else {
            this.id = FSProviderCache.DEFAULT_PROVIDER_ID; //default
            this.className = providerWithIdPrefix;
        }
        
        this.props = props;
    }
    
    public FSProviderCacheEntry(String id, String className, String props) {
        this.id = id;
        this.className = className;
        this.props = props;
    }

    public String getId() {
        return this.id;
    }

    public String getClassName() {
        return this.className;
    }

    public String getProps() {
        return this.props;
    }
    
    public XanbooFSInterface getInstance() {
        return this.instance;
    }

    public void setInstance(XanbooFSInterface inst) {
        this.instance = inst;
    }
    
    public String toString() {
        return "id=" + id + ", class=" + className + ", props=" + props + ", inst=" + (instance==null ? "null" : "OK");
    }
    
}
