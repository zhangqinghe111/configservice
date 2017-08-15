package com.weibo.qa.vintage.client.testcases;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.weibo.vintage.exception.ExcepFactor;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.utils.VintageConfigWebUtils;
import com.weibo.vintage.utils.VintageTestLogger;

/*
 * test cases for register
 */
public class ClientConfigRegisterTest extends ConfigBaseTest {
	@Before
	public void setUp() throws Exception {
		groupString = getRandomString(10);
        keyString = getRandomString(5);
        valueString = getRandomString(20);
	}

	@After
	public void tearDown() throws Exception {
		configClient.unregister(groupString, keyString);
	}

	/**
	 * repeat test,contains three points: register with same group ,key and
	 * values for many times; register with same group, key and different values
	 * for many times; unregister many times.
	 */
	@Test
	public void testRepeatRegister() {
		// the first step
		// with the same group ,key and values
		int oldsize = configClient.lookup(groupString).size();
		for (int i = 0; i < 100; i++) {
			assertEquals(true, configClient.register(groupString, keyString, valueString));			
		}
		
		sleep(HEARTBEATINTERVAL);
		
		Map<String, String> nodesList = configClient.lookup(groupString);
		System.out.print(nodesList.toString());
		int newsize = nodesList.size();
		assertEquals(oldsize + 1, newsize);

		// the second step
		// with the same group ,key and different values
		nodesList = configClient.lookup(groupString);
		String nodeValue = nodesList.values().iterator().next();
		assertEquals(valueString, nodeValue);

		// modify the value
		String valueString2 = "anothervalue";
		assertEquals(true, configClient.register(groupString, keyString, valueString2));
		sleep(HEARTBEATINTERVAL);
		assertEquals(newsize, configClient.lookup(groupString).size());

		nodesList = configClient.lookup(groupString);
		nodeValue = nodesList.values().iterator().next();
		assertEquals(valueString2, nodeValue);

		// test unregister for many times
		for (int i = 0; i < 10; i++) {
			assertEquals(true, configClient.unregister(groupString, keyString));
		}
		sleep(HEARTBEATINTERVAL);
		assertEquals(oldsize, configClient.lookup(groupString).size());
	}

	/**
	 * repeat with different group,contains two points: register with different
	 * group, same multi keys for each group unregister with different group
	 */
	@Test
	public void testMultiRegister() {
		List<String> groupList = new ArrayList<String>();
		groupList.add(groupString + "1");
		groupList.add(groupString + "2");
		groupList.add(groupString + "3");

		List<String> keyList = new ArrayList<String>();
		keyList.add(keyString + "1");
		keyList.add(keyString + "2");
		keyList.add(keyString + "3");
		keyList.add(keyString + "4");

		List<String> valueList = new ArrayList<String>();
		valueList.add(valueString + "1");
		valueList.add(valueString + "2");
		valueList.add(valueString + "3");
		valueList.add(valueString + "4");

		// get old key number and register
		int[] expSizeArray = new int[3];
		int i = 0;
		for (String groupStr : groupList) {
			expSizeArray[i] = configClient.lookup(groupStr).size();
			expSizeArray[i] = expSizeArray[i] + 4;

			int j = 0;
			for (String keyStr : keyList) {
				configClient.register(groupStr, keyStr,
						valueList.get(j));
				j++;
			}
			i++;
		}

		sleep(2 * HEARTBEATINTERVAL);
		// get new key number
		int[] newsizeArray = new int[3];
		i = 0;
		for (String groupStr : groupList) {
			newsizeArray[i] = configClient.lookup(groupStr).size();
			i++;
		}

		// check the key number
		assertArrayEquals(expSizeArray, newsizeArray);

		// check the value
		for (String groupStr : groupList) {
			int j = 0;

			Map<String, String> nodeList = configClient.lookup(groupStr);
			for (String keyStr : keyList) {
				String nodeValue = nodeList.get(keyStr);
				assertEquals(valueList.get(j), nodeValue);
				j++;
			}
		}

		// unregister the keys
		for (String groupStr : groupList) {
			for (String keyStr : keyList) {
				configClient.unregister(groupStr, keyStr);
			}
		}

		sleep(2 * HEARTBEATINTERVAL);

		i = 0;
		for (String groupStr : groupList) {
			expSizeArray[i] = expSizeArray[i] - 4;
			newsizeArray[i] = configClient.lookup(groupStr).size();
			i++;
		}
		// check the key number
		assertArrayEquals(expSizeArray, newsizeArray);
	}

