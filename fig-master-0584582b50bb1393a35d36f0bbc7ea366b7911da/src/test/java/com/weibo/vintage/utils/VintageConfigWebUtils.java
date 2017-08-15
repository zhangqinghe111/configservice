package com.weibo.vintage.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.JsonNode;
import org.omg.CORBA.PRIVATE_MEMBER;

import com.weibo.vintage.exception.ExcepFactor;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.json.JsonUtil;
import com.weibo.vintage.json.JsonUtil;
import com.weibo.vintage.listener.ConfigServiceChangeListener;
import com.weibo.vintage.listener.ConfigServiceKeyChangeListener;
import com.weibo.vintage.processor.SnapshotProcessor;
import com.weibo.vintage.service.StaticConfigService;
import com.weibo.vintage.utils.ApacheHttpClient;
import com.weibo.vintage.utils.ClusterSignAlgorithm;
import com.weibo.vintage.utils.MD5Utils;
import com.weibo.vintage.utils.NamedThreadFactory;
import com.weibo.vintage.utils.UrlHelper;
import com.weibo.vintage.utils.VintageConfigWrapper;

import cn.sina.api.commons.util.Assert;

import com.weibo.vintage.exception.HttpRequestException;
import com.weibo.vintage.model.*;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;


/**
 * 
 * @author liuyu9
 * 
 */

public class VintageConfigWebUtils {
	
	public static final String IP = VintageConstantsTest.IP;
	public static final String PORT = VintageConstantsTest.PORT;
	public static final String VINTAGE_CONFIG_SERVICE_URL = "http://" + IP
			+ ":" + PORT + "/1/config/service";
	private static HttpClient client = new DefaultHttpClient();
	private static VintageConfigWrapper configWrapper;


