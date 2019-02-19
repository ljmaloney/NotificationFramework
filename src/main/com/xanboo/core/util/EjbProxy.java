/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/util/EjbProxy.java,v $
 * $Id: EjbProxy.java,v 1.5 2011/07/01 16:11:30 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.ejb.EJBException;
import javax.ejb.EJBHome;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;






/** 
 * A utility helper class to locate and create EJB references.<br>
 * An example use of EjbProxy, to get an AccountManager refernce would be something like this:<br>
 *<pre>
 *        proxy = new EjbProxy();
 *        AccountManager aManager = (AccountManager) proxy.getObj("xanboo/sdk/ejb/XanbooCoreAccountManager");
 *</pre>
 * Then you can do thing like:<br>
 *<pre>
 *        aManager.newAccount(.....);
 *</pre>
 */
public class EjbProxy
{
    private Properties _prop = null;
    static Logger logger = LoggerFactory.getLogger(EjbProxy.class.getName());
    private static ConcurrentHashMap<String , InitialContext> contextCache = new ConcurrentHashMap<String , InitialContext>();
    public static ArrayBlockingQueue<String> _queue = new ArrayBlockingQueue<String>(1);
    private static boolean isJboss5 = false;
    
    //enable by default for dlcore.ejproxy.cacheRef=true
    private static boolean enableRefCache= true;
    private static boolean useRHPatch = false;
    private static int poolSize = 16;
    private static ConcurrentHashMap<String , EJBPoolUtil> ejbPool = new ConcurrentHashMap<String , EJBPoolUtil>();
    
    private static ConcurrentHashMap<String , Object> refCache = new ConcurrentHashMap<String , Object>();
    
	 public static final ReentrantReadWriteLock proxyLock = new ReentrantReadWriteLock();
	 public static long resetTS=0;
	 public static long resetCacheTS=0;
    static{
           _queue.add("lock");
           
         try {
			String strRefCache = System.getProperty("dlcore.ejproxy.cacheRef");
			
			String strUseRHPatch = System.getProperty("enable.ejproxy.rh.patch");
			
			String resetCacheTime = System.getProperty("enable.ejproxy.resetCacheTS");
			
			if(logger.isDebugEnabled()){
				logger.debug("System.getProperty(dlcore.ejproxy.cacheRef) : " + strRefCache);
				logger.debug("System.getProperty(enable.ejproxy.rh.patch) : " + strUseRHPatch);
				logger.debug("System.getProperty(enable.ejproxy.resetCacheTS) : " + resetCacheTime);
			}
			
			if (resetCacheTime != null) {
			

				try {
					resetCacheTS = Integer.parseInt(resetCacheTime);
				} catch (Exception e) {
					resetCacheTS = 0;
				}

			}
			 
			if (strRefCache != null) {
				try {
					enableRefCache = Boolean.parseBoolean(strRefCache);
				} catch (Exception e1) {
					enableRefCache = true;

				}
			}

			if (strUseRHPatch != null) {
				useRHPatch = true;

				try {
					poolSize = Integer.parseInt(strUseRHPatch);
				} catch (Exception e) {
					poolSize = 16;
				}

			}
			 
		} catch (Exception e) {
			//ignore
			
		}
         
     	if(logger.isDebugEnabled()){
			logger.debug("enableRefCache : " + enableRefCache);
		} 	   
        
    }

