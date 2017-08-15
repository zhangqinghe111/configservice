package com.weibo.qa.vintage.client.testcases;

import static org.junit.Assert.*;

import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.weibo.qa.vintage.config.testcases.BaseTest;
import com.weibo.vintage.exception.ExcepFactor;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.listener.ConfigServiceChangeListener;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageConfigWebUtils;

/**
 * 此类为订阅功能相关测试用例
 * 
 * 验证方式：心跳线程数据验证 + lookup出来的数据验证 + 两者一致性验证
 * 
 * @author huqian
 * 
 */
public class ClientConfigSubscribeTest extends BaseTest {
	private String groupString = "subscribeString";

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
		groupSizeMap.clear();
		groupDataMap.clear();
		changeKeyDataSize = 0;
		groupKeyDataMap.clear();
		VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
	}
	
	/**
	 * subscribe one group which already has configures
	 */
	@Test
	public void testSubGroupWithConfigure() throws InterruptedException {
		int keysize = 10;
		try {
			for (int i = 0; i < keysize; i++) {
				VintageConfigWebUtils.register(groupString, keyString + i,
						valueString + i);
			}
			Thread.sleep(2 * HEARTBEATINTERVAL);
			assertEquals(0, changeDataSize);
			configClient.subscribeGroupDataChanges(groupString, configListener);
			Thread.sleep(15 * HEARTBEATINTERVAL);
			assertEquals(keysize, changeDataSize);
			assertEquals(changeDataSize, configClient.lookup(groupString)
					.size());

			for (int i = 0; i < keysize; i++) {
				VintageConfigWebUtils.unregister(groupString, keyString + i);
			}
			Thread.sleep(5 * HEARTBEATINTERVAL);
			assertEquals(0, changeDataSize);

			configClient.unsubscribeNodeChanges(groupString, configListener);
		} finally {
			for (int i = 0; i < keysize; i++) {
				VintageConfigWebUtils.unregister(groupString, keyString + i);
			}
		}

	}

	/**
	 * client subscribes blank group
	 */
	@Test
	public void testSubBlankGroup() throws InterruptedException {
		int keySize = 10;
		try {
			assertTrue(configClient.lookup(groupString) == null
					|| configClient.lookup(groupString).size() == 0);
			configClient.subscribeGroupDataChanges(groupString, configListener);

			// register new configure information
			for (int i = 0; i < keySize; i++) {
				VintageConfigWebUtils.register(groupString, keyString + i,
						valueString + i);
			}
			Thread.sleep(2 * HEARTBEATINTERVAL);
			// check the result
			assertEquals(keySize, changeDataSize);
			assertEquals(changeDataSize, configClient.lookup(groupString)
					.size());

			for (int i = 0; i < keySize; i++) {
				VintageConfigWebUtils.unregister(groupString, keyString + i);
			}
			Thread.sleep(4000);
			assertEquals(0, changeDataSize);
			// Thread.sleep(2 * HEARTBEATINTERVAL);
			Map<String, String> lookMap = configClient.lookup(groupString);
			assertTrue(lookMap == null || lookMap.isEmpty());

			configClient.unsubscribeNodeChanges(groupString, configListener);

		} finally {
			for (int i = 0; i < keySize; i++) {
				VintageConfigWebUtils.unregister(groupString, keyString + i);
			}
			configClient.unsubscribeNodeChanges(groupString, configListener);
		}

	}

	

	/*
	 * 前提：Client订阅一个没有节点的cluster 操作：cluster中注册一个节点，sleep一段时间后将该节点取消注册
	 * 预期结果：Client端能收到节点取消注册的信息
	 */
	@Test
	public void testSubBlankGroupUnreg() throws InterruptedException {
		try {

			configClient.subscribeGroupDataChanges(groupString, configListener);
			VintageConfigWebUtils.register(groupString, keyString, valueString);

			// sleep的时间与心跳时间有十分紧密的关系
			Thread.sleep(2 * HEARTBEATINTERVAL);
			assertEquals(1, changeDataSize);

			VintageConfigWebUtils.unregister(groupString, keyString);
			Thread.sleep(2 * HEARTBEATINTERVAL);
			assertEquals(0, changeDataSize);

			configClient.unsubscribeNodeChanges(groupString, configListener);
		} finally {
			VintageConfigWebUtils.unregister(groupString, keyString);
			configClient.unsubscribeNodeChanges(groupString, configListener);
		}

	}

	/*
	 * 前提：Client订阅一个非空的cluster 操作：cluster中注册一个节点，sleep一段时间后将该节点全部取消注册
	 * 预期结果：Client端能收到节点取消注册的信息
	 */
	@Test
	public void testSubGroupUnreg() throws InterruptedException {
		String keyTemp = "keyTemp";
		String valueTemp = "valueTemp";
		try {
			VintageConfigWebUtils.register(groupString, keyString, valueString);
			Thread.sleep(HEARTBEATINTERVAL);

			configClient.subscribeGroupDataChanges(groupString, configListener);

			VintageConfigWebUtils.register(groupString, keyTemp, valueTemp);
			Thread.sleep(2 * HEARTBEATINTERVAL);
			assertEquals(2, changeDataSize);

			VintageConfigWebUtils.unregister(groupString, keyString);
			VintageConfigWebUtils.unregister(groupString, keyTemp);

			Thread.sleep(2 * HEARTBEATINTERVAL);
			assertEquals(0, changeDataSize);

			configClient.unsubscribeNodeChanges(groupString, configListener);
		} finally {
			VintageConfigWebUtils.unregister(groupString, keyString);
			VintageConfigWebUtils.unregister(groupString, keyTemp);
			configClient.unsubscribeNodeChanges(groupString, configListener);
		}
	}

	/**
	 * 注册 200个配置信息，逐渐取消注册 验证：最后一个节点取消注册时,是否可正常收到通知
	 */
	@Test
	public void testSubUnregLast() throws InterruptedException {
		int keySize = 200;

		try {
			configClient.subscribeGroupDataChanges(groupString, configListener);

			for (int i = 0; i < keySize; i++) {
				VintageConfigWebUtils.register(groupString, keyString + i,
						valueString + i);
			}

			Thread.sleep(3 * HEARTBEATINTERVAL);
			assertEquals(keySize, changeDataSize);

			for (int i = 0; i < keySize - 1; i++) {
				VintageConfigWebUtils.unregister(groupString, keyString + i);
			}
			Thread.sleep(3 * HEARTBEATINTERVAL);
			assertEquals(1, changeDataSize);

			VintageConfigWebUtils.unregister(groupString, keyString
					+ (keySize - 1));
			Thread.sleep(HEARTBEATINTERVAL);
			assertEquals(0, changeDataSize);

			configClient.unsubscribeNodeChanges(groupString, configListener);
		} finally {
			for (int i = 0; i < keySize; i++) {
				VintageConfigWebUtils.unregister(groupString, keyString + i);
			}
			configClient.unsubscribeNodeChanges(groupString, configListener);
		}
	}

	@Test
	public void testSubRepeat() throws InterruptedException {
		int repeatCount = 200;
		int keySize = 10;

		try {
			for (int i = 0; i < repeatCount; i++) {
				configClient.subscribeGroupDataChanges(groupString,
						configListener);
			}

			assertEquals(0, changeDataSize);

			for (int i = 0; i < keySize; i++) {
				VintageConfigWebUtils.register(groupString, keyString + i,
						valueString + i);
			}
			Thread.sleep(2 * HEARTBEATINTERVAL);
			assertEquals(keySize, changeDataSize);

			for (int i = 0; i < keySize; i++) {
				VintageConfigWebUtils.unregister(groupString, keyString + i);
			}
			Thread.sleep(2 * HEARTBEATINTERVAL);
			assertEquals(0, changeDataSize);

			configClient.unsubscribeNodeChanges(groupString, configListener);
		} finally {
			for (int i = 0; i < keySize; i++) {
				VintageConfigWebUtils.unregister(groupString, keyString + i);
			}
			configClient.unsubscribeNodeChanges(groupString, configListener);
		}
	}

	/*
	 * case 8：重复订阅 前提：Client订阅同一个service的80个cluster 操作：每一个cluter节点变更（节点注册和取消注册）
	 * 预期结果：Client能收到所有cluster节点变更的信息
	 */
	@Test
	public void testSubRepeatDifGroup() throws InterruptedException {
		int groupSize = 100;
		try {
			for (int i = 0; i < groupSize; i++) {
				configClient.subscribeGroupDataChanges(groupString + i,
						configListener);
			}
			for (int i = 0; i < groupSize; i++) {
				VintageConfigWebUtils.register(groupString + i, keyString,
						valueString);
			}
			Thread.sleep(4 * HEARTBEATINTERVAL);

			for (int i = 0; i < groupSize; i++) {
				assertEquals(1, groupSizeMap.get(groupString + i).intValue());
			}

			for (int i = 0; i < groupSize; i++) {
				VintageConfigWebUtils.unregister(groupString + i, keyString);
			}
			Thread.sleep(4 * HEARTBEATINTERVAL);

			for (int i = 0; i < groupSize; i++) {
				assertEquals(0, groupSizeMap.get(groupString + i).intValue());
			}
			configClient.unsubscribeNodeChanges(groupString, configListener);

		} finally {
			for (int i = 0; i < groupSize; i++) {
				VintageConfigWebUtils.unregister(groupString + i, keyString);
			}
			configClient.unsubscribeNodeChanges(groupString, configListener);
		}
	}

	/**
	 * case12 & case13 测试点：订阅两组不同的group，操作：分别变更两个group的配置信息， 验证：是否可正常收到变更
	 * 操作2：取消订阅一个 cluster节点信息 操作3：变更取消订阅的cluster中节点信息 验证3：验证收不到已取消订阅的cluster节点信息
	 * 操作4：变更未取消的 cluster中节点信息 验证4：验证可收到订阅cluster中节点变更信息
	 */
	@Test
	public void testSubTwoGroups() throws InterruptedException {
		String groupTemp = "groupTemp";
		String valueTemp = "valueTemp";

		int keySize1 = 10;
		int keySize2 = 15;

		try {
			configClient.subscribeGroupDataChanges(groupString, configListener);
			configClient.subscribeGroupDataChanges(groupTemp, configListener);

			// 注册节点
			for (int i = 0; i < keySize1; i++) {
				VintageConfigWebUtils.register(groupString, keyString + i,
						valueString + i);
			}
			for (int i = 0; i < keySize2; i++) {
				VintageConfigWebUtils.register(groupTemp, keyString + i,
						valueString + i);
			}
			Thread.sleep(2 * HEARTBEATINTERVAL);

			assertEquals(keySize1, groupSizeMap.get(groupString).intValue());
			assertEquals(keySize2, groupSizeMap.get(groupTemp).intValue());

			int index = 5;
			for (int i = 0; i < index; i++) {
				VintageConfigWebUtils.unregister(groupString, keyString + i);
				VintageConfigWebUtils.unregister(groupTemp, keyString + i);
			}

			Thread.sleep(2 * HEARTBEATINTERVAL);
			assertEquals(keySize1 - index, groupSizeMap.get(groupString)
					.intValue());
			assertEquals(keySize2 - index, groupSizeMap.get(groupTemp)
					.intValue());

			// 给extinfo赋不同的值，重新注册。（首先清空nodemap，为了证明改变extinfo能向client推送信息）
			groupSizeMap.clear();
			VintageConfigWebUtils.register(groupString, keyString + index,
					valueTemp);
			VintageConfigWebUtils.register(groupTemp, keyString + index,
					valueTemp);

			Thread.sleep(2 * HEARTBEATINTERVAL);
			assertEquals(keySize1 - index, groupSizeMap.get(groupString)
					.intValue());
			assertEquals(keySize2 - index, groupSizeMap.get(groupTemp)
					.intValue());
			assertEquals(valueTemp,
					groupDataMap.get(groupString).get(keyString + index));
			assertEquals(valueTemp,
					groupDataMap.get(groupTemp).get(keyString + index));

			configClient.unsubscribeNodeChanges(groupTemp, configListener);

			for (int i = 0; i < index; i++) {
				VintageConfigWebUtils.register(groupString, keyString + i,
						valueString + i);
				VintageConfigWebUtils.register(groupTemp, keyString + i,
						valueString + i);
			}
			Thread.sleep(2 * HEARTBEATINTERVAL);

			assertEquals(keySize1, groupSizeMap.get(groupString).intValue());
			assertEquals(keySize2 - index, groupSizeMap.get(groupTemp)
					.intValue());

			for (int i = 0; i < keySize1; i++) {
				VintageConfigWebUtils.unregister(groupString, keyString + i);
			}
			for (int i = 0; i < keySize2; i++) {
				VintageConfigWebUtils.unregister(groupTemp, keyString + i);
			}

			Thread.sleep(2 * HEARTBEATINTERVAL);
			assertEquals(0, groupSizeMap.get(groupString).intValue());
			assertEquals(keySize2 - index, groupSizeMap.get(groupTemp)
					.intValue());

			configClient.unsubscribeNodeChanges(groupString, configListener);
		} finally {
			for (int i = 0; i < keySize1; i++) {
				VintageConfigWebUtils.unregister(groupString, keyString + i);
			}
			for (int i = 0; i < keySize2; i++) {
				VintageConfigWebUtils.unregister(groupTemp, keyString + i);
			}
			configClient.unsubscribeNodeChanges(groupString, configListener);
		}
	}

	/**
	 * case 14: 先lookup，再订阅，最后进行节点变更
	 * 预期结果：订阅之前lookup，订阅不会引起心跳线程向client端Notify消息;订阅之后的节点变化可以Notify消息
	 */
	@Test
	public void testSubLookup() throws InterruptedException {
		try {
			VintageConfigWebUtils.register(groupString, keyString, valueString);
			Thread.sleep(HEARTBEATINTERVAL);
			assertEquals(1, configClient.lookup(groupString).size());

			configClient.subscribeGroupDataChanges(groupString, configListener);
			Thread.sleep(HEARTBEATINTERVAL);

			assertNull(groupDataMap.get(groupString));
			assertNull(groupSizeMap.get(groupString));

			VintageConfigWebUtils.unregister(groupString, keyString);
			Thread.sleep(HEARTBEATINTERVAL);
			assertEquals(0, groupSizeMap.get(groupString).intValue());

			configClient.unsubscribeNodeChanges(groupString, configListener);
		} finally {
			VintageConfigWebUtils.unregister(groupString, keyString);
			configClient.unsubscribeNodeChanges(groupString, configListener);
		}
	}

	@Ignore
	@Test
	public void testParameterNull() {
		parameterException(null, configListener);
		parameterException(groupString, null);
	}

	@Test
	public void testSubNoKeyRegKey() {
		String newKey = "newKeyTest";
		try {
			configClient.subscribeKeyChanges(groupString, newKey,
					configKeyListener);
			configClient.register(groupString, newKey, valueString);

			sleep(10 * HEARTBEATINTERVAL);

			assertEquals(1, changeKeyDataSize);
			assertEquals(valueString,
					groupKeyDataMap.get(groupString).get(newKey));
			assertEquals("NodeCreated", keyNodeAction);
		} finally {
			configClient.unregister(groupString, newKey);
			configClient.unsubscribeKeyChanges(groupString, newKey,
					configKeyListener);
		}
	}

	@Test
	public void testSubKeyModifyKey() {
		String newValue = "newValueTest";
		try {
			configClient.register(groupString, keyString, valueString);

			configClient.subscribeKeyChanges(groupString, keyString,
					configKeyListener);
			assertEquals(0, changeKeyDataSize);

			sleep(2 * HEARTBEATINTERVAL);

			configClient.register(groupString, keyString, newValue);
			sleep(2 * HEARTBEATINTERVAL);
			assertEquals(1, changeKeyDataSize);
			assertEquals(newValue,
					groupKeyDataMap.get(groupString).get(keyString));
			assertEquals("NodeValueChanged", keyNodeAction);
		} finally {
			configClient.unregister(groupString, keyString);
			configClient.unsubscribeKeyChanges(groupString, keyString,
					configKeyListener);
		}
	}

	@Test
	public void testSubKeyUnregKey() {
		String newValue = "newValueTest";
		try {
			configClient.register(groupString, keyString, valueString);

			configClient.subscribeKeyChanges(groupString, keyString,
					configKeyListener);

			sleep(2 * HEARTBEATINTERVAL);

			configClient.unregister(groupString, keyString);
			sleep(10 * HEARTBEATINTERVAL);
			assertEquals(1, changeKeyDataSize);
			assertNull(groupKeyDataMap.get(groupString).get(keyString));
			assertEquals("NodeDeleted", keyNodeAction);
		} finally {
			configClient.unregister(groupString, keyString);
			configClient.unsubscribeKeyChanges(groupString, keyString,
					configKeyListener);
		}
	}

	@Test
	public void testSubKeyUnregRegKey() {
		String newValue = "newValueTest";
		try {
			configClient.register(groupString, keyString, valueString);

			configClient.subscribeKeyChanges(groupString, keyString,
					configKeyListener);

			sleep(2 * HEARTBEATINTERVAL);

			configClient.unregister(groupString, keyString);
			sleep(5 * HEARTBEATINTERVAL);
			assertEquals(1, changeKeyDataSize);
			assertNull(groupKeyDataMap.get(groupString).get(keyString));

			configClient.register(groupString, keyString, valueString);
			sleep(5 * HEARTBEATINTERVAL);
			assertEquals(1, changeKeyDataSize);
			assertEquals(valueString,
					groupKeyDataMap.get(groupString).get(keyString));
			assertEquals("NodeCreated", keyNodeAction);
		} finally {
			configClient.unregister(groupString, keyString);
			configClient.unsubscribeKeyChanges(groupString, keyString,
					configKeyListener);
		}
	}

	@Test
	public void testSubscribeMultiKey() {
		String valueAnother = "valueAnother";
		int keysize = 10;
		int groupsize = 5;
		try {
			for (int i = 0; i < groupsize; i++) {
				for (int j = 0; j < keysize; j++) {
					configClient.register(groupString + i, keyString + j,
							valueString);

					configClient.subscribeKeyChanges(groupString + i, keyString
							+ j, configKeyListener);
				}
			}

			sleep(10 * HEARTBEATINTERVAL);

			for (int i = 0; i < groupsize; i++) {
				for (int j = 0; j < keysize; j++) {
					configClient.register(groupString + i, keyString + j,
							valueAnother + j + i);
				}
			}

			sleep(10 * HEARTBEATINTERVAL);

			for (int i = 0; i < groupsize; i++) {
				for (int j = 0; j < keysize; j++) {
					assertEquals(
							valueAnother + j + i,
							groupKeyDataMap.get(groupString + i).get(
									keyString + j));
				}
			}

			// modify partial key and group
			for (int i = 0; i < groupsize / 2; i++) {
				for (int j = 0; j < keysize / 2; j++) {
					configClient.register(groupString + i, keyString + j,
							valueString + j + i);
				}
			}

			sleep(10 * HEARTBEATINTERVAL);

			for (int i = 0; i < groupsize / 2; i++) {
				for (int j = 0; j < keysize / 2; j++) {
					assertEquals(
							valueString + j + i,
							groupKeyDataMap.get(groupString + i).get(
									keyString + j));
				}
				for (int j = keysize / 2; j < keysize; j++) {
					assertEquals(
							valueAnother + j + i,
							groupKeyDataMap.get(groupString + i).get(
									keyString + j));
				}
			}
			for (int i = groupsize / 2; i < groupsize; i++) {
				for (int j = 0; j < keysize; j++) {
					assertEquals(
							valueAnother + j + i,
							groupKeyDataMap.get(groupString + i).get(
									keyString + j));
				}
			}

		} finally {
			for (int i = 0; i < groupsize; i++) {
				for (int j = 0; j < keysize; j++) {
					configClient.unregister(groupString + i, keyString + j);
					configClient.unsubscribeKeyChanges(groupString + i,
							keyString + j, configKeyListener);
				}
			}
		}

	}

	@Test
	public void testSubscribePartialKey() {
		String valueAnother = "valueAnother";
		int keysize = 10;
		int groupsize = 5;
		try {
			for (int i = 0; i < groupsize; i++) {
				for (int j = 0; j < keysize; j++) {
					configClient.register(groupString + i, keyString + j,
							valueString);
				}
			}
			for (int i = 0; i < groupsize / 2; i++) {
				for (int j = 0; j < keysize / 2; j++) {
					configClient.subscribeKeyChanges(groupString + i, keyString
							+ j, configKeyListener);
				}
			}

			sleep(5 * HEARTBEATINTERVAL);

			for (int i = 0; i < groupsize; i++) {
				for (int j = 0; j < keysize; j++) {
					configClient.register(groupString + i, keyString + j,
							valueAnother + j + i);
				}
			}

			sleep(5 * HEARTBEATINTERVAL);

			for (int i = 0; i < groupsize / 2; i++) {
				for (int j = 0; j < keysize / 2; j++) {
					System.out.print(groupKeyDataMap.get(groupString + i).get(
							keyString + j));
					assertEquals(
							valueAnother + j + i,
							groupKeyDataMap.get(groupString + i).get(
									keyString + j));
				}
				for (int j = keysize / 2; j < keysize; j++) {
					assertTrue(groupKeyDataMap.get(groupString + i).get(
							keyString + j) == null);
				}
			}

			for (int i = groupsize / 2; i < groupsize; i++) {
				for (int j = 0; j < keysize; j++) {
					assertTrue(groupKeyDataMap.get(groupString + i) == null);
				}
			}
		} finally {
			for (int i = 0; i < groupsize; i++) {
				for (int j = 0; j < keysize; j++) {
					configClient.unregister(groupString + i, keyString + j);
					configClient.unsubscribeKeyChanges(groupString + i,
							keyString + j, configKeyListener);
				}
			}
		}

	}

	@Test
	public void testSubKeySub() {
		String valueAnother = "valueAnother";
		String keySecond = "keySecond";
		try {
			configClient.register(groupString, keyString, valueString);

			configClient.subscribeGroupDataChanges(groupString, configListener);
			configClient.subscribeKeyChanges(groupString, keyString,
					configKeyListener);

			configClient.register(groupString, keySecond, valueString);
			sleep(5 * HEARTBEATINTERVAL);
			// check subscribe
			assertEquals(2, changeDataSize);
			// check subscribe key
			assertEquals(1, changeKeyDataSize);
			assertEquals(valueString,
					groupKeyDataMap.get(groupString).get(keyString));

			configClient.register(groupString, keyString, valueAnother);
			sleep(3 * HEARTBEATINTERVAL);
			// check subscribe
			assertEquals(2, changeDataSize);
			assertEquals(valueAnother,
					groupDataMap.get(groupString).get(keyString));
			// check subscribe key
			assertEquals(1, changeKeyDataSize);
			assertEquals(valueAnother,
					groupKeyDataMap.get(groupString).get(keyString));

			configClient.unsubscribeNodeChanges(groupString, configListener);
			configClient.register(groupString, keyString, valueString);
			sleep(3 * HEARTBEATINTERVAL);
			assertEquals(2, changeDataSize);
			assertEquals(valueAnother,
					groupDataMap.get(groupString).get(keyString));
			// check subscribe key
			assertEquals(1, changeKeyDataSize);
			assertEquals(valueString,
					groupKeyDataMap.get(groupString).get(keyString));

			configClient.subscribeGroupDataChanges(groupString, configListener);
			sleep(HEARTBEATINTERVAL);
			configClient.unsubscribeAllNodeChanges(groupString);
			configClient.register(groupString, keyString, valueAnother);
			sleep(3 * HEARTBEATINTERVAL);
			assertEquals(1, changeKeyDataSize);
			assertEquals(valueAnother,
					groupKeyDataMap.get(groupString).get(keyString));
		} finally {
			configClient.unregister(groupString, keyString);
			configClient.unregister(groupString, keySecond);

			configClient.unsubscribeNodeChanges(groupString, configListener);
			configClient.unsubscribeKeyChanges(groupString, keyString,
					configKeyListener);
		}
	}

	@Test
	public void testSubKeyException() {
		String groupNoExist = "groupNoExist";
		String keyNoExist = "groupNoExist";
		try {
			configClient.subscribeKeyChanges(groupNoExist, keyString,
					configKeyListener);
			configClient.subscribeKeyChanges(groupNoExist, keyNoExist,
					configKeyListener);

			try {
				configClient.subscribeKeyChanges(null, keyString,
						configKeyListener);
				fail();
			} catch (VintageException ex) {
				// TODO: handle exception
				assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR, ex.getFactor());
			}

			try {
				configClient.subscribeKeyChanges(groupString, null,
						configKeyListener);
				fail();
			} catch (VintageException ex) {
				// TODO: handle exception
				assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR, ex.getFactor());
			}

			try {
				configClient.subscribeKeyChanges(groupString, keyString, null);
			} catch (VintageException ex) {
				// TODO: handle exception
				fail();
			}

		} catch (VintageException ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * case12 & case13 测试点：订阅两组不同的group，操作：分别变更两个group的配置信息， 验证：是否可正常收到变更
	 * 操作2：取消订阅一个 cluster节点信息 操作3：变更取消订阅的cluster中节点信息 验证3：验证收不到已取消订阅的cluster节点信息
	 * 操作4：变更未取消的 cluster中节点信息 验证4：验证可收到订阅cluster中节点变更信息
	 */
	@Test
	public void testGroupKey() throws InterruptedException {
		String groupTemp = "testGroupKeygroup";
		String keyTemp = "testGroupKeykey";
		String valueTemp = "testGroupKeyvalue";
		String newvalueTemp = "testGroupKeynewvalue";

		try {
			
			VintageConfigWebUtils.register(groupTemp, keyTemp, valueTemp);

			configClient.subscribeGroupDataChanges(groupTemp, configListener);
			configClient.subscribeKeyChanges(groupTemp, keyTemp, configKeyListener);
			
			Thread.sleep(2 * HEARTBEATINTERVAL);

			assertEquals(1, groupSizeMap.get(groupTemp).intValue());
			assertEquals(1, changeKeyDataSize);

			configClient.register(groupTemp, keyTemp, newvalueTemp);
			sleep(2 * HEARTBEATINTERVAL);
			
			assertEquals(1, groupSizeMap.get(groupTemp).intValue());			
			assertEquals(1, changeKeyDataSize);
			assertEquals(newvalueTemp,
					groupKeyDataMap.get(groupTemp).get(keyTemp));
			assertEquals("NodeValueChanged", keyNodeAction);
			
		} finally {
			VintageConfigWebUtils.unregister(groupTemp, keyTemp);
			configClient.unsubscribeNodeChanges(groupTemp, configListener);
			configClient.subscribeKeyChanges(groupTemp, keyTemp, configKeyListener);
		}
	}


	private void parameterException(String group,
			ConfigServiceChangeListener listener) {
		try {
			configClient.subscribeGroupDataChanges(group, listener);
			fail("Error in parameterException");
		} catch (VintageException ex) {
			assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR.getErrorCode(), ex
					.getFactor().getErrorCode());
		}
	}

}
