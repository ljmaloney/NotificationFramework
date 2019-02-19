/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/gateway/rules/XanbooRuleStateGuard.java,v $
 * $Id: XanbooRuleStateGuard.java,v 1.17 2004/06/09 19:27:03 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.gateway.rules;

import com.xanboo.core.util.XanbooException;


/**
 * Abstract base class to represent guards of state condition type. State Guards are essentially logical state checks of some sort,
 * performed right after the rules's event guard is triggered.
 * <br><br>
 * Concrete implementations of state guards extend this class. Currently there are two implementations of this class,
 * one being a device state condition ({@link com.xanboo.core.sdk.gateway.rules.XanbooRuleDeviceStateGuard XanbooRuleDeviceStateGuard}),
 * and the other being a time range state condition ({@link com.xanboo.core.sdk.gateway.rules.XanbooRuleTimeStateGuard XanbooRuleTimeStateGuard}).
 * <br>
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleDeviceStateGuard XanbooRuleDeviceStateGuard
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleTimeStateGuard XanbooRuleTimeStateGuard
 */

public abstract class XanbooRuleStateGuard extends XanbooRuleGuard {
    protected XanbooRuleStateGuard() {
        this.isTrigger = false;
    }
}
