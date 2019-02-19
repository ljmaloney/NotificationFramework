package com.xanboo.core.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class RateControlCheckTask implements Runnable {
	static Logger log = Logger.getLogger(RateControlCheckTask.class);
	@Override
	public void run() {
		
		log.info(" Started RateControlCheckTask Thread  ");
		//long lastModified=0;
		
		while(true){
			
		try {
			
				if (log.isDebugEnabled()) {
					log.debug("executing RateControlCheckTask ");
				}
				Thread.sleep(GlobalNames.RATECONTROL_CHECK_TIME);
				File file ;
				Reader reader;
				RateControlFile rcFile ;
				try {
					if(GlobalNames.RATECONTROL_FILE_LOCATION != null && GlobalNames.RATECONTROL_FILE_LOCATION.length()>0){
						
						String[] fileList = GlobalNames.RATECONTROL_FILE_LOCATION.split(",");
						
						if (log.isDebugEnabled()) {
							log.debug("fileList :"+ fileList);
						}
						
						for(int i=0;i< fileList.length;++i){
							String fileName =  fileList[i];
						
							file = new File(fileName);
						
						
						
						
						GlobalNames.IS_FLOWCONTROL_ENABLED = true;
						
						
						if(GlobalNames.rcFileMap.containsKey(i+"."+fileName)){
							rcFile = GlobalNames.rcFileMap.get(i+"."+fileName);
						}else{
							rcFile = new RateControlFile(fileName);
						}
						
						
						
						if(rcFile.getLastModified() == file.lastModified()) continue;
						
						reader = new FileReader(file);
						
						
						rcFile.clean();
					
						rcFile.setLastModified(file.lastModified());
						
						//lastModified = file.lastModified();
						JSONObject json =	 (JSONObject) new JSONParser().parse(reader);
						JSONArray  rateToGroupMapArray ;
						JSONArray  rateEntryArray ;
						
						if (log.isDebugEnabled()) {
							log.debug("json :  "+ json.toString());
						}
						
						
						 
						 	
							if(json.containsKey("RateToGroupMap")) {						
								  rateToGroupMapArray = (JSONArray) json.get("RateToGroupMap");
								 Iterator<JSONObject> rateToGroupMapItr = rateToGroupMapArray.iterator();
								 while(rateToGroupMapItr.hasNext()){
									 JSONObject rateToGroup =	 (JSONObject) rateToGroupMapItr.next();
									 if(rateToGroup.containsKey("Group") && rateToGroup.containsKey("RateEntry"))
										 
										 rcFile.addRateToGroup((String)rateToGroup.get("Group"), (String)rateToGroup.get("RateEntry"));
										 
										 if(rateToGroup.containsKey("DLCList")){
											List<String> dlcList =  (List<String>) rateToGroup.get("DLCList");
											rcFile.addListToGroup((String)rateToGroup.get("Group"),dlcList);
										 }
										 
									 
									 
								  }
								 
								
							
							}
							
							
							if(json.containsKey("RateEntry")) {
								rateEntryArray = (JSONArray) json.get("RateEntry");
								 Iterator<JSONObject> rateEntryArrayItr = rateEntryArray.iterator();
								 
								 while(rateEntryArrayItr.hasNext()){
									 JSONObject rateEntry =	 (JSONObject) rateEntryArrayItr.next();
									 
									if( rateEntry.containsKey("Rate") && rateEntry.containsKey("Entry") )
										 rcFile.addrateEntryMap((String)rateEntry.get("Entry") , (String) rateEntry.get("Rate"));
									 
								 }
								
							
							
							}
							
							if(json.containsKey("DLCGroupCount")){
								
								String count = (String) json.get("DLCGroupCount");
								
							
									rcFile.setDlcGroupCount(count);
								
								
							}
							
							if(!GlobalNames.rcFileMap.containsKey(i+"."+fileName)){
								GlobalNames.rcFileMap.put(i+"."+fileName, rcFile);
								if (log.isDebugEnabled()) {
									log.debug(fileName + " :  "+ rcFile);
								}	
								
							}
						
						}
						
					}else{
						GlobalNames.IS_FLOWCONTROL_ENABLED = false;
						
						continue;
					}
					
				} catch (FileNotFoundException e) {
					GlobalNames.IS_FLOWCONTROL_ENABLED = false;
					continue;
				}
				
				if (log.isDebugEnabled()) {
					log.debug("GlobalNames.IS_FLOWCONTROL_ENABLED :  "+ GlobalNames.IS_FLOWCONTROL_ENABLED);
				}	
				
				
		
			
			
		} catch (Exception e) {
			log.error(e.toString());
			if (log.isDebugEnabled()) {
                log.error("[RateControlCheckTask()]: Exception:" + e.getLocalizedMessage(), e);
            }
			
		}
		if (log.isDebugEnabled()) {
			log.debug("GlobalNames.rcFileMap :  "+ GlobalNames.rcFileMap);
		}		
	
		}
		

	}
	
	

	

}
