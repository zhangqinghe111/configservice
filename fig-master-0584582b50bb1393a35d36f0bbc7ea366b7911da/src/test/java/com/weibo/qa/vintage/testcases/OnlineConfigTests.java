package com.weibo.qa.vintage.testcases;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.weibo.qa.vintage.online.config.testcases.ConfigGetKeysTest;
import com.weibo.qa.vintage.online.config.testcases.ConfigGetSignTest;
import com.weibo.qa.vintage.online.config.testcases.ConfigGetgroupTest;
import com.weibo.qa.vintage.online.config.testcases.ConfigLookupTest;
import com.weibo.qa.vintage.online.config.testcases.ConfigRegisterTest;

/**
 * execution time: about 5min
 * */
@RunWith(Suite.class)
@SuiteClasses({ConfigGetgroupTest.class,ConfigGetKeysTest.class,ConfigGetSignTest.class,
	ConfigLookupTest.class,ConfigRegisterTest.class})
public class OnlineConfigTests {

}