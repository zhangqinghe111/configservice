package com.weibo.qa.vintage.client.testcases;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import com.weibo.qa.vintage.config.testcases.BaseTest;
import com.weibo.vintage.exception.ExcepFactor;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.listener.ConfigServiceChangeListener;
import com.weibo.vintage.utils.VintageConfigWebUtils;

public class ClientConfigUnsubscribeTest extends BaseTest {
	@Before
	public void setUp() throws Exception {
		groupString = getRandomString(10);
		keyString = getRandomString(5);
		valueString = getRandomString(20);
	}

	@After
	public void tearDown() throws Exception {
		configClient.unsubscribeNodeChanges(groupString, configListener);
		changeDataSize = 0;
		groupDataMap.clear();
		groupSizeMap.clear();
	}

	/**
	 * 重复取消订阅100次，验证：节点变更不会通知到client; 最后订阅一次，验证：节点变更会通知client
	 */
	@Test
	public void testRepeatUnsub() {
		try {
			
			int repeatCount = 50;
			for (int i = 0; i < repeatCount; i++) {
				configClient
						.unsubscribeNodeChanges(groupString, configListener);
			}

			VintageConfigWebUtils.register(groupString, keyString, valueString);
			sleep(2 * HEARTBEATINTERVAL);
			assertEquals(0, changeDataSize);
			assertNull(groupSizeMap.get(groupString));

			configClient.subscribeGroupDataChanges(groupString, configListener);
			sleep(10 * HEARTBEATINTERVAL);
			assertEquals(1, changeDataSize);
		} finally {
			VintageConfigWebUtils.unregister(groupString, keyString);
			configClient.unsubscribeNodeChanges(groupString, configListener);
		}
	}

	/**
	 * 订阅cluster后，取消订阅，不能收到变更
	 */
	@Test
	public void testUnsub() {
		try {
			configClient.subscribeGroupDataChanges(groupString, configListener);
			sleep(HEARTBEATINTERVAL);
			configClient.unsubscribeNodeChanges(groupString, configListener);
			sleep(HEARTBEATINTERVAL);
			// VintageConfigWebUtils.register(groupString, keyString,
			// valueString);
			sleep(2 * HEARTBEATINTERVAL);
			assertEquals(0, changeDataSize);
			assertNull(groupSizeMap.get(groupString));

		} finally {
			VintageConfigWebUtils.unregister(groupString, keyString);
		}
	}

	/**
	 * 重复订阅100次，验证 ：节点变更会通知到client; 最后取消订阅一次，验证：节点变更不会通知client
	 */
	@Test
	public void testRepeatsubUnsub() {
		try {
			int repeatCount = 100;
			for (int i = 0; i < repeatCount; i++) {
				configClient.subscribeGroupDataChanges(groupString,
						configListener);
			}

			VintageConfigWebUtils.register(groupString, keyString, valueString);
			sleep(2 * HEARTBEATINTERVAL);
			assertEquals(1, changeDataSize);

			configClient.unsubscribeNodeChanges(groupString, configListener);
			VintageConfigWebUtils.unregister(groupString, keyString);
			sleep(2 * HEARTBEATINTERVAL);
			assertEquals(1, changeDataSize);
		} finally {
			VintageConfigWebUtils.unregister(groupString, keyString);
		}
	}

	/**
	 * 测试点：重复执行订阅及取消订阅，最终为订阅状态
	 * 
	 * 操作：变更节点的注册状态 验证：能收到变更通知 操作2:取消订阅 验证2：无法收到变更通知
	 */
	@Test
	public void testSwitchSub() {
		try {
			int repeatCount = 4;
			for (int i = 0; i < repeatCount; i++) {
				configClient.subscribeGroupDataChanges(groupString,
						configListener);
				sleep(HEARTBEATINTERVAL);
				configClient
						.unsubscribeNodeChanges(groupString, configListener);
			}

			configClient.subscribeGroupDataChanges(groupString, configListener);
			VintageConfigWebUtils.register(groupString, keyString, valueString);
			sleep(2 * HEARTBEATINTERVAL);
			assertEquals(1, changeDataSize);

			configClient.unsubscribeNodeChanges(groupString, configListener);
			sleep(HEARTBEATINTERVAL);
			VintageConfigWebUtils.unregister(groupString, keyString);
			sleep(2 * HEARTBEATINTERVAL);
			assertEquals(1, changeDataSize);
		} finally {
			VintageConfigWebUtils.unregister(groupString, keyString);
		}
	}

