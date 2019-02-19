package com.xanboo.core.sdk.util;

import com.xanboo.core.util.SBNSynchronizer;


public enum SubscriptionMonitorStatus {
	PM("professional-monitoring", SBNSynchronizer.MONSTATUS_ACTIVE), 
        PP("pending professional-monitoring", null), 
        SM("self-monitoring", SBNSynchronizer.MONSTATUS_ACTIVE), 
        NM("no-monitoring", SBNSynchronizer.MONSTATUS_ACTIVE);

	
	/*There are constants defined in SBNSynchronizer for values allowed:
	    public static final String MONSTATUS_PENDING   = "PEND";
	    public static final String MONSTATUS_ACTIVE    = "QAP";        //MON";
	    public static final String MONSTATUS_SUSPENDED = "SUSP";
	    public static final String MONSTATUS_CANCELLED = "CANX";

	For now, do the following:
	PM   MONSTATUS_ACTIVE
	SM   MONSTATUS_SUSPENDED
	NM  MONSTATUS_SUSPENDED
	Pending PM  no update*/

	private String name;
	private String sbnVal;

	SubscriptionMonitorStatus(String name, String sbnVal) {

		this.name = name;
		this.sbnVal = sbnVal;

	}

	public String getName() {
		return name;
	}

	public String getSBNStatusValue() {

		return sbnVal;
	}

}
