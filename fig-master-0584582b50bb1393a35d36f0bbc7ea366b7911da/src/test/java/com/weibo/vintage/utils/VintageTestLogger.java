package com.weibo.vintage.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VintageTestLogger {
	private static final Logger infoLog = LoggerFactory.getLogger("test_info");
	private static final Logger errorLog = LoggerFactory.getLogger("test_error");
	private static final Logger accessLog = LoggerFactory.getLogger("test_access");
	private static final Logger nodeLog = LoggerFactory.getLogger("test_node");
	
	public static void info (StringBuilder msg) {
		info(msg.toString());
	}
	public static void info (String msg) {
		infoLog.info(msg);
	}
	public static void error(StringBuilder msg){
		error(msg.toString());
	}
	public static void error(String msg) {
		errorLog.error(msg);
	}
	public static void accessLog (StringBuilder msg) {
		accessLog(msg.toString());
	}
	public static void accessLog (String msg) {
		accessLog.info(msg);
	}
	
	public static void nodeLog (StringBuilder msg) {
		nodeLog(msg.toString());
	}
	public static void nodeLog(String msg){
		nodeLog.info(msg);
	}
}