    /**
     * Constructs an EjbProxy for local ejb invocations
     */ 
    public EjbProxy()  {
    	
    	
    	try {
			if(useRHPatch && !ejbPool.containsKey("local")){
				EjbProxy._queue.take();
				
				if(!ejbPool.containsKey("local"))			
				ejbPool.put("local", new EJBPoolUtil(null, null, null, poolSize));
				
				EjbProxy._queue.put("lock");
			}
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
    	
    }

    /**
     * Constructs an EjbProxy with a particular jndi factory class and url for remote ejb invocations
     * @param initContextFactory optional jndi factory class impl. If passed null, "org.jnp.interfaces.NamingContextFactory" will be defaulted
     * @param providerUrl url for jndi lookups (e.g. jnp://192.168.1.131:1099)
     */
    public EjbProxy (String initContextFactory, String providerUrl)   {
    	if(providerUrl!=null && providerUrl.length()>0 ) {
            // if(initContextFactory==null) initContextFactory="org.jnp.interfaces.NamingContextFactory";
            setContextProperties (initContextFactory, providerUrl, null, null);
        }
    	
    	try {
			if(useRHPatch && providerUrl!=null && !ejbPool.containsKey(providerUrl)){
				EjbProxy._queue.take();
				
				if(!ejbPool.containsKey(providerUrl))			
				ejbPool.put(providerUrl, new EJBPoolUtil(providerUrl, null, null, poolSize));
				
				EjbProxy._queue.put("lock");
			}else if( useRHPatch && providerUrl==null && !ejbPool.containsKey("local")){
				EjbProxy._queue.take();
				
				if(!ejbPool.containsKey("local"))			
				ejbPool.put("local", new EJBPoolUtil(null, null, null, poolSize));
				
				EjbProxy._queue.put("lock");
			}
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
    }

    public  EjbProxy (Properties prop)  {
        
         
         if(prop != null){
             setContextProperties (prop);
        	
        	
             String providerUrl = null;
             String userName = null;
             String passwd = null;
             if(prop.containsKey(Context.PROVIDER_URL))   providerUrl = prop.getProperty(Context.PROVIDER_URL);
             if(prop.containsKey(Context.SECURITY_PRINCIPAL))   userName = prop.getProperty(Context.SECURITY_PRINCIPAL);
             if(prop.containsKey(Context.SECURITY_CREDENTIALS))   passwd = prop.getProperty(Context.SECURITY_CREDENTIALS);
             
           	try {
    			if(logger.isDebugEnabled()){
    				logger.debug(" ejbPool : " + ejbPool);
    			}
    			
    			if(providerUrl!=null && !ejbPool.containsKey(providerUrl)){
    				EjbProxy._queue.take();
    				
    				if(useRHPatch && !ejbPool.containsKey(providerUrl))			
    				ejbPool.put(providerUrl, new EJBPoolUtil(providerUrl, userName, passwd, poolSize));
    				
    				EjbProxy._queue.put("lock");
    			}else if(useRHPatch && providerUrl==null && !ejbPool.containsKey("local")){
    				EjbProxy._queue.take();
    				
    				if(!ejbPool.containsKey("local"))			
    				ejbPool.put("local", new EJBPoolUtil(null, userName, passwd, poolSize));
    				
    				EjbProxy._queue.put("lock");
    			}
    		} catch (InterruptedException e) {
    			
    			logger.error(e.getMessage());
    		}
        	}
    }

    public void setContextProperties (Properties prop)   {
        _prop = prop;
    }

    public void setContextProperties (String initContextFactory, String providerUrl,
                                      String user, String password)   {
        _prop = new Properties ();
        if(initContextFactory !=null)  _prop.put (Context.INITIAL_CONTEXT_FACTORY, initContextFactory);
        _prop.put (Context.PROVIDER_URL, providerUrl);
        if (user != null)  {
            _prop.put(Context.SECURITY_PRINCIPAL, user);
            if (password == null) password = "";
            _prop.put(Context.SECURITY_CREDENTIALS, password);
        }
    }

    
    public void setContextUserParam (String user, String password)  {
        if (_prop == null) _prop = new Properties ();

        _prop.put(Context.SECURITY_PRINCIPAL, user);
        _prop.put(Context.SECURITY_CREDENTIALS, password);
    }

    
    public Object getNoCacheObj(String beanJndiLookupName) throws EJBException {
    	
    	Object obj = null;
		try {
			
			if (useRHPatch) {

				try {
					
					
					String providerUrl = null;
					
					if (_prop != null && _prop.containsKey(Context.PROVIDER_URL)
							&& _prop.get(Context.PROVIDER_URL) != null){
						 providerUrl = (String) _prop.get(Context.PROVIDER_URL);
					}
					
					if(proxyLock.isWriteLocked()) {
						proxyLock.readLock().lock();
						
						if(ejbPool.size()>0){
							if (providerUrl != null) {
								EJBPoolUtil poolUtil = ejbPool.get(providerUrl);

								return poolUtil.getEjbProxy(beanJndiLookupName);
							} else {
								EJBPoolUtil poolUtil = ejbPool.get("local");

								return poolUtil.getEjbProxy(beanJndiLookupName);
							}
						}
					}
					
					
					if(resetCacheTS>0){
						
						
					
					 EjbProxy._queue.take();
				
					//&& ejbPool.size()==0
								
					if (System.currentTimeMillis() >  resetTS + resetCacheTS ) { 
						proxyLock.writeLock().lock();
						ejbPool.clear();

						if (useRHPatch && providerUrl != null) {

							if (!ejbPool.containsKey(providerUrl))
								ejbPool.put(providerUrl, new EJBPoolUtil(providerUrl, null, null, poolSize));

						} else if (useRHPatch && providerUrl == null) {

							if (!ejbPool.containsKey("local"))
								ejbPool.put("local", new EJBPoolUtil(null, null, null, poolSize));

						}
						resetTS = System.currentTimeMillis();
						
						proxyLock.writeLock().unlock();
					}
					EjbProxy._queue.put("lock");
					
					}
						
					if(ejbPool.size()>0){
							if (providerUrl != null) {
								EJBPoolUtil poolUtil = ejbPool.get(providerUrl);

								return poolUtil.getEjbProxy(beanJndiLookupName);
							} else {
								EJBPoolUtil poolUtil = ejbPool.get("local");

								return poolUtil.getEjbProxy(beanJndiLookupName);
							}
					}
						
					
					
					
					return null;
				
					
				} catch (Exception e) {
					logger.error(e.getMessage());
				}

			}
			
			
			InitialContext ctx = null;

			if (logger.isDebugEnabled()) {
				logger.debug("jndiName : " + beanJndiLookupName);
			}

			String url = "local";

			if (_prop != null && _prop.containsKey(Context.PROVIDER_URL)
					&& _prop.get(Context.PROVIDER_URL) != null) {
				url = (String) _prop.get(Context.PROVIDER_URL);
			}

			if (url != null && contextCache.containsKey(url)) {
				ctx = contextCache.get(url);
				if (logger.isDebugEnabled()) {
					logger.debug("getting from cache - ctx : " + ctx.toString());
					logger.debug("getting from cache -contextCache : "
							+ contextCache);
				}
			} else {

				try {
					EjbProxy._queue.take();

					if (_prop != null) {
						_prop.put(Context.URL_PKG_PREFIXES,
								"org.jboss.ejb.client.naming");
						if (_prop.containsKey(Context.INITIAL_CONTEXT_FACTORY))
							_prop.remove(Context.INITIAL_CONTEXT_FACTORY);

						if (url != null && url.startsWith("remote")) {
							// isUpdate = true;
							_prop.put(Context.INITIAL_CONTEXT_FACTORY,
									"org.jboss.naming.remote.client.InitialContextFactory");
							_prop.put("jboss.naming.client.ejb.context", true);
							
						} else if (url != null && url.startsWith("jnp")) {
							_prop.put(Context.INITIAL_CONTEXT_FACTORY,
									"org.jnp.interfaces.NamingContextFactory");
						}

					} else {
						_prop = new Properties();
						_prop.put(Context.URL_PKG_PREFIXES,
								"org.jboss.ejb.client.naming");

					}
					if (!contextCache.containsKey(url)) {
						ctx = new InitialContext(_prop);
						contextCache.put(url, ctx);
					} else {
						ctx = contextCache.get(url);
					}
					EjbProxy._queue.put("lock");
				} catch (InterruptedException e) {
					logger.error(e.getMessage());
				}

				if (logger.isDebugEnabled()) {
					logger.debug("ctx : " + ctx.toString());
				}

				if (logger.isDebugEnabled()) {
					logger.debug("_prop : " + _prop);
				}
			}

			
				if (logger.isDebugEnabled()) {
					logger.debug("using ctx : " + ctx.toString());
					logger.debug("using contextCache : " + contextCache);
				}
				if (url != null && !url.equals("local")) {
					obj = ctx.lookup(beanJndiLookupName);

				} else {
					if (isJboss5) {
						obj = ctx.lookup(beanJndiLookupName);
					} else {
						obj = ctx.lookup("ejb:" + beanJndiLookupName);
					}
				}
			

			
		} catch (NamingException ne) {
			throw new EJBException(ne);
		}
		
		if(enableRefCache && obj != null) refCache.put(beanJndiLookupName, obj);

		return obj;
    	
    }
    	
    	
    
	public Object getObj(String beanJndiLookupName) throws EJBException {
		
		if (useRHPatch) {

			try {
				String url = "local";

				if (_prop != null && _prop.containsKey(Context.PROVIDER_URL)
						&& _prop.get(Context.PROVIDER_URL) != null) {
					url = (String) _prop.get(Context.PROVIDER_URL);
				}

				if(proxyLock.isWriteLocked()) proxyLock.readLock().lock();
				EJBPoolUtil poolUtil = ejbPool.get(url);
				return poolUtil.getEjbProxy(beanJndiLookupName);
			} catch (Exception e) {
				logger.error(e.getMessage());
			}

		}
		
			
			if(enableRefCache && refCache.containsKey(beanJndiLookupName)){
				if(logger.isDebugEnabled()){
					logger.debug("using enableRefCache  for " + beanJndiLookupName );
					logger.debug("refCache : " + refCache.toString());
				} 	
				return refCache.get(beanJndiLookupName);
			}else{
				if(logger.isDebugEnabled()){
					logger.debug("using getNoCacheObj  for " + beanJndiLookupName );
				} 	
			  return getNoCacheObj(beanJndiLookupName);
			}
			
			
		
	}
	

	/*private String getModuleName(String beanJndiLookupName) {
		if( Arrays.asList(GlobalNames.SDK_EJB_ARRAY).contains(beanJndiLookupName)) return GlobalNames.SDK_MODULE_NAME;
		  
		if(Arrays.asList(GlobalNames.XAIL_EJB_ARRAY).contains(beanJndiLookupName)) return GlobalNames.XAIL_MODULE_NAME;
		  
		if(Arrays.asList(GlobalNames.EXTSERVICES_EJB_ARRAY).contains(beanJndiLookupName)) return GlobalNames.EXTSERVICES_MODULE_NAME;
		
		return beanJndiLookupName;
	}*/
    
    
   
    
   public EJBHome getHome (String beanJndiLookupName) throws EJBException   {
        try  {
            InitialContext ctx = null;

	    if (_prop != null)
                ctx = new InitialContext (_prop);
            else
                ctx = new InitialContext ();

            Object home = ctx.lookup(beanJndiLookupName);
            EJBHome obHome = (EJBHome)PortableRemoteObject.narrow (home, EJBHome.class);
            return obHome;
        }catch (NamingException ne) {
            throw new EJBException (ne);
        }
    }

   /*   *//**
     * Returns a reference to the bean with specified JNDI name
     */
    public Object getObjOld (String beanJndiLookupName) throws EJBException  {
        try  {
            EJBHome obHome = getHome (beanJndiLookupName);
            //get the method of create
            Method m = obHome.getClass().getDeclaredMethod("create", new Class[0]);
            //invoke the create method
            Object obj = m.invoke (obHome, new Object[0]);
            return obj;
        }catch (NoSuchMethodException ne)  {
            throw new EJBException (ne);
        }catch (InvocationTargetException ie)  {
            throw new EJBException (ie);
        }catch (IllegalAccessException iae) {
            throw new EJBException (iae);
        }
    }
}



