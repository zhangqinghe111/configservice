package com.weibo.qa.vintage.testcases;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.weibo.qa.vintage.liuyu.testcases.GetWorkingNodeAllUnreachableTest;
import com.weibo.qa.vintage.liuyu.testcases.GetWorkingNodeAllUnreachableTest2;

@RunWith(Suite.class)
@SuiteClasses({
			GetWorkingNodeAllUnreachableTest.class, GetWorkingNodeAllUnreachableTest2.class})
public class newInterfaceTests {

}
