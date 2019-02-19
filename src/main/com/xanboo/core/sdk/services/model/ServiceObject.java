/**
 * Created on Apr 17, 2014
 */
package com.xanboo.core.sdk.services.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.xanboo.core.model.XanbooMobject;

/** Class to represent external parent service object entity */
public class ServiceObject implements Serializable {
    private static final long serialVersionUID = 5571491080326331387L;

    private String extObjectId;       // External id of service object generating the event.
    private String parentExtObjectId; // External id of service object's parent.
    private String deviceGuid;        // device guid of the device generating the event.
    private String parentDeviceGuid;  // device guid of the device's parent. 
    private String name;  
    private String parentName;
    private String catalogId;
    private List<XanbooMobject> mobjects;
    private List<ServiceObject> childObjects;
    
    private String hwSerial;	// HW Serial number of the device.
    private String swVersion;   // Software version of the device.
    private String hwVersion;   // Hardware version of the device.
    private String hwModel;     // Hardware Model of the device.
    private String sourceId; 	// source id of the device 
            
    public static final int SERVICE_OBJECT_ALREADY_IMPORTED = 0;
    public static final int SERVICE_OBJECT_IMPORTED_NOW = 1;
    public static final int SERVICE_OBJECT_DELETED = 2;
    private int serviceObjectState=-1;
    
    public ServiceObject() {
        super();
    }

    public ServiceObject(String deviceGUID) {
        super();
        this.deviceGuid = deviceGUID;
    }
    
    public ServiceObject(String id, String name) {
        super();
        this.extObjectId = id;
        this.name = name;
    }

    public ServiceObject(String id, String name, String parentId) {
        super();
        this.extObjectId = id;
        this.name = name;
        this.parentExtObjectId = parentId;
    }


    /**
     * @return the external object identifier for the service object
     */
    public String getId() {
        return extObjectId;
    }
    /**
     * @param id the external object identifier for the service object to set
     */
    public void setId(String id) {
        this.extObjectId = id;
    }
    /**
     * @return the name/label for the service object
     */
    public String getName() {
        return name;
    }
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
	 * @return the parentName
	 */
	public String getParentName() {
		return parentName;
	}

	/**
	 * @param parentName the parentName to set
	 */
	public void setParentName(String parentName) {
		this.parentName = parentName;
	}

	/**
     * @return the external parent identifier for the service object, if applicable
     */
    public String getParentId() {
        return parentExtObjectId;
    }
    /**
     * @param parentId external parent identifier for the service object to set, if applicable
     */
    public void setParentId(String parentId) {
        this.parentExtObjectId = parentId;
    }
    /**
     * @return the DL catalogId for the service object
     */
    public String getCatalogId() {
        return catalogId;
    }
    /**
     * @param catalogId the catalogId to set
     */
    public void setCatalogId(String catalogId) {
        this.catalogId = catalogId;
    }
    /**
     * @return the DL device guid
     */
    public String getDeviceGuid() {
        return deviceGuid;
    }
    /**
     * @param id the DL device guid to set
     */
    public void setDeviceGuid(String dguid) {
        this.deviceGuid = dguid;
    }
    /**
     * @return the parent DL device guid for the service object
     */
    public String getParentDeviceGuid() {
        return parentDeviceGuid;
    }
    /**
     * @param id the parent DL device guid for the service object to set
     */
    public void setParentDeviceGuid(String dguid) {
        this.parentDeviceGuid = dguid;
    }
    /**
     * @return the import status of the service Object if it is already imported,deleted
     */
    public int getServiceObjectImportState() {
        return serviceObjectState;
    }
    /**
     * @param id import status of the service Object to set
     */
    public void setServiceObjectImportState(int serviceObjectState) {
        this.serviceObjectState = serviceObjectState;
    }

    /**
     * Validates if ServiceObject instance is already registered to the External Service or not, by checking necessary 
     * external service attributes (e.g. extObjectId)
     */
    public boolean isRegisteredToService() {
        if(extObjectId==null || extObjectId.length()==0) return false;
        //if(catalogId==null || catalogId.length()==0) return false;
        return true;
    }

    /**
     * Validates if ServiceObject instance is already registered to DL or not, by checking necessary 
     * DL device attributes (e.g. deviceGUID)
     */
    public boolean isRegisteredToDL() {
        if(deviceGuid==null || deviceGuid.length()==0) return false;
        //if(catalogId==null || catalogId.length()==0) return false;
        return true;
    }
    public boolean isDeletedFromExternalService() {
        if(getServiceObjectImportState() == SERVICE_OBJECT_DELETED) return true;
        return false;
    }
    public boolean isAlreadyImported() {
    	if(getServiceObjectImportState() == SERVICE_OBJECT_ALREADY_IMPORTED) return true;
        //if(catalogId==null || catalogId.length()==0) return false;
        return false;
    }

