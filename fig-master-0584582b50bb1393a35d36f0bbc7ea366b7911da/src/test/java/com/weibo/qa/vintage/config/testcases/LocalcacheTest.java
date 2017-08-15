package com.weibo.qa.vintage.config.testcases;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.vintage.utils.RedisWebUtils;
import com.weibo.vintage.utils.VintageConfigWebUtils;

public class LocalcacheTest extends BaseTest {

	@Before
	public void setUp() throws Exception {
		groupString = getRandomString(10);
		keyString = getRandomString(5);
		valueString = getRandomString(20);
	}

	@After
	public void tearDown() throws Exception {
		RedisWebUtils.StartRedis();
	}

	/*
	 * 
	 * 1. register configuration & no localcache 2. stop redis 3. check if
	 * client can get the configuration
	 */
	@Test
	public void testRegNoLocalcache() {
		RedisWebUtils.StartRedis();

		for (int i = 0; i < 10; i++) {
			VintageConfigWebUtils.register(groupString + i, keyString,
					valueString);
		}

		RedisWebUtils.StopRedis();

		for (int i = 0; i < 10; i++) {
			List<String> dataMap = VintageConfigWebUtils.lookup(groupString + i);
			assertTrue(dataMap == null || dataMap.isEmpty() || dataMap.size() == 0);
		}

		RedisWebUtils.StartRedis();
		
		for (int i = 0; i < 10; i++) {
			VintageConfigWebUtils.unregister(groupString + i, keyString);
		}
	}

	/*
	 * 
	 * 1. register configuration & lookup operation has localcache 2. stop redis
	 * 3. check if client can get the configuration
	 */
	@Test
	public void testRegLocalcache() {
		RedisWebUtils.StartRedis();

		for (int i = 0; i < 10; i++) {
			VintageConfigWebUtils.register(groupString + i, keyString,
					valueString);
		}

		for (int i = 0; i < 10; i++) {
			List<String> dataMap = VintageConfigWebUtils.lookup(groupString + i);
			Map<String, String> nodesMap = VintageConfigWebUtils
					.getConfigMap(dataMap);
			assertEquals(valueString, nodesMap.get(keyString));
		}

		RedisWebUtils.StopRedis();

		for (int i = 0; i < 10; i++) {
			List<String> dataList = VintageConfigWebUtils.lookup(groupString + i);
			Map<String, String> dataMap = VintageConfigWebUtils
					.getConfigMap(dataList);
			assertEquals(valueString, dataMap.get(keyString));
		}

		RedisWebUtils.StartRedis();
		for (int i = 0; i < 10; i++) {
			VintageConfigWebUtils.unregister(groupString + i, keyString);
		}
	}

	/*
	 * 
	 * 1. register configuration & lookup operation has localcache 2. register
	 * configuration again 3. stop redis 4. check if client can get the
	 * configuration
	 */
	@Test
	public void testRegLocalcacheReg() {
		RedisWebUtils.StartRedis();

		for (int i = 0; i < 10; i++) {
			VintageConfigWebUtils.register(groupString + i, keyString,
					valueString);
		}

		for (int i = 0; i < 10; i++) {
			List<String> dataList = VintageConfigWebUtils.lookup(groupString + i);
			Map<String, String> dataMap = VintageConfigWebUtils
					.getConfigMap(dataList);
			assertEquals(valueString, dataMap.get(keyString));
		}

		// register another configuration after lookup
		String keySecond = "keySecond";
		String valueSecond = "valueSecond";
		for (int i = 0; i < 10; i++) {
			VintageConfigWebUtils.register(groupString + i, keySecond,
					valueSecond);
		}
		
		RedisWebUtils.StopRedis();

		for (int i = 0; i < 10; i++) {
			List<String> dataList = VintageConfigWebUtils.lookup(groupString + i);
			Map<String, String> dataMap = VintageConfigWebUtils
					.getConfigMap(dataList);
			assertEquals(valueString, dataMap.get(keyString));
			assertEquals(valueSecond, dataMap.get(keySecond));
		}

		RedisWebUtils.StartRedis();
		for (int i = 0; i < 10; i++) {
			VintageConfigWebUtils.unregister(groupString + i, keyString);
			VintageConfigWebUtils.unregister(groupString + i, keySecond);
		}
	}