	/**
	 * register group naming a and a/b/c, check the group a/b
	 */
	@Test
	public void testGroupName() {
		String groupStr1 = "a";
		String groupStr2 = "a/b/c";

		int oldsize1 = configClient.lookup(groupStr1).size();
		int oldsize2 = configClient.lookup(groupStr2).size();

		configClient.register(groupStr1, keyString, valueString);
		configClient.register(groupStr2, keyString, valueString);

		sleep(2 * HEARTBEATINTERVAL);

		int newsize1 = configClient.lookup(groupStr1).size();
		int newsize2 = configClient.lookup(groupStr2).size();

		assertEquals(oldsize1 + 1, newsize1);
		assertEquals(oldsize2 + 1, newsize2);

		Map<String, String> nodesList = configClient.lookup("a/b");

		configClient.unregister(groupStr1, keyString);
		configClient.unregister(groupStr2, keyString);

		sleep(2 * HEARTBEATINTERVAL);

		// check the key number of group "a/b"
		System.out.print(nodesList.size());
		assertEquals(0, nodesList.size());
	}	

	/*
	 * register large amount of keys within one group
	 */
	@Test
	public void testGroupNumerousKeys() {
		int max = 1000;
		int oldsize = 0;
		int newsize = 0;
		try {
			oldsize = configClient.lookup(groupString).size();
			for (int i = 0; i < max; i++) {
				configClient.register(groupString, keyString + i,
						valueString + i);
			}
			sleep(10 * HEARTBEATINTERVAL);
			newsize = configClient.lookup(groupString).size();
			assertEquals(oldsize + max, newsize);

			Map<String, String> nodesList = configClient.lookup(groupString);
			for (int i = 0; i < max; i++) {
				String nodeValueString = nodesList.get(keyString + i);
				assertEquals(valueString + i, nodeValueString);
			}
		} catch (Exception e) {
			// TODO: handle exception
			System.out.print(e.getMessage());
			fail("error in testGroupNumerousKeys");
		} finally {
			for (int i = 0; i < max; i++) {
				configClient.unregister(groupString, keyString + i);
			}
			sleep(10 * HEARTBEATINTERVAL);
			assertEquals(oldsize, configClient.lookup(groupString).size());
		}
	}

	/*
	 * register large amount of keys and long value within one group
	 */	
	@Test
	public void testGroupNumerousKeysValues() {
		int max = 10;
		int oldsize = 0;
		int newsize = 0;
		String valueTemp = "";
		StringBuilder sBuilder = new StringBuilder();

		for (int i = 0; i < 2000; i++) {
			sBuilder = sBuilder.append('a');
		}

		valueTemp = sBuilder.toString();

		try {
//			VintageTestLogger.info(configClient.lookup(groupString));
			oldsize = configClient.lookup(groupString).size();
			for (int i = 0; i < max; i++) {
				configClient.register(groupString, keyString + i,
						valueTemp + i);
				sleep(100);
			}
			sleep(10 * HEARTBEATINTERVAL);
			newsize = configClient.lookup(groupString).size();
			System.out.print(oldsize + max);
			System.out.print("newsize:" + newsize);
			assertEquals(oldsize + max, newsize);

			Map<String, String> nodesList = configClient.lookup(groupString);
			for (int i = 0; i < max; i++) {
				String nodeValueString = nodesList.get(keyString + i);
				assertEquals(valueTemp + i, nodeValueString);
			}
		} catch (Exception e) {
			// TODO: handle exception
			System.out.print(e.getMessage());
			fail("error in testGroupNumerousKeys");
		} finally {
			for (int i = 0; i < max; i++) {
				configClient.unregister(groupString, keyString + i);
			}
			sleep(10 * HEARTBEATINTERVAL);
			assertEquals(oldsize, configClient.lookup(groupString).size());
		}
	}

	/**
	 * parameters null return E_SERVICE_PACKET_EMPTY, but expect
	 * E_PARAM_INVALID_ERROR
	 */
	@Test
	public void testParamNull() {
		registerParamException(null, keyString, valueString);
		registerParamException(groupString, null, valueString);
		registerParamException(groupString, keyString, null);
	}

	@Test
	public void testParamBlank() {
		registerParamException("", keyString, valueString);
		registerParamException(groupString, "", valueString);
		registerParamException(groupString, keyString, "");
	}	

	/*
	 * test parameters with null
	 */
	private void registerParamException(String groupStr, String keyStr,
			String valueStr) {
		try {
			configClient.register(groupStr, keyStr, valueStr);
			fail("error in registerParamNull");
		} catch (Exception e) {
			System.out.print(e.getMessage());
			VintageException vintageException = (VintageException) e;
			assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR.getErrorCode(),
					vintageException.getFactor().getErrorCode());
		}
	}

}
