package com.weibo.qa.vintage.client.testcases;

import com.weibo.vintage.client.NamingServiceClient;
import com.weibo.vintage.failover.NodeExciseStrategy;
import com.weibo.vintage.model.NamingServiceType;
import com.weibo.vintage.processor.SnapshotProcessor;
import com.weibo.vintage.utils.RedisWebUtils;
import com.weibo.vintage.utils.SwitcherUtils;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageLogger;
import com.weibo.vintage.utils.VintageNamingWebUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;

/**
 * Created by zhang3 on 14-7-10.
 *
 * 默认snapshot开关和snapshot_disaster开关都为true
 * snapshot开关 》 snapshot_disaster
 * */
public class ClientNamingSnapshotReadTest extends BaseTest{
    private NamingServiceClient client;
    private String exteninfo = "exteninfo";
    private String exteninfoB = "exteninfoB";
    private int port = 1234;
    //private int port = 8080;
	private static SnapshotProcessor snapshotProcessor;

    @BeforeClass
    public static void initialize() throws Exception {
    	snapshotDir = System.getProperty("user.dir");
    	snapshotProcessor = new SnapshotProcessor(snapshotDir.toString()+"/configserver/naming");
    }

    @Before
    public void setUp() throws Exception{
        super.setUp();
        serviceId = getRandomString(10);
        clusterId = getRandomString(20);
        
        config.setServiceId(serviceId);
        client = new NamingServiceClient(config);
        client.setNodeExciseStrategy(new NodeExciseStrategy.Dynamic());
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER, true);
        client.start();
        VintageNamingClientUtils.sleep(2*HEARTBEATINTERVAL);
        init();
    }

    private void init() {
        for (int i = 0; i < 2; i++) {
        	addService(serviceId + i, NamingServiceType.statics.toString());
        }
        VintageNamingClientUtils.sleep(10*HEARTBEATINTERVAL);
        for (int i = 0; i < 2; i++){
        	addWhiteList(serviceId + i, localNodes);
        }
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 5; j++) {
            	addCluster(serviceId + i, clusterId + j);
            }
        }
    }

    private void clear() {

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 5; j++) {
            	delCluster(serviceId + i, clusterId + j);
            }
        }
        for (int i = 0; i < 2; i++) {
        	delWhiteList(serviceId+i, localNodes);
        }
        VintageNamingClientUtils.sleep(10*HEARTBEATINTERVAL);
        for (int i = 0; i < 2; i++) {
            delService(serviceId + i);
        }
        VintageNamingClientUtils.sleep(serviceCacheInterval);
    }

    @After
    public void tearDown() throws Exception{
        super.tearDown();
        clear();
    }

    /**
     * 校验点:server，redis可用，snapshot文件和server一致，启动client,启动成功，从snapshot读取数据
     * 校验方法：查看日志
     *
     * 触发50301错误
     * */
    @Test
    public void testexteninfo1() {  
        try{
        for(int i = 0;i<2;i++) {
            for(int j=0;j<2;j++) {
                for(int k=0;k<3;k++) {
                    VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port + k, exteninfoB);        
                }
                snapshotProcessor.removeSnapshot(clusterId+j,serviceId+i);
                String s =  VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                System.out.println(s);
                client.lookup(serviceId+i,clusterId+j);
            }
        }

        VintageNamingClientUtils.sleep(16*1000);

        //新启动一个client
        NamingServiceClient clientNew = new NamingServiceClient(config);
        clientNew.start();

        for(int i = 0 ;i < 2; i++) {
            for(int j = 0;j < 3;j++) {
                VintageLogger.info("newClientLookUP");
                clientNew.lookup(serviceId+i,clusterId+j);
            }
        }

        VintageNamingClientUtils.sleep(2*1000);


        }catch (Exception ex) {
        	fail(ex.getMessage());
        }finally {
            for(int i = 0 ;i < 2 ;i++) {
                for(int j=0;j<2;j++) {
                    for(int k = 0 ;k<3;k++) {
                        VintageNamingClientUtils.unregister(client,serviceId+i,clusterId+j,localIP,port+k);
                    }
                }
            }
        }        
    }

    /**
     * 校验点:server，redis可用，snapshot文件和server一致，启动client,启动成功，从snapshot读取数据
     * 校验方法：查看日志
     *
     * 触发50301错误
     * */
    @Test
    public void testServeravailSnapIdenticalServer() {
//    	System.out.println("hello");
    	
        for(int i = 0;i<2;i++) {
            for(int j=0;j<2;j++) {
                for(int k=0;k<3;k++) {
                	
                    VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port + k, exteninfo);
                   
                }
                snapshotProcessor.removeSnapshot(clusterId+j,serviceId+i);
                String s =  VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                System.out.println(s);
                client.lookup(serviceId+i,clusterId+j);
            }
        }

        VintageNamingClientUtils.sleep(16*1000);

        //新启动一个client
        NamingServiceClient clientNew = new NamingServiceClient(config);
        clientNew.start();

        for(int i = 0 ;i < 2; i++) {
            for(int j = 0;j < 3;j++) {
                VintageLogger.info("newClientLookUP");
                clientNew.lookup(serviceId+i,clusterId+j);
            }
        }

        VintageNamingClientUtils.sleep(2*1000);

        try{
        }catch (Exception ex) {
        }finally {
            for(int i = 0 ;i < 2 ;i++) {
                for(int j=0;j<2;j++) {
                    for(int k = 0 ;k<3;k++) {
                        VintageNamingClientUtils.unregister(client,serviceId+i,clusterId+j,localIP,port+k);
                    }
                }
            }
        }        
    }

    /**
     * 校验点:server，redis可用，snapshot文件和server不一致，启动client,启动成功，从server读取数据
     * 查看info.log，snapshot与server不一致，从server获取数据。
     * 测试方法：更改原来client的注册信息，在snapshot文件没有来得及被更新时，新启动一个client，该client跟原client的配置文件相同，会使用相同的文件目录。
     * */
    @Test
    public void testServeravailSnapUnIdenticalServer() {
        String exteninfoA = "exteninfoA";
        String[] snaps =  new String[4];
        String[] serverValues =  new String[8];
        int num = 0;
        for(int i = 0;i<2;i++) {
            for(int j=0;j<2;j++) {
                for(int k=0;k<3;k++) {
                    VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port + k, exteninfo);
                }
                snapshotProcessor.removeSnapshot(clusterId+j,serviceId+i);
                //lookup
                client.lookup(serviceId+i,clusterId+j);
                //取得server端数据
                serverValues[num] =  VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                System.out.println("\n serverValues[" + num + "]" + serverValues[num]);
                num++;
            }
        }

        VintageNamingClientUtils.sleep(15*1000);

        try{
            int m = 0;
            for(int i = 0;i<2;i++){
                for(int j = 0;j<2;j++){
                    snaps[m] =  snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j,serviceId +i);
                    System.out.print("\n snaps[" + m + "]" + snaps[m]);
                    assertNotNull(snaps[m]);
                    m++;
                }
            }

            //更改exteninfo,重新注册
            for(int i = 0;i<2;i++) {
                for(int j=0;j<2;j++) {
                    for(int k=0;k<3;k++) {
                        VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port + k, exteninfoA);
                    }
                    serverValues[num] =  VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                    System.out.println("\n serverValues[" + num + "]" + serverValues[num]);
                   // snaps[num] =  snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j,serviceId +i);
                    //System.out.println("\n snaps[" + num + "]" + snaps[num]);
                    num++;
                }
            }

            //新启动一个client
            NamingServiceClient clientNew = new NamingServiceClient(config);
            clientNew.start();

            for(int i = 0 ;i < 2; i++) {
                for(int j = 0;j < 2;j++) {
                    VintageLogger.info("newClientLookUP");
                    clientNew.lookup(serviceId+i,clusterId+j);
                }
            }

            VintageNamingClientUtils.sleep(5*1000);

        }catch (Exception ex) {
        }finally {
            for(int i = 0 ;i < 2 ;i++) {
                for(int j=0;j<2;j++) {
                    for(int k = 0 ;k<3;k++) {
                        VintageNamingClientUtils.unregister(client,serviceId+i,clusterId+j,localIP,port+k);
                    }
                }
            }
        }
    }

    /**
     * 校验点:server，redis可用，snapshot文件和server不一致，启动client,启动成功，从server读取数据
     * 查看info.log，snapshot与server不一致，从server获取数据。
     * 测试方法：更改原来client的注册信息，在snapshot文件没有来得及被更新时，新启动一个client，该client跟原client的配置文件相同，会使用相同的文件目录。
     * */
    @Test
    public void testexteninfo2() {
        String exteninfoA = "exteninfoA";
        String[] snaps =  new String[4];
        String[] serverValues =  new String[8];
        int num = 0;
        for(int i = 0;i<2;i++) {
            for(int j=0;j<2;j++) {
                for(int k=0;k<3;k++) {
                    VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port + k, exteninfo);
                }
                snapshotProcessor.removeSnapshot(clusterId+j,serviceId+i);
                //lookup
                client.lookup(serviceId+i,clusterId+j);
                //取得server端数据
                serverValues[num] =  VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                System.out.println("\n serverValues[" + num + "]" + serverValues[num]);
                num++;
            }
        }

        VintageNamingClientUtils.sleep(15*1000);

        try{
            int m = 0;
            for(int i = 0;i<2;i++){
                for(int j = 0;j<2;j++){
                    snaps[m] =  snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j,serviceId +i);
                    System.out.print("\n snaps[" + m + "]" + snaps[m]);
                    assertNotNull(snaps[m]);
                    m++;
                }
            }

            //更改exteninfo,重新注册
            for(int i = 0;i<2;i++) {
                for(int j=0;j<2;j++) {
                    for(int k=0;k<3;k++) {
                        VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port + k, exteninfoB);
                    }
                    serverValues[num] =  VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                    System.out.println("\n serverValues[" + num + "]" + serverValues[num]);
                   // snaps[num] =  snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j,serviceId +i);
                    //System.out.println("\n snaps[" + num + "]" + snaps[num]);
                    num++;
                }
            }

            //新启动一个client
            NamingServiceClient clientNew = new NamingServiceClient(config);
            clientNew.start();

            for(int i = 0 ;i < 2; i++) {
                for(int j = 0;j < 2;j++) {
                    VintageLogger.info("newClientLookUP");
                    clientNew.lookup(serviceId+i,clusterId+j);
                }
            }

            VintageNamingClientUtils.sleep(5*1000);

        }catch (Exception ex) {
        }finally {
            for(int i = 0 ;i < 2 ;i++) {
                for(int j=0;j<2;j++) {
                    for(int k = 0 ;k<3;k++) {
                        VintageNamingClientUtils.unregister(client,serviceId+i,clusterId+j,localIP,port+k);
                    }
                }
            }
        }
    }
    
    /**
     * 测试点:server可用，redis可用，snapshot文件为空，启动client,从server读取数据，启动成功
     * 校验方法：查看日志，从server端获取数据
     * */
    @Test
    public void testServerAvailSnapEmpty() {
        String[] serverValues =  new String[6];
        String[] snaps = new String[6];
        int num = 0;
        for(int i = 0;i<2;i++) {
            for(int j=0;j<2;j++) {
                for(int k=0;k<3;k++) {
                    VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port + k, exteninfo);
                }

                snapshotProcessor.removeSnapshot(clusterId+j,serviceId+i);

                //取得server端数据
                serverValues[num] =  VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                System.out.println("\n serverValues[" + num + "]" + serverValues[num]);
                num++;
            }
        }

        VintageNamingClientUtils.sleep(15*1000);

        try{
            int m = 0;
            for(int i = 0;i<2;i++){
                for(int j = 0;j<2;j++){
                    snaps[m] =  snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j,serviceId +i);
                    System.out.print("\n snaps[" + m + "]" + snaps[m]);
                    assertNull(snaps[m]);
                    m++;
                }
            }

            //重新启动一个client
           NamingServiceClient clientNew = new NamingServiceClient(config);
           clientNew.start();

           for(int i = 0; i < 2; i++){
               for(int j = 0; j < 2;j++){
                   VintageLogger.info("newClientLookUP");
                   clientNew.lookup(serviceId+i,clusterId+j);
               }
           }
           VintageNamingClientUtils.sleep(2*1000);

        }catch (Exception ex) {
        }finally {
            for(int i = 0 ;i < 2 ;i++) {
                for(int j=0;j<3;j++) {
                    for(int k = 0 ;k<3;k++) {
                        VintageNamingClientUtils.unregister(client,serviceId+i,clusterId+j,localIP,port+k);
                    }
                }
            }
        }
    }

    /**
     * 测试点:server可用，redis不可用，snapshot文件为空，启动client,启动失败
     * notice:还是会从server获得数据，但是不完整，该数据是server端缓存的，且缓存了所有的serverId，没有node信息。
     * 20140720   测试结果，从server获取数据失败，从snapshot获取数据也失败
     *vintage remote server is invalid, but get data from snapshot failed, key=/naming/regService_init0#regCluster_init0 value=null
     * 记得关闭redis.
     * */
    @Test
    public void testServerAvailRedisUnavailSnapEmpty() {
        String[] serverValues =  new String[6];
        int num = 0;
        for(int i = 0;i<2;i++) {
            for(int j=0;j<2;j++) {
                for(int k=0;k<3;k++) {
                    VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port + k, exteninfo);
                }
                //取得server端数据
                serverValues[num] =  VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                System.out.println("\n serverValues[" + num + "]" + serverValues[num]);
                num++;
            }
        }
        //!!!!!关闭redis
        RedisWebUtils.StopRedis();
        try{
            NamingServiceClient clientNew = new NamingServiceClient(config);
            clientNew.start();
            for(int i = 0 ; i< 2 ;i++) {
                for(int j = 0;j<2;j++) {
                    VintageLogger.info("newClientLookUP");
                    clientNew.lookup(serviceId+i,clusterId+j);
                }
            }

            VintageNamingClientUtils.sleep(2*1000);

        }catch (Exception ex) {
        }finally {
        	RedisWebUtils.StartRedis();
        	VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
            for(int i = 0 ;i < 2 ;i++) {
                for(int j=0;j<2;j++) {
                    for(int k = 0 ;k<3;k++) {
                        VintageNamingClientUtils.unregister(client,serviceId+i,clusterId+j,localIP,port+k);
                    }
                }
            }   
        }
    }

    /**
     *测试点:server不可用，snpashot文件存在，启动client,启动成功，数据从snapshot读取
     * */
    @Test
    public void testServerUnavailSnapOpenSnapExist() {
        String[] serverValues =  new String[4];
        int num = 0;
        for(int i = 0;i<2;i++) {
            for(int j=0;j<2;j++) {
                for(int k=0;k<3;k++) {
                    VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port + k, exteninfo);
                }
                //删除snapshot文件
                snapshotProcessor.removeSnapshot(clusterId+j,serviceId+i);
                //client lookup
                client.lookup(serviceId+i,clusterId+j);

                //get server data
                serverValues[num] =  VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                System.out.println("\n serverValues[" + num + "]" + serverValues[num]);
                num++;
            }
        }

        VintageNamingClientUtils.sleep(15*1000);

        //!!!!!!!关闭tomcat

        //再启动，启动成功
        try{
            NamingServiceClient clientNew =new NamingServiceClient(config);
            clientNew.start();
            for(int i = 0;i < 2;i++) {
                for(int j = 0;j < 2;j++) {
                    VintageLogger.info("newClientLookUP");
                    clientNew.lookup(serviceId+i,clusterId+j);
                }
            }
            //check the snapshotFiles updated
            VintageNamingClientUtils.sleep(6*1000);

        }catch (Exception ex) {
        }finally {
            for(int i = 0 ;i < 2 ;i++) {
                for(int j=0;j<2;j++) {
                    for(int k = 0 ;k<3;k++) {
                        VintageNamingClientUtils.unregister(client,serviceId+i,clusterId+j,localIP,port+k);
                    }
                }
            }
        }
    }

    /**
     *测试点:server不可用，snpashot文件不存在，启动client,启动失败
     * */
    @Test
    public void testServerUnavailSnapOpenSnapNonExist() {
        String[] serverValues =  new String[6];
        int num = 0;
        for(int i = 0;i<2;i++) {
            for(int j=0;j<2;j++) {
                for(int k=0;k<3;k++) {
                    VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port + k, exteninfo);
                }

                //删除snapshot文件
                snapshotProcessor.removeSnapshot(clusterId+j,serviceId+i);

                //get server data
                serverValues[num] =  VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                System.out.println("\n serverValues[" + num + "]" + serverValues[num]);
                num++;
            }
        }
        VintageNamingClientUtils.sleep(5*1000);

        //!!!!!关闭tomcat

        //再启动
        try{
            NamingServiceClient clientNew = new NamingServiceClient(config);
            clientNew.start();
            for(int i = 0;i<2;i++) {
                for(int j = 0;j < 2;j++){
                    VintageLogger.info("newClientLookUP");
                    clientNew.lookup(serviceId+i,clusterId+j);
                }
            }

            VintageNamingClientUtils.sleep(5*1000);

        }catch (Exception ex) {
        }finally {
            for(int i = 0 ;i < 2 ;i++) {
                for(int j=0;j<2;j++) {
                    for(int k = 0 ;k<3;k++) {
                        VintageNamingClientUtils.unregister(client,serviceId+i,clusterId+j,localIP,port+k);
                    }
                }
            }
        }
    }


    /**
     *测试点:server不可用,snapshot_process开关关闭，snpashot文件存在，启动client,启动失败
     * */
    @Test
    public void testServerUnavailSnapClosedSnapExist() {
        String[] serverValues =  new String[6];
        int num = 0;
        for(int i = 0;i<2;i++) {
            for(int j=0;j<3;j++) {
                for(int k=0;k<3;k++) {
                    VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port + k, exteninfo);
                }
                //snapshot文件删除
                snapshotProcessor.removeSnapshot(clusterId+j,serviceId+i);

                //lookup
                client.lookup(serviceId+i,clusterId+j);

                //get server data
                serverValues[num] =  VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                System.out.println("\n serverValues[" + num + "]" + serverValues[num]);
                num++;
            }
        }

        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER, false);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,true);

        VintageNamingClientUtils.sleep(15*1000);

        try {
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 3; j++) {
                    String s = snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j, serviceId + i);
                    System.out.println("snaps" + s);
                }
            }
        }catch(IOException e) {
            System.out.print(e);
        }

        //!!!!!关闭tomcat

        //再启动，启动失败
        try{
            NamingServiceClient clientNew =  new NamingServiceClient(config);
            clientNew.start();

            for(int i = 0 ; i < 2 ;i++) {
                for(int j = 0; j < 3;j++) {
                    VintageLogger.info("newClientLookUP");
                    clientNew.lookup(serviceId+i,clusterId+j);
                }
            }

            VintageNamingClientUtils.sleep(3*1000);

        }catch (Exception ex) {
        }finally {
            for(int i = 0 ;i < 2 ;i++) {
                for(int j=0;j<3;j++) {
                    for(int k = 0 ;k<3;k++) {
                        VintageNamingClientUtils.unregister(client,serviceId+i,clusterId+j,localIP,port+k);
                    }
                }
            }
        }
    }

    /**
     *测试点:server不可用，snapshot_process开关开，distater开关关，snpashot文件存在，启动client,启动失败
     * */
    @Test
    public void testServerUnavailSnapClosedSnapNonExist() {
        String[] serverValues =  new String[6];
        int num = 0;
        for(int i = 0;i<2;i++) {
            for(int j=0;j<2;j++) {
                for(int k=0;k<3;k++) {
                    VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port + k, exteninfo);
                }

                //snapshot文件删除
                snapshotProcessor.removeSnapshot(clusterId+j,serviceId+i);

                //lookup
                client.lookup(serviceId+i,clusterId+j);

                //get server data
                serverValues[num] =  VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                System.out.println("\n serverValues[" + num + "]" + serverValues[num]);
                num++;
            }
        }

        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER, true);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,false);

        VintageNamingClientUtils.sleep(15*1000);

        //关闭tomcat

        //再启动，启动失败
        try{
            NamingServiceClient clientNew =  new NamingServiceClient(config);
            clientNew.start();

            for(int i = 0;i < 2; i++) {
                for(int j = 0;j < 2;j++) {
                    VintageLogger.info("newClientLookUP");
                    clientNew.lookup(serviceId+i,clusterId+j);
                }
            }

            VintageNamingClientUtils.sleep(3*1000);

        }catch (Exception ex) {
        }finally {
            for(int i = 0 ;i < 2 ;i++) {
                for(int j=0;j<2;j++) {
                    for(int k = 0 ;k<3;k++) {
                        VintageNamingClientUtils.unregister(client,serviceId+i,clusterId+j,localIP,port+k);
                    }
                }
            }
        }
    }
}
