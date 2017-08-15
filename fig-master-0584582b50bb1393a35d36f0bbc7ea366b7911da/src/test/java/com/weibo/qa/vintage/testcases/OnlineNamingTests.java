package com.weibo.qa.vintage.testcases;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.weibo.qa.vintage.online.naming.testcases.SubscribeTest;
import com.weibo.qa.vintage.online.naming.testcases.LookupforupdateTest;
import com.weibo.qa.vintage.online.naming.testcases.NamingNodeStatusTest;
import com.weibo.qa.vintage.online.naming.testcases.NamingRegisterTest;
import com.weibo.qa.vintage.online.naming.testcases.NamingWhitelistTest;
import com.weibo.qa.vintage.online.naming.testcases.NaminggetSignTest;

@RunWith(Suite.class)
//NamingAdminTest.class, 
@SuiteClasses({
	NaminggetSignTest.class,NamingRegisterTest.class,NamingWhitelistTest.class,
	LookupforupdateTest.class, SubscribeTest.class, NamingNodeStatusTest.class})
public class OnlineNamingTests {

}