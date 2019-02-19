/**
 * Created on Apr 15, 2014
 */
package com.xanboo.core.sdk.services.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.xanboo.core.util.XanbooException;

/**
 * 
 * @author mr117n
 */
public class ServiceSubscription implements Serializable {

    private static final long serialVersionUID = -6961998640024519030L;
	
    private String serviceId;
    private String sgGuid;
    private String gguid;
    private String targetGguid;
    private long  accountId;
    //By default enable service subscription.
    private int statusId=1; 
    private String extAccountId;
    private String userName;
    private String password;
    private String authCode;
    private String accessToken;
    private boolean isVerified;
    private String serviceName;
    //catalog id
    private String sgwyCatalogId;
    private List<ServiceObject> serviceObjects;
    private String refershToken;
    private String tokenExpiration;
    private String redirectUrl;
    
    private String deviceGuid;
    private String domainId;
    private long userId;
    private String subsId;
    
    
    public ServiceSubscription() {
    	super();
    }

    public ServiceSubscription(String serviceId, long accountId) {
    	this.serviceId = serviceId;
    	this.accountId = accountId;
    }
	
	/**
	 * @return the sgwyCatalogId
	 */
	public String getSgwyCatalogId() {
		return sgwyCatalogId;
	}
	/**
	 * @param sgwyCatalogId the sgwyCatalogId to set
	 */
	public void setSgwyCatalogId(String sgwyCatalogId) {
		this.sgwyCatalogId = sgwyCatalogId;
	}
	/**
	 * @return the serviceId
	 */
	public String getServiceId() {
		return serviceId;
	}
	/**
	 * @param serviceId the serviceId to set
	 */
	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}
	/**
	 * @return the sgGuid
	 */
	public String getSgGuid() {
		return sgGuid;
	}
	/**
	 * @param sgGuid the sgGuid to set
	 */
	public void setSgGuid(String sgGuid) {
		this.sgGuid = sgGuid;
	}
	
	/**
	 * @return the gguid
	 */
	public String getGguid() {
		return gguid;
	}
	/**
	 * @param gguid the gguid to set
	 */
	public void setGguid(String gguid) {
		this.gguid = gguid;
	}
	/**
	 * @return the targetGguid
	 */
	public String getTargetGguid() {
		return targetGguid;
	}
	/**
	 * @param targetGguid the targetGguid to set
	 */
	public void setTargetGguid(String targetGguid) {
		this.targetGguid = targetGguid;
	}

	/**
	 * @return the accountId
	 */
	public long getAccountId() {
		return accountId;
	}
	/**
	 * @param accountId the accountId to set
	 */
	public void setAccountId(long accountId) {
		this.accountId = accountId;
	}
	
	/**
	 * @return the statusId
	 */
	public int getStatusId() {
		return statusId;
	}
	/**
	 * @param statusId the statusId to set
	 */
	public void setStatusId(int statusId) throws XanbooException {
		this.statusId = statusId;
	}
	/**
	 * @return the extAccountId
	 */
	public String getExtAccountId() {
		return extAccountId;
	}
	/**
	 * @param extAccountId the extAccountId to set
	 */
	public void setExtAccountId(String extAccountId) {
		this.extAccountId = extAccountId;
	}
	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}
	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}
	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}
	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	
	/**
	 * @return the authCode
	 */
	public String getAuthCode() {
		return authCode;
	}

	/**
	 * @param authCode the authCode to set
	 */
	public void setAuthCode(String authCode) {
		this.authCode = authCode;
	}

	/**
	 * @return the accessToken
	 */
	public String getAccessToken() {
		return accessToken;
	}
	/**
	 * @param accessToken the accessToken to set
	 */
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
	
	/**
	 * @return the serviceObjects
	 */
	public List<ServiceObject> getServiceObjects() {
		return serviceObjects;
	}
	/**
	 * @param serviceObjects the serviceObjects to set
	 */
	public void setServiceObjects(List<ServiceObject> serviceObjects) {
		this.serviceObjects = serviceObjects;
	}
	
    /** Helper method for adding serviceObject.
     * @return non
     */
   public void addServiceObject(ServiceObject sobject) {
    	if(serviceObjects == null) {
    		serviceObjects = new ArrayList<ServiceObject>();
    	}
    	serviceObjects.add(sobject);
   }
   
    /** Helper method for adding serviceObjects.
    * @return non
    */
    public void addServiceObjects(List<ServiceObject> sobjects) {
	   	if(serviceObjects == null) {
	   		serviceObjects = new ArrayList<ServiceObject>();
	   	}
	   	serviceObjects.addAll(sobjects);
    }

	public boolean isVerified() {
		return isVerified;
	}

	public void setVerified(boolean isVerified) {
		this.isVerified = isVerified;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	
	/**
	 * @return the refershToken
	 */
	public String getRefershToken() {
		return refershToken;
	}

	/**
	 * @param refershToken the refershToken to set
	 */
	public void setRefershToken(String refershToken) {
		this.refershToken = refershToken;
	}
	
	
    /**
	 * @return the tokenExpiration
	 */
	public String getTokenExpiration() {
		return tokenExpiration;
	}

	/**
	 * @param tokenExpiration the tokenExpiration to set
	 */
	public void setTokenExpiration(String tokenExpiraton) {
		this.tokenExpiration = tokenExpiraton;
	}

	/**
	 * @return the redirectUrl
	 */
	public String getRedirectUrl() {
		return redirectUrl;
	}

	/**
	 * @param redirectUrl the redirectUrl to set
	 */
	public void setRedirectUrl(String redirectUrl) {
		this.redirectUrl = redirectUrl;
	}

	/**
	 * @return the deviceGuid
	 */
	public String getDeviceGuid() {
		return deviceGuid;
	}

	/**
	 * @param deviceGuid the deviceGuid to set
	 */
	public void setDeviceGuid(String deviceGuid) {
		this.deviceGuid = deviceGuid;
	}

	/**
	 * @return the domainId
	 */
	public String getDomainId() {
		return domainId;
	}

	/**
	 * @param domainId the domainId to set
	 */
	public void setDomainId(String domainId) {
		this.domainId = domainId;
	}

	/**
	 * @return the userId
	 */
	public long getUserId() {
		return userId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(long userId) {
		this.userId = userId;
	}

	/**
	 * @return the subsId
	 */
	public String getSubsId() {
		return subsId;
	}

	/**
	 * @param subsId the subsId to set
	 */
	public void setSubsId(String subsId) {
		this.subsId = subsId;
	}

	@Override
    public String toString()
    {
        return "ServiceSubscription{" + "serviceId=" + serviceId + ", sgGuid=" + sgGuid + ", gguid=" + gguid + ", accountId=" + accountId + ", statusId=" + statusId + ", extAccountId=" + extAccountId + ", userName=" + userName + ", serviceName=" + serviceName + ", sgwyCatalogId=" + sgwyCatalogId + ",tokenExpiration=" + tokenExpiration + ",redirectUrl=" + redirectUrl + ",deviceGuid=" + deviceGuid + ",userId=" + userId + ",subsId=" + subsId +"}";
    }
}