	/**
	 * 测试点：对同一service下的多个cluster进行部分取消,及对最后一个的取消操作
	 * 
	 * 验证：对于未取消的cluster可收到节点变更
	 * 
	 * 操作2：取消订阅最后一个cluster
	 * 
	 * 验证：收不到任何节点变更
	 */
	@Test
	public void testMultiCluster() {
		// 订阅同一个 service下的 5个cluster
		int groupsize = 5;
		int keysize = 10;
		try {
			for (int i = 0; i < groupsize; i++) {
				configClient.subscribeGroupDataChanges(groupString + i,
						configListener);
			}

			for (int i = 0; i < groupsize; i++) {
				for (int j = 0; j < keysize; j++) {
					VintageConfigWebUtils.register(groupString + i, keyString
							+ j, valueString + j);
				}
			}

			sleep(3 * HEARTBEATINTERVAL);

			// 验证 client可收到 5个cluster的节点变更通知
			for (int i = 0; i < groupsize; i++) {
				assertEquals(keysize, groupSizeMap.get(groupString + i)
						.intValue());
			}

			// 取消订阅 4个cluster的节点变更
			for (int i = 0; i < groupsize - 1; i++) {
				configClient.unsubscribeNodeChanges(groupString + i,
						configListener);
			}

			for (int i = 0; i < groupsize; i++) {
				for (int j = 0; j < keysize - 5; j++) {
					VintageConfigWebUtils.unregister(groupString + i, keyString
							+ j);
				}
			}
			sleep(3 * HEARTBEATINTERVAL);

			for (int i = 0; i < groupsize - 1; i++) {
				assertEquals(keysize, groupSizeMap.get(groupString + i)
						.intValue());
			}

			assertEquals(5, groupSizeMap.get(groupString + (groupsize - 1))
					.intValue());

			configClient.unsubscribeNodeChanges(groupString + (groupsize - 1),
					configListener);

			for (int i = 0; i < groupsize; i++) {
				for (int j = keysize - 5; j < keysize; j++) {
					VintageConfigWebUtils.unregister(groupString + i, keyString
							+ j);
				}
			}

			sleep(3 * HEARTBEATINTERVAL);

			for (int i = 0; i < groupsize - 1; i++) {
				assertEquals(keysize, groupSizeMap.get(groupString + i)
						.intValue());
			}

			assertEquals(5, groupSizeMap.get(groupString + (groupsize - 1))
					.intValue());
		} finally {
			for (int i = 0; i < groupsize; i++) {
				for (int j = 0; j < keysize; j++) {
					VintageConfigWebUtils.unregister(groupString + i, keyString
							+ j);
				}
			}
		}

	}

	/*
	 * sub and unsub, don't change the configuration, then sub last
	 */
	@Test
	public void testSubUnSub() {
		int keysize = 10;

		try {
			System.out.println(configListener);
			configClient.subscribeGroupDataChanges(groupString, configListener);
			sleep(this.HEARTBEATINTERVAL);
			configClient.unsubscribeNodeChanges(groupString, configListener);

			sleep(this.HEARTBEATINTERVAL);
			configClient.subscribeGroupDataChanges(groupString, configListener);
			for (int i = 0; i < keysize; i++) {
				VintageConfigWebUtils.register(groupString, keyString + i,
						valueString);
			}

			sleep(3 * HEARTBEATINTERVAL);

			assertEquals(keysize, changeDataSize);

			for (int i = 0; i < keysize; i++) {
				VintageConfigWebUtils.unregister(groupString, keyString + i);
			}
			sleep(3 * HEARTBEATINTERVAL);
			assertEquals(0, changeDataSize);

			configClient.unsubscribeNodeChanges(groupString, configListener);
		} finally {
			for (int i = 0; i < keysize; i++) {
				VintageConfigWebUtils.unregister(groupString, keyString + i);
			}
		}
	}