	/*
	 * 
	 * 1. register configuration & unregister all & no localcache 2. stop redis
	 * 3. check if client can get the configuration
	 */
	@Test
	public void testUnRegAllNoLocalcache() {
		RedisWebUtils.StartRedis();

		for (int i = 0; i < 10; i++) {
			VintageConfigWebUtils.register(groupString + i, keyString,
					valueString);
		}

		for (int i = 0; i < 10; i++) {
			VintageConfigWebUtils.unregister(groupString + i, keyString);
		}

		RedisWebUtils.StopRedis();

		for (int i = 0; i < 10; i++) {
			List<String> dataList = VintageConfigWebUtils.lookup(groupString + i);
			Map<String, String> dataMap = VintageConfigWebUtils
					.getConfigMap(dataList);
			assertTrue(dataMap == null || dataMap.isEmpty());
		}
	}

	/*
	 * 
	 * 1. register configuration & unregister all & lookup operation has
	 * localcache 2. stop redis 3. check if client can get the configuration
	 */
	@Test
	public void testUnRegAllLocalcache() {
		RedisWebUtils.StartRedis();

//		RedisWebUtils.Flushall();
		//Utils.redis.flushAll();
		for (int i = 0; i < 10; i++) {
			VintageConfigWebUtils.register(groupString + i, keyString,
					valueString);
		}

		for (int i = 0; i < 10; i++) {
			List<String> dataList = VintageConfigWebUtils.lookup(groupString + i);
			Map<String, String> dataMap = VintageConfigWebUtils
					.getConfigMap(dataList);
			assertEquals(valueString, dataMap.get(keyString));
		}

		for (int i = 0; i < 10; i++) {
			VintageConfigWebUtils.unregister(groupString + i, keyString);
		}

		for (int i = 0; i < 10; i++) {
			List<String> dataList = VintageConfigWebUtils.lookup(groupString + i);
			assertTrue(dataList == null || dataList.isEmpty() || dataList.size() == 0);
		}

		RedisWebUtils.StopRedis();

		for (int i = 0; i < 10; i++) {
			List<String> dataList = VintageConfigWebUtils.lookup(groupString + i);
			assertTrue(dataList == null || dataList.isEmpty() || dataList.size() == 0);
		}
	}

	/*
	 * 
	 * 1. register configuration & lookup operation has localcache & unregister
	 * all configuration 2. stop redis 3. check if client can get the
	 * configuration
	 */
	@Test
	public void testLocalcacheUnRegAll() {
		
		RedisWebUtils.StartRedis();
		for (int i = 0; i < 10; i++) {
			VintageConfigWebUtils.register(groupString + i, keyString,
					valueString);
		}

		for (int i = 0; i < 10; i++) {
			List<String> dataList = VintageConfigWebUtils.lookup(groupString + i);
			Map<String, String> dataMap = VintageConfigWebUtils
					.getConfigMap(dataList);
			assertEquals(valueString, dataMap.get(keyString));
		}

		for (int i = 0; i < 10; i++) {
			VintageConfigWebUtils.unregister(groupString + i, keyString);
		}

		RedisWebUtils.StopRedis();

		for (int i = 0; i < 10; i++) {
			List<String> dataList = VintageConfigWebUtils.lookup(groupString + i);
			assertTrue(dataList == null || dataList.isEmpty() || dataList.size() == 0);
		}
	}

	/*
	 * 
	 * 1. register configuration & unregister part of configuration & no
	 * localcache 2. stop redis 3. check if client can get the configuration
	 */
	@Test
	public void testUnRegNoLocalcache() {
		RedisWebUtils.StartRedis();

		for (int i = 0; i < 10; i++) {
			VintageConfigWebUtils.batchunregister(groupString+i, keyString, 0, 0, 5);
		}

		for (int i = 0; i < 10; i++) {
			VintageConfigWebUtils.batchunregister(groupString+i, keyString, 0, 0, 3);
		}

		RedisWebUtils.StopRedis();

		for (int i = 0; i < 10; i++) {
			List<String> dataList = VintageConfigWebUtils.lookup(groupString + i);
			assertTrue(dataList == null || dataList.isEmpty() || dataList.size() == 0);
		}

		RedisWebUtils.StartRedis();
		for (int i = 0; i < 10; i++) {
			VintageConfigWebUtils.batchunregister(groupString+i, keyString, 0, 3, 5);
		}
	}

