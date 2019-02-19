package com.xanboo.core.util;

import java.io.Serializable;
import java.util.Map;

public class LicenseData implements Serializable {
	
	private static final long serialVersionUID = 4057521983444394602L;

	private String domainId;
	private String licenseKey;
	private String domainAdmins;
	private long subsCountActive;
	private long subsCountCancelled;
	private Map<String, String> licenseDatas; // key value pair.
		
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
	 * @return the licenseKey
	 */
	public String getLicenseKey() {
		return licenseKey;
	}
	/**
	 * @param licenseKey the licenseKey to set
	 */
	public void setLicenseKey(String licenseKey) {
		this.licenseKey = licenseKey;
	}
	/**
	 * @return the domainAdmins
	 */
	public String getDomainAdmins() {
		return domainAdmins;
	}
	/**
	 * @param domainAdmins the domainAdmins to set
	 */
	public void setDomainAdmins(String domainAdmins) {
		this.domainAdmins = domainAdmins;
	}
	
	/**
	 * @return the subsCountActive
	 */
	public long getSubsCountActive() {
		return subsCountActive;
	}
	/**
	 * @param subsCountActive the subsCountActive to set
	 */
	public void setSubsCountActive(long licenseUsageCountActive) {
		this.subsCountActive = licenseUsageCountActive;
	}
	/**
	 * @return the subsCountCancelled
	 */
	public long getSubsCountCancelled() {
		return subsCountCancelled;
	}
	/**
	 * @param subsCountCancelled the subsCountCancelled to set
	 */
	public void setSubsCountCancelled(long licenseUsageCountCancelled) {
		this.subsCountCancelled = licenseUsageCountCancelled;
	}
	/**
	 * @return the licenseDatas
	 */
	public Map<String, String> getLicenseDatas() {
		return licenseDatas;
	}
	/**
	 * @param licenseDatas the licenseDatas to set
	 */
	public void setLicenseDatas(Map<String, String> licenseDatas) {
		this.licenseDatas = licenseDatas;
	}
}