	/*
	 * sub and unsub, change the configuration, then sub last
	 */
	@Test
	public void testSubUnSubChange() {
		int keysize = 10;

		try {
			configClient.subscribeGroupDataChanges(groupString, configListener);
			sleep(this.HEARTBEATINTERVAL);
			configClient.unsubscribeNodeChanges(groupString, configListener);
			sleep(this.HEARTBEATINTERVAL);

			for (int i = 0; i < keysize; i++) {
				VintageConfigWebUtils.register(groupString, keyString + i,
						valueString);
			}

			configClient.subscribeGroupDataChanges(groupString, configListener);
			sleep(3 * HEARTBEATINTERVAL);
			assertEquals(keysize, changeDataSize);

			for (int i = 0; i < keysize; i++) {
				VintageConfigWebUtils.unregister(groupString, keyString + i);
			}
			sleep(3 * HEARTBEATINTERVAL);
			assertEquals(0, changeDataSize);

			configClient.unsubscribeNodeChanges(groupString, configListener);
		} finally {
			for (int i = 0; i < keysize; i++) {
				VintageConfigWebUtils.unregister(groupString, keyString + i);
			}
		}
	}

	/**
	 * 取消未订阅过的group，服务正常，不抛异常
	 */
	@Test
	public void testUnsubNoSub() {
		try {
			String groupNoSubString = "groupNoSub";
			configClient.unsubscribeNodeChanges(groupNoSubString,
					configListener);
			//fail("ERROR in testUnsubNoSub;There should be some exceptions");
		} catch (VintageException ex) {
			System.out.println(ex.getMessage());
		}
	}

	/**
	 * 测试点：参数异常测试 操作：分别将cluster service listener设置为null 预期结果：参数异常
	 * E_PARAM_INVALID_ERROR
	 * Ignored by liuyu
	 */
	@Ignore
	@Test
	public void testParamNull() {
		parameterException(null, configListener);
		parameterException(groupString, null);		
	}

