package com.weibo.qa.vintage.naming.testcases;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;
import com.weibo.vintage.client.NamingServiceClient;
import com.weibo.vintage.exception.ExcepFactor;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.failover.NodeExciseStrategy;
import com.weibo.vintage.model.NamingServiceType;

/**
 * getsign
 * 
 * @author huqian2
 * 
 */
public class GetSignTest extends BaseTest {

//	private NamingServiceClient client;

	private Set<String> wNode = new HashSet<String>();

	private String signService = "signservice";
	private String signCluster = "signcluster";
	private String noexist_service="noexist_service";
	private String noexist_cluster="noexist_cluster";
	private String extinfo = "ext";
	private String extinfoA = "\"extA\"";
	private int port = 1111;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		
		signService = getRandomString(10);
		signCluster = getRandomString(20);
		noexist_service=getRandomString(10);
		noexist_cluster=getRandomString(20);
		
		init(signService, signCluster);
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
//		clean(signService, signCluster);
	}

	protected void init(String service, String cluster) {
		addService(service, NamingServiceType.statics.toString());
		addWhiteList(service, localNodes);
		addCluster(service, cluster);
	}

	protected void clean(String service, String cluster) {
		try {
			delWhiteList(service, localNodes);
			delCluster(service, cluster);
			delService(service);
		} catch (VintageException e) {
			e.printStackTrace();
		}
	}

	private void deletewhitelist(String service, Set<String> nodeSet) {
		VintageNamingWebUtils.deleteWhitelist(service, nodeSet);
	}

	private void deletecluster(String service, String cluster) {
		VintageNamingWebUtils.deleteCluster(service, cluster);
	}

	private void deleteservice(String service) {
		VintageNamingWebUtils.deleteService(service);
	}

	private String getsign(String service, String cluster) {
		return VintageNamingWebUtils.getsign(service, cluster);
	}

	private String lookupsign(String service, String cluster) {
		return VintageNamingWebUtils.lookupSign(service, cluster);
	}

	@Test
	/*
	 * compare the sign results between getsign and lookup
	 */
	public void testGetSignLookup() {
		try {
			String lookup_sign = lookupsign(signService, signCluster);
			String getsign_sign = getsign(signService, signCluster);

			assertEquals(getsign_sign, lookup_sign);

			VintageNamingWebUtils.register(signService, signCluster, localIP, port,
					signService);

			lookup_sign = lookupsign(signService, signCluster);
			getsign_sign = getsign(signService, signCluster);

			assertEquals(getsign_sign, lookup_sign);
		} finally {
			VintageNamingWebUtils.unregister(signService, signCluster, localIP, port);
		}

	}

	@Test
	/*
	 * compare the sign results between getsign and lookup
	 */
	public void testexteninfo1() {
		try {
			String lookup_sign = lookupsign(signService, signCluster);
			String getsign_sign = getsign(signService, signCluster);

			assertEquals(getsign_sign, lookup_sign);

			VintageNamingWebUtils.register(signService, signCluster, localIP, port,
					extinfo);

			lookup_sign = lookupsign(signService, signCluster);
			getsign_sign = getsign(signService, signCluster);

			assertEquals(getsign_sign, lookup_sign);
		} finally {
			VintageNamingWebUtils.unregister(signService, signCluster, localIP, port);
		}
	}
	
	@Test
	public void testexteninfo2()
	{
		try {
			
			VintageNamingWebUtils.register(signService, signCluster, localIP, port, extinfo);
			String sign1 = getsign(signService, signCluster);
			//modify the extinfo 
			VintageNamingWebUtils.register(signService, signCluster, localIP, port, extinfoA);
			String sign2 = getsign(signService, signCluster);
			
			assertFalse(sign1.equals(sign2));
		} finally{
			VintageNamingWebUtils.unregister(signService, signCluster, localIP, port);
		}
		
	}
	
	@Test
	public void testReg() {
		try {
			String sign1 = getsign(signService, signCluster);
			VintageNamingWebUtils.register(signService, signCluster, localIP, port,
					extinfo);

			String sign2 = getsign(signService, signCluster);
			assertFalse(sign1.equals(sign2));

			String oldSignString = sign2;

			// register in a row
			for (int i = 0; i < 5; i++) {
				port++;

				VintageNamingWebUtils.register(signService, signCluster, localIP, port,
						signService);
				String newsignString = getsign(signService, signCluster);
				System.out.println(newsignString);
				assertFalse(newsignString.equals(oldSignString));

				oldSignString = newsignString;
			}

			// repeat register
			for (int i = 0; i < 5; i++) {
				VintageNamingWebUtils.register(signService, signCluster, localIP, port,
						signService);
				String newsignString = getsign(signService, signCluster);
				assertEquals(newsignString, oldSignString);

				oldSignString = newsignString;
			}

			// unregister in row
			for (int i = 0; i < 5; i++) {
				VintageNamingWebUtils.unregister(signService, signCluster, localIP,
						port);
				String newsignString = getsign(signService, signCluster);
				assertFalse(newsignString.equals(oldSignString));

				oldSignString = newsignString;
				port--;
			}

			assertEquals(sign2, oldSignString);

			// unregister the last
			VintageNamingWebUtils.unregister(signService, signCluster, localIP, port);
			String newsignString = getsign(signService, signCluster);
			assertEquals(sign1, newsignString);
		} catch (VintageException ex) {
			System.out.print(ex.getMessage());
			// TODO: handle exception
		} finally {
			VintageNamingWebUtils.unregister(signService, signCluster, localIP, port);

			for (int i = 0; i < 5; i++) {
				port++;
				VintageNamingWebUtils.unregister(signService, signCluster, localIP,
						port);
			}
		}
	}

	@Test
	public void testRegUnreg() {
		try {
			VintageNamingWebUtils.register(signService, signCluster, localIP, port,
					extinfo);
			String oldsignString = getsign(signService, signCluster);
			VintageNamingWebUtils.unregister(signService, signCluster, localIP, port);
			VintageNamingWebUtils.register(signService, signCluster, localIP, port,
					extinfo);
			String newsignString = getsign(signService, signCluster);

			assertEquals(oldsignString, newsignString);

			// register node2
			VintageNamingWebUtils.unregister(signService, signCluster, localIP, port);
			VintageNamingWebUtils.register(signService, signCluster, localIP, port + 1,
					extinfo);
			String newsignString2 = getsign(signService, signCluster);

			assertFalse(newsignString2.equals(newsignString));
		} finally {
			VintageNamingWebUtils.unregister(signService, signCluster, localIP, port);
			VintageNamingWebUtils.unregister(signService, signCluster, localIP,
					port + 1);
		}

	}

	@Test
	public void testRepeatUnreg() {
		try {
			for (int i = 0; i < 2; i++) {
				VintageNamingWebUtils.register(signService, signCluster, localIP, port + i, extinfo);
			}
			// unregister the first node
			VintageNamingWebUtils.unregister(signService, signCluster, localIP, port);
			String oldSign = getsign(signService, signCluster);
			
			for (int i = 0; i < 5; i++) {
				VintageNamingWebUtils.unregister(signService, signCluster, localIP,
						port);
				String newSign = getsign(signService, signCluster);
				assertEquals(oldSign, newSign);
			}

			// unregister the last node
			VintageNamingWebUtils.unregister(signService, signCluster, localIP, port + 1);
			oldSign = getsign(signService, signCluster);

			for (int i = 0; i < 5; i++) {
				VintageNamingWebUtils.unregister(signService, signCluster, localIP,
						port + 1);
				String newSign = getsign(signService, signCluster);
				assertEquals(oldSign, newSign);
			}
		} finally {
			for (int i = 0; i < 2; i++) {
				if (VintageNamingWebUtils.existsService(signService) && VintageNamingWebUtils.existCluster(signService, signCluster)) {
				VintageNamingWebUtils.unregister(signService, signCluster, localIP,
						port + i);
				}
			}
		}

	}

	@Test
	public void testMultiCluster() {
		String clusterString = signCluster + 1;
		try {
			init(signService, clusterString);

			String sign1 = getsign(signService, signCluster);
			String sign2 = getsign(signService, clusterString);

			assertEquals(sign1, sign2);

			for (int i = 0; i < 5; i++) {
				VintageNamingWebUtils.register(signService, signCluster, localIP, port
						+ i, extinfo);
				VintageNamingWebUtils.register(signService, clusterString, localIP,
						port + i, extinfo);
			}
			sign1 = getsign(signService, signCluster);
			sign2 = getsign(signService, clusterString);

			assertEquals(sign1, sign2);

		} finally {
			for (int i = 0; i < 5; i++) {
				VintageNamingWebUtils.unregister(signService, signCluster, localIP,
						port + i);
				VintageNamingWebUtils.unregister(signService, clusterString, localIP,
						port + i);
			}
			deletecluster(signService, clusterString);
		}

	}

	@Test
	public void testMultiClusterDiff() {
		String clusterString = signCluster + 1;
		try {
			init(signService, clusterString);
			String sign1 = getsign(signService, signCluster);
			String sign2 = getsign(signService, clusterString);

			assertEquals(sign1, sign2);

			// register diff node
			VintageNamingWebUtils.register(signService, signCluster, localIP, port,
					extinfo);
			VintageNamingWebUtils.register(signService, clusterString, localIP,
					port + 1, extinfo);

			// register same node
			VintageNamingWebUtils.register(signService, signCluster, localIP, port + 2,
					extinfo);
			VintageNamingWebUtils.register(signService, clusterString, localIP,
					port + 2, extinfo);
			sign1 = getsign(signService, signCluster);
			sign2 = getsign(signService, clusterString);

			assertFalse(sign1.equals(sign2));

		} finally {
			VintageNamingWebUtils.unregister(signService, signCluster, localIP, port);
			VintageNamingWebUtils.unregister(signService, signCluster, localIP,
					port + 2);
			VintageNamingWebUtils.unregister(signService, clusterString, localIP,
					port + 1);
			VintageNamingWebUtils.unregister(signService, clusterString, localIP,
					port + 2);
			deletecluster(signService, clusterString);
		}

	}

	@Test
	public void testMultiServiceMultiCluster() {
		try {
			for (int i = 0; i < 2; i++) {
				for (int j = 0; j < 2; j++) {
					init(signService + i, signCluster + j);
				}
			}

			// register same node to same cluster of the different service
			VintageNamingWebUtils.register(signService + 0, signCluster + 0, localIP,
					port, extinfo);
			VintageNamingWebUtils.register(signService + 1, signCluster + 0, localIP,
					port, extinfo);
			String sign1 = getsign(signService + 0, signCluster + 0);
			String sign2 = getsign(signService + 1, signCluster + 0);
			assertEquals(sign1, sign2);

			// register different node to same cluster of the different service
			VintageNamingWebUtils.register(signService + 0, signCluster + 1, localIP,
					port + 1, extinfo);
			VintageNamingWebUtils.register(signService + 1, signCluster + 1, localIP,
					port, extinfo);
			String sign3 = getsign(signService + 0, signCluster + 1);
			String sign4 = getsign(signService + 1, signCluster + 1);
			assertFalse(sign3.equals(sign4));

			// register same node to diff cluster of diff service
			assertEquals(sign1, sign4);

			// register diff node to diff cluster of diff service
			assertFalse(sign2.equals(sign3));

		} finally {
			VintageNamingWebUtils.unregister(signService + 0, signCluster + 0, localIP,
					port);
			VintageNamingWebUtils.unregister(signService + 1, signCluster + 0, localIP,
					port);
			VintageNamingWebUtils.unregister(signService + 0, signCluster + 1, localIP,
					port + 1);
			VintageNamingWebUtils.unregister(signService + 1, signCluster + 1, localIP,
					port);

			deletecluster(signService + 0, signCluster + 0);
			clean(signService + 0, signCluster + 1);
			deletecluster(signService + 1, signCluster + 0);
			clean(signService + 1, signCluster + 1);
		}

	}
	
	@Test
	public void testModifyExtInfo()
	{
		try {
			String extinfo_another= "extinfo_modify";
			
			VintageNamingWebUtils.register(signService, signCluster, localIP, port, extinfo);
			String sign1 = getsign(signService, signCluster);
			//modify the extinfo 
			VintageNamingWebUtils.register(signService, signCluster, localIP, port, extinfo_another);
			String sign2 = getsign(signService, signCluster);
			
			assertFalse(sign1.equals(sign2));
		} finally{
			VintageNamingWebUtils.unregister(signService, signCluster, localIP, port);
		}
		
	}
	
	@Test
	public void testNoexist()
	{
		try {
			String sign = getsign(signService, noexist_cluster);
			System.out.print(sign);
//			fail("");
		} catch (VintageException ex) {
			// TODO: handle exception
			assertEquals(ExcepFactor.E_CLUSTER_ID_NOT_EXISTS, ex.getFactor());
		}
		
		try {
			getsign(noexist_service, signCluster);
//			fail("");
		} catch (VintageException ex) {
			// TODO: handle exception
			assertEquals(ExcepFactor.E_SERVICE_ID_NOT_EXISTS, ex.getFactor());
		}
		
	}
	
	@Test
	public void testNullPara()
	{
		try {
			String sign = getsign(null, signCluster);
			System.out.print(sign);
//			fail("");
		} catch (VintageException ex) {
			// TODO: handle exception
//			assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR, ex.getFactor());
		}
		
		try {
			String signString = getsign(signService, null);
			System.out.print(signString);
//			fail("");
		} catch (VintageException ex) {
			// TODO: handle exception
//			assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR, ex.getFactor());
		}
	}

}
