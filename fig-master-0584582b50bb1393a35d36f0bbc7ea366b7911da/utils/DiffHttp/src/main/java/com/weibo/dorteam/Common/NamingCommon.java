package com.weibo.dorteam.Common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.weibo.dorteam.Bean.ErrorInfo;
import com.weibo.dorteam.Bean.NamingInfo;
import com.weibo.dorteam.Bean.NamingNodeInfo;
import com.weibo.dorteam.Bean.NamingServiceInfo;

public class NamingCommon {
	
	public static NamingInfo getNamingInfos(String jsonString) throws Exception{
		JSONObject jo = new JSONObject(jsonString);
		NamingInfo issue = (NamingInfo) namingCommon(jo.getString("body"));
		return issue;
	}
	
	public static Object namingCommon(String issueInfo) throws Exception {
		if (issueInfo.contains("error_code")) {
			ErrorInfo error = (ErrorInfo) JsonCommon.getJavabean(issueInfo,
					ErrorInfo.class);
			return error;
		} else {
			NamingInfo serviceInfo = (NamingInfo) JsonCommon.getJavabean(issueInfo, NamingInfo.class);
			return serviceInfo;
		}
	}
	
	public static Set<NamingServiceInfo> getServiceInfos(String jsonString) throws Exception{
		String js = new JSONObject(jsonString).getString("body");
		JSONArray ja = new JSONObject(js).getJSONArray("services");
		Set<NamingServiceInfo> issues = new HashSet<NamingServiceInfo>();
		for (int i = 0; i < ja.length(); i++){
			issues.add((NamingServiceInfo) namingServiceCommon(ja.get(i).toString()));
		}
		return issues;
	}
	
	public static Object namingServiceCommon(String issueInfo) throws Exception {
		if (issueInfo.contains("error_code")) {
			ErrorInfo error = (ErrorInfo) JsonCommon.getJavabean(issueInfo,
					ErrorInfo.class);
			return error;
		} else {
			NamingServiceInfo serviceInfo = (NamingServiceInfo) JsonCommon.getJavabean(issueInfo, NamingServiceInfo.class);
			return serviceInfo;
		}
	}
	
	public static Set<String> getClusterInfos(String jsonString) throws Exception{
		String js = new JSONObject(jsonString).getString("body");
		JSONArray ja = new JSONObject(js).getJSONArray("clusters");
		Set<String> issues = new HashSet<String>();
		for (int i = 0; i < ja.length(); i++){
			issues.add(ja.get(i).toString());
		}
		return issues;
	}
	
	public static Map<String,Set<NamingNodeInfo>> getNamingNodesInfos(String jsonString) throws Exception{
		String js = new JSONObject(jsonString).getString("body");
		JSONObject jo = new JSONObject(js).getJSONObject("nodes");
		Set<NamingNodeInfo> issuew = new HashSet<NamingNodeInfo>();
		Map<String,Set<NamingNodeInfo>> res = new HashMap<String, Set<NamingNodeInfo>>();
		JSONArray jaw = jo.getJSONArray("working");
		for (int i = 0; i < jaw.length(); i++) {
			issuew.add((NamingNodeInfo) namingNodesCommon(jaw.get(i).toString()));
		}
		res.put("working", issuew);
		
		Set<NamingNodeInfo> issueu = new HashSet<NamingNodeInfo>();
		JSONArray jau = jo.getJSONArray("unreachable");
		for (int i = 0; i < jau.length(); i++) {
			issueu.add((NamingNodeInfo) namingNodesCommon(jau.get(i).toString()));
		}
		res.put("unreachable", issueu);
		return res;
	}
	
	public static Object namingNodesCommon(String issueInfo) throws Exception {
		if (issueInfo.contains("error_code")) {
			ErrorInfo error = (ErrorInfo) JsonCommon.getJavabean(issueInfo,
					ErrorInfo.class);
			return error;
		} else {
			NamingNodeInfo serviceInfo = (NamingNodeInfo) JsonCommon.getJavabean(issueInfo, NamingNodeInfo.class);
			return serviceInfo;
		}
	}
}