	/*
	 * 
	 * 1. register configuration & unregister part of configuration & lookup
	 * operation has localcache 2. stop redis 3. check if client can get the
	 * configuration
	 */
	@Test
	public void testUnRegLocalcache() {
		RedisWebUtils.StartRedis();

		for (int i = 0; i < 10; i++) {
			VintageConfigWebUtils.batchregister(groupString+i, keyString, valueString, 1, 5);
		}

		for (int i = 0; i < 10; i++) {
			List<String> dataList = VintageConfigWebUtils.lookup(groupString + i);
			Map<String, String> dataMap = VintageConfigWebUtils
					.getConfigMap(dataList);
			for (int j = 0; j < 5; j++) {
				assertEquals(valueString + j, dataMap.get(keyString + j));
			}
		}

		for (int i = 0; i < 10; i++) {
			VintageConfigWebUtils.batchunregister(groupString+i, keyString, 0, 3);
		}

		for (int i = 0; i < 10; i++) {
			List<String> dataList = VintageConfigWebUtils.lookup(groupString + i);
			Map<String, String> dataMap = VintageConfigWebUtils
					.getConfigMap(dataList);
			for (int j = 0; j < 3; j++) {
				assertNull(dataMap.get(keyString + j));
			}
			for (int j = 3; j < 5; j++) {
				assertEquals(valueString + j, dataMap.get(keyString + j));
			}
		}

		RedisWebUtils.StopRedis();

		for (int i = 0; i < 10; i++) {
			List<String> dataList = VintageConfigWebUtils.lookup(groupString + i);
			Map<String, String> dataMap = VintageConfigWebUtils
					.getConfigMap(dataList);
			for (int j = 0; j < 3; j++) {
				assertNull(dataMap.get(keyString + j));
			}
			for (int j = 3; j < 5; j++) {
				assertEquals(valueString + j, dataMap.get(keyString + j));
			}
		}

		RedisWebUtils.StartRedis();
		for (int i = 0; i < 10; i++) {
			for (int j = 3; j < 5; j++) {
				VintageConfigWebUtils
						.unregister(groupString + i, keyString + j);
			}
		}
	}

	/*
	 * 
	 * 1. register configuration & lookup operation has localcache & unregister
	 * part of configuration 2. stop redis 3. check if client can get the
	 * configuration
	 */
	@Test
	public void testLocalcacheUnReg() {
		RedisWebUtils.StartRedis();

		for (int i = 0; i < 10; i++) {
			VintageConfigWebUtils.batchregister(groupString+i, keyString, valueString, 1, 5);
		}

		for (int i = 0; i < 10; i++) {
			List<String> dataList = VintageConfigWebUtils.lookup(groupString + i);
			Map<String, String> dataMap = VintageConfigWebUtils
					.getConfigMap(dataList);
			for (int j = 0; j < 5; j++) {
				assertEquals(valueString + j, dataMap.get(keyString + j));
			}
		}

		for (int i = 0; i < 10; i++) {
			VintageConfigWebUtils.batchunregister(groupString+i, keyString, 0, 3);
		}

		RedisWebUtils.StopRedis();

		for (int i = 0; i < 10; i++) {
			List<String> dataList = VintageConfigWebUtils.lookup(groupString + i);
			Map<String, String> dataMap = VintageConfigWebUtils
					.getConfigMap(dataList);
			for (int j = 0; j < 3; j++) {
				assertNull(dataMap.get(keyString + j));
			}
			for (int j = 3; j < 5; j++) {
				assertEquals(valueString + j, dataMap.get(keyString + j));
			}
		}
		
		RedisWebUtils.StartRedis();

		for (int i = 0; i < 10; i++) {
			VintageConfigWebUtils.batchunregister(groupString+i, keyString, 0, 3, 5);
		}
	}

