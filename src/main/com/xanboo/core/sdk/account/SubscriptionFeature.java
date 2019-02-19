package com.xanboo.core.sdk.account;

import java.io.Serializable;

/**
 * Class to represent Subscription Feature
 */
public class SubscriptionFeature implements Serializable{

	private static final long serialVersionUID = -6911316979664126394L;
	
	/** Holds feature id. */
    private String featureId;
    /** Holds description of feature id. */
    private String description;    
    /** Holds mapping value. */
    private String mapping;
    /** Holds tc_Acceptance flag */
    private String  tc_acceptance;
    
	/**
	 * Get the feature id
	 * @return faetureId value
	 */
	public String getFeatureId() {
		return featureId;
	}
	
	 /** Sets the featureId.
     * @param featureId value to set the feature id value.
     */
	public void setFeatureId(String featureId) {
		this.featureId = featureId;
	}
	
	/**
	 * Get the description of the feature id.
	 * @return description
	 */
	public String getDescription() {
		return description;
	}
	
	 /** Sets the description of the feature id
     * @param description value to set description of feature id
     */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * Get the mapping soc of the feature id.
	 * @return mapping
	 */
	public String getMapping() {
		return mapping;
	}
	/**
	 * Set the mapping of the feature id.
	 * @param mapping soc value to set the mapping of feature id
	 */
	public void setMapping(String mapping) {
		this.mapping = mapping;
	}
	/**
	 * Get the tc_acceptance of the feature id.
	 * @return tc_acceptance
	 */
	public String getTc_acceptance() {
		return tc_acceptance;
	}
	 /** Sets the tc_acceptance of the feature id
     * @param tc_acceptance value to set tc_acceptance of feature id
     */
	public void setTc_acceptance(String tc_acceptance) {
		this.tc_acceptance = tc_acceptance;
	}
    
    
    
}
