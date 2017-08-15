package com.weibo.qa.vintage.online.naming.testcases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.weibo.qa.vintage.liuyu.testcases.BaseTest;
import com.weibo.vintage.client.NamingServiceClient;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.failover.NodeExciseStrategy;
import com.weibo.vintage.model.NamingServiceInfo;
import com.weibo.vintage.model.NamingServiceType;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;

public class NaminggetSignTest extends BaseTest{

	private String extinfo = "ext";
	private int port = 1111;
	private static String serviceId;
	private static String clusterId;
	
	@BeforeClass
	public static void init(){
//		serviceId = "vintage-test-qa-liuyu9-"+getRandomString(5);
		serviceId = "vintage-test-qa-liuyu9-test";
		clusterId = "com.weibo.vintage.test.qa.liuyu9.RandomService."+getRandomString(10);		
	}
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
		initENV(serviceId, clusterId);
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
		cleanENV(serviceId, clusterId);
		VintageNamingWebUtils.deleteCluster(serviceId, clusterId);
	}
	
	@Test
	/*
	 * compare the sign results between getsign and lookup
	 */
	public void testGetSignLookup() {
		try {
			String lookup_sign = lookupsign(serviceId, clusterId);
			String getsign_sign = getsign(serviceId, clusterId);

			assertEquals(getsign_sign, lookup_sign);

			VintageNamingWebUtils.register(serviceId, clusterId, localIP, port,
					serviceId);
			lookup_sign = lookupsign(serviceId, clusterId);
			getsign_sign = getsign(serviceId, clusterId);

			assertEquals(getsign_sign, lookup_sign);
		} finally {
			VintageNamingWebUtils.unregister(serviceId, clusterId, localIP, port);
		}

	}
	
	@Test
	/*
	 * compare the sign results between getsign and lookup
	 */
	public void testexteninfo1() {
		try {
			String lookup_sign = lookupsign(serviceId, clusterId);
			String getsign_sign = getsign(serviceId, clusterId);

			assertEquals(getsign_sign, lookup_sign);

			VintageNamingWebUtils.register(serviceId, clusterId, localIP, port,
					extinfo);
			lookup_sign = lookupsign(serviceId, clusterId);
			getsign_sign = getsign(serviceId, clusterId);

			assertEquals(getsign_sign, lookup_sign);
		} finally {
			VintageNamingWebUtils.unregister(serviceId, clusterId, localIP, port);
		}

	}
	
	@Test
	public void testReg() {
		try {
			String sign1 = getsign(serviceId, clusterId);
			VintageNamingWebUtils.register(serviceId, clusterId, localIP, port,
					extinfo);

			String sign2 = getsign(serviceId, clusterId);
			assertFalse(sign1.equals(sign2));

			String oldSignString = sign2;

			// register in a row
			for (int i = 0; i < 5; i++) {
				port++;

				VintageNamingWebUtils.register(serviceId, clusterId, localIP, port,
						serviceId);
				String newsignString = getsign(serviceId, clusterId);
				System.out.println(newsignString);
				assertFalse(newsignString.equals(oldSignString));

				oldSignString = newsignString;
			}

			// repeat register
			for (int i = 0; i < 5; i++) {
				VintageNamingWebUtils.register(serviceId, clusterId, localIP, port,
						serviceId);
				String newsignString = getsign(serviceId, clusterId);
				assertEquals(newsignString, oldSignString);

				oldSignString = newsignString;
			}

			// unregister in row
			for (int i = 0; i < 5; i++) {
				VintageNamingWebUtils.unregister(serviceId, clusterId, localIP,
						port);
				String newsignString = getsign(serviceId, clusterId);
				assertFalse(newsignString.equals(oldSignString));

				oldSignString = newsignString;
				port--;
			}

			assertEquals(sign2, oldSignString);

			// unregister the last
			VintageNamingWebUtils.unregister(serviceId, clusterId, localIP, port);
			String newsignString = getsign(serviceId, clusterId);
			assertEquals(sign1, newsignString);
		} catch (VintageException ex) {
			System.out.print(ex.getMessage());
			// TODO: handle exception
		} finally {
			VintageNamingWebUtils.unregister(serviceId, clusterId, localIP, port);

			for (int i = 0; i < 5; i++) {
				port++;
				VintageNamingWebUtils.unregister(serviceId, clusterId, localIP,
						port);
			}
		}
	}

	@Test
	public void testRegUnreg() {
		try {
			VintageNamingWebUtils.register(serviceId, clusterId, localIP, port,
					extinfo);
			String oldsignString = getsign(serviceId, clusterId);
			VintageNamingWebUtils.unregister(serviceId, clusterId, localIP, port);
			VintageNamingWebUtils.register(serviceId, clusterId, localIP, port,
					extinfo);
			String newsignString = getsign(serviceId, clusterId);

			assertEquals(oldsignString, newsignString);

			// register node2
			VintageNamingWebUtils.unregister(serviceId, clusterId, localIP, port);
			VintageNamingWebUtils.register(serviceId, clusterId, localIP, port + 1,
					extinfo);
			String newsignString2 = getsign(serviceId, clusterId);

			assertFalse(newsignString2.equals(newsignString));
		} finally {
			VintageNamingWebUtils.unregister(serviceId, clusterId, localIP, port);
			VintageNamingWebUtils.unregister(serviceId, clusterId, localIP,
					port + 1);
		}

	}

	protected void initENV(String service, String cluster) {
		if (! VintageNamingWebUtils.existsService(service)){
			VintageNamingWebUtils.addService(service, NamingServiceType.statics.toString());
		}
		if (! VintageNamingWebUtils.existCluster(service, cluster)) {
			VintageNamingWebUtils.addCluster(service, cluster);
		}
		VintageNamingWebUtils.addWhitelist(service, localNodes);
		
	}

	protected void cleanENV(String service, String cluster) {
		try {
			delWhiteList(service, localNodes);
		} catch (VintageException e) {
			e.printStackTrace();
		}
	}
	
	private String getsign(String service, String cluster) {
		return VintageNamingWebUtils.getsign(service, cluster);
	}

	private String lookupsign(String service, String cluster) {
		return VintageNamingWebUtils.lookupSign(service, cluster);
	}
}
