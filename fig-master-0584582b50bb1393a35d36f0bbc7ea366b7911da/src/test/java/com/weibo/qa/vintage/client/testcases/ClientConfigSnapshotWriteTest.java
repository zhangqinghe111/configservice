package com.weibo.qa.vintage.client.testcases;

import com.weibo.qa.vintage.naming.testcases.*;
import com.weibo.vintage.model.VintageConfig;
import com.weibo.vintage.processor.SnapshotProcessor;
import com.weibo.vintage.utils.JsonHelper;
import com.weibo.vintage.utils.SwitcherUtils;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageConfigWebUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.IOException;

/**
 * Created by zhang3 on 14-7-8.
 */
public class ClientConfigSnapshotWriteTest extends ConfigBaseTest{
   private static SnapshotProcessor snapshotProcessor;

   @BeforeClass
   public static void setUp() {
   	snapshotDir = System.getProperty("user.dir");
   	snapshotProcessor = new SnapshotProcessor(snapshotDir + "/configserver/config");
   }

   @After
   public void tearDown() throws Exception {
       VintageConfigWebUtils.unregister(groupString, keyString);
       VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
   }

   @Before
   public void init(){
	   groupString = getRandomString(10);
       keyString = getRandomString(5);
       valueString = getRandomString(20);
   }
   //pass
   /**
    * 测试点:注册多个group,不lookup,snapshot文件不会更新
    * */
   @Test
   public void testAllNoLookup() {
       SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER, true);
       SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,true);
       String[] snaps = new String[3];
       String[] serverValues = new String[3];

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
               snapshotProcessor.removeStaticsConfigInfo(group);
           }

           VintageNamingClientUtils.sleep(16*1000);

           int i = 0;
           for(String group : groupList) {
               //server的数据
                serverValues[i] = VintageConfigWebUtils.lookupForAll(group);
                System.out.print("\n servalues[" + i + "]" + serverValues[i]);
               //snapshotFileCotent
                snaps[i] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                assertNull(snaps[i]);
                i++;
           }

       }catch (IOException e) {
           System.out.print("IOEXCEPTION");
       }
       finally {
           for(String group : groupList) {
               for(String key : keyList) {
                   VintageConfigWebUtils.unregister(group,key);
               }
           }
       }
   }


    //pass
    /**
     * 测试点:注册多个group,lookup部分group的部分key,lookup的group的文件不更新
     * notice：有时候remove文件不生效，需要提前把文件删除
     * */
    @Test
    public void testPartGroupPartkeyLookup() {
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER, true);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,true);
        String[] snaps = new String[3];

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
                snapshotProcessor.removeStaticsConfigInfo(group);
            }

            configClient.lookup(groupList.get(0),keyList.get(0));

            VintageNamingClientUtils.sleep(16*1000);

            int i = 0;
            String temp[] = new String[3];
            for(String group : groupList) {
                snaps[i] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                temp[i] =  VintageConfigWebUtils.lookupForAll(group);
                System.out.print("\n snaps[" + i + "]" + snaps[i]);
                System.out.print("\n serverValues[" + i + "]" + temp[i]);
                assertNull(snaps[i]);

                i++;
            }
        }catch (IOException e) {
            System.out.print(e);
        }finally {
            for(String group : groupList ){
                for(String key : keyList) {
                    VintageConfigWebUtils.unregister(group,key);
                }
            }
        }
    }

    //passed
    /**
     * 测试点:注册多个group,部分lookup,lookup的group的snapshot文件会更新
     * */
    @Test
    public void testPartGroupLookup() {
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER, true);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,true);
        String[] snaps = new String[3];
        String[] serverSign = new String[3];
        String[] snapsSign = new String[3];

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
                snapshotProcessor.removeStaticsConfigInfo(group);
            }

            configClient.lookup(groupList.get(0));

            VintageNamingClientUtils.sleep(16*1000);

            int i = 0;
            String temp[] = new String[3];
            for(String group : groupList) {
                snaps[i] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                temp[i] =  VintageConfigWebUtils.lookupForAll(group);

                System.out.print("\n snaps[" + i + "]" + snaps[i]);
                System.out.print("\n serverValues[" + i + "]" + temp[i]);
                if(i==0) {
                    serverSign[i] =  JsonHelper.parserStringToJsonNode(temp[i]).getFieldValue("body").getFieldValue("sign").toString();
                    snapsSign[i] = JsonHelper.parserStringToJsonNode(snaps[i]).getFieldValue("sign").toString();
                assertEquals(snapsSign[i],serverSign[i]);
                }else {
                   assertNull(snaps[i]);
                }
                i++;
            }
        }catch (Exception e) {
            System.out.print(e);
        }finally {
            for(String group : groupList ){
                for(String key : keyList) {
                    VintageConfigWebUtils.unregister(group,key);
                }
            }
        }
    }

    //passed
    /**
     * 测试点:注册多个group,全部lookup,lookup的group的snapshot文件会更新，且和server端的数据一致
     * */
   @Test
   public void testAllLookup() {
       SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER, true);
       SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,true);
       String[] snaps = new String[3];
       String[] serverSign = new String[3];
       String[] snapsSign = new String[3];

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
            for(String group : groupList){
                int i = 0;
                for(String key : keyList) {
                    VintageConfigWebUtils.register(group,key,valueList.get(i));
                    i++;
                }
                snapshotProcessor.removeStaticsConfigInfo(group);

                configClient.lookup(group);
            }

            VintageNamingClientUtils.sleep(16*1000);
            int i =  0;
            for(String group : groupList) {
                String[] temp = new String[3];
                snaps[i] = snapshotProcessor.getConfigInfoFromSnapshot(group, null);
                temp[i] =  VintageConfigWebUtils.lookupForAll(group);
                serverSign[i] = JsonHelper.parserStringToJsonNode(temp[i]).getFieldValue("body").getFieldValue("sign").toString();
                snapsSign[i] = JsonHelper.parserStringToJsonNode(snaps[i]).getFieldValue("sign").toString();
                System.out.print("\n snaps[" + i + "]" + snaps[i]);
                System.out.print("\n serverValues[" + i + "]" + temp[i]);
                assertEquals(snapsSign[i], serverSign[i]);
                i++;
             }
       }catch(IOException e){
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
    * 测试点：lookup所有的group,修改部分group的节点信息，snapshot文件会更新
    * */
    @Test
    public void testModifyPartGroup() {
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER, true);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,true);
        String[] snaps = new String[6];
        String[] serverSign = new String[6];
        String[] snapsSign = new String[6];

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


        List<String> valueList1 = new ArrayList<String>();
        valueList1.add(valueString + "1new");
        valueList1.add(valueString + "2new");

        try{
            for(String group : groupList){
                for(String key : keyList) {
                    int i = 0;
                    VintageConfigWebUtils.register(group,key,valueList.get(i));
                    i++;
                }
                 snapshotProcessor.removeStaticsConfigInfo(group);
                configClient.lookup(group);
            }

            VintageNamingClientUtils.sleep(16*1000);

            int i = 0;
            String temp[] = new String[6];
            for(String group : groupList){
                snaps[i] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                temp[i] =  VintageConfigWebUtils.lookupForAll(group);
                serverSign[i] = JsonHelper.parserStringToJsonNode(temp[i]).getFieldValue("body").getFieldValue("sign").toString();
                snapsSign[i] = JsonHelper.parserStringToJsonNode(snaps[i]).getFieldValue("sign").toString();
                System.out.print("\n snaps[" + i + "]" + snaps[i]);
                System.out.print("\n serverValues[" + i + "]" + temp[i]);
                assertEquals(serverSign[i],snapsSign[i]);
                i++;
            }

            for(int m = 0; m < 2;m++) {
                VintageConfigWebUtils.register(groupList.get(0),keyList.get(0),valueList1.get(m));
            }

            VintageNamingClientUtils.sleep(6*1000);

            for(String group : groupList){
                snaps[i] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                temp[i] =  VintageConfigWebUtils.lookupForAll(group);
                serverSign[i] = JsonHelper.parserStringToJsonNode(temp[i]).getFieldValue("body").getFieldValue("sign").toString();
                snapsSign[i] = JsonHelper.parserStringToJsonNode(snaps[i]).getFieldValue("sign").toString();
                System.out.print("\n snaps[" + i + "]" + snaps[i]);
                System.out.print("\n serverValues[" + i + "]" + temp[i]);
                assertEquals(serverSign[i],snapsSign[i]);
                i++;
            }

            for(int n = 0; n<3; n++) {
                if( n == 0) {
                    assertFalse(snapsSign[n].contentEquals(serverSign[n+3]));
                    continue;
                }
                assertEquals(snapsSign[n],snapsSign[n+3]);
            }
        }catch (IOException e) {
            System.out.print("IOEXCEPTION");
        }finally {
            for(String group : groupList) {
                for(String key : keyList) {
                    VintageConfigWebUtils.unregister(group,key);
                }
            }
        }
    }

    /**
     * 测试点：modify某一组group的所有key，lookup,该组的snapshot文件全部更新
     * */
    @Test
    public void testModifyOneServerAllkey() {
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER, true);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,true);
        String[] snaps = new String[6];
        String[] serverSign = new String[6];
        String[] snapsSign = new String[6];

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


        List<String> valueList1 = new ArrayList<String>();
        valueList1.add(valueString + "1new");
        valueList1.add(valueString + "2new");

        try{
            for(String group : groupList){
                int i = 0;
                for(String key : keyList) {
                    VintageConfigWebUtils.register(group,key,valueList.get(i));
                    i++;
                }
                snapshotProcessor.removeStaticsConfigInfo(group);                                                                                                                  snapshotProcessor.removeStaticsConfigInfo(group);
                configClient.lookup(group);
            }

            VintageNamingClientUtils.sleep(16*1000);

            int i = 0;
            String temp[] = new String[6];
            for(String group : groupList){
                snaps[i] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                temp[i] =  VintageConfigWebUtils.lookupForAll(group);
                serverSign[i] = JsonHelper.parserStringToJsonNode(temp[i]).getFieldValue("body").getFieldValue("sign").toString();
                snapsSign[i] = JsonHelper.parserStringToJsonNode(snaps[i]).getFieldValue("sign").toString();
                System.out.print("\n snaps[" + i + "]" + snaps[i]);
                System.out.print("\n serverValues[" + i + "]" + temp[i]);
                assertEquals(serverSign[i],snapsSign[i]);
                i++;
            }

            for(int m = 0; m < 2;m++) {
                VintageConfigWebUtils.register(groupList.get(0),keyList.get(m),valueList1.get(m));
            }

            VintageNamingClientUtils.sleep(6*1000);

            for(String group : groupList){
                snaps[i] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                temp[i] =  VintageConfigWebUtils.lookupForAll(group);
                serverSign[i] = JsonHelper.parserStringToJsonNode(temp[i]).getFieldValue("body").getFieldValue("sign").toString();
                snapsSign[i] = JsonHelper.parserStringToJsonNode(snaps[i]).getFieldValue("sign").toString();
                System.out.print("\n snaps[" + i + "]" + snaps[i]);
                System.out.print("\n serverValues[" + i + "]" + temp[i]);
                assertEquals(serverSign[i],snapsSign[i]);
                i++;
            }

            for(int n = 0; n<3; n++) {
                if( n == 0) {
                    assertFalse(snapsSign[n].contentEquals(snapsSign[n+3]));
                    continue;
                }
                    assertEquals(snapsSign[n],snapsSign[n+3]);
            }
        }catch (IOException e) {
            System.out.print("IOEXCEPTION");
        }finally {
            for(String group : groupList) {
                for(String key : keyList) {
                    VintageConfigWebUtils.unregister(group,key);
                }
            }
        }
    }

    /**
     * 测试点:client取消注册部分group的部分key
     * */
    @Test
    public void testClientUnregisterPartGroupPartKey() {
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER, true);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,true);
        String[] snaps = new String[6];
        String[] serverSign = new String[6];
        String[] snapsSign = new String[6];

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
            for(String group : groupList){
                for(String key : keyList) {
                    int i = 0;
                    VintageConfigWebUtils.register(group,key,valueList.get(i));
                    i++;
                }
                snapshotProcessor.removeStaticsConfigInfo(group);
                configClient.lookup(group);
            }

            VintageNamingClientUtils.sleep(16*1000);

            int i = 0;
            String temp[] = new String[6];
            for(String group : groupList){
                snaps[i] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                temp[i] =  VintageConfigWebUtils.lookupForAll(group);
                serverSign[i] = JsonHelper.parserStringToJsonNode(temp[i]).getFieldValue("body").getFieldValue("sign").toString();
                snapsSign[i] = JsonHelper.parserStringToJsonNode(snaps[i]).getFieldValue("sign").toString();
                System.out.print("\n snaps[" + i + "]" + snaps[i]);
                System.out.print("\n serverValues[" + i + "]" + temp[i]);
                assertEquals(serverSign[i],snapsSign[i]);
                i++;
            }

            //client unregister
            configClient.unregister(groupList.get(0),keyList.get(0));

            VintageNamingClientUtils.sleep(6*1000);

            for(String group : groupList){
                snaps[i] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                temp[i] =  VintageConfigWebUtils.lookupForAll(group);
                serverSign[i] = JsonHelper.parserStringToJsonNode(temp[i]).getFieldValue("body").getFieldValue("sign").toString();
                snapsSign[i] = JsonHelper.parserStringToJsonNode(snaps[i]).getFieldValue("sign").toString();
                System.out.print("\n snaps[" + i + "]" + snaps[i]);
                System.out.print("\n serverValues[" + i + "]" + temp[i]);
                assertEquals(serverSign[i],snapsSign[i]);
                i++;
            }

            for(int n = 0; n<3; n++) {
                if( n == 0) {
                    assertFalse(snapsSign[n].contentEquals(serverSign[n+3]));
                }
                else{
                    assertEquals(snapsSign[n],snapsSign[n+3]);
                }
            }
        }catch (IOException e) {
            System.out.print("IOEXCEPTION");
        }finally {
            for(String group : groupList) {
                for(String key : keyList) {
                    VintageConfigWebUtils.unregister(group,key);
                }
            }
        }
    }

    /**
     * 测试点:server取消注册部分group的部分节点的部分key
     * */
    @Test
    public void testServerUnregisterPartGroupPartKey() {
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER, true);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,true);
        String[] snaps = new String[6];
        String[] serverSign = new String[6];
        String[] snapsSign = new String[6];

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
            for(String group : groupList){
                for(String key : keyList) {
                    int i = 0;
                    VintageConfigWebUtils.register(group,key,valueList.get(i));
                    i++;
                }
                snapshotProcessor.removeStaticsConfigInfo(group);
                configClient.lookup(group);
            }

            VintageNamingClientUtils.sleep(16*1000);

            int i = 0;
            String temp[] = new String[6];
            for(String group : groupList){
                snaps[i] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                temp[i] =  VintageConfigWebUtils.lookupForAll(group);
                serverSign[i] = JsonHelper.parserStringToJsonNode(temp[i]).getFieldValue("body").getFieldValue("sign").toString();
                snapsSign[i] = JsonHelper.parserStringToJsonNode(snaps[i]).getFieldValue("sign").toString();
                System.out.print("\n snaps[" + i + "]" + snaps[i]);
                System.out.print("\n serverValues[" + i + "]" + temp[i]);
                assertEquals(serverSign[i],snapsSign[i]);
                i++;
            }

            //server unregister
            VintageConfigWebUtils.unregister(groupList.get(0),keyList.get(0));

            VintageNamingClientUtils.sleep(6*1000);

            for(String group : groupList){
                snaps[i] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                temp[i] =  VintageConfigWebUtils.lookupForAll(group);
                serverSign[i] = JsonHelper.parserStringToJsonNode(temp[i]).getFieldValue("body").getFieldValue("sign").toString();
                snapsSign[i] = JsonHelper.parserStringToJsonNode(snaps[i]).getFieldValue("sign").toString();
                System.out.print("\n snaps[" + i + "]" + snaps[i]);
                System.out.print("\n serverValues[" + i + "]" + temp[i]);
                assertEquals(serverSign[i],snapsSign[i]);
                i++;
            }

            for(int n = 0; n<3; n++) {
                if( n == 0) {
                    assertFalse(snapsSign[n].contentEquals(serverSign[n+3]));
                    continue;
                }
                    assertEquals(snapsSign[n],snapsSign[n+3]);
            }
        }catch (IOException e) {
            System.out.print("IOEXCEPTION");
        }finally {
            for(String group : groupList) {
                for(String key : keyList) {
                    VintageConfigWebUtils.unregister(group,key);
                }
            }
        }
    }


    /**
     * 测试点：client取消注册部分group的全部key
     * */
    @Test
    public void testClientUnregisterPartServerAllNode() {
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER, true);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,true);
        String[] snaps = new String[6];
        String[] serverSign = new String[6];
        String[] snapsSign = new String[6];

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
            for(String group : groupList){
                for(String key : keyList) {
                    int i = 0;
                    VintageConfigWebUtils.register(group,key,valueList.get(i));
                    i++;
                }
                snapshotProcessor.removeStaticsConfigInfo(group);
                configClient.lookup(group);
            }

            VintageNamingClientUtils.sleep(16*1000);

            int i = 0;
            String temp[] = new String[6];
            for(String group : groupList){
                snaps[i] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                temp[i] =  VintageConfigWebUtils.lookupForAll(group);
                serverSign[i] = JsonHelper.parserStringToJsonNode(temp[i]).getFieldValue("body").getFieldValue("sign").toString();
                snapsSign[i] = JsonHelper.parserStringToJsonNode(snaps[i]).getFieldValue("sign").toString();
                System.out.print("\n snaps[" + i + "]" + snaps[i]);
                System.out.print("\n serverValues[" + i + "]" + temp[i]);
                assertEquals(serverSign[i],snapsSign[i]);
                i++;
            }

            //client unregister
            for(String key : keyList) {
                configClient.unregister(groupList.get(0),key);
            }

            VintageNamingClientUtils.sleep(6*1000);

            for(String group : groupList){
                snaps[i] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                temp[i] =  VintageConfigWebUtils.lookupForAll(group);
                serverSign[i] = JsonHelper.parserStringToJsonNode(temp[i]).getFieldValue("body").getFieldValue("sign").toString();
                snapsSign[i] = JsonHelper.parserStringToJsonNode(snaps[i]).getFieldValue("sign").toString();
                System.out.print("\n snaps[" + i + "]" + snaps[i]);
                System.out.print("\n serverValues[" + i + "]" + temp[i]);
                assertEquals(serverSign[i],snapsSign[i]);
                i++;
            }

            for(int n = 0; n<3; n++) {
                if( n == 0) {
                    assertFalse(snapsSign[n].contentEquals(snapsSign[n+3]));
                }
                else{
                    assertEquals(snapsSign[n],snapsSign[n+3]);
                }
            }
        }catch (IOException e) {
            System.out.print("IOEXCEPTION");
        }finally {
            for(String group : groupList) {
                for(String key : keyList) {
                    VintageConfigWebUtils.unregister(group,key);
                }
            }
        }
    }


    /**
     * 测试点:server取消注册某个group的所有节点
     * */
    @Test
    public void testServerUnregisterPartServerAllNode() {
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER, true);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,true);
        String[] snaps = new String[6];
        String[] serverSign = new String[6];
        String[] snapsSign = new String[6];

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
            for(String group : groupList){
                for(String key : keyList) {
                    int i = 0;
                    VintageConfigWebUtils.register(group,key,valueList.get(i));
                    i++;
                }
                snapshotProcessor.removeStaticsConfigInfo(group);
                configClient.lookup(group);
            }

            VintageNamingClientUtils.sleep(16*1000);

            int i = 0;
            String temp[] = new String[6];
            for(String group : groupList){
                snaps[i] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                temp[i] =  VintageConfigWebUtils.lookupForAll(group);
                serverSign[i] = JsonHelper.parserStringToJsonNode(temp[i]).getFieldValue("body").getFieldValue("sign").toString();
                snapsSign[i] = JsonHelper.parserStringToJsonNode(snaps[i]).getFieldValue("sign").toString();
                System.out.print("\n snaps[" + i + "]" + snaps[i]);
                System.out.print("\n serverValues[" + i + "]" + temp[i]);
                assertEquals(serverSign[i],snapsSign[i]);
                i++;
            }

            //server unregister
            for(String key : keyList) {
                VintageConfigWebUtils.unregister(groupList.get(0), key);
            }

            VintageNamingClientUtils.sleep(6*1000);

            for(String group : groupList){
                snaps[i] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                temp[i] =  VintageConfigWebUtils.lookupForAll(group);
                serverSign[i] = JsonHelper.parserStringToJsonNode(temp[i]).getFieldValue("body").getFieldValue("sign").toString();
                snapsSign[i] = JsonHelper.parserStringToJsonNode(snaps[i]).getFieldValue("sign").toString();
                System.out.print("\n snaps[" + i + "]" + snaps[i]);
                System.out.print("\n serverValues[" + i + "]" + temp[i]);
                assertEquals(serverSign[i],snapsSign[i]);
                i++;
            }

            for(int n = 0; n<3; n++) {
                if( n == 0) {
                    assertFalse(snapsSign[n].contentEquals(snapsSign[n+3]));
                }
                else{
                    assertEquals(snapsSign[n],snapsSign[n+3]);
                }
            }
        }catch (IOException e) {
            System.out.print("IOEXCEPTION");
        }finally {
            for(String group : groupList) {
                for(String key : keyList) {
                    VintageConfigWebUtils.unregister(group,key);
                }
            }
        }
    }


    /**
     * 测试点：client取消注册所有group的所有节点
     * */
    @Test
    public void testClientUnregisterAllGroupAllNode() {
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER, true);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,true);
        String[] snaps = new String[6];
        String[] serverSign = new String[6];
        String[] snapsSign = new String[6];

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
            for(String group : groupList){
                for(String key : keyList) {
                    int i = 0;
                    VintageConfigWebUtils.register(group,key,valueList.get(i));
                    i++;
                }
                snapshotProcessor.removeStaticsConfigInfo(group);
                configClient.lookup(group);
            }
            VintageNamingClientUtils.sleep(16*1000);

            int i = 0;
            String temp[] = new String[6];
            for(String group : groupList){
                snaps[i] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                temp[i] =  VintageConfigWebUtils.lookupForAll(group);
                serverSign[i] = JsonHelper.parserStringToJsonNode(temp[i]).getFieldValue("body").getFieldValue("sign").toString();
                snapsSign[i] = JsonHelper.parserStringToJsonNode(snaps[i]).getFieldValue("sign").toString();
                System.out.print("\n snaps[" + i + "]" + snaps[i]);
                System.out.print("\n serverValues[" + i + "]" + temp[i]);
                assertEquals(serverSign[i],snapsSign[i]);
                i++;
            }

            //client unregister
            for(String group : groupList) {
                for(String key : keyList) {
                    configClient.unregister(group,key);
                }
            }

            VintageNamingClientUtils.sleep(6*1000);

            for(String group : groupList){
                snaps[i] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                temp[i] =  VintageConfigWebUtils.lookupForAll(group);
                serverSign[i] = JsonHelper.parserStringToJsonNode(temp[i]).getFieldValue("body").getFieldValue("sign").toString();
                snapsSign[i] = JsonHelper.parserStringToJsonNode(snaps[i]).getFieldValue("sign").toString();
                System.out.print("\n snaps[" + i + "]" + snaps[i]);
                System.out.print("\n serverValues[" + i + "]" + temp[i]);
                assertEquals(serverSign[i],snapsSign[i]);
                i++;
            }
            for(int n = 0; n<3; n++) {
                    assertFalse(snapsSign[n].contentEquals(snapsSign[n+3]));
            }
        }catch (IOException e) {
            System.out.print("IOEXCEPTION");
        }finally {
            for(String group : groupList) {
                for(String key : keyList) {
                    VintageConfigWebUtils.unregister(group,key);
                }
            }
        }
    }

    /**
     * 测试点：server取消所有group的所有key
     * */

    @Test
    public void testServerUnregisterAllServiceAllNode() {
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER, true);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,true);
        String[] snaps = new String[6];
        String[] serverSign = new String[6];
        String[] snapsSign = new String[6];

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
            for(String group : groupList){
                for(String key : keyList) {
                    int i = 0;
                    VintageConfigWebUtils.register(group,key,valueList.get(i));
                    i++;
                }
                snapshotProcessor.removeStaticsConfigInfo(group);
                configClient.lookup(group);
            }
            VintageNamingClientUtils.sleep(16*1000);

            int i = 0;
            String temp[] = new String[6];
            for(String group : groupList){
                snaps[i] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                temp[i] =  VintageConfigWebUtils.lookupForAll(group);
                serverSign[i] = JsonHelper.parserStringToJsonNode(temp[i]).getFieldValue("body").getFieldValue("sign").toString();
                snapsSign[i] = JsonHelper.parserStringToJsonNode(snaps[i]).getFieldValue("sign").toString();
                System.out.print("\n snaps[" + i + "]" + snaps[i]);
                System.out.print("\n serverValues[" + i + "]" + temp[i]);
                assertEquals(serverSign[i],snapsSign[i]);
                i++;
            }

            //server unregister
            for(String group : groupList){
                for(String key:keyList){
                    VintageConfigWebUtils.unregister(group,key);
                }
            }

            VintageNamingClientUtils.sleep(6*1000);

            for(String group : groupList){
                snaps[i] = snapshotProcessor.getConfigInfoFromSnapshot(group,null);
                temp[i] =  VintageConfigWebUtils.lookupForAll(group);
                serverSign[i] = JsonHelper.parserStringToJsonNode(temp[i]).getFieldValue("body").getFieldValue("sign").toString();
                snapsSign[i] = JsonHelper.parserStringToJsonNode(snaps[i]).getFieldValue("sign").toString();
                System.out.print("\n snaps[" + i + "]" + snaps[i]);
                System.out.print("\n serverValues[" + i + "]" + temp[i]);
                assertEquals(serverSign[i],snapsSign[i]);
                i++;
            }
            for(int n = 0; n<3; n++) {
                assertFalse(snapsSign[n].contentEquals(snapsSign[n+3]));
            }
        }catch (IOException e) {
            System.out.print("IOEXCEPTION");
        }finally {
            for(String group : groupList) {
                for(String key : keyList) {
                    VintageConfigWebUtils.unregister(group,key);
                }
            }
        }
    }

    /**
     * 测试点：snapshot开关被关闭，snapshot文件不写
     * */
    @Test
    public void testSnapCloseSnapNotWrite() {
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER, false);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,true);
        String[] snaps = new String[3];

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
            for(String group : groupList){
                int i = 0;
                for(String key : keyList) {
                    VintageConfigWebUtils.register(group,key,valueList.get(i));
                    i++;
                }
                snapshotProcessor.removeStaticsConfigInfo(group);

                configClient.lookup(group);
            }

            VintageNamingClientUtils.sleep(20*1000);
            int i =  0;
            for(String group : groupList) {
                String[] temp = new String[3];
                snaps[i] = snapshotProcessor.getConfigInfoFromSnapshot(group, null);
                temp[i] =  VintageConfigWebUtils.lookupForAll(group);
                System.out.print("\n snaps[" + i + "]" + snaps[i]);
                System.out.print("\n serverValues[" + i + "]" + temp[i]);
                assertNull(snaps[i]);
                i++;
            }
        }catch(IOException e){
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



