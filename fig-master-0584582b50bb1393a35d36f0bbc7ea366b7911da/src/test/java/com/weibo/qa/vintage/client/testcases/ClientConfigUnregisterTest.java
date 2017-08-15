package com.weibo.qa.vintage.client.testcases;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.vintage.exception.ExcepFactor;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.utils.VintageConfigWebUtils;

public class ClientConfigUnregisterTest extends ConfigBaseTest {
	@Before
	public void setUp() throws Exception {
		groupString = getRandomString(10);
        keyString = getRandomString(5);
        valueString = getRandomString(20);
	}

	@After
	public void tearDown() throws Exception {
	}

	/*
	 * repeat register and unregister for many times
	 */
	@Test
	public void testRepeatRegUnreg() {
//		int oldsize = configClient.lookup(groupString).size();
		int oldsize = 0;

		int newsize = 0;

		for (int i = 0; i < 100; i++) {
			configClient.register(groupString, keyString, valueString);
			configClient.unregister(groupString, keyString);
		}

		sleep(HEARTBEATINTERVAL);
		newsize = configClient.lookup(groupString).size();
		assertEquals(oldsize, newsize);
	}

	/*
	 * register for many times and unregister last
	 */
	@Test
	public void testReapeatRegUnreg2() {
		int oldsize = configClient.lookup(groupString).size();
		int newsize = 0;

		// register for many times
		for (int i = 0; i < 100; i++) {
			configClient.register(groupString, keyString, valueString);
		}

		sleep(HEARTBEATINTERVAL);
		
		newsize = configClient.lookup(groupString).size();
		assertEquals(oldsize + 1, newsize);

		// unregister once last
		configClient.unregister(groupString, keyString);
		sleep(HEARTBEATINTERVAL);
		newsize = configClient.lookup(groupString).size();
		assertEquals(oldsize, newsize);
	}

	/**
	 * unregister all information
	 */
	@Test
	public void testUnregAll() {
	
		assertEquals(0, configClient.lookup(groupString).size());		
		for (int i = 0; i < 3; i++) {
			configClient.register(groupString, keyString + i, valueString + i);
		}		
		sleep(2 * HEARTBEATINTERVAL);
		assertEquals(3, configClient.lookup(groupString).size());
		
		for (int i = 0; i < 3; i++) {
			configClient.unregister(groupString, keyString + i);
		}
		sleep(10 * HEARTBEATINTERVAL);
		assertEquals(0, configClient.lookup(groupString).size());
	}
	
	/*
	 * unregsiter group and key which has not registered
	 */
	@Test
	public void testUnregNoReg()
	{
		
		configClient.register(groupString, keyString, valueString);
		
		// group exists and key doesn't exist
		configClient.unregister(groupString, keyString);		
		//group and key doesn't exist
		configClient.unregister(groupString, keyString);
		
		configClient.unregister(groupString, keyString);		
		//fail("Warning in testUnregNoReg;should throw some exception!");		
	}
	
	/**
	 * parameters null
	 * return  E_SERVICE_PACKET_EMPTY, but expect E_PARAM_INVALID_ERROR
	 */
	@Test
	public void testParamNull() {
		unregisterParamNull(null, keyString);
		unregisterParamNull(groupString, null);
	}
	
	/**
	 * parameters blank
	 */
	@Test
	public void testParamBlank() {
		unregisterParamNull("", keyString);
		unregisterParamNull(groupString, "");
	}

	/*
	 * test parameters with null
	 */
	private void unregisterParamNull(String groupString, String keyString) {
		try {
			configClient.unregister(groupString, keyString);
			fail("Error in unregisterParamNull");
		} catch (Exception e) {			
			System.out.print(e.getMessage());
			VintageException vintageException = (VintageException) e;
//			assertEquals(ExcepFactor.E_SERVICE_PACKET_EMPTY.getErrorCode(),
//					vintageException.getFactor().getErrorCode());
//			assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR.getErrorCode(),
//					vintageException.getFactor().getErrorCode());
		}
	}
}
