package com.weibo.dorteam.Common;

import org.json.JSONObject;

import com.weibo.dorteam.Bean.ConfigInfo;
import com.weibo.dorteam.Bean.ErrorInfo;


public class ConfigCommon {

	public static ConfigInfo getConfigInfos(String jsonString) throws Exception{
		JSONObject jo = new JSONObject(jsonString);
		ConfigInfo issue = (ConfigInfo) configCommon(jo.getString("body"));
		return issue;
	}
	
	public static Object configCommon(String issueInfo) throws Exception {
		if (issueInfo.contains("error_code")) {
			ErrorInfo error = (ErrorInfo) JsonCommon.getJavabean(issueInfo,
					ErrorInfo.class);
			return error;
		} else {
			ConfigInfo serviceInfo = (ConfigInfo) JsonCommon.getJavabean(issueInfo, ConfigInfo.class);
			return serviceInfo;
		}
	}
}
