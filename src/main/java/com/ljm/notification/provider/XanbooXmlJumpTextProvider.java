/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ljm.notification.provider;

import java.util.HashMap;

import com.ljm.notification.AbstractXMLNotificationProvider;
import com.ljm.notification.NotificationDestination;
import com.ljm.notification.NotificationMessageInterface;
import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;

/**
 * Class implementing a Xml Jump Text provider for sending XML messages
 */
public class XanbooXmlJumpTextProvider extends AbstractXMLNotificationProvider {
	Logger logger = LoggerFactory.getLogger(getClass().getName());
	private String shortCode = null;
	private String senderAlias = null;
	private String phoneContext = null;
	private String countryCode = null;
	private boolean usePlus = false;
	private boolean useCountryCode = false;
	private boolean stripSeparatorChar = false;
	private String separatorChar = " ";

	public XanbooXmlJumpTextProvider() {

	}

    @Override
	public void initialize(HashMap config) {
		logger.info("[intialize()] ");
		super.initialize(config);
		// provider specific configs and defaults
		this.shortCode = config.containsKey("shortcode") ? (String) config.get("shortcode") : "224663";
		this.phoneContext = config.containsKey("phonecontext") ? (String) config.get("phonecontext") : "";
		this.countryCode = config.containsKey("countrycode") ? (String) config.get("countrycode") : phoneContext;
		this.senderAlias = config.containsKey("senderAlias") ? (String) config.get("senderAlias") : null;

		// prepend the "+" symbol to the phone number
		String usePStr = config.containsKey("useplusintl") ? (String) config.get("useplusintl") : "false";
		usePlus = new Boolean(usePStr);
		if (!phoneContext.equalsIgnoreCase("") && usePlus && !phoneContext.startsWith("+")) {
			phoneContext = "+" + phoneContext;
		}

		String useCountryCdStr = config.containsKey("usecountrycd") ? (String) config.get("usecountrycd") : "false";
		useCountryCode = new Boolean(useCountryCdStr);

		String stripSepChar = config.containsKey("stripseparator") ? (String) config.get("stripseparator") : "false";
		stripSeparatorChar = new Boolean(stripSepChar);
		if (stripSeparatorChar)
			separatorChar = config.containsKey("separatorchar") ? (String) config.get("separatorchar") : " ";
	}

    @Override
	public String createXML(NotificationDestination destination, NotificationMessageInterface message) {
		logger.debug("[createXML] - destination=" + destination.toString() + " message=" + message.toString());
		String destPhone = destination.getDestinationAddress();
		String msgID = null; // Need to know from where to fetch the message id
		String sendOnGroup = null; // need to know for sendonGroup 
		StringBuilder str = new StringBuilder();

		str.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		str.append("<!DOCTYPE xiamSMS SYSTEM \"xiamSMSMessage.dtd\">");
		str.append("<xiamSMS>");
			str.append("<submitRequest id=" + msgID + ">");
				str.append("<from>" + shortCode + "</from>");
				str.append("<to>");
				if (usePlus && !destPhone.startsWith("+"))
					str.append("+");
				if (useCountryCode)
					str.append(countryCode);
				if (stripSeparatorChar)
					str.append(destPhone.replace(separatorChar, ""));
				else
					str.append(destPhone);
				str.append("</to>");
				str.append("<content type='text'>" + message.getMessage() + "</content>");
				str.append("<sendOnGroup value=" + sendOnGroup + "/>");
			str.append("</submitRequest>");
		str.append("</xiamSMS>");
		if (this.logger.isDebugEnabled())
		logger.debug("[createXML] - XML Message : \r\n" + str);
		return str.toString();
	}
}
