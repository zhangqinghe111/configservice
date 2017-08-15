package com.weibo.vintage.utils;

public class VintageConstantsTest {

	private static ConfParser confParser =  new ConfParser();
	public static final String IP = confParser.getParameter("serverIP");
	public static final String PORT = confParser.getParameter("serverPort");
	public static final String ADPORT = confParser.getParameter("adminPort");
	public static final String REDIS_IP = confParser.getParameter("redisMIP");
	public static final int REDIS_PORT = Integer.valueOf(confParser.getParameter("redisMPort"));
	public static final String REDIS_SLAVE_IP = confParser.getParameter("redisSIP");
	public static final int REDIS_SLAVE_PORT = Integer.valueOf(confParser.getParameter("redisSPort"));
	public static final int Server_Cache_Write_Time = Integer.valueOf(confParser.getParameter("servercacheWritetime"));
	public static final int Server_Cache_Visit_Time = Integer.valueOf(confParser.getParameter("servercachevisittime"));
	public static final int serviceCacheInterval = Integer.valueOf(confParser.getParameter("serviceCacheInterval"));
	public static final int HEARTBEATINTERVAL = Integer.valueOf(confParser.getParameter("HEARTBEATINTERVAL"));
	public static final int snapInterval = Integer.valueOf(confParser.getParameter("snapInterval"));
	public static final String ApacheIp = confParser.getParameter("apacheIP");
	public static final String ApachePort = confParser.getParameter("apachePort");
	public static final String VINTAGE_NAMING_SERVICE_URL = "http://" + IP + ":" + PORT + "/naming/service";
	public static final String VINTAGE_NAMING_SMARTSERVICE_URL = "http://" + IP + ":" + PORT + "/naming/smartservice";
	public static final String VINTAGE_NAMING_ADMIN_URL = "http://" + IP + ":"
			+ PORT + "/naming/admin";
	public static final String VINTAGE_NAMING_WHITELIST_URL = "http://" + IP
			+ ":" + PORT + "/naming/whitelist";
	public static final String VINTAGE_STATICS_CONFIG_URL = "http://" + IP
			+ ":" + PORT + "/1/config/service";

	public static final String VINTAGE_NAMING_MCQ_SERVICE_URL = "http://" + IP
			+ ":" + PORT + "/naming/resources/mcq";
	public static final String VINTAGE_NAMING_MCQ_ADMIN_URL = "http://" + IP
			+ ":" + PORT + "/naming/admin/mcq";
	public static final String VINTAGE_NAMING_MCQ_WHITELIST_URL = "http://"
			+ IP + ":" + PORT + "/naming/whitelist/mcq";
	
}
