package com.ljm.notification.domain;

import java.util.HashMap;

public class ProviderConfig {
	
	Integer providerId;
	String providerClassName;
	String providerUserName;
	String providerPassword;
	String description;
	String configFileName;
	HashMap configMap;
	
	public Integer getProviderId() {
		return providerId;
	}
	public void setProviderId(Integer providerId) {
		this.providerId = providerId;
	}
	public String getProviderClassName() {
		return providerClassName;
	}
	public void setProviderClassName(String providerClassName) {
		this.providerClassName = providerClassName;
	}
	public String getProviderUserName() {
		return providerUserName;
	}
	public void setProviderUserName(String providerUserName) {
		this.providerUserName = providerUserName;
	}
	public String getProviderPassword() {
		return providerPassword;
	}
	public void setProviderPassword(String providerPassword) {
		this.providerPassword = providerPassword;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getConfigFileName() {
		return configFileName;
	}
	public void setConfigFileName(String configFileName) {
		this.configFileName = configFileName;
	}
	public HashMap getConfigMap() {
		return configMap;
	}
	public void setConfigMap(HashMap configMap) {
		this.configMap = configMap;
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ProviderConfig [providerId=").append(providerId).append(", providerClassName=")
				.append(providerClassName).append(", providerUserName=").append(providerUserName)
				.append(", providerPassword=").append(providerPassword).append(", description=").append(description)
				.append(", configFileName=").append(configFileName).append(", configMap=").append(configMap)
				.append("]");
		return builder.toString();
	}
}
