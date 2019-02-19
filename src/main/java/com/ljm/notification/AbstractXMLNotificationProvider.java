/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ljm.notification;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import com.att.dlife.httplib.http.XanbooHttpResponse;

/**
 *
 * @author us7256
 */
public abstract class AbstractXMLNotificationProvider extends AbstractRESTNotificationProvider {
	public static final String CONTENT_TYPE_XML = "text/xml";
	public static final String ACCEPT_TYPE_XML = "text/xml";
	public static final String HEADER_PROVIDER_ID="X-XIAM-Provider-ID";
	

	/**
	 * Initialization routines for this class.
	 * 
	 * @param config
	 */
	@Override
	public void initialize(HashMap config) {
		super.initialize(config);
		LogManager.getLogger(getClass().getName()).info("[initialize()] Initialize XML Provider");
	}

	@Override
	public String getAcceptHeader() {
		return ACCEPT_TYPE_XML;
	}

	@Override
	public String getContentTypeHeader() {
		return CONTENT_TYPE_XML;
	}

	@Override
	public Map<String, String> getRequestHeaders() {
		Map<String,String> headers = new HashMap<String,String>();
		headers.put(HEADER_PROVIDER_ID, "");
		return headers;
	}

	public abstract String createXML(NotificationDestination destination, NotificationMessageInterface message);

	@Override
	public void sendMessage(NotificationDestination destination, NotificationMessageInterface message)
			throws NotificationException {
		LogManager.getLogger(getClass().getName()).debug("[sendMessage()] sendMessage called");
		String xmlRequestData = createXML(destination, message);
		LogManager.getLogger(getClass().getName()).debug("[sendMessage()] XML request data : " + xmlRequestData);
		try {
			XanbooHttpResponse response = sendPostRequest(null, xmlRequestData);

			if (response.isSuccess()) {
				LogManager.getLogger(getClass().getName()).debug("[sendMessage()] Successful response ("
						+ response.getContent() + ") sending message to " + destination.getDestinationAddress());
			} else {
				LogManager.getLogger(getClass().getName()).debug("[sendMessage()] Error response ("
						+ response.getContent() + ") sending message to " + destination.getDestinationAddress());
			}
		} catch (NotificationException xe) {
			throw xe;
		} catch (IOException ioe) {
			LogManager.getLogger(getClass().getName()).warn("[sendMessage()] - IOException sending notification",
					ioe);
			logNotificationError(destination, message,
					"IOException sending notification using XML/REST. XML Request=" + xmlRequestData, ioe);
			throw new NotificationException(
					"IOException sending notification using XML/REST. XML Request=" + xmlRequestData, ioe);
		} catch (Exception e) {
			logNotificationError(destination, message, "Exception sending notification", e);
			LogManager.getLogger(getClass().getName())
					.error("[sendMessage()] - Exception sending notification using XML/REST", e);
			throw new NotificationException("Exception sending notification using soundbite", e);
		}
	}
}
