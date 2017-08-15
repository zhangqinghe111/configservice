package com.weibo.qa.vintage.client.testcases;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.vintage.client.StaticsConfigServiceClient;
import com.weibo.vintage.utils.VintageConfigWebUtils;

/*
 * client process test
 */
public class ClientConfigConfigureTest extends ConfigBaseTest {

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
	}
	
	/*
	 * test whether client can get configures when group has configures before start
	 */
	@Test
	public void testHasConfigureBeforeStart() {
		try {
			// first step,register some configurations in one group;run the
			// first two lines,then stop
			VintageConfigWebUtils.register(groupString, keyString, valueString);
			sleep(HEARTBEATINTERVAL);

			//comment the lines above; then rerun the following
			configClient.subscribeGroupDataChanges(groupString, configListener);
			sleep(3 * HEARTBEATINTERVAL);
			assertTrue(changeDataSize > 0);
		} finally {
			VintageConfigWebUtils.unregister(groupString, keyString);
		}
	}

	/*
	 * get configuration
	 */
	@Test
	public void testGetConfigOneClient() {
		try {

			for (int i = 0; i < 10; i++) {
				VintageConfigWebUtils.register(groupString, keyString + i,
						valueString + i);
			}
			configClient.subscribeGroupDataChanges(groupString, configListener);
			sleep(HEARTBEATINTERVAL);

			// configuration has no change
			for (int i = 0; i < 10; i++) {
				assertEquals(valueString + i, groupDataMap.get(groupString)
						.get(keyString + i));
			}

			Map<String, String> dataMap = configClient.lookup(groupString);
			for (int i = 0; i < 10; i++) {
				assertEquals(valueString + i, dataMap.get(keyString + i));
			}

			// configuration has change
			String valueTemp = "valueTemp";
			for (int i = 0; i < 10; i++) {
				VintageConfigWebUtils.register(groupString, keyString + i,
						valueTemp + i);
			}
			sleep(2 * HEARTBEATINTERVAL);
			for (int i = 0; i < 10; i++) {
				assertEquals(valueTemp + i,
						groupDataMap.get(groupString).get(keyString + i));
			}

			dataMap = configClient.lookup(groupString);
			for (int i = 0; i < 10; i++) {
				assertEquals(valueTemp + i, dataMap.get(keyString + i));
			}

			// add new configuration
			for (int i = 10; i < 15; i++) {
				VintageConfigWebUtils.register(groupString, keyString + i,
						valueTemp + i);
			}
			sleep(HEARTBEATINTERVAL);
			for (int i = 0; i < 15; i++) {
				assertEquals(valueTemp + i,
						groupDataMap.get(groupString).get(keyString + i));
			}

			dataMap = configClient.lookup(groupString);
			for (int i = 0; i < 15; i++) {
				assertEquals(valueTemp + i, dataMap.get(keyString + i));
			}

			// delete configuration
			for (int i = 0; i < 15; i++) {
				VintageConfigWebUtils.unregister(groupString, keyString + i);
			}
			sleep(2 * HEARTBEATINTERVAL);
			assertEquals(0, groupSizeMap.get(groupString).intValue());
			dataMap = configClient.lookup(groupString);
			assertTrue(dataMap == null || dataMap.isEmpty());

			configClient.unsubscribeNodeChanges(groupString, configListener);
		} finally {
			for (int i = 0; i < 15; i++) {
				VintageConfigWebUtils.unregister(groupString, keyString + i);
			}
			configClient.unsubscribeNodeChanges(groupString, configListener);
		}
	}

	/*
	 * get configuration for several clients
	 */
	@Test
	public void testGetConfigMultiClient() {
		try {
			StaticsConfigServiceClient tmpClient1 = new StaticsConfigServiceClient(
					getVintageConfig());
			StaticsConfigServiceClient tmpClient2 = new StaticsConfigServiceClient(
					getVintageConfig());
			tmpClient1.start();
			tmpClient2.start();

			for (int i = 0; i < 10; i++) {
				VintageConfigWebUtils.register(groupString, keyString + i,
						valueString + i);
			}
			configClient.subscribeGroupDataChanges(groupString, configListener);
			tmpClient1.subscribeGroupDataChanges(groupString, configListener);
			tmpClient2.subscribeGroupDataChanges(groupString, configListener);
			sleep(HEARTBEATINTERVAL);

			for (int i = 0; i < 10; i++) {
				assertEquals(valueString + i, groupDataMap.get(groupString)
						.get(keyString + i));
			}

			Map<String, String> dataMap = configClient.lookup(groupString);
			for (int i = 0; i < 10; i++) {
				assertEquals(valueString + i, dataMap.get(keyString + i));
			}

			Map<String, String> dataMap1 = tmpClient1.lookup(groupString);
			for (int i = 0; i < 10; i++) {
				assertEquals(valueString + i, dataMap1.get(keyString + i));
			}

			Map<String, String> dataMap2 = tmpClient2.lookup(groupString);
			for (int i = 0; i < 10; i++) {
				assertEquals(valueString + i, dataMap2.get(keyString + i));
			}

			// configuration has change
			String valueTemp = "valuetemp";
			for (int i = 0; i < 10; i++) {
				VintageConfigWebUtils.register(groupString, keyString + i,
						valueTemp + i);
			}
			sleep(2 * HEARTBEATINTERVAL);
			for (int i = 0; i < 10; i++) {
				assertEquals(valueTemp + i,
						groupDataMap.get(groupString).get(keyString + i));
			}

			dataMap = configClient.lookup(groupString);
			for (int i = 0; i < 10; i++) {
				assertEquals(valueTemp + i, dataMap.get(keyString + i));
			}

			dataMap1 = tmpClient1.lookup(groupString);
			for (int i = 0; i < 10; i++) {
				assertEquals(valueTemp + i, dataMap1.get(keyString + i));
			}

			dataMap2 = tmpClient2.lookup(groupString);
			for (int i = 0; i < 10; i++) {
				assertEquals(valueTemp + i, dataMap2.get(keyString + i));
			}

			// add new configuration
			for (int i = 10; i < 15; i++) {
				VintageConfigWebUtils.register(groupString, keyString + i,
						valueTemp + i);
			}
			sleep(HEARTBEATINTERVAL);
			for (int i = 0; i < 15; i++) {
				assertEquals(valueTemp + i,
						groupDataMap.get(groupString).get(keyString + i));
			}

			dataMap = configClient.lookup(groupString);
			for (int i = 0; i < 15; i++) {
				assertEquals(valueTemp + i, dataMap.get(keyString + i));
			}

			dataMap1 = tmpClient1.lookup(groupString);
			for (int i = 0; i < 15; i++) {
				assertEquals(valueTemp + i, dataMap1.get(keyString + i));
			}

			dataMap2 = tmpClient2.lookup(groupString);
			for (int i = 0; i < 15; i++) {
				assertEquals(valueTemp + i, dataMap2.get(keyString + i));
			}

			// delete configuration
			for (int i = 0; i < 15; i++) {
				VintageConfigWebUtils.unregister(groupString, keyString + i);
			}
			sleep(2 * HEARTBEATINTERVAL);
			assertEquals(0, groupSizeMap.get(groupString).intValue());
			dataMap = configClient.lookup(groupString);
			dataMap1 = tmpClient1.lookup(groupString);
			dataMap2 = tmpClient2.lookup(groupString);
			assertTrue(dataMap == null || dataMap.isEmpty());
			assertTrue(dataMap1 == null || dataMap1.isEmpty());
			assertTrue(dataMap2 == null || dataMap2.isEmpty());

			configClient.unsubscribeNodeChanges(groupString, configListener);
			tmpClient1.unsubscribeNodeChanges(groupString, configListener);
			tmpClient2.unsubscribeNodeChanges(groupString, configListener);
		} finally {
			for (int i = 0; i < 15; i++) {
				VintageConfigWebUtils.unregister(groupString, keyString + i);
			}
			configClient.unsubscribeNodeChanges(groupString, configListener);
		}

	}

	/*
	 * 
	 */

	
}