	/*
	 * update configuration ,then stop redis localcache don't update in
	 * time,still get the last configuration
	 */
	@Test
	public void testRedisCloseConnection() {
		RedisWebUtils.StartRedis();
		VintageConfigWebUtils.register(groupString, keyString, valueString);

		List<String> dataList = VintageConfigWebUtils.lookup(groupString);
		Map<String, String> dataMap = VintageConfigWebUtils
				.getConfigMap(dataList);
		assertEquals(valueString, dataMap.get(keyString));

		String valueSecond = "valueSecond";
		VintageConfigWebUtils.register(groupString, keyString, valueSecond);

		// stop redis or close the connection or simulate bad network
		RedisWebUtils.StopRedis();

		dataList = VintageConfigWebUtils.lookup(groupString);
		dataMap = VintageConfigWebUtils
				.getConfigMap(dataList);
		assertEquals(valueSecond, dataMap.get(keyString));

		RedisWebUtils.StartRedis();
		VintageConfigWebUtils.unregister(groupString, keyString);
	}

	/*
	 * stop redis for a while
	 */
	@Test
	public void testRedisCloseWait() {
		RedisWebUtils.StartRedis();

		for (int i = 0; i < 10; i++) {
			VintageConfigWebUtils.register(groupString + i, keyString,
					valueString);
		}

		for (int i = 0; i < 10; i++) {
			List<String> dataList = VintageConfigWebUtils.lookup(groupString + i);
			Map<String, String> dataMap = VintageConfigWebUtils
					.getConfigMap(dataList);
			assertEquals(valueString, dataMap.get(keyString));
		}

		// stop redis for a while; then localcache will be updated, blank.
		RedisWebUtils.StopRedis();

		for (int i = 0; i < 10; i++) {
			List<String> dataList = VintageConfigWebUtils.lookup(groupString + i);
			Map<String, String> dataMap = VintageConfigWebUtils
					.getConfigMap(dataList);
			assertTrue(dataMap.isEmpty());
		}

		RedisWebUtils.StartRedis();
		for (int i = 0; i < 10; i++) {
			VintageConfigWebUtils.unregister(groupString + i, keyString);
		}
		
	}

	/*
	 * update configuration; stop redis; restart redis
	 */
	@Test
	public void testRedisModifyReConnection() {
		RedisWebUtils.StartRedis();
		VintageConfigWebUtils.register(groupString, keyString, valueString);

		List<String> dataList = VintageConfigWebUtils.lookup(groupString);
		Map<String, String> dataMap = VintageConfigWebUtils
				.getConfigMap(dataList);
		assertEquals(valueString, dataMap.get(keyString));

		String valueSecond = "valueSecond";
		VintageConfigWebUtils.register(groupString, keyString, valueSecond);

		RedisWebUtils.StopRedis();

		dataList = VintageConfigWebUtils.lookup(groupString);
		dataMap = VintageConfigWebUtils.getConfigMap(dataList);
		assertEquals(valueString, dataMap.get(keyString));

		RedisWebUtils.StartRedis();

		dataList = VintageConfigWebUtils.lookup(groupString);
		dataMap = VintageConfigWebUtils.getConfigMap(dataList);
		assertEquals(valueSecond, dataMap.get(keyString));

		RedisWebUtils.StartRedis();
		VintageConfigWebUtils.unregister(groupString, keyString);
	}

	/*
	 * stop redis; restart redis;update configuration
	 */
	@Test
	public void testRedisReconnectionModify() {
		RedisWebUtils.StartRedis();
		VintageConfigWebUtils.register(groupString, keyString, valueString);

		List<String> dataList = VintageConfigWebUtils.lookup(groupString);
		Map<String, String> dataMap = VintageConfigWebUtils
				.getConfigMap(dataList);
		assertEquals(valueString, dataMap.get(keyString));

		RedisWebUtils.StopRedis();

		RedisWebUtils.StartRedis();

		String valueSecond = "valueSecond";
		VintageConfigWebUtils.register(groupString, keyString, valueSecond);

		dataList = VintageConfigWebUtils.lookup(groupString);
		dataMap = VintageConfigWebUtils.getConfigMap(dataList);
		assertEquals(valueSecond, dataMap.get(keyString));

		RedisWebUtils.StartRedis();
		VintageConfigWebUtils.unregister(groupString, keyString);
	}
	
}