	public boolean isImportedNow() {
    	if(getServiceObjectImportState()==this.SERVICE_OBJECT_IMPORTED_NOW) return true;
        //if(catalogId==null || catalogId.length()==0) return false;
        return false;
    }
    
    /**
     * Returns mobject attributes for the Service Object initialization.
     * @return the List of XanbooMobjects for Service Object initialization
     */
    public List<XanbooMobject> getInitialMObjects() {
        return this.mobjects;
    }

    /**
     * Sets mobject attributes for the Service Object on creation/initialization. These attributes are typically set
     * during Service OBject/Device creation time to create the object at the external network with given attribute values.
     * @param a List of XanbooMobject objects

     */
    public void setInitialMObjects(List<XanbooMobject> mobjects) {
        this.mobjects = mobjects;
    }
    
    /** Helper method for adding mobject.
     * @return none
     */
    public void addMobject(String oid, Object value) {
    	if(mobjects == null) {
    		mobjects = new ArrayList<XanbooMobject>();
    	}
    	mobjects.add(new XanbooMobject(oid, value));
    }
    
    /** Helper method for adding mobject.
     * @return none
     */
    public void addMobject(String oid, Object value, boolean binaryOid) {
    	if(mobjects == null) {
    		mobjects = new ArrayList<XanbooMobject>();
    	}
    	mobjects.add(new XanbooMobject(oid, value, false, binaryOid));
    }  
    
    /** Helper method for adding child Devices.
     * @return non
     */
    public void addChildObject(ServiceObject device) {
    	if(childObjects == null) {
    		childObjects = new ArrayList<ServiceObject>();
    	}
    	childObjects.add(device);
    }

	/**
	 * @return the childObjects
	 */
	public List<ServiceObject> getChildObjects() {
		return childObjects;
	}

	/**
	 * @param childObjects the childObjects to set
	 */
	public void setChildObjects(List<ServiceObject> childObjects) {
		this.childObjects = childObjects;
	}

	/**
	 * @return the hwSerial
	 */
	public String getHwSerial() {
		return hwSerial;
	}

	/**
	 * @param hwSerial the hwSerial to set
	 */
	public void setHwSerial(String hwSerial) {
		this.hwSerial = hwSerial;
	}

	/**
	 * @return the swVersion
	 */
	public String getSwVersion() {
		return swVersion;
	}

	/**
	 * @param swVersion the swVersion to set
	 */
	public void setSwVersion(String swVersion) {
		this.swVersion = swVersion;
	}

	/**
	 * @return the hwVersion
	 */
	public String getHwVersion() {
		return hwVersion;
	}

	/**
	 * @param hwVersion the hwVersion to set
	 */
	public void setHwVersion(String hwVersion) {
		this.hwVersion = hwVersion;
	}

	/**
	 * @return the hwModel
	 */
	public String getHwModel() {
		return hwModel;
	}

	/**
	 * @param hwModel the hwModel to set
	 */
	public void setHwModel(String hwModel) {
		this.hwModel = hwModel;
	}
	
	public XanbooMobject getMobject(String oid, String value) {
		XanbooMobject mObject = null;
		if(mobjects!=null && mobjects.size() > 0) {
			for(XanbooMobject mobject : mobjects) {
			   if(oid!=null && oid.equals(mobject.getOid())) {
				   mObject = mobject;
				   if((value == null && mobject.getStringValue().equals("null"))
						   || (value != null && value.equalsIgnoreCase(mobject.getStringValue()))) {
					   mObject = null;
					   break;
				   }
			   }
			}
		}
		return mObject;
    }
	
	// getter and setter methods of the source id
    public String getSourceId() {		return sourceId;	}
	public void setSourceId(String sourceId) {		this.sourceId = sourceId;	}

	
	public boolean isLabelUpdateRequested() {
		if(mobjects!=null && mobjects.size() > 0) {
			for(XanbooMobject mOjb : mobjects) {
				if(mOjb!=null && "0".equalsIgnoreCase(mOjb.getOid())) {
					return true;
				}
			}
		}
		return false;
	}


	@Override
    public String toString()
    {
        return "ServiceObject{" + "extObjectId=" + extObjectId + ", parentExtObjectId=" + parentExtObjectId + ", deviceGuid=" + deviceGuid + ", parentDeviceGuid=" + parentDeviceGuid + ", name=" + name + ", parentName=" + parentName + ", catalogId=" + catalogId + ", mobjects=" + mobjects + ", childObjects=" + childObjects + ", hwSerial=" + hwSerial + ", swVersion=" + swVersion + ", hwVersion=" + hwVersion + ", hwModel=" + hwModel + ", serviceObjectState=" + serviceObjectState + ", sourceId=" + sourceId +'}';
    }
}
