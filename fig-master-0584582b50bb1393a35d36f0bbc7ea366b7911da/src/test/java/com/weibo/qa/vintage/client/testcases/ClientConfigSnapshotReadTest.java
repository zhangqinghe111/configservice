package com.weibo.qa.vintage.client.testcases;

import com.weibo.vintage.client.StaticsConfigServiceClient;
import com.weibo.vintage.processor.SnapshotProcessor;
import com.weibo.vintage.utils.ConfParser;
import com.weibo.vintage.utils.RedisWebUtils;
import com.weibo.vintage.utils.SwitcherUtils;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageConfigWebUtils;
import com.weibo.vintage.utils.VintageConstantsTest;
import com.weibo.vintage.utils.VintageLogger;
import redis.clients.jedis.Jedis;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhang3 on 14-7-9.
 */
public class ClientConfigSnapshotReadTest extends ConfigBaseTest {
	
    private static SnapshotProcessor snapshotProcessor;
    private Jedis jedis;
    private String m_redisIP,s_redisIP,s_redisPort;
	int m_redisPort;
    
    @BeforeClass
    public static void setUp() {
    	snapshotDir = System.getProperty("user.dir");
    	snapshotProcessor = new SnapshotProcessor(snapshotDir + "/configserver/config");
    }
    
    @Before
    public void setup(){
    	groupString = getRandomString(10);
        keyString = getRandomString(5);
        valueString = getRandomString(20);
        m_redisIP = VintageConstantsTest.REDIS_IP;
        m_redisPort = VintageConstantsTest.REDIS_PORT;
        jedis = new Jedis(m_redisIP, m_redisPort);
    	for (int i = 1; i < 4; i++){
        	File file = new File(snapshotDir + "/configserver/config/"+groupString+i+".config");
        	if (file.exists()){
        		file.delete();
        	}
        }
    	VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
    }

    @After
    public void tearDown() throws Exception{
            VintageConfigWebUtils.unregister(groupString, keyString);
    }