    /**
     * @param group
     * @param key
     * @param value
     * @return
     */
	public static boolean register(String group, String key, String value) {
		String params = "action=register&group="+group+"&key="+key+"&value="+value;
		String result = "";
		try {
			result = VintageConfigWebUtils.doPost(VINTAGE_CONFIG_SERVICE_URL, params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ResponsePacket packet = parser(result);
		Boolean res = validateResponse(packet);
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return res;
	}
	
	public static boolean register(String prefix, String group, String key, String value) {
		String params = "action=register&group="+group+"&key="+key+"&value="+value;
		String result = "";
		try {
			result = VintageConfigWebUtils.doPost(prefix, params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ResponsePacket packet = parser(result);
		Boolean res = validateResponse(packet);
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return res;
	}
	
	public static boolean register_forbatch(String group, String key, String value) {
		String params = "action=register&group="+group+"&key="+key+"&value="+value;
		String result = "";
		try {
			result = VintageConfigWebUtils.doPost(VINTAGE_CONFIG_SERVICE_URL, params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ResponsePacket packet = parser(result);
		return validateResponse(packet);
	}
	
	public static void batchregister(String group, String key, String value, int status, int num){
		for (int i = 0; i < num; i++){
			if (status == 0){
				register_forbatch(group, key+i, value);
			} else if (status == 1) {
				register_forbatch(group, key+i, value+i);
			} else if (status == 2) {
				register_forbatch(group, key, value);
			}
		}
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void batchregister(String group, String key, String value, int status, int start, int end){
		for (int i = start; i < end; i++){
			if (status == 0){
				register_forbatch(group, key+i, value);
			} else if (status == 1) {
				register_forbatch(group, key+i, value+i);
			}
		}
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * unregister
	 */

    /**
     * @param group
     * @param key
     * @return
     */
	public static boolean unregister(String group, String key) {
		String params = "action=unregister&group="+group+"&key="+key;
		String result = "";
		try {
			result = VintageConfigWebUtils.doPost(VINTAGE_CONFIG_SERVICE_URL, params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ResponsePacket packet = parser(result);
		Boolean res = validateResponse(packet);
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return res;
	}
	
	public static boolean unregister(String prefix, String group, String key) {
		String params = "action=unregister&group="+group+"&key="+key;
		String result = "";
		try {
			result = VintageConfigWebUtils.doPost(prefix, params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ResponsePacket packet = parser(result);
		Boolean res = validateResponse(packet);
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return res;
	}
	
	public static boolean unregister_forbatch(String group, String key) {
		String params = "action=unregister&group="+group+"&key="+key;
		String result = "";
		try {
			result = VintageConfigWebUtils.doPost(VINTAGE_CONFIG_SERVICE_URL, params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ResponsePacket packet = parser(result);
		return validateResponse(packet);
	}
	
	public static void batchunregister(String group, String key, int status, int num){
		for (int i = 0; i < num; i++){
			if (status == 0){
				unregister_forbatch(group, key+i);
			} else if (status == 1){
				unregister_forbatch(group, key);
			}
		}
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void batchunregister(String group, String key, int status, int start, int end){
		for (int i = start; i < end; i++){
			if (status == 0){
				unregister_forbatch(group, key+i);
			} else if (status == 1){
				unregister_forbatch(group, key);
			}
		}
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void batchreg_unreg(String group, String key, String value, int status, int num){
		for (int i = 0; i < num; i++){
			if (status == 0){
				register_forbatch(group, key+i, value);
				unregister_forbatch(group, key+i);
			} else if (status == 1) {
				register_forbatch(group, key+i, value+i);
				unregister_forbatch(group, key+i);
			} else if (status == 2) {
				register_forbatch(group, key, value);
				unregister_forbatch(group, key);
			}
		}
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 * lookup
	 */
	public static List<String> lookup(String group, String key) {
		String params = "action=lookup&group="+group+"&key="+key;
		String result = "";
		try {
			result = VintageConfigWebUtils.doGet(VINTAGE_CONFIG_SERVICE_URL, params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ResponsePacket packet = parser(result);
		if (validateResponse(packet)) {
			JsonNode jsNode = JsonHelper.parserStringToJsonNode(packet
					.getBody());
			List<String> nodeList = JsonUtil.getJsonMultiValues(jsNode
					.getFieldValue("nodes"));
			return nodeList;
		}
		return null;
	}

	public static List<String> lookup(String prefix, String group, String key) {
		String params = "action=lookup&group="+group+"&key="+key;
		String result = "";
		try {
			result = VintageConfigWebUtils.doGet(prefix, params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ResponsePacket packet = parser(result);
		if (validateResponse(packet)) {
			JsonNode jsNode = JsonHelper.parserStringToJsonNode(packet
					.getBody());
			List<String> nodeList = JsonUtil.getJsonMultiValues(jsNode
					.getFieldValue("nodes"));
			return nodeList;
		}
		return null;
	}
	
	/**
	 * lookup
	 **/
	public static List<String> lookup(String group) {
		String params = "action=lookup&group="+group;
		String result = "";
		try {
			result = VintageConfigWebUtils.doGet(VINTAGE_CONFIG_SERVICE_URL, params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ResponsePacket packet = parser(result);
		if (validateResponse(packet)) {
			JsonNode jsNode = JsonHelper.parserStringToJsonNode(packet
					.getBody());
			List<String> nodeList = JsonUtil.getJsonMultiValues(jsNode
					.getFieldValue("nodes"));
			return nodeList;
		}
		return null;
	}


    /*
    * lookup for snapshot,get result directly
    * */
    public static String lookupForAll(String group) {
    	String params = "action=lookup&group="+group;
		String result = "";
		try {
			result = VintageConfigWebUtils.doGet(VINTAGE_CONFIG_SERVICE_URL, params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return result;
    }

    public static String lookupForAll(String prefix, String group) {
    	String params = "action=lookup&group="+group;
		String result = "";
		try {
			result = VintageConfigWebUtils.doGet(prefix, params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return result;
    }

    
	/**
	 * get map from list
	 */

	public static Map<String, String> getConfigMap(List<String> nodesList) {
		Map<String, String> nodesMap = new HashMap<String, String>();

		for (String str : nodesList) {
			JsonNode jsonNode = JsonHelper.parserStringToJsonNode(str);
			String key = jsonNode.getFieldValue("key") == null ? "" : jsonNode
					.getFieldValue("key").getTextValue();
			String value = jsonNode.getFieldValue("value") == null ? ""
					: jsonNode.getFieldValue("value").getTextValue();

			nodesMap.put(key, value);
		}

		return nodesMap;
	}

	/*
	 * get configInfo
	 */
	public static List<ConfigInfo> getConfigInfos(List<String> configList) {
		List<ConfigInfo> configInfos = new ArrayList<ConfigInfo>();
		for (String str : configList) {
			JsonNode jsonNode = JsonHelper.parserStringToJsonNode(str);
			String group = jsonNode.getFieldValue("groupId") == null ? ""
					: jsonNode.getFieldValue("groupId").toString();
			String md5 = jsonNode.getFieldValue("md5") == null ? "" : jsonNode
					.getFieldValue("md5").toString();
			String key = jsonNode.getFieldValue("key") == null ? "" : jsonNode
					.getFieldValue("key").toString();
			String value = jsonNode.getFieldValue("value") == null ? ""
					: jsonNode.getFieldValue("value").toString();
			ConfigInfo configInfo = new ConfigInfo(group, key, value, md5);
			configInfos.add(configInfo);
		}
		return configInfos;
	}

	/*
	 * getsign
	 */
	public static String getsign(String group) {
		String params = "action=getsign&group="+group;
		String result = "";
		try {
			result = VintageConfigWebUtils.doGet(VINTAGE_CONFIG_SERVICE_URL, params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = parser(result);
		if (validateResponse(packet)) {			
			//JsonNode jsNode = JsonHelper.parserStringToJsonNode(packet.getBody());
			//return jsNode.toString();
			String sign = packet.getBody();
			String sign1=sign.replaceAll("\\p{Punct}","");
			String signvalue=sign1.replace("sign", "");
			return signvalue;
		}
		return null;
	}

	private static boolean validateResponse(ResponsePacket packet) {
		if (packet != null && packet.getStatusCode().isOk()) {
			return true;
		} else {
			throw buildException(packet);
		}
	}

	private static VintageException buildException(ResponsePacket packet) {
		if (packet != null && StringUtils.isNotBlank(packet.getBody())) {
			return VintageException.parser(packet.getBody());
		} else {
			return new VintageException(ExcepFactor.E_SERVICE_PACKET_EMPTY);
		}
	}

	public static ResponsePacket parser(String json) {
		if (json == null || json.isEmpty()) {
			return null;
		}

		JsonNode jsNode = JsonHelper.parserStringToJsonNode(json);
		if (jsNode != null) {
			String code = JsonUtil.getJsonTextValue(jsNode.getFieldValue("code"), "");
			String body = String.valueOf(jsNode.getFieldValue("body"));
			
		// TODO: parser the status code
			return new ResponsePacket(HttpStatusCode.parser(code), body);
		}

		return null;
	}

	/*
	 * getgroup
	 */
	public static List<String> getgroup() {
		String params = "action=getgroup";
		String result = "";
		try {
			result = VintageConfigWebUtils.doGet(VINTAGE_CONFIG_SERVICE_URL, params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = parser(result);
		if (validateResponse(packet)) {
			JsonNode jsNode = JsonHelper.parserStringToJsonNode(packet
					.getBody());
			List<String> nodeList = JsonUtil.getJsonMultiValues(jsNode
					.getFieldValue("groups"));
			return nodeList;
		}
		return null;
	}
	
	/*
	 * getgroup
	 */
	public static List<String> getkeys(String group) {
		String params = "action=getkeys&group="+group;
		String result = "";
		try {
			result = VintageConfigWebUtils.doGet(VINTAGE_CONFIG_SERVICE_URL, params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = parser(result);
		if (validateResponse(packet)) {
			JsonNode jsNode = JsonHelper.parserStringToJsonNode(packet
					.getBody());
			List<String> nodeList = JsonUtil.getJsonMultiValues(jsNode
					.getFieldValue("keys"));
			return nodeList;
		}
		return null;
	}

    public static String calculate(Set<ConfigInfo> configSets){
        if(configSets != null && !configSets.isEmpty() ){
            SortedSet<String> sortedList = new TreeSet<String>();
            for(ConfigInfo configInfo: configSets){
                sortedList.add(configInfo.getKey()+configInfo.getValue()+configInfo.getMd5());
            }
            return MD5Utils.md5(sortedList.toString());
        }
        else{
            return ClusterSignAlgorithm.DEFAULT_SIGN;
        }
    }


    public static String doPost(String relativeurl, String parameters) throws Exception {
		BufferedReader in = null;
		
		if(relativeurl == null || relativeurl.equals(""))
			return null;
		String localurl = relativeurl;
		HttpPost httppost = new HttpPost(localurl);
		StringEntity reqEntity = new StringEntity(parameters,"UTF-8");
		reqEntity.setContentType("application/x-www-form-urlencoded");
		httppost.setEntity(reqEntity);
		HttpResponse response = client.execute(httppost);

		in = new BufferedReader(new InputStreamReader(response.getEntity()
				.getContent()));
		StringBuffer sb = new StringBuffer("");
		String line = "";
		String NL = System.getProperty("line.separator");
		while ((line = in.readLine()) != null) {
			sb.append(line + NL);
		}
		in.close();
		VintageTestLogger.accessLog("curl \"" + localurl + "\" -d \"" + parameters + "\"");
		String result = sb.toString();
		VintageTestLogger.accessLog(result);
		httppost.releaseConnection();
		return result;
	}
	
	public static String doGet(String relativeurl, String parameters) throws Exception {
		BufferedReader in = null;
		
		if(relativeurl == null || relativeurl.equals(""))
			return null;
		String localurl = relativeurl + "?" + parameters;
		HttpGet httpget = new HttpGet(localurl);
		HttpResponse response = client.execute(httpget);

		in = new BufferedReader(new InputStreamReader(response.getEntity()
				.getContent()));
		StringBuffer sb = new StringBuffer("");
		String line = "";
		String NL = System.getProperty("line.separator");
		while ((line = in.readLine()) != null) {
			sb.append(line + NL);
		}
		in.close();
		VintageTestLogger.accessLog("curl \"" + localurl + "\"");
		String result = sb.toString();
		VintageTestLogger.accessLog(result);
		httpget.releaseConnection();
		return result;	
	}
    

	public static StaticsConfigMap lookupForUpdate(String groupId, String sign) {
	    VintageUtils.isBlank(groupId, "查看配置服务时，配置服务标识不能为空");      
	   
	    String action = VintageConstants.CONFIG_SERVICE_LOOKUPFORUPDATE;
		String params = "action="+action+"&group="+groupId+"&sign="+sign;
		try {
			String localurl = VINTAGE_CONFIG_SERVICE_URL + "?" + params;    
			HttpGet httpget = new HttpGet(localurl);
			HttpResponse response = client.execute(httpget);   
			if (response == null) {
	    	VintageLogger.error("config group lookupForUpdate failed, result is null, " 
			+ " group = " + groupId +  ", sign: " + sign);
	        throw new HttpRequestException("Fail to lookup for update, case result message is null! ");
	        } else if (response.getStatusLine().getStatusCode() == 304) {
	            return null;
	        } 
	        else {
	        	BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity()
						.getContent()));
				StringBuffer sb = new StringBuffer("");
				String line = "";
				String NL = System.getProperty("line.separator");
				while ((line = in.readLine()) != null) {
					sb.append(line + NL);
				}
				in.close();
			
				ResponsePacket packet = ResponsePacket.parser(sb.toString(),true);
	        	if (VintageUtils.validateResponse(packet, VintageConstants.CONFIG_SERVICE_LOOKUPFORUPDATE)) {
	        		String jsonData = packet.getJsonNodeBody().toString();
	                StaticsConfigMap staticConfig = StaticsConfigMap.parser(jsonData);
	                if (staticConfig != null) {
	                    VintageLogger.info("statics config lookup for update success, " 
	                            + " code = " + packet.getStatusCode()  
	                            + " response="+ staticConfig.toString());
	         
	                    return staticConfig;
	                } else {
	                    VintageLogger.error("statics config lookup for update failed, result is null, " 
	                            + " code = " + packet.getStatusCode()
	                            + " response=" + packet.toString());
	                    return null;
	                }
	            }
	        	throw new HttpRequestException(
	        			"Fail to validate response from server, package: "
								+ (packet == null ? null : packet.toJson())
								+ ", group = " + groupId 
								+ ", sign=" + sign);
	        }    
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
	  
	        return null;
	    }
	/*
	public static void main(String[] args) {
		String groupId="abcd";
		VintageConfigWebUtils.register(groupId, "1234", "lalalal");
		String oldsign=VintageConfigWebUtils.getsign(groupId);
		
		StaticsConfigMap staticConfig =VintageConfigWebUtils.lookupForUpdate(groupId, oldsign);
	
		Assert.isNull(staticConfig);

		
		staticConfig =VintageConfigWebUtils.lookupForUpdate(groupId, "5s4d5as4d5a64d");
		
		
		//VintageConfigWebUtils.register(groupId, "963258", "123456");
		//String newsign=getsign(groupId);
		//staticConfig=lookupForUpdate(groupId, oldsign);
		System.out.println("==========2===="+staticConfig.getMaps().size());

		
	}
	*/
  

}
