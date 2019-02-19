package com.xanboo.core.util;

import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RateControlFile {
	
	private static Logger log = LoggerFactory.getLogger(RateControlFile.class.getName());
	 private  ConcurrentHashMap<String,String> entryGroupRateMap = new ConcurrentHashMap<String,String>();
	 private  ConcurrentHashMap<String,List<String>> entryGroupListMap = new ConcurrentHashMap<String,List<String>>();
	 private  ConcurrentHashMap<String,String> rateEntryMap = new ConcurrentHashMap<String,String>();
	 private  int dlcGroupCount = 32;
	 public int getDlcGroupCount() {
		return dlcGroupCount;
	}

	public void setDlcGroupCount(String count) {
		
		try {
			dlcGroupCount = Integer.parseInt(count);
		} catch (Exception e) {
			dlcGroupCount = entryGroupRateMap.size();
		}
	
	}

	private String fileName;
	 public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	private long lastModified = 0;    
	    

	public RateControlFile(String fileName) {
		 this.fileName = fileName;
	}

	public void clean() {
		entryGroupRateMap.clear();
		rateEntryMap.clear();
	}

	

	/**
	 * @return the lastModified
	 */
	public long getLastModified() {
		return lastModified;
	}

	/**
	 * @param lastModified the lastModified to set
	 */
	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	public void addRateToGroup(String key, String value) {
		entryGroupRateMap.put(key, value);
		
	}

	public void addrateEntryMap(String key, String value) {
		rateEntryMap.put(key, value);
		
	}

	public void addListToGroup(String key, List<String> dlcList) {
		entryGroupListMap.put(key, dlcList);
		
	}

	public String getRate(String gguid) {

		String value = null;
		
		if(entryGroupListMap.size()>0){
		Enumeration<String> keys =	entryGroupListMap.keys();
			
			while(keys.hasMoreElements() ){
				String key = keys.nextElement();
				
				List<String> gguidList = entryGroupListMap.get(key);
				
				if(gguidList.contains(gguid)){
				String  key1 =	entryGroupRateMap.get(key);
				if(key1 != null) value = rateEntryMap.get(key1);
				 if(log.isDebugEnabled()){
		 	    	 log.debug(fileName +" : GGUID : " +gguid + " : Group : " + key + " : Entry : " + key1 + "  : Rate : " + value);
		 	     }
				 return value;
				}
				
			}
		}
		

	  	 String lastFour = gguid.substring((gguid.length() - 4));
	        int lastBytes = 0;
	        char[] chars = lastFour.toCharArray();
	        for (int i = 0; i < chars.length; i++) {
	                lastBytes += chars[i];
	        }
	      
	     
	                int idx = 0;
	           
	     idx = lastBytes % dlcGroupCount;
	     
	    		     
	    if(entryGroupRateMap.containsKey(idx+"")){
	    	String key = entryGroupRateMap.get(idx+"");
	    	
	    	if(key != null) value = rateEntryMap.get(key);
	    	
	    	if(value == null){
	    		log.warn(fileName +" : RateControl NOT defined for GGUID : " +gguid + " : Group : "+ idx + " : Entry : " + key + " : Rate : " + value );
	    	}
	    }
	   
	    if(log.isDebugEnabled()){
	    	 log.debug(fileName +" : GGUID : " +gguid + " : Group : "+ idx + " : Rate : " + value);
	     }
		return value;
	}

	@Override
	public String toString() {
		return "RateControlFile [entryGroupRateMap=" + entryGroupRateMap + ", entryGroupListMap=" + entryGroupListMap
				+ ", rateEntryMap=" + rateEntryMap + ", dlcGroupCount=" + dlcGroupCount + ", fileName=" + fileName
				+ ", lastModified=" + lastModified + "]";
	}
	
	
	
	
	

}
