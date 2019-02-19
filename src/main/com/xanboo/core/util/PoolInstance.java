/*
To the extent possible under law, Red Hat, Inc. has dedicated all copyright to this software to the public domain worldwide, pursuant to the CC0 Public Domain Dedication.
This software is distributed without any warranty.  See <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package com.xanboo.core.util;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *
 */
public class PoolInstance {

    private String providerUrl;
    private String username;
    private String password;
    private Context initialContext = null;

    private Map<String, Object> ejbProxies = new ConcurrentHashMap<String, Object>();

    static Logger log = LoggerFactory.getLogger(PoolInstance.class.getName());
    
    public PoolInstance(String providerUrl, String username, String password) {
        this.providerUrl = providerUrl;
        this.username = username;
        this.password = password;
    }

    public Context getInitialContext() throws Exception {
        if(initialContext == null) {
            synchronized (this) {
                if(initialContext == null)
                    initialContext = newRemoteNamingInitialContext(providerUrl, username, password);
            }
        }
        return initialContext;
    }
    
 
    public Object getEjbProxy(String jndiPath) throws NamingException, Exception {
        Object ejbProxy = this.ejbProxies.get(jndiPath);
        if(log.isDebugEnabled()){
        	log.debug(" jndiPath :" + jndiPath);
	    	log.debug(" ejbProxies :" + ejbProxies);
	    	log.debug(" ejbProxies.size :" + ejbProxies.size());
	    }
        if(ejbProxy == null) {
            synchronized (this) {
                ejbProxy = this.ejbProxies.get(jndiPath);
                if(ejbProxy == null) {
                    ejbProxy = getInitialContext().lookup(jndiPath);
                    this.ejbProxies.put(jndiPath, ejbProxy);
                }
            }
        }
        return ejbProxy;
    }

    private static Context newRemoteNamingInitialContext(String providerUrl, String username, String password) throws Exception {
    	
        if(providerUrl == null)
            return new InitialContext();

    	
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        env.put("java.naming.factory.initial", "org.jboss.naming.remote.client.InitialContextFactory");
        if(providerUrl != null)  env.put("java.naming.provider.url", providerUrl);
        env.put("jboss.naming.client.ejb.context", "true");

        if(username != null)
            env.put(Context.SECURITY_PRINCIPAL, username);
        if(password != null)
            env.put(Context.SECURITY_CREDENTIALS, password);
        return new InitialContext(env);
    }

}
