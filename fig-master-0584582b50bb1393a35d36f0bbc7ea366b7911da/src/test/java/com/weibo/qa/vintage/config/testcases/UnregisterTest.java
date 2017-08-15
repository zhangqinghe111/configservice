package com.weibo.qa.vintage.config.testcases;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.vintage.exception.ExcepFactor;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.utils.VintageConfigWebUtils;

public class UnregisterTest extends BaseTest {
	@Before
	public void setUp() throws Exception {
		groupString = getRandomString(10);
		keyString = getRandomString(5);
		valueString = getRandomString(20);
	}

	@After
	public void tearDown() throws Exception {
	}

	/**
	 * unregister all information
	 */
	@Test
	public void testUnregAll() {
		assertEquals(0, VintageConfigWebUtils.lookup(groupString).size());	
		VintageConfigWebUtils.batchregister(groupString, keyString, valueString, 1, 3);
		
		assertEquals(3, VintageConfigWebUtils.lookup(groupString).size());
		
		VintageConfigWebUtils.batchunregister(groupString, keyString, 0, 3);

		assertEquals(0, VintageConfigWebUtils.lookup(groupString).size());
	}
	
	/*
	 * repeat register and unregister for many times
	 */
	@Test
	public void testRepeatRegUnreg() {
		int oldsize = VintageConfigWebUtils.lookup(groupString).size();
		int newsize = 0;

		VintageConfigWebUtils.batchreg_unreg(groupString, keyString, valueString, 2, 100);

		newsize = VintageConfigWebUtils.lookup(groupString).size();
		assertEquals(oldsize, newsize);
	}

	/*
	 * register for many times and unregister last
	 */
	@Test
	public void testReapeatRegUnreg2() {
		int oldsize = VintageConfigWebUtils.lookup(groupString).size();
		int newsize = 0;

		// register for many times
		VintageConfigWebUtils.batchregister(groupString, keyString, valueString, 2, 100);
		
		newsize = VintageConfigWebUtils.lookup(groupString).size();
		assertEquals(oldsize + 1, newsize);

		// unregister once last
		VintageConfigWebUtils.unregister(groupString, keyString);
		newsize = VintageConfigWebUtils.lookup(groupString).size();
		assertEquals(oldsize, newsize);
	}


	/*
	 * unregsiter group and key which has not registered，不抛异常
	 */
	@Test
	public void testUnregNoReg()
	{
		VintageConfigWebUtils.register(groupString, keyString, valueString);
		
		// group exists and key doesn't exist
		VintageConfigWebUtils.unregister(groupString, keyString);		
		//group and key doesn't exist
		VintageConfigWebUtils.unregister(groupString, keyString);
		
		VintageConfigWebUtils.unregister(groupString, keyString);		
		//fail("Warning in testUnregNoReg;should throw some exception!");		
	}
	
	/**
	 * parameters null
	 * return  E_SERVICE_PACKET_EMPTY, but expect E_PARAM_INVALID_ERROR
	 */
	@Test
	public void testParamNull() {
		try {
			VintageConfigWebUtils.unregister(null, keyString);
		} catch (Exception e) {
			fail("Error in unregisterParamNull");
		}
		
		try {
			VintageConfigWebUtils.unregister(groupString, null);
		} catch (Exception e) {
			fail("Error in unregisterParamNull");
		}
		
		try {
			VintageConfigWebUtils.unregister("", keyString);
			fail("Error in unregisterParamNull");
		} catch (Exception e) {
		}
		
		try {
			VintageConfigWebUtils.unregister(groupString, "");
			fail("Error in unregisterParamNull");
		} catch (Exception e) {
		}
	}
}
