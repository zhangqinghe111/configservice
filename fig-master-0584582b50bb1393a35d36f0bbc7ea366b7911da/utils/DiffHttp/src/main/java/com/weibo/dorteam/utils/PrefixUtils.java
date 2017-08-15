package com.weibo.dorteam.utils;

public class PrefixUtils {
	private static final String VINTAGE_PROTOCOL_PREFIX = "http://";
	private static final String VINTAGE_NAMING_ADMIN_SUFFIX = "/naming/admin";
	private static final String VINTAGE_NAMING_SERVICE_SUFFIX = "/naming/service";
	private static final String VINTAGE_NAMING_WHITELIST_SUFFIX = "/naming/whitelist";
	
	private static final String VINTAGE_CONFIG_SUFFIX = "/1/config/service";
	
	public String getUrlPrefix(String host, String prefix) {
		switch(prefix) {
		case "naming_admin":
			return VINTAGE_PROTOCOL_PREFIX + host + VINTAGE_NAMING_ADMIN_SUFFIX;
		case "naming_service":
			return VINTAGE_PROTOCOL_PREFIX + host + VINTAGE_NAMING_SERVICE_SUFFIX;
		case "naming_whitelist":
			return VINTAGE_PROTOCOL_PREFIX + host + VINTAGE_NAMING_WHITELIST_SUFFIX;
		case "config":
			return VINTAGE_PROTOCOL_PREFIX + host + VINTAGE_CONFIG_SUFFIX;
		default:
			return "Error: The prefix is incorrect";
		}
	}

	public static void main(String[] args){
		PrefixUtils cu = new PrefixUtils();
		ConfParser cp = new ConfParser();
		
		System.out.println(cu.getUrlPrefix(cp.getParameter("hostA"), "naming_admin"));
	}
}