	private void parameterException(String group,
			ConfigServiceChangeListener listener) {
		try {
			configClient.unsubscribeNodeChanges(group, listener);
			fail("Error in parameterException");
		} catch (VintageException ex) {
			assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR.getErrorCode(), ex
					.getFactor().getErrorCode());
		}
	}

	@Test
	public void testSubNoKeyRegKey() {
		String newKey = "newKeyTest";
		try {
			configClient.subscribeKeyChanges(groupString, newKey,
					configKeyListener);
			configClient.unsubscribeKeyChanges(groupString, newKey,
					configKeyListener);
			sleep(3 * HEARTBEATINTERVAL);
			configClient.register(groupString, newKey, valueString);

			assertEquals(0, changeKeyDataSize);
			assertNull(groupDataMap.get(groupString));
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
			sleep(2 * HEARTBEATINTERVAL);

			configClient.unsubscribeKeyChanges(groupString, keyString,
					configKeyListener);

			sleep(2 * HEARTBEATINTERVAL);

			configClient.register(groupString, keyString, newValue);
			sleep(2 * HEARTBEATINTERVAL);
			assertEquals(1, changeKeyDataSize);
			assertEquals(valueString,
					groupKeyDataMap.get(groupString).get(keyString));
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

			for (int i = 0; i < groupsize / 2; i++) {
				for (int j = 0; j < keysize / 2; j++) {
					configClient.unsubscribeKeyChanges(groupString + i,
							keyString + j, configKeyListener);
				}
			}

			for (int i = 0; i < groupsize; i++) {
				for (int j = 0; j < keysize; j++) {
					configClient.register(groupString + i, keyString + j,
							valueAnother + j + i);
				}
			}

			sleep(10 * HEARTBEATINTERVAL);

			for (int i = 0; i < groupsize / 2; i++) {
				for (int j = 0; j < keysize / 2; j++) {
					assertEquals(
							valueString,
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
			for (int i = 0; i < groupsize; i++) {
				for (int j = 0; j < keysize; j++) {
					configClient.subscribeKeyChanges(groupString + i, keyString
							+ j, configKeyListener);
				}
			}

			sleep(5 * HEARTBEATINTERVAL);

			for (int i = 0; i < groupsize; i++) {
				for (int j = 0; j < keysize; j++) {
					configClient.unsubscribeKeyChanges(groupString + i,
							keyString + j, configKeyListener);
				}
			}

			for (int i = 0; i < groupsize; i++) {
				for (int j = 0; j < keysize; j++) {
					configClient.register(groupString + i, keyString + j,
							valueAnother + j + i);
				}
			}

			sleep(5 * HEARTBEATINTERVAL);

			for (int i = 0; i < groupsize; i++) {
				for (int j = 0; j < keysize; j++) {
					System.out.print(groupKeyDataMap.get(groupString + i).get(
							keyString + j));
					assertEquals(
							valueString,
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
	public void testSubKeySub() {
		String valueAnother = "valueAnother";
		String keySecond = "keySecond";
		String valueThird = "valueThird";
		String valueFourth = "valueFourth";
		try {
			configClient.register(groupString, keyString, valueString);

			configClient.subscribeGroupDataChanges(groupString, configListener);
			configClient.subscribeKeyChanges(groupString, keyString,
					configKeyListener);
			sleep(2 * HEARTBEATINTERVAL);
			configClient.unsubscribeKeyChanges(groupString, keyString,
					configKeyListener);

			configClient.register(groupString, keyString, valueAnother);
			sleep(3 * HEARTBEATINTERVAL);
			// check subscribe
			assertEquals(1, changeDataSize);
			assertEquals(valueAnother,
					groupDataMap.get(groupString).get(keyString));
			// check subscribe key
			assertEquals(1, changeKeyDataSize);
			assertEquals(valueString,
					groupKeyDataMap.get(groupString).get(keyString));

			configClient.unsubscribeNodeChanges(groupString, configListener);
			configClient.register(groupString, keyString, valueThird);
			sleep(3 * HEARTBEATINTERVAL);
			assertEquals(1, changeDataSize);
			assertEquals(valueAnother,
					groupDataMap.get(groupString).get(keyString));
			// check subscribe key
			assertEquals(1, changeKeyDataSize);
			assertEquals(valueString,
					groupKeyDataMap.get(groupString).get(keyString));

			configClient.subscribeGroupDataChanges(groupString, configListener);
			sleep(HEARTBEATINTERVAL);
			configClient.unsubscribeAllNodeChanges(groupString);
			configClient.register(groupString, keyString, valueFourth);
			sleep(3 * HEARTBEATINTERVAL);
			assertEquals(1, changeDataSize);
			assertEquals(valueAnother,
					groupDataMap.get(groupString).get(keyString));
			assertEquals(1, changeKeyDataSize);
			assertEquals(valueString,
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
			configClient.unsubscribeKeyChanges(groupNoExist, keyString,
					configKeyListener);
			configClient.unsubscribeKeyChanges(groupNoExist, keyNoExist,
					configKeyListener);

			try {
				configClient.unsubscribeKeyChanges(null, keyString,
						configKeyListener);
				fail();
			} catch (VintageException ex) {
				// TODO: handle exception
				assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR, ex.getFactor());
			}

			try {
				configClient.unsubscribeKeyChanges(groupString, null,
						configKeyListener);
				fail();
			} catch (VintageException ex) {
				// TODO: handle exception
				assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR, ex.getFactor());
			}

			try {
				configClient
						.unsubscribeKeyChanges(groupString, keyString, null);
			} catch (VintageException ex) {
				// TODO: handle exception
				fail();
			}

		} catch (VintageException ex) {
			ex.printStackTrace();
		}
	}
}
