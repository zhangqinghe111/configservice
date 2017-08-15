package com.weibo.qa.vintage.testcases;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.weibo.qa.vintage.client.testcases.ClientConfigLookupTest;
import com.weibo.qa.vintage.config.testcases.GetKeysTest;
import com.weibo.qa.vintage.config.testcases.GetSignTest;
import com.weibo.qa.vintage.config.testcases.GetgroupTest;
import com.weibo.qa.vintage.config.testcases.LocalcacheTest;
import com.weibo.qa.vintage.config.testcases.LookupForUpdateTest;
import com.weibo.qa.vintage.config.testcases.LookupTest;
import com.weibo.qa.vintage.config.testcases.RegisterTest;
import com.weibo.qa.vintage.config.testcases.UnregisterTest;

@RunWith(Suite.class)
@SuiteClasses({ GetgroupTest.class, GetKeysTest.class,
		GetSignTest.class, LookupTest.class, LocalcacheTest.class,
		RegisterTest.class, UnregisterTest.class,LookupForUpdateTest.class
		 })
public class ConfigServiceTests {

}