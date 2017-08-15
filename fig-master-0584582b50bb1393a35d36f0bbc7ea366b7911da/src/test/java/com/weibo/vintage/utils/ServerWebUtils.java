package com.weibo.vintage.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.weibo.vintage.utils.ApacheHttpClient;
import com.weibo.vintage.utils.UrlHelper;

/*
 * control the node status 
 * for all functions:
 * num=10,nodes:6071-6080
 * num=20,nodes:6081-6100
 * num=30,nodes:6071-6100
 */
public class ServerWebUtils {
	private static final String IP = VintageConstantsTest.ApacheIp;
	private static final String PORT = VintageConstantsTest.ApachePort;
	private static final String Status200_URL = "http://" + IP + ":" + PORT
			+ "/200.php";
	private static final String Status404_URL = "http://" + IP + ":" + PORT
			+ "/404.php";
	private static final String Status503_URL = "http://" + IP + ":" + PORT
			+ "/503.php";
	private static final String StatusSlow_URL = "http://" + IP + ":" + PORT
			+ "/slow.php";
	private static final String Kill_URL = "http://" + IP + ":" + PORT
			+ "/kill.php";
	private static final String Status8080_URL = "http://" + IP + ":" + PORT
			+ "/8080.php";
	private static final String Status8090_URL = "http://" + IP + ":" + PORT
			+ "/8090.php";
	private static final String HeartBeat_URL = "http://" + IP + ":" + PORT
			+ "/configserver/heartbeat.php";
	private static final String setHeartBeatHost_URL = "http://" + IP + ":" + PORT
			+ "/configserver/setHeartbeatHost.php";
	
	static ApacheHttpClient httpclient = new ApacheHttpClient(150, 3000, 3000,
			1024 * 1024);

	public static boolean Status200(int num) {
		String url = Status200_URL + "?"
				+ UrlHelper.buildUrlParams("num", String.valueOf(num));
		httpclient.getAsync(url);
		System.out.println(url);
		return true;
	}

	public static boolean Status404(int num) {
		String url = Status404_URL + "?"
				+ UrlHelper.buildUrlParams("num", String.valueOf(num));
		httpclient.getAsync(url);
		System.out.println(url);
		return true;
	}

	public static boolean Status503(int num) {
		String url = Status503_URL + "?"
				+ UrlHelper.buildUrlParams("num", String.valueOf(num));
		httpclient.getAsync(url);
		System.out.println(url);
		return true;
	}

	public static boolean Status8080() {
		String url = Status8080_URL;
		httpclient.getAsync(url);
		System.out.println(url);
		return true;
	}

	public static boolean Status8090() {
		String url = Status8090_URL;
		httpclient.getAsync(url);
		System.out.println(url);
		return true;
	}

	public static boolean StatusSlow(int num) {
		String url = StatusSlow_URL + "?"
				+ UrlHelper.buildUrlParams("num", String.valueOf(num));
		httpclient.getAsync(url);
		System.out.println(url);
		return true;
	}

	public static boolean kill(int num) {
		String url = Kill_URL + "?"
				+ UrlHelper.buildUrlParams("num", String.valueOf(num));
		httpclient.getAsync(url);
		System.out.println(url);
		return true;
	}

	/*
	 * control the dynamic detect switch
	 */
	public static boolean dynamicDetect(String value) {
		String switchString = "feature.configserver.dynamicDetect";
		ServerWebUtils.SetSwitch(switchString, value);
		return true;
	}

	public static boolean activeDetect(String value) {
		String switchString = "feature.configserver.active.detecting";
		ServerWebUtils.SetSwitch(switchString, value);
		return true;
	}

	public static boolean whitelist(String value) {
		String switchString = "feature.configserver.whitelist";
		ServerWebUtils.SetSwitch(switchString, value);
		return true;
	}

	public static boolean Heartbeat(String value) {
		String switchString = "feature.configserver.heartbeat";
		ServerWebUtils.SetSwitch(switchString, value);
		return true;
	}
	
	public static boolean setHeartbeatHost(String host) {
		String url = setHeartBeatHost_URL + "?"
				+ UrlHelper.buildUrlParams("host", host);
		httpclient.getAsync(url);
		System.out.println(url);
		return true;	
	}

	public static boolean HeartbeatProtection(String value) {
		String switchString = "feature.configserver.heartbeat.protection";
		ServerWebUtils.SetSwitch(switchString, value);
		return true;
	}
	
	public static boolean ClusterCache(String value) {
		String switchString = "feature.configserver.cluster.localcache";
		ServerWebUtils.SetSwitch(switchString, value);
		return true;
	}
	
	public static boolean GetNodeFromMaster(String value) {
		String switchString = "feature.configserver.getnodelist.frommaster";
		ServerWebUtils.SetSwitch(switchString, value);
		return true;
	}
	
	public static void SetSwitch(String switchString, String value) {
		try {
			ArrayList cmdsList = new ArrayList();
			cmdsList.add("sh");
			cmdsList.add("-c");
			String cmd = "printf '"+value+" resource "+switchString+" \r\n' | nc "+ VintageConstantsTest.IP +" "+VintageConstantsTest.ADPORT;
			cmdsList.add(cmd);
			int size=cmdsList.size();  
	        String[] cmds = (String[])cmdsList.toArray(new String[size]); 
			InputStream in = null;
			Process pro = Runtime.getRuntime().exec(cmds);
			pro.waitFor();  
	        in = pro.getInputStream();  
	        BufferedReader read = new BufferedReader(new InputStreamReader(in));  
	        String result = read.readLine();  
	        System.out.println(cmd);
	        System.out.println(result);
        
		} catch (Exception e){
			System.out.println(e.getMessage());
		}
	}
	
	public static String HeartBeatStatus(String serviceId, String clusterId, String start, String num, String type){
		String url = HeartBeat_URL + "?"
				+ UrlHelper.buildUrlParams("service", serviceId) + "&"
				+ UrlHelper.buildUrlParams("cluster", clusterId) + "&"
				+ UrlHelper.buildUrlParams("port", start) + "&"
				+ UrlHelper.buildUrlParams("num", num) + "&"
				+ UrlHelper.buildUrlParams("type", type);
		System.out.println(url);
		return httpclient.getAsync(url);
	}
	
	public static String stopHeartBeat(String type){
		String url = "http://" + IP + ":" + PORT
				+ "/configserver/stopHeartBeat.php?"
				+  UrlHelper.buildUrlParams("type", type);
		System.out.println(url);
		return httpclient.getAsync(url);
	}


	public static String getservercache(String key) {
		String result = "";
		try {
			ArrayList cmdsList = new ArrayList();
			cmdsList.add("sh");
			cmdsList.add("-c");
			String cmd = "printf 'hotLocalCache getIfPresent "+key+" \r\n' | nc "+ VintageConstantsTest.IP +" "+VintageConstantsTest.ADPORT;
			cmdsList.add(cmd);
			int size=cmdsList.size();  
	        String[] cmds = (String[])cmdsList.toArray(new String[size]); 
			InputStream in = null;
			Process pro = Runtime.getRuntime().exec(cmds);
			pro.waitFor();  
	        in = pro.getInputStream();  
	        BufferedReader read = new BufferedReader(new InputStreamReader(in));  
	        result = read.readLine();  
	        System.out.println(cmd);
	        System.out.println(result);
        
		} catch (Exception e){
			System.out.println(e.getMessage());
		}
		return result;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ServerWebUtils.HeartbeatProtection("on");
	}
}
