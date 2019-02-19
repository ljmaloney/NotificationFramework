/*
 * XanbooGuardContainer.java
 *
 * Created on December 30, 2004, 10:21 AM
 */

package com.xanboo.core.sdk.gateway.rules;

import com.xanboo.core.util.XanbooException;

/**
 *
 * @author  rking
 */
public interface XanbooGuardContainer {
    
    public void addRuleGuard(XanbooRuleGuard guard, int operator) throws XanbooException;
    public XanbooRuleGuard[] getRuleGuardList();
}
