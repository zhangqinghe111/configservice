package com.weibo.qa.vintage.client.testcases;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
//import org.springframework.jdbc.config.InitializeDatabaseBeanDefinitionParser;

import com.weibo.qa.vintage.config.testcases.BaseTest;
import com.weibo.vintage.exception.ExcepFactor;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.utils.VintageConfigWebUtils;

public class ClientConfigLookupTest extends BaseTest {
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

	/*
	 * repeat lookup for many times
	 */
	@Test
	public void testRepeatLookup() {
		for (int i = 0; i < 100; i++) {
			assertEquals(0, configClient.lookup(groupString).size());
		}

		int newsize = 0;
		configClient.register(groupString, keyString, valueString);
		sleep(2 * HEARTBEATINTERVAL);
		for (int i = 0; i < 100; i++) {
			newsize = configClient.lookup(groupString).size();
			System.out.print(i);
			assertEquals(1, newsize);
		}

		configClient.unregister(groupString, keyString);
	
	}

	/*
	 * lookup for many groups and keys
	 */
	@Test
	public void testMultiLookup() {
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
		try {
			for (String groupStr : groupList) {
				assertEquals(0, configClient.lookup(groupStr).size());
				// for (String keyStr : keyList) {
				// assertEquals(0, configClient.lookup(groupStr,
				// keyStr).size());
				// }
			}

			for (String groupStr : groupList) {
				int i = 0;
				for (String keyStr : keyList) {
					configClient.register(groupStr, keyStr,
							valueList.get(i));
					i++;
				}
			}
			
			sleep(2*HEARTBEATINTERVAL);
			
			for (String groupStr : groupList) {
				assertEquals(4, configClient.lookup(groupStr).size());
				// for (String keyStr : keyList) {
				// assertEquals(1, configClient.lookup(groupStr,
				// keyStr).size());
				// }
			}

		} finally {
			for (String groupStr : groupList) {
				for (String keyStr : keyList) {
					configClient.unregister(groupStr, keyStr);
				}
			}
		}

	}	

	/**
	 * lookup group and key which didn't register，不抛异常
	 */
	@Test
	public void testLookupNoExists() {
		String groupTemp = "hqgroup";
		String keyTemp = "hqkey";
		VintageConfigWebUtils.register(groupString, keyString, valueString);

		try {
			sleep(HEARTBEATINTERVAL);
			
			Map<String, String> nodeList = configClient.lookup(groupString);
			assertEquals(1, nodeList.size());

			nodeList = configClient.lookup(groupTemp);
			assertEquals(0, nodeList.size());

			//nodeList = configClient.lookup(groupTemp, keyTemp);
			//assertEquals(0, nodeList.size());

			//fail("error in testLookupNoExists");
		} finally {
			configClient.unregister(groupString, keyString);
		}
	}

	/*
	 * check the result when appointing group only need to know the original
	 * result of the lookup interface, so use the function lookup of class
	 * VintageConfigWebUtils, not the class StaticsConfigServiceClient
	 */
	@Test
	public void testLookupResultGroup() {
		configClient.register(groupString, keyString, valueString);

		Map<String, String> nodesList = configClient.lookup(groupString);

		assertEquals(valueString, nodesList.get(keyString));
		configClient.unregister(groupString, keyString);
	}

	/*
	 * check the result appointing group and key, the same above
	 */
	@Test
	public void testLookupResult() {
		configClient.register(groupString, keyString, valueString);

		String nodesValue = configClient.lookup(groupString,
				keyString);
		assertEquals(valueString, nodesValue);
		VintageConfigWebUtils.unregister(groupString, keyString);
	}

	/**
	 * parameters null
	 */
	@Test
	public void testParamNull() {
		lookupParamNull(null);
		// lookupParamNull(groupString, null); // in accordance with the logic
	}

	/*
	 * parameters blank
	 */
	@Test
	public void testParamBlank() {
		lookupParamNull("");
		// lookupParamNull(groupString, ""); //lookup按照只提供了group查找
	}

	/*
	 * test parameters with null return E_SERVICE_PACKET_EMPTY, but expect
	 * E_PARAM_INVALID_ERROR
	 */
	private void lookupParamNull(String groupStr) {
		try {
			configClient.lookup(groupStr);
			fail("error in lookupParamNull");
		} catch (VintageException e) {
			System.out.print(e.getMessage());
		}
	}
}
