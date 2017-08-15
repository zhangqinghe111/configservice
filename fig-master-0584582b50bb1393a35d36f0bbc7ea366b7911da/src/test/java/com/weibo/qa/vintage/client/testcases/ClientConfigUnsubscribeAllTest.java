package com.weibo.qa.vintage.client.testcases;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import javax.rmi.CORBA.UtilDelegate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.qa.vintage.config.testcases.BaseTest;
import com.weibo.vintage.client.NamingServiceClient;
import com.weibo.vintage.failover.NodeExciseStrategy;
import com.weibo.vintage.model.NamingServiceNode;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageConfigWebUtils;

public class ClientConfigUnsubscribeAllTest extends BaseTest {
	@Before
	public void setUp() throws Exception {
		groupString = getRandomString(10);
		keyString = getRandomString(5);
		valueString = getRandomString(20);
	}

	@After
	public void tearDown() throws Exception {
		configClient.unsubscribeNodeChanges(groupString, configListener);
		sleep(HEARTBEATINTERVAL);
		changeDataSize = 0;
		groupDataMap.clear();
		groupSizeMap.clear();		
	}

	/**
	 * 前提：没有注册node到节点到cluster && 未订阅cluster 验证：变更
	 */
	@Test
	public void testNoNode() {
		try {
			configClient.unsubscribeAllNodeChanges();
		} catch (Exception e) {
			e.printStackTrace();
			fail("error in testNoNode");
		}
	}

	/**
	 * 测试点：unsubscribe after subscribe 验证：变更
	 */
	@Test
	public void testUnsubAfterSub() {
		String keyString2 = "keystring2";
		try {
			// register one
			configClient.subscribeGroupDataChanges(groupString, configListener);
			VintageConfigWebUtils.register(groupString, keyString, valueString);
			Thread.sleep(3 * HEARTBEATINTERVAL);
			// check
			assertEquals(1, (int) groupSizeMap.get(groupString)); // exception
			// unsubscribeAll
			configClient.unsubscribeAllNodeChanges(groupString);

			// 在取消订阅后，注册节点，则无法收到变更
			VintageConfigWebUtils
					.register(groupString, keyString2, valueString);
			Thread.sleep(3 * HEARTBEATINTERVAL);

			// check
			assertEquals(1, (int) groupSizeMap.get(groupString));
			assertEquals(2, configClient.lookup(groupString).size());

		} catch (Exception e) {
			fail("error in testUnsubAfterSub");
		} finally {
			VintageConfigWebUtils.unregister(groupString, keyString2);
			VintageConfigWebUtils.unregister(groupString, keyString);
		}
	}

	/**
	 * 测试点：subscribe 10 clusters --> unsubscribeAll 验证：变更
	 */
	@Test
	public void testMultiGroups() {
		try {
			// subscribe 10 clusters
			for (int i = 0; i < 10; i++) {
				configClient.subscribeGroupDataChanges(groupString + i,
						configListener);
			}

			// 取消clusterId0的所有节点变更
			configClient.unsubscribeAllNodeChanges(groupString + 0);

			VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
			// 注册节点信息 10 个

			for (int i = 0; i < 10; i++) {
				for (int num = 0; num < 10; num++) {
					VintageConfigWebUtils.register(groupString + i, keyString
							+ num, valueString);
				}
			}
			Thread.sleep(10 * HEARTBEATINTERVAL);

			// nodeMap中有clusterId1~cluster9的信息
			System.out.print(VintageNamingClientUtils.nodeMap.size());
			assertEquals(9, groupSizeMap.size());

			// 验证 nodeMap中每个集群的节点数目为 10个
			for (int i = 1; i < 10; i++) {
				assertEquals(10, (int) groupSizeMap.get(groupString + i));
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("error in testMultiCluster");
		} finally {
			configClient.unsubscribeAllNodeChanges();
			for (int i = 0; i < 10; i++) {
				for (int num = 0; num < 10; num++) {
					VintageConfigWebUtils.unregister(groupString + i, keyString
							+ num);
				}
			}
		}
	}

	/**
	 * 测试点：subscribe 100 clusters --> unsubscribeAll 验证：变更
	 */
	@Test
	public void testUnsub() {
		try {
			changeDataSize = 0;
			groupDataMap.clear();
			groupSizeMap.clear();
			
			// 订阅 100 cluster
			for (int i = 0; i < 10; i++) {
				configClient.subscribeGroupDataChanges(groupString + i,
						configListener);
			}

			VintageNamingClientUtils.sleep(4 * HEARTBEATINTERVAL);
			// 取消订阅所有cluster
			configClient.unsubscribeAllNodeChanges();

			VintageNamingClientUtils.sleep(2 * HEARTBEATINTERVAL);

			// 注册 10 个结点到 10个 cluster
			for (int i = 0; i < 10; i++) {
				for (int num = 0; num < 10; num++) {
					VintageConfigWebUtils.register(groupString + i, keyString
							+ num, valueString);
				}
			}

			Thread.sleep(10 * HEARTBEATINTERVAL);
			System.out.print("\ntest\n");
			assertEquals(0, groupSizeMap.size());

			for (int i = 0; i < 10; i++) {
				assertEquals(10, configClient.lookup(groupString + i).size());
			}

		} catch (Exception e) {
			e.printStackTrace();
			fail("error in testUnsub");
		} finally {
			for (int i = 0; i < 10; i++) {
				for (int num = 0; num < 10; num++) {
					VintageConfigWebUtils.unregister(groupString + i, keyString
							+ num);
				}
			}

		}
	}

	/**
	 * 测试点：重复多次 unsubscribeAll
	 */
	@Test
	public void testRepeatUnsuball() {
		try {
			for (int i = 0; i < 10; i++) {
				configClient.unsubscribeAllNodeChanges(groupString);
			}

			// 多次取消订阅可，可再次订阅
			configClient.subscribeGroupDataChanges(groupString, configListener);

			VintageConfigWebUtils.register(groupString, keyString, valueString);

			Thread.sleep(2 * HEARTBEATINTERVAL);

			assertEquals(1, (int) groupSizeMap.get(groupString));
		} catch (Exception e) {
			e.printStackTrace();
			fail("error in testRepeatUnsuball");
		} finally {
			VintageConfigWebUtils.unregister(groupString, keyString);
			configClient.unsubscribeAllNodeChanges();
		}
	}
}
