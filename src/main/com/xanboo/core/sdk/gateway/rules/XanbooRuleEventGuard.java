/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/gateway/rules/XanbooRuleEventGuard.java,v $
 * $Id: XanbooRuleEventGuard.java,v 1.12 2004/06/09 19:27:02 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.gateway.rules;

import com.xanboo.core.util.XanbooException;

/**
 * Abstract base class to represent guards of event/trigger type. An event or trigger guard is essentially an event of some
 * sort that can trigger the execution of a rule on rising edge.
 * <br><br>
 * Concrete implementations of event guards extend this class. Currently there are two implementations of this class,
 * one being the occurrence of a device event ({@link com.xanboo.core.sdk.gateway.rules.XanbooRuleDeviceEventGuard XanbooRuleDeviceEventGuard}),
 * and the other being a scheduled time/timer event ({@link com.xanboo.core.sdk.gateway.rules.XanbooRuleTimeEventGuard XanbooRuleTimeEventGuard}).
 * <br>
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleDeviceEventGuard XanbooRuleDeviceEventGuard
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleTimeEventGuard XanbooRuleTimeEventGuard
 */
public abstract class XanbooRuleEventGuard extends XanbooRuleGuard {
    
    protected XanbooRuleEventGuard() {
        this.isTrigger = true;
    }
    
}