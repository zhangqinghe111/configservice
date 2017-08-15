package com.weibo.dorteam.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

import com.weibo.vintage.exception.HttpRequestException;
import com.weibo.vintage.failover.NodeExciseStrategy;
import com.weibo.vintage.model.ResponsePacket;
import com.weibo.vintage.utils.VintageConstants;
import com.weibo.vintage.utils.VintageLogger;
import com.weibo.vintage.utils.VintageUtils;

public class httpClient {

	private static HttpClient client = new DefaultHttpClient();
	private static Logger log = Logger.getLogger(httpClient.class);
	
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
		log.info("curl \"" + localurl + "\" -d \"" + parameters + "\"");
//		System.out.println("curl \"" + localurl + "\" -d \"" + parameters + "\"");	
		String result = sb.toString();
//		System.out.println(result);
		log.info(result);
//		System.out.println("-----------------------------------------------");
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
		log.info("curl \"" + localurl + "\"");
//		System.out.println("curl \"" + localurl + "\"");	
		String result = sb.toString();
		log.info(result);
//		System.out.println(result);
//		System.out.println("-----------------------------------------------");
		httpget.releaseConnection();
		return result;	
	}
	
	public static String lookupforupdate(String relativeurl, String parameters) {
		
		try {
			BufferedReader in = null;
			
			if(relativeurl == null || relativeurl.equals(""))
				return null;
			String localurl = relativeurl + "?" + parameters;
			HttpGet httpget = new HttpGet(localurl);
			HttpResponse response = client.execute(httpget);
			
			if (response == null){
				throw new HttpRequestException(
				"Fail to lookup for update, case result message is null! ");
			} else if (response.getStatusLine().getStatusCode() == 304) {
				return null;
			} else {
				in = new BufferedReader(new InputStreamReader(response.getEntity()
						.getContent()));
				StringBuffer sb = new StringBuffer("");
				String line = "";
				String NL = System.getProperty("line.separator");
				while ((line = in.readLine()) != null) {
					sb.append(line + NL);
				}
				in.close();
				log.info("curl \"" + localurl + "\"");
//				System.out.println("curl \"" + localurl + "\"");	
				String result = sb.toString();
				log.info(result);
				httpget.releaseConnection();
				return result;	
			
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
}
