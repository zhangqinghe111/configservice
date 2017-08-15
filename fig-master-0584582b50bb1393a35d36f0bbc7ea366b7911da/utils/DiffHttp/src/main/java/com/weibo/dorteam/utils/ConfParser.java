package com.weibo.dorteam.utils;

import java.io.IOException;
import java.util.Properties;


/**
 * add by liuyu9
 * change the constants.xml to the conf.properties
 * */
public class ConfParser {
	private Properties properties = new Properties();
	
	public String getParameter(String para){
		try {
			properties.load(getClass().getResourceAsStream("/conf.properties"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		return properties.getProperty(para);
	}
	
	public static void main(String[] args){
		ConfParser vt = new ConfParser();
		System.out.println(vt.getParameter("hostA"));
	}
	
}
