/*
To the extent possible under law, Red Hat, Inc. has dedicated all copyright to this software to the public domain worldwide, pursuant to the CC0 Public Domain Dedication.
This software is distributed without any warranty.  See <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package com.xanboo.core.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import javax.naming.Context;
import javax.naming.NamingException;



/**
 *
 */
public class EJBPoolUtil {

	private String providerUrl;
	private String username;
	private String password;
	private final int poolSize;
	private boolean initialized = false;

	private Map<Integer, PoolInstance> remoteNamingInitialContextPool;

	static Logger log = LoggerFactory.getLogger(EJBPoolUtil.class.getName());
	

	public EJBPoolUtil(String providerUrl, String username, String password, int poolSize) {
	    this.providerUrl = providerUrl;
	    this.username = username;
	    this.password = password;
	    this.poolSize = poolSize;
	    this.remoteNamingInitialContextPool = new ConcurrentHashMap<Integer,PoolInstance>(this.poolSize);
        for(int i=0; i<poolSize; i++)
            this.remoteNamingInitialContextPool.put(i, new PoolInstance(providerUrl, username, password));
        initContexts();
	}

	private void initContexts() {
	    if(!initialized) {
	        synchronized (this) {
	            if(!initialized) {
	                try {
	                    for(PoolInstance instance : remoteNamingInitialContextPool.values())
	                        instance.getInitialContext();
	                } catch(Exception e) {
	                    e.printStackTrace();
	                }
	                log.info("Prefilled IniitalContexts");
	                initialized = true;
	            }
            }
	    }
 	}

	public Context getInitialContext() throws Exception {
	    int random = ThreadLocalRandom.current().nextInt(poolSize);
	    return remoteNamingInitialContextPool.get(random).getInitialContext();
	}

	public Object getEjbProxy(String jndiPath) throws NamingException, Exception {
	    int random = ThreadLocalRandom.current().nextInt(poolSize);
	    if(log.isDebugEnabled()){
	    	log.debug(" jndiPath :" + jndiPath);
	    	log.debug(" remoteNamingInitialContextPool :" + remoteNamingInitialContextPool);
	    	log.debug(" remoteNamingInitialContextPool.size :" + remoteNamingInitialContextPool.size());
	    }
	    return remoteNamingInitialContextPool.get(random).getEjbProxy(jndiPath);
	}
}