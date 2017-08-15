package com.weibo.dorteam.utils;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.json.JSONArray;
import org.json.JSONObject;

import com.weibo.dorteam.Bean.ConfigInfo;
import com.weibo.dorteam.Common.ConfigCommon;
import com.weibo.vintage.exception.ExcepFactor;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.json.JsonUtil;
import com.weibo.vintage.model.HttpStatusCode;
import com.weibo.vintage.model.ResponsePacket;
import com.weibo.vintage.utils.JsonHelper;

public class VintageConfigUtils {
	private static PrefixUtils pu = new PrefixUtils();
	
	/**
     * @param group
     * @param key
     * @param value
     * @return
     */
	public static boolean register(String host, String group, String key, String value) {
		String params = "action=register&group="+group+"&key="+key+"&value="+value;
		String result = "";
		try {
			result = httpClient.doPost(pu.getUrlPrefix(host, "config"), params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = parser(result);
		return validateResponse(packet);
	}

	/*
	 * unregister
	 */

    /**
     * @param group
     * @param key
     * @return
     */
	public static boolean unregister(String host, String group, String key) {
		String params = "action=unregister&group="+group+"&key="+key;
		String result = "";
		try {
			result = httpClient.doPost(pu.getUrlPrefix(host, "config"), params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = parser(result);
		return validateResponse(packet);
	}

	/*
	 * getgroup
	 */
	public static Set<String> getgroup(String host) {
		String params = "action=getgroup";
		Set<String> groups = new HashSet<String>(); 
		String result = "";
		try {
			result = httpClient.doGet(pu.getUrlPrefix(host, "config"), params);
			JSONObject jo = new JSONObject(result).getJSONObject("body");
			JSONArray ja = jo.getJSONArray("groups");
			for (int i = 0; i < ja.length(); i++) {
				String issue = ja.getString(i);
				groups.add(issue);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return groups;
	}
	
	/*
	 * lookup
	 */
	public static ConfigInfo lookup(String host, String paras){
		String params = "action=lookup&"+paras;
		String result = "";
		ConfigInfo ci = null;
		try {
			result = httpClient.doGet(pu.getUrlPrefix(host, "config"), params);		
			ci = ConfigCommon.getConfigInfos(result);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ci;
	}

	/*
	 * getsign
	 */
	public static String getsign(String host, String group) {
		String params = "action=getsign&group="+group;
		String sign = "";
		String result = "";
		try {
			result = httpClient.doGet(pu.getUrlPrefix(host, "config"), params);
			JSONObject jo = new JSONObject(result).getJSONObject("body");
			sign = jo.getString("sign");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		return sign;
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
			String code = JsonUtil.getJsonTextValue(
					jsNode.getFieldValue("code"), "");
			String body = String.valueOf(jsNode.getFieldValue("body"));
			// TODO: parser the status code
			return new ResponsePacket(HttpStatusCode.parser(code), body);
		}

		return null;
	}

	/*
	 * getgroup
	 */
	public static Set<String> getkeys(String host, String group) {
		String params = "action=getkeys&group="+group;
		Set<String> keys = new HashSet<String>(); 
		String result = "";
		try {
			result = httpClient.doGet(pu.getUrlPrefix(host, "config"), params);
			JSONObject jo = new JSONObject(result).getJSONObject("body");
			JSONArray ja = jo.getJSONArray("keys");
			for (int i = 0; i < ja.length(); i++) {
				String issue = ja.getString(i);
				keys.add(issue);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return keys;
	}
    
    public static void main(String[] args) throws Exception {
    	Set<String> groups = VintageConfigUtils.getgroup("10.210.130.47:8090");
    	for(String group:groups){
    		System.out.println(group);
    		System.out.println(VintageConfigUtils.getsign("10.210.130.47:8090", group));
    		Set<String>keys = VintageConfigUtils.getkeys("10.210.130.47:8090", group);
    		for (String key: keys) {
    			System.out.println(key);
    			ConfigInfo ci = VintageConfigUtils.lookup("10.210.130.47:8090", "group="+group+"&key="+key);
    			System.out.println(ci.getNodes()[0].getValue());
    		}
    	}
    }
}
