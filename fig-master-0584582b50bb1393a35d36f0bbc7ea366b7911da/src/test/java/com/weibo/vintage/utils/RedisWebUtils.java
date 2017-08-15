package com.weibo.vintage.utils;

import com.weibo.vintage.utils.ApacheHttpClient;

/*
 * Control redis: start and stop and flushall
 */
public class RedisWebUtils {
	
	private static final String IP = VintageConstantsTest.ApacheIp;
	private static final String PORT = VintageConstantsTest.ApachePort;
	private static final String RedisStart_URL = "http://" + IP + ":" + PORT
			+ "/configserver/startredis.php";
	private static final String RedisStop_URL = "http://" + IP + ":" + PORT
			+ "/configserver/stopredis.php";

	private static ApacheHttpClient httpclient = new ApacheHttpClient(150,
			3000, 3000, 1024 * 1024);

	/*
	 * start the redis through httpclient
	 */
	public static void StartRedis() {
		String url = RedisStart_URL;
		httpclient.getAsync(url);
	}

	/*
	 * stop the redis through httpclient
	 */
	public static void StopRedis() {
		String url = RedisStop_URL;
		httpclient.getAsync(url);
	}

}