    //pass
     /**
     * 测试点:server可用，snapshot文件跟server一致，启动client,从snapshot读取数据
     * step:1.注册多个group的多组key
     *      2.lookup每组group
     *      3.取得每个snapshot文件
     *      4.关闭client
     *      5.启动client,启动成功，校验日志，启动数据从snapshot读取。
     * */
    @Test
    public void testServiceAvailSnapExistIdenticalServer() {
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER,true);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,true);

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

        try{
            for(String group : groupList) {
                int i = 0;
                for(String key : keyList) {
                    VintageConfigWebUtils.register(group,key,valueList.get(i));
                    i++;
                }
                configClient.lookup(group);
                snapshotProcessor.removeStaticsConfigInfo(group);
            }

            VintageNamingClientUtils.sleep(5*1000);

            //另启动一个static的client
            StaticsConfigServiceClient configClientNew = new StaticsConfigServiceClient(getVintageConfig());

            configClientNew.start();
            for(String group : groupList) {
                  configClientNew.lookup(group);
            }
            VintageNamingClientUtils.sleep(2*1000);
            for (int i = 1; i < 4; i++){
            	File file = new File(snapshotDir + "/configserver/config/"+groupString+i+".config");
            	if (!file.exists()){
            		fail("Error in testServiceAvailSnapExistIdenticalServer: no snapshot file generated");
            	}
            }
        }catch (Exception e ) {
            System.out.print(e);
        }finally {
            for(String group : groupList) {
                for(String key : keyList) {
                    VintageConfigWebUtils.unregister(group,key);
                }
            }
        }
    }

    //pass
    /**
     * 测试点：缓存读取失败，server可用，redis可用，snapshot为空，从server读取数据
     * step:1.注册多个group的多组key
     *      2.取得每个snapshot文件为空
     *      3.关闭client
     *      4.启动client,启动成功，校验日志，启动数据从server读取
     * */
    @Test
    public void testServiceAvailSnapshotNonExist() {
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER,true);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,true);
        String[] snaps = new String[3];
        int num = 0;

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

        try{
            for(String group : groupList) {
                for(String key : keyList) {
                    int i = 0;
                    VintageConfigWebUtils.register(group,key,valueList.get(i));
                    i++;
                }

                snapshotProcessor.removeStaticsConfigInfo(group);
            }

            VintageNamingClientUtils.sleep(5*1000);

            for(String group : groupList) {
                snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                System.out.print("\n" + snaps[num]);
                assertNull(snaps[num]);
                num++;
            }

            //另启动一个static的client
            StaticsConfigServiceClient configClientNew = new StaticsConfigServiceClient(getVintageConfig());

            configClientNew.start();
            for(String group : groupList) {
                configClientNew.lookup(group);
            }

            VintageNamingClientUtils.sleep(2*1000);

        }catch (Exception e ) {
            System.out.print(e);
        }finally {
            for(String group : groupList) {
                for(String key : keyList) {
                    VintageConfigWebUtils.unregister(group,key);
                }
            }
        }
    }


    //pass
    /**
     * 测试点：client启动，server可用，redis不可用，snapshot为空，启动失败
     * Notice：server端会缓存所有的serviceId，也能取得sign值，没有节点信息，sign为默认值
     * */
    @Test
    public void testServiceAvailRedisUnableSnapshotNonExist() {
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER,true);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,true);

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

        try{
            for(String group:groupList) {
                int i = 0;
                for(String key : keyList) {
                    VintageConfigWebUtils.register(group,key,valueList.get(i));
                    i++;
                }
                snapshotProcessor.removeStaticsConfigInfo(group);
            }

            VintageNamingClientUtils.sleep(5*1000);

            //!!!!关闭redis            
            RedisWebUtils.StopRedis();

            //另启动一个static的client
            StaticsConfigServiceClient configClientNew = new StaticsConfigServiceClient(getVintageConfig());

            configClientNew.start();
            for(String group : groupList) {
                VintageLogger.info("newClient lookup");
                configClientNew.lookup(group);
            }

            VintageNamingClientUtils.sleep(2*1000);

        }catch (Exception e ) {
            System.out.print(e);
        }finally {
            for(String group : groupList) {
                for(String key : keyList) {
                    VintageConfigWebUtils.unregister(group,key);
                }
            }
            RedisWebUtils.StartRedis();
        }
    }

    //pass
    /**
     * 校验点:service和redis可用，snapshot文件存在，和server的数据不一致，数据从server取得，启动成功；校验：从remote读取数据
     * 从loalCache获取数据
     * */
    @Test
    public void testServiceAvailSnapExistDiffServer() {
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER,true);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER, true);
        String[] snaps = new String[3];
        int num = 0 ;

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

        List<String> valueListNew = new ArrayList<String>();
        valueListNew.add(valueString + "1new");
        valueListNew.add(valueString + "2new");
        valueListNew.add(valueString + "3new");
        valueListNew.add(valueString + "4new");

        for(String group : groupList) {
            int i = 0;
            for(String key : keyList) {
                VintageConfigWebUtils.register(group,key,valueList.get(i));
                i++;
            }
            snapshotProcessor.removeStaticsConfigInfo(group);

            configClient.lookup(group);
        }

        VintageNamingClientUtils.sleep(5*1000);

        try{
            for(String group : groupList) {
                snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                System.out.print("\n" + snaps[num]);
                System.out.print("\n" + VintageConfigWebUtils.lookup(group));
                assertNotNull(snaps[num]);
                num++;
            }

            //更改valueList，再次注册
            for(String group : groupList) {
                int j = 0;
                for(String key : keyList) {
                    VintageConfigWebUtils.register(group,key,valueListNew.get(j));
                    j++;
                }
                configClient.lookup(group);
            }

            VintageNamingClientUtils.sleep(1*1000);

            //另启动一个static的client
            StaticsConfigServiceClient configClientNew = new StaticsConfigServiceClient(getVintageConfig());

            configClientNew.start();
            for(String group : groupList) {
                        VintageLogger.info("ClientNewLookup");
                configClientNew.lookup(group);
            }

            VintageNamingClientUtils.sleep(1*1000);

            //查看log,snapshot文件跟remote不一致，从remote读取数据
        }catch (IOException e) {
            System.out.print(e);
        }finally {
            for(String group : groupList) {
                for(String key : keyList) {
                    VintageConfigWebUtils.unregister(group,key);
                }
            }
        }
    }


    //pass
    /**
    * 测试点：启动client:server不可用，snapshot_processor开关open,容灾开关open,snapshot文件存在，从snapshot读取配置，client启动成功，会更新snapshot文件
    */
    @Test
    public void testServiceUnavailSnapOpenDisasterOpenSnapExist() {
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER,true);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,true);
        String[] snaps = new String[3];
        int num = 0;

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

        for(String group : groupList) {
            for(String key : keyList) {
                int i = 0;
                VintageConfigWebUtils.register(group,key,valueList.get(i));
                i++;
            }
            snapshotProcessor.removeStaticsConfigInfo(group);

            configClient.lookup(group);
        }

        VintageNamingClientUtils.sleep(5*1000);

        try{
            for(String group : groupList) {
                snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                System.out.print("\n" + snaps[num]);
                assertNotNull(snaps[num]);
                num++;
            }

        //关闭tomcat
            VintageNamingClientUtils.sleep(1*1000);

            //另启动一个static的client
            StaticsConfigServiceClient configClientNew = new StaticsConfigServiceClient(getVintageConfig());

            configClientNew.start();
            for(String group : groupList) {
                VintageLogger.info("ClientNewLookup");
                configClientNew.lookup(group);
            }

        //查看log,server连接不上，从snapshot读取data
        }catch (IOException e) {
            System.out.print(e);
        }finally {
            for(String group : groupList) {
                for(String key : keyList) {
                    VintageConfigWebUtils.unregister(group,key);
                }
            }
        }
    }

    //pass,只读取sign值来判断么？？？
    /**
     * 测试点：启动client:server不可用，snapshot_processor开关open,容灾开关open,snapshot文件存在，更改snapshot文件，从snapshot读取配置，client启动成功
     */
    @Test
    public void testServiceUnavailSnapOpenDisasterOpenSnapExistModifySnap() {
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER,true);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,true);
        String[] snaps = new String[3];
        int num = 0;

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

        for(String group : groupList) {
            for(String key : keyList) {
                int i = 0;
                VintageConfigWebUtils.register(group,key,valueList.get(i));
                i++;
            }
            snapshotProcessor.removeStaticsConfigInfo(group);

            configClient.lookup(group);
        }

        VintageNamingClientUtils.sleep(6*1000);

        try{
            for(String group : groupList) {
                snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                System.out.print("\n" + snaps[num]);
                assertNotNull(snaps[num]);
                num++;
            }

            //关闭tomcat
            //更改snapshot文件

            //另启动一个static的client
            StaticsConfigServiceClient configClientNew = new StaticsConfigServiceClient(getVintageConfig());

            configClientNew.start();
            for(String group : groupList) {
                VintageLogger.info("newclientLookup");
                configClientNew.lookup(group);
            }

            VintageNamingClientUtils.sleep(5*1000);

            //查看log,server连接不上，从snapshot读取文件，查看newClient启动的文件跟snapshot文件一致
        }catch (IOException e) {
            System.out.print(e);
        }finally {
            for(String group : groupList) {
                for(String key : keyList) {
                    VintageConfigWebUtils.unregister(group,key);
                }
            }
        }
    }
    //pass
    /**
     * 测试点:server不可用，snapshot_processor开关open,容灾开关为open,snapshot文件不存在，client启动失败；会更新snapshot文件
     */
    @Test
    public void testServiceUnavailSnapOpenDisasterOpenSnapNonExist() {
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER,true);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,true);
        String[] snaps = new String[3];
        int num = 0;

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

        for(String group : groupList) {
            int i = 0;
            for(String key : keyList) {
                VintageConfigWebUtils.register(group,key,valueList.get(i));
                i++;
            }
            snapshotProcessor.removeStaticsConfigInfo(group);
        }

        VintageNamingClientUtils.sleep(16*1000);

        try{
            for(String group : groupList) {
                snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                System.out.print("\n" + snaps[num]);
                assertNull(snaps[num]);
                num++;
            }

            //关闭tomcat

             VintageNamingClientUtils.sleep(1*1000);

            //另启动一个static的client
            StaticsConfigServiceClient configClientNew = new StaticsConfigServiceClient(getVintageConfig());

            configClientNew.start();
            for(String group : groupList) {
                VintageLogger.info("newClientLookup");
                configClientNew.lookup(group);
            }

            VintageNamingClientUtils.sleep(5*1000);

            //查看log,server连接失败，从snapshot读取文件失败
        }catch (IOException e) {
            System.out.print(e);
        }finally {
            for(String group : groupList) {
                for(String key : keyList) {
                    VintageConfigWebUtils.unregister(group,key);
                }
            }
        }
    }

    //pass
    /**
     * 测试点：启动client:server不可用，snapshot_processor开关open,容灾开关close,snapshot文件存在，不从snapshot读取配置，client启动失败;会更新snapshot文件
     */
    @Test
    public void testServiceUnavailSnapOpenDisasterCloseSnapExist() {
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER,true);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,false);
        String[] snaps = new String[3];
        int num = 0;

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

        for(String group : groupList) {
            for(String key : keyList) {
                int i = 0;
                VintageConfigWebUtils.register(group,key,valueList.get(i));
                i++;
            }
            snapshotProcessor.removeStaticsConfigInfo(group);
            configClient.lookup(group);
        }

        VintageNamingClientUtils.sleep(6*1000);

        try{
            for(String group : groupList) {
                snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                System.out.print("\n" + snaps[num]);
                assertNotNull(snaps[num]);
                num++;
            }

        VintageNamingClientUtils.sleep(1*1000);
            //关闭tomcat

            //另启动一个static的client
            StaticsConfigServiceClient configClientNew = new StaticsConfigServiceClient(getVintageConfig());

            configClientNew.start();
            for(String group : groupList) {
                VintageLogger.info("newClientLookup");
                configClientNew.lookup(group);
            }

            VintageNamingClientUtils.sleep(5*1000);

            //查看log,server连接不上，不从snapshot读取文件，启动失败
        }catch (IOException e) {
            System.out.print(e);
        }finally {
            for(String group : groupList) {
                for(String key : keyList) {
                    VintageConfigWebUtils.unregister(group,key);
                }
            }
        }
    }

    //todo:totest
    /**
     * 测试点：启动client,server不可用，snapshot_processor开关close,容灾开关open，snapshot文件存在，snapshot文件不会被更新，client启动失败
     * Notice:在执行该用例之前，snapshot文件存在，最好和client启动以后要更新的文件不一致
     */
    @Test
    public void testServiceUnavailSnapClosedDisasterOpenSnapExist() {
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER,false);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,true);
        String[] snaps = new String[3];
        int num = 0;

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

        for(String group : groupList) {
            for(String key : keyList) {
                int i = 0;
                VintageConfigWebUtils.register(group,key,valueList.get(i));
                i++;
            }
            snapshotProcessor.removeStaticsConfigInfo(group);
            configClient.lookup(group);
        }

        VintageNamingClientUtils.sleep(16*1000);

        try{
            for(String group : groupList) {
                snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                System.out.print("\n" + snaps[num]);
                assertNull(snaps[num]);
                num++;
            }

        VintageNamingClientUtils.sleep(1*1000);
            //关闭tomcat

            //另启动一个static的client
            StaticsConfigServiceClient configClientNew = new StaticsConfigServiceClient(getVintageConfig());

            configClientNew.start();
            for(String group : groupList) {
                VintageLogger.info("newClientLookup");
                configClientNew.lookup(group);
            }

            VintageNamingClientUtils.sleep(5*1000);

        }catch (IOException e) {
            System.out.print(e);
        }finally {
            for(String group : groupList) {
                for(String key : keyList) {
                    VintageConfigWebUtils.unregister(group,key);
                }
            }
        }
    }

    //pass
    /**
     * 测试点：启动client,server不可用，snapshot_processor开关close,容灾开关open，snapshot文件不存在，snapshot文件不会被更新，client启动失败
     */
    @Test
    public void testServiceUnavailSnapClosedDisasterOpenSnapNonExist() {
//        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER,false);
//        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,true);
        String[] snaps = new String[3];
        int num = 0;

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

        for(String group : groupList) {
            for(String key : keyList) {
                int i = 0;
                VintageConfigWebUtils.register(group,key,valueList.get(i));
                i++;
            }
            configClient.lookup(group);
            snapshotProcessor.removeStaticsConfigInfo(group);
        }

        //校验文件不会被更新
        VintageNamingClientUtils.sleep(15*1000);

        try{
            for(String group : groupList) {
                snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                System.out.print("\n" + snaps[num]);
                assertNotNull(snaps[num]);
                num++;
            }

            //关闭tomcat

            //另启动一个static的client
            StaticsConfigServiceClient configClientNew = new StaticsConfigServiceClient(getVintageConfig());

            configClientNew.start();
            for(String group : groupList) {
                configClientNew.lookup(group);
            }

            VintageNamingClientUtils.sleep(5*1000);

        }catch (IOException e) {
            System.out.print(e);
        }finally {
            for(String group : groupList) {
                for(String key : keyList) {
                    VintageConfigWebUtils.unregister(group,key);
                }
            }
        }
    }

    /**
     * 测试点：server不可用，snapshot_processor开关close,容灾开关close,snapshot文件存在，client启动失败，snapshot文件不会被更新
     * Notice:snapshot文件存在
     */
    @Test
    public void testServiceUnavailSnapClosedDisasterClosedSnapNonExist() {
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER,true);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,true);
        String[] snaps = new String[3];
        int num = 0;

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

        for(String group : groupList) {
            for(String key : keyList) {
                int i = 0;
                VintageConfigWebUtils.register(group,key,valueList.get(i));
                i++;
            }
            configClient.lookup(group);
            snapshotProcessor.removeStaticsConfigInfo(group);
        }

        VintageNamingClientUtils.sleep(15*1000);

        try{
            for(String group : groupList) {
                snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                System.out.print("\n" + snaps[num]);
                assertNull(snaps[num]);
                num++;
            }

            SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER,false);
            SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,false);

            VintageNamingClientUtils.sleep(1*1000);

            //!!!!!关闭tomcat

            //另启动一个static的client
            StaticsConfigServiceClient configClientNew = new StaticsConfigServiceClient(getVintageConfig());

            configClientNew.start();
            for(String group : groupList) {
                configClientNew.lookup(group);
            }

        }catch (IOException e) {
            System.out.print(e);
        }finally {
            for(String group : groupList) {
                for(String key : keyList) {
                    VintageConfigWebUtils.unregister(group,key);
                }
            }
        }
    }
}
