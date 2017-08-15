package com.weibo.qa.vintage.client.testcases;

import com.weibo.vintage.client.NamingServiceClient;
import com.weibo.vintage.failover.NodeExciseStrategy;
import com.weibo.vintage.model.NamingServiceNode;
import com.weibo.vintage.processor.SnapshotProcessor;
import com.weibo.vintage.utils.JsonHelper;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import java.io.IOException;
import java.util.Set;
import static org.junit.Assert.*;

/**
 * Created by zhangjuan3 on 14-6-25.
 */
public class ClientNamingSnapshotWriteTest extends BaseTest {
    
    private NamingServiceClient client;
    private String exteninfo = "exteninfo";
    private String exteninfoB = "\"exteninfoB\"";
    private int port = 1234;
	private static SnapshotProcessor snapshotProcessor;

    @BeforeClass
    public static void initialize() throws Exception {
    	snapshotDir = System.getProperty("user.dir");
    	//snapshotDir = Thread.currentThread().getContextClassLoader().getResource("");   	
    	snapshotProcessor = new SnapshotProcessor(snapshotDir.toString()+"/configserver/naming");
    	System.out.println(snapshotDir.toString()+"/configserver/naming");
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        serviceId = getRandomString(10);
        clusterId = getRandomString(20);
        
        config.setServiceId(serviceId);
        client = new NamingServiceClient(config);
        client.setNodeExciseStrategy(new NodeExciseStrategy.Dynamic());
        client.start();

        init();

    }

    private void init() {
        for (int i = 0; i < 2; i++) {
            if (!VintageNamingWebUtils.existsService(serviceId + i)) {
                VintageNamingWebUtils.addService(serviceId + i);
                VintageNamingClientUtils.sleep(serviceCacheInterval);
            }
            for (int j = 0; j < 2; j++) {
                if (VintageNamingWebUtils.existsService(serviceId + i) && (!VintageNamingWebUtils.existCluster(serviceId + i, clusterId + j))) {
                	VintageNamingWebUtils.addCluster(serviceId + i, clusterId + j);
                }
            }
            VintageNamingWebUtils.addWhitelist(serviceId + i, localNodes);            
        }
    }

    private void clear() {
        for (int i = 0; i < 2; i++) {
        	for (String nodes:this.localNodes) {
        		if (VintageNamingWebUtils.existsService(serviceId+i) && VintageNamingWebUtils.existsWhitelist(serviceId+i, nodes)) {
        			VintageNamingWebUtils.deleteWhitelist(serviceId+i, nodes);
        		}
        	}
            for (int j = 0; j < 2; j++) {
            	if (VintageNamingWebUtils.existsService(serviceId+i) && VintageNamingWebUtils.existCluster(serviceId+i, clusterId+j)) {
            		VintageNamingWebUtils.deleteCluster(serviceId + i, clusterId + j);
            	}
            }
            VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
            if (VintageNamingWebUtils.existsService(serviceId+i)) {
            	VintageNamingWebUtils.deleteService(serviceId + i);
            }        
        }
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        clear();
    }

    /**
     * add multi service and cluster,register node,no Lookup,the snapshot file is nonexist;
     */
    @Test
    public void testexteninfo1() {
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 3; k++) {
                    VintageNamingClientUtils.register(client, serviceId + i , clusterId + j, localIP, port + k, exteninfo);
                }
                snapshotProcessor.removeSnapshot(clusterId+j,serviceId+i);
            }
        }

        VintageNamingClientUtils.sleep(snapInterval);

        try {
            for (int i = 0; i < 2; i++) {
                for (int j = 0;j < 2; j++) {
                    String serverValue = VintageNamingWebUtils.lookup(serviceId+i,clusterId+j);
                    String s = snapshotProcessor.getConfigInfoFromSnapshot(clusterId+j, serviceId + i);
                    System.out.print("\n serverValue" + serverValue);
                    assertNull(s);
                }
            }
        } catch (IOException e) {
            System.out.println("Snapshot IoException");
        } finally {
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    for (int k = 0; k < 3; k++) {
                        VintageNamingClientUtils.unregister(client, serviceId + i, clusterId + j, localIP, port + k);
                    }
                }
            }
        }
    }

    
    /**
     * add multi service and cluster,register node,no Lookup,the snapshot file is nonexist;
     */
    @Test
    public void testMulNoLookup() {
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 3; k++) {
                    VintageNamingClientUtils.register(client, serviceId + i , clusterId + j, localIP, port + k, exteninfo);
                }
                snapshotProcessor.removeSnapshot(clusterId+j,serviceId+i);
            }
        }

        VintageNamingClientUtils.sleep(snapInterval);

        try {
            for (int i = 0; i < 2; i++) {
                for (int j = 0;j < 2; j++) {
                    String serverValue = VintageNamingWebUtils.lookup(serviceId+i,clusterId+j);
                    String s = snapshotProcessor.getConfigInfoFromSnapshot(clusterId+j, serviceId + i);
                    System.out.print("\n serverValue" + serverValue);
                    assertNull(s);
                }
            }
        } catch (IOException e) {
            System.out.println("Snapshot IoException");
        } finally {
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    for (int k = 0; k < 3; k++) {
                        VintageNamingClientUtils.unregister(client, serviceId + i, clusterId + j, localIP, port + k);
                    }
                }
            }
        }
    }

    /**
     * add multi service and cluster,register nodes and lookup,the snapshot is exist
     */
    @Test
    public void tesMulAllLookup() {
        String serverValue[] = new String[4];
        String snaps[]  = new String[4];

        int num = 0 ;
        for (int i = 0; i <2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 3; k++) {
                    VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port + k, exteninfo);
                }
                snapshotProcessor.removeSnapshot(clusterId + j, serviceId + i);

               // Utils.sleep(2*HEARTBEATINTERVAL);

                Set<NamingServiceNode> set = client.lookup(serviceId + i, clusterId + j);
                System.out.println(set);

//                Utils.register(client,serviceId+1,clusterId+1,localIP,port+5,exteninfo);

//                client.subscribeNodeChanges(serviceId+i, clusterId+j,
//                        new NamingServiceChangeListener() {
//                            @Override
//                            public void handleNodeChange(String serviceId,
//                                                         String clusterId, Set<NamingServiceNode> nodes,
//                                                         NotifyAction notifyAction) throws Exception {
//                                System.out.println("----nodes changed ,now node size: "
//                                        + nodes.size());
//                            }
//                            @Override
//                            public Executor getExecutor() {
//                                return null;
//                            }
//                        });
            }
        }

        VintageNamingClientUtils.sleep(snapInterval);
        System.out.println("sleep over");

        try {
            for(int i = 0 ;i <2 ;i++) {
                for(int j = 0;j<2;j++) {
                    snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j, serviceId + i);
                    serverValue[num] = VintageNamingWebUtils.lookup(serviceId+i,clusterId+j);
                    System.out.print("\n snaps[" + num + "]" + snaps[num]);
                    System.out.print("\n serverValue[" + num + "]" + serverValue[num]);
                    String signServer = VintageNamingWebUtils.getsign(serviceId+i,clusterId+j);
                    String signSnap = JsonHelper.parserStringToJsonNode(snaps[num]).getFieldValue("sign").toString().substring(1,33);
                    assertEquals(signServer,signSnap);
                    num++;
                 }
            }
        } catch (IOException e) {
            System.out.println(e);
        } finally {
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                     for (int k = 0;k < 3;k++) {
                         VintageNamingClientUtils.unregister(client, serviceId + i, clusterId + j, localIP, port + k);
                     }
                }
            }
        }
    }
    
    /**
     * "\"exteninfoB\"" 在json解析时，由于字符转义导致json库解析失败，因此在remote.get(cluster)时导致返回null
     * remote.get完整的结果为：
     * {"service":"QZnQ7cneMZ0","cluster":"wkr9dNprq0xBO0fzjRVo0","sign":"b042208d18aeb6a0f90729196ca620ff","nodes":{"working":[{"host":"10.236.25.71:1234","extInfo":""exteninfoB""},{"host":"10.236.25.71:1236","extInfo":""exteninfoB""},{"host":"10.236.25.71:1235","extInfo":""exteninfoB""}],"unreachable":[]}}
     * */
    @Ignore
    @Test
    public void testexteninfo2() {
        String serverValue[] = new String[4];
        String snaps[]  = new String[4];
        int num = 0 ;
        for (int i = 0; i <2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 3; k++) {
                    VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port + k, exteninfoB);
                }
                snapshotProcessor.removeSnapshot(clusterId+j,serviceId+i);
            }
        }
        client.lookup(serviceId + 0, clusterId + 0);

        VintageNamingClientUtils.sleep(snapInterval);

        try {
            for(int i = 0 ;i <2 ;i++) {
                for(int j = 0;j<2;j++) {
                    snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j, serviceId + i);
                    serverValue[num] = VintageNamingWebUtils.lookup(serviceId+i,clusterId+j);
                    System.out.print("\n snaps[" + num + "]" + snaps[num]);
                    System.out.print("\n serverValue[" + num + "]" + serverValue[num]);
                    if(i==0 && j==0) {
                        String signServer = VintageNamingWebUtils.getsign(serviceId+i,clusterId+j);
                        String signSnap = JsonHelper.parserStringToJsonNode(snaps[num]).getFieldValue("sign").toString().substring(1,33);
                        assertEquals(signServer,signSnap);
                    }else{
                        assertNull(snaps[num]);
                    }
                    num++;
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        } finally {
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    for (int k = 0;k < 3;k++) {
                        VintageNamingClientUtils.unregister(client, serviceId + i, clusterId + j, localIP, port + k);
                    }
                }
            }
        }
    }

    @Test
    public void testMulPartLookup() {
        String serverValue[] = new String[4];
        String snaps[]  = new String[4];
        int num = 0 ;
        for (int i = 0; i <2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 3; k++) {
                    VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port + k, exteninfo);
                }
                snapshotProcessor.removeSnapshot(clusterId+j,serviceId+i);
            }
        }
        client.lookup(serviceId + 0, clusterId + 0);

        VintageNamingClientUtils.sleep(snapInterval);

        try {
            for(int i = 0 ;i <2 ;i++) {
                for(int j = 0;j<2;j++) {
                    snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j, serviceId + i);
                    serverValue[num] = VintageNamingWebUtils.lookup(serviceId+i,clusterId+j);
                    System.out.print("\n snaps[" + num + "]" + snaps[num]);
                    System.out.print("\n serverValue[" + num + "]" + serverValue[num]);
                    if(i==0 && j==0) {
                        String signServer = VintageNamingWebUtils.getsign(serviceId+i,clusterId+j);
                        String signSnap = JsonHelper.parserStringToJsonNode(snaps[num]).getFieldValue("sign").toString().substring(1,33);
                        assertEquals(signServer,signSnap);
                    }else{
                        assertNull(snaps[num]);
                    }
                    num++;
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        } finally {
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    for (int k = 0;k < 3;k++) {
                        VintageNamingClientUtils.unregister(client, serviceId + i, clusterId + j, localIP, port + k);
                    }
                }
            }
        }
    }
    
    /*没有注册，lookup会抛异常*/
    /**
     * 只有部分注册，全部lookup.校验：只有注册的节点有snapshot文件
     * */
    @Test
    public void testMulPartRegist() {
        String serverValue[] = new String[4];
        String snaps[]  = new String[4];
        int num = 0;

        for(int i = 0; i < 1; i++){
            for(int j = 0; j < 1; j++) {
                VintageNamingClientUtils.register(client,serviceId+i,clusterId+j,localIP,port,exteninfo);
            }
        }

        for(int i = 0; i < 2; i++){
            for(int j = 0; j < 2; j++) {
                snapshotProcessor.removeSnapshot(clusterId+j,serviceId+i);
                client.lookup(serviceId+i,clusterId+j);
            }
        }

        VintageNamingClientUtils.sleep(snapInterval);

        try{
            for(int i = 0; i < 2; i++) {
                for(int j = 0; j < 2; j++) {
                    snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j, serviceId + i);
                    serverValue[num] = VintageNamingWebUtils.lookup(serviceId+i,clusterId+j);
                    System.out.print("\n snaps[" + num + "]" + snaps[num]);
                    System.out.print("\n serverValue[" + num + "]" + serverValue[num]);
                    String signServer = VintageNamingWebUtils.getsign(serviceId+i,clusterId+j);
                    String signSnap = JsonHelper.parserStringToJsonNode(snaps[num]).getFieldValue("sign").toString().substring(1,33);
                    if(i!=0 && j!=0) {
                        assertNotSame(signServer,signSnap);
                    }else{

                        assertEquals(signServer,signSnap);
                    }
                    num++;
                }
            }
        }catch (IOException e) {
            System.out.print("IOEXCEPTION");
        }finally {
            for (int i = 0;i < 1;i++) {
                for(int j = 0;j < 1;j++) {
                    VintageNamingClientUtils.unregister(client,serviceId + i,clusterId + j,localIP,port);
                }
            }
          }
        }


    /**
     * 测试点：有部分节点更改注册信息，snapshot文件有变更。
     * */
    @Test
    public void testModifyPartNode() {
        String serverValue[] = new String[8];
        String snaps[]  = new String[8];
        String signServer[] = new String[8];
        String signSnap[] =  new String[8];
        int num = 0;
        String exteninfoA = "exteninfoA";

        for(int i = 0; i < 2; i++){
            for(int j = 0; j < 2; j++) {
                for(int k = 0;k < 3;k++) {
                    VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port+k, exteninfo);
                }
                snapshotProcessor.removeSnapshot(clusterId+j,serviceId+i);
                client.lookup(serviceId+i,clusterId+j);
            }
        }

        VintageNamingClientUtils.sleep(snapInterval);

        try{
            for(int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j, serviceId + i);
                    serverValue[num] = VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                    System.out.print("\n snaps[" + num + "]" + snaps[num]);
                    System.out.print("\n serverValue[" + num + "]" + serverValue[num]);
                    signServer[num] = VintageNamingWebUtils.getsign(serviceId+i,clusterId+j);
                    signSnap[num] = JsonHelper.parserStringToJsonNode(snaps[num]).getFieldValue("sign").toString().substring(1,33);
                    num++;
                }
            }

            VintageNamingClientUtils.register(client,serviceId+0,clusterId+0,localIP,port+0,exteninfoA);

            VintageNamingClientUtils.sleep(snapInterval);

            for(int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j, serviceId + i);
                    serverValue[num] = VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                    System.out.print("\n snaps[" + num + "]" + snaps[num]);
                    System.out.print("\n serverValue[" + num + "]" + serverValue[num]);
                    signServer[num] = VintageNamingWebUtils.getsign(serviceId+i,clusterId+j);
                    signSnap[num] = JsonHelper.parserStringToJsonNode(snaps[num]).getFieldValue("sign").toString().substring(1,33);
                    num++;
                }
            }

            for(int n = 0;n<4;n++) {
                if(n ==0) {
                    assertNotSame(signServer,signSnap[n+4]);
                }else{
                    assertEquals(signServer[n],signSnap[n+4]);
                }
                assertEquals(signServer[n],signSnap[n]);
            }
        }catch (IOException e) {
            System.out.print("IOEXCEPTION");
        }finally {
            for (int i = 0;i < 2;i++) {
                for(int j = 0;j < 2;j++) {
                    for(int k = 0;k <3;k++) {
                        VintageNamingClientUtils.unregister(client,serviceId + i,clusterId + j,localIP,port+k);
                    }
                }
            }
        }
    }

    /**
     * 测试点：更改所有的node信息，所有的snapshot文件被更新
     * */
    @Test
    public void testModifyAllNode() {
        String serverValue[] = new String[8];
        String snaps[]  = new String[8];
        String signServer[] = new String[8];
        String signSnap[] =  new String[8];
        int num = 0;
        String exteninfoA = "exteninfoA";

        for(int i = 0; i < 2; i++){
            for(int j = 0; j < 2; j++) {
                for(int k = 0;k < 3;k++) {
                    VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port+k, exteninfo);
                }
                snapshotProcessor.removeSnapshot(clusterId+j,serviceId+i);
                client.lookup(serviceId+i,clusterId+j);
            }
        }

        VintageNamingClientUtils.sleep(snapInterval);

        try{
            for(int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j, serviceId + i);
                    serverValue[num] = VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                    System.out.print("\n snaps[" + num + "]" + snaps[num]);
                    System.out.print("\n serverValue[" + num + "]" + serverValue[num]);
                    signServer[num] = VintageNamingWebUtils.getsign(serviceId+i,clusterId+j);
                    signSnap[num] = JsonHelper.parserStringToJsonNode(snaps[num]).getFieldValue("sign").toString().substring(1,33);
                    num++;
                }
            }

            for(int i = 0;i<2;i++) {
                for(int j=0;j<2;j++) {
                    for(int k = 0;k<3;k++) {
                        VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port + k, exteninfoA);
                    }
                }
            }

            VintageNamingClientUtils.sleep(snapInterval);

            for(int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j, serviceId + i);
                    serverValue[num] = VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                    System.out.print("\n snaps[" + num + "]" + snaps[num]);
                    System.out.print("\n serverValue[" + num + "]" + serverValue[num]);
                    signServer[num] = VintageNamingWebUtils.getsign(serviceId+i,clusterId+j);
                    signSnap[num] = JsonHelper.parserStringToJsonNode(snaps[num]).getFieldValue("sign").toString().substring(1,33);
                    num++;
                }
            }

            for(int n = 0;n<4;n++) {
                assertNotSame(signServer[n],signServer[n+4]);
                assertNotSame(signSnap[n],signSnap[n+4]);
                assertEquals(signServer[n+4],signSnap[n+4]);
            }
        }catch (IOException e) {
            System.out.print("IOEXCEPTION");
        }finally {
            for (int i = 0;i < 2;i++) {
                for(int j = 0;j < 2;j++) {
                    for(int k = 0;k <3;k++) {
                        VintageNamingClientUtils.unregister(client,serviceId + i,clusterId + j,localIP,port+k);
                    }
                }
            }
        }
    }


    /**
     * 测试点：取消注册部分服务的部分节点，snapshot文件更新
     * */
    @Test
    public void testClientUnregistPartServicePartNode() {
        String serverValue[] = new String[8];
        String snaps[]  = new String[8];
        String signServer[] = new String[8];
        String signSnap[] = new String[8];
        int num = 0;

        for(int i = 0; i < 2; i++){
            for(int j = 0; j < 2; j++) {
                for(int k = 0;k < 3;k++) {
                    VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port+k, exteninfo);
                }
                snapshotProcessor.removeSnapshot(clusterId+j,serviceId+i);
                client.lookup(serviceId+i,clusterId+j);
            }
        }

        VintageNamingClientUtils.sleep(snapInterval);

        try{
            for(int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j, serviceId + i);
                    serverValue[num] = VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                    System.out.print("\n snaps[" + num + "]" + snaps[num]);
                    System.out.print("\n serverValue[" + num + "]" + serverValue[num]);
                    signServer[num] = VintageNamingWebUtils.getsign(serviceId+i,clusterId+j);
                    signSnap[num] = JsonHelper.parserStringToJsonNode(snaps[num]).getFieldValue("sign").toString().substring(1,33);
                    num++;
                }
            }

            VintageNamingClientUtils.unregister(client,serviceId+0,clusterId+0,localIP,port+0);

            VintageNamingClientUtils.sleep(snapInterval);

            for(int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j, serviceId + i);
                    serverValue[num] = VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                    System.out.print("\n snaps[" + num + "]" + snaps[num]);
                    System.out.print("\n serverValue[" + num + "]" + serverValue[num]);
                    signServer[num] = VintageNamingWebUtils.getsign(serviceId+i,clusterId+j);
                    signSnap[num] = JsonHelper.parserStringToJsonNode(snaps[num]).getFieldValue("sign").toString().substring(1,33);
                    num++;
                }
            }

            for(int n = 0;n<4;n++) {
                if(n == 0) {
                    assertNotSame(snaps[n], snaps[n + 4]);
                }else {
                    assertEquals(signServer[n],signSnap[n+4]);
                }
                assertEquals(signServer[n],signSnap[n]);
                assertEquals(signServer[n+4],signSnap[n+4]);
            }
        }catch (IOException e) {
            System.out.print("IOEXCEPTION");
        }finally {
            for (int i = 0;i < 2;i++) {
                for(int j = 0;j < 2;j++) {
                    for(int k = 0;k <3;k++) {
                        VintageNamingClientUtils.unregister(client,serviceId + i,clusterId + j,localIP,port+k);
                    }
                }
            }
        }
    }

    /**
     * 测试点：server端取消注册部分cluster的部分node，snapshot文件更新
     * */
    @Test
    public void testServerUnregistPartServicePartNode() {
        String serverValue[] = new String[8];
        String snaps[]  = new String[8];
        String signServer[] = new String[8];
        String signSnap[] = new String[8];
        int num = 0;

        for(int i = 0; i < 2; i++){
            for(int j = 0; j < 2; j++) {
                for(int k = 0;k < 3;k++) {
                    VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port+k, exteninfo);
                }
                snapshotProcessor.removeSnapshot(clusterId+j,serviceId+i);
                client.lookup(serviceId+i,clusterId+j);
            }
        }

        VintageNamingClientUtils.sleep(snapInterval);

        try{
            for(int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j, serviceId + i);
                    serverValue[num] = VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                    System.out.print("\n snaps[" + num + "]" + snaps[num]);
                    System.out.print("\n serverValue[" + num + "]" + serverValue[num]);
                    signServer[num] = VintageNamingWebUtils.getsign(serviceId+i,clusterId+j);
                    signSnap[num] = JsonHelper.parserStringToJsonNode(snaps[num]).getFieldValue("sign").toString().substring(1,33);
                    num++;
                }
            }

            //server取消注册部分server_cluster部分node
            VintageNamingWebUtils.unregister(serviceId + 0, clusterId + 0, localIP, port+0);

            VintageNamingClientUtils.sleep(snapInterval);

            for(int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j, serviceId + i);
                    serverValue[num] = VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                    System.out.print("\n snaps[" + num + "]" + snaps[num]);
                    System.out.print("\n serverValue[" + num + "]" + serverValue[num]);
                    signServer[num] = VintageNamingWebUtils.getsign(serviceId+i,clusterId+j);
                    signSnap[num] = JsonHelper.parserStringToJsonNode(snaps[num]).getFieldValue("sign").toString().substring(1,33);
                    num++;
                }
            }

            for(int n = 0;n<4;n++) {
                if(n == 0) {
                    assertNotSame(snaps[n], snaps[n + 4]);
                }else {
                    assertEquals(signServer[n],signSnap[n+4]);
                }
                assertEquals(signServer[n],signSnap[n]);
                assertEquals(signServer[n+4],signSnap[n+4]);
            }
        }catch (IOException e) {
            System.out.print("IOEXCEPTION");
        }finally {
            for (int i = 0;i < 2;i++) {
                for(int j = 0;j < 2;j++) {
                    for(int k = 0;k <3;k++) {
                        VintageNamingClientUtils.unregister(client,serviceId + i,clusterId + j,localIP,port+k);
                    }
                }
            }
        }
    }

    /**
     * 测试点：client取消注册多个服务中的一个的全部节点，snapshot文件更新
     * */
    @Test
    public void testClientUnregistPartServiceAllNode() {
        String serverValue[] = new String[8];
        String snaps[]  = new String[8];
        String signServer[] = new String[8];
        String signSnap[] = new String[8];
        int num = 0;

        for(int i = 0; i < 2; i++){
            for(int j = 0; j < 2; j++) {
                for(int k = 0;k < 3;k++) {
                    VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port+k, exteninfo);
                }
                snapshotProcessor.removeSnapshot(clusterId+j,serviceId+i);
                client.lookup(serviceId+i,clusterId+j);
            }
        }

        VintageNamingClientUtils.sleep(snapInterval);

        try{
            for(int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j, serviceId + i);
                    serverValue[num] = VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                    System.out.print("\n snaps[" + num + "]" + snaps[num]);
                    System.out.print("\n serverValue[" + num + "]" + serverValue[num]);
                    signServer[num] = VintageNamingWebUtils.getsign(serviceId+i,clusterId+j);
                    signSnap[num] = JsonHelper.parserStringToJsonNode(snaps[num]).getFieldValue("sign").toString().substring(1,33);
                    num++;
                }
            }

            //client端取消注册一个service全部node
            for(int i =0 ;i < 1; i++) {
                for(int j=0;j<2;j++) {
                    for(int k=0;k<3;k++) {
                        VintageNamingClientUtils.unregister(client,serviceId+i,clusterId+j,localIP,port+k);
                    }
                }
            }

            VintageNamingClientUtils.sleep(snapInterval);

            for(int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j, serviceId + i);
                    serverValue[num] = VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                    System.out.print("\n snaps[" + num + "]" + snaps[num]);
                    System.out.print("\n serverValue[" + num + "]" + serverValue[num]);
                    signServer[num] = VintageNamingWebUtils.getsign(serviceId+i,clusterId+j);
                    signSnap[num] = JsonHelper.parserStringToJsonNode(snaps[num]).getFieldValue("sign").toString().substring(1,33);
                    num++;
                }
            }

            for(int n = 0;n<4;n++) {
                if(n +4 <= 5 && n+4 >=4) {
                    assertEquals(signSnap[n+4],"c21f969b5f03d33d43e04f8f136e7682");
                }else {
                    assertEquals(signSnap[n],signSnap[n+4]);
                }
                assertEquals(signServer[n],signSnap[n]);
                assertEquals(signServer[n+4],signSnap[n+4]);
            }
        }catch (IOException e) {
            System.out.print("IOEXCEPTION");
        }finally {
            for (int i = 0;i < 2;i++) {
                for(int j = 0;j < 2;j++) {
                    for(int k = 0;k <3;k++) {
                        VintageNamingClientUtils.unregister(client,serviceId + i,clusterId + j,localIP,port+k);
                    }
                }
            }
        }
    }

    /**
     * 测试点：取消注册多个服务中的一个的所有节点，snapshot文件更新
     * */
    @Test
    public void testServerUnregistPartServiceAllNode() {
        String serverValue[] = new String[8];
        String snaps[]  = new String[8];
        String signServer[] = new String[8];
        String signSnap[] = new String[8];
        int num = 0;

        for(int i = 0; i < 2; i++){
            for(int j = 0; j < 2; j++) {
                for(int k = 0;k < 3;k++) {
                    VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port+k, exteninfo);
                }
                snapshotProcessor.removeSnapshot(clusterId+j,serviceId+i);
                client.lookup(serviceId+i,clusterId+j);
            }
        }

        VintageNamingClientUtils.sleep(snapInterval);

        try{
            for(int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j, serviceId + i);
                    serverValue[num] = VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                    System.out.print("\n snaps[" + num + "]" + snaps[num]);
                    System.out.print("\n serverValue[" + num + "]" + serverValue[num]);
                    signServer[num] = VintageNamingWebUtils.getsign(serviceId+i,clusterId+j);
                    signSnap[num] = JsonHelper.parserStringToJsonNode(snaps[num]).getFieldValue("sign").toString().substring(1,33);
                    num++;
                }
            }

            //server端取消注册一个service全部node
            for(int i =0 ;i < 1; i++) {
                for(int j=0;j<2;j++) {
                    for(int k=0;k<3;k++) {
                        VintageNamingWebUtils.unregister(serviceId+i,clusterId+j,localIP,port+k);
                    }
                }
            }

            VintageNamingClientUtils.sleep(snapInterval);

            for(int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j, serviceId + i);
                    serverValue[num] = VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                    System.out.print("\n snaps[" + num + "]" + snaps[num]);
                    System.out.print("\n serverValue[" + num + "]" + serverValue[num]);
                    signServer[num] = VintageNamingWebUtils.getsign(serviceId+i,clusterId+j);
                    signSnap[num] = JsonHelper.parserStringToJsonNode(snaps[num]).getFieldValue("sign").toString().substring(1,33);
                    num++;
                }
            }

            for(int n = 0;n<4;n++) {
                if(n +4 <= 5 && n+4 >=4) {
                    assertEquals(signSnap[n+4],"c21f969b5f03d33d43e04f8f136e7682");
                }else {
                    assertEquals(signSnap[n],signSnap[n+4]);
                }
                assertEquals(signServer[n],signSnap[n]);
                assertEquals(signServer[n+4],signSnap[n+4]);
            }
        }catch (IOException e) {
            System.out.print("IOEXCEPTION");
        }finally {
            for (int i = 0;i < 2;i++) {
                for(int j = 0;j < 2;j++) {
                    for(int k = 0;k <3;k++) {
                        VintageNamingClientUtils.unregister(client,serviceId + i,clusterId + j,localIP,port+k);
                    }
                }
            }
        }
    }

    /**
     * 测试点：client取消注册所有service，snapshot文件更新
     * */
    @Test
    public void testClientUnregisterAllService() {
        String serverValue[] = new String[8];
        String snaps[]  = new String[8];
        String signServer[] = new String[8];
        String signSnap[] = new String[8];
        int num = 0;

        for(int i = 0; i < 2; i++){
            for(int j = 0; j < 2; j++) {
                for(int k = 0;k < 3;k++) {
                    VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port+k, exteninfo);
                }
                snapshotProcessor.removeSnapshot(clusterId+j,serviceId+i);
                client.lookup(serviceId+i,clusterId+j);
            }
        }

        VintageNamingClientUtils.sleep(snapInterval);

        try{
            for(int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j, serviceId + i);
                    serverValue[num] = VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                    System.out.print("\n snaps[" + num + "]" + snaps[num]);
                    System.out.print("\n serverValue[" + num + "]" + serverValue[num]);
                    signServer[num] = VintageNamingWebUtils.getsign(serviceId+i,clusterId+j);
                    signSnap[num] = JsonHelper.parserStringToJsonNode(snaps[num]).getFieldValue("sign").toString().substring(1,33);
                    num++;
                }
            }

            //client端取消注册全部service全部node
            for(int i = 0 ; i<2;i++) {
                for(int j = 0;j<2;j++) {
                    for (int k=0;k<3;k++) {
                        VintageNamingClientUtils.unregister(client,serviceId+i,clusterId+j,localIP,port+k);
                    }
                }
            }

            VintageNamingClientUtils.sleep(snapInterval);

            for(int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j, serviceId + i);
                    serverValue[num] = VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                    System.out.print("\n snaps[" + num + "]" + snaps[num]);
                    System.out.print("\n serverValue[" + num + "]" + serverValue[num]);
                    signServer[num] = VintageNamingWebUtils.getsign(serviceId+i,clusterId+j);
                    signSnap[num] = JsonHelper.parserStringToJsonNode(snaps[num]).getFieldValue("sign").toString().substring(1,33);
                    num++;
                }
            }

            for(int n = 0;n<4;n++) {
                    assertEquals(signSnap[n+4],"c21f969b5f03d33d43e04f8f136e7682");
                    assertNotSame(signSnap[n], signSnap[n + 4]);
                assertEquals(signServer[n],signSnap[n]);
                assertEquals(signServer[n+4],signSnap[n+4]);
            }
        }catch (IOException e) {
            System.out.print("IOEXCEPTION");
        }finally {
            for (int i = 0;i < 2;i++) {
                for(int j = 0;j < 2;j++) {
                    for(int k = 0;k <3;k++) {
                        VintageNamingClientUtils.unregister(client,serviceId + i,clusterId + j,localIP,port+k);
                    }
                }
            }
        }
    }

    /**
     * 测试点：server端取消注册所有节点，snapshot文件更新
     * */
    @Test
    public void testServerUnregisterAllService() {
        String serverValue[] = new String[8];
        String snaps[]  = new String[8];
        String signServer[] = new String[8];
        String signSnap[] = new String[8];
        int num = 0;

        for(int i = 0; i < 2; i++){
            for(int j = 0; j < 2; j++) {
                for(int k = 0;k < 3;k++) {
                    VintageNamingClientUtils.register(client, serviceId + i, clusterId + j, localIP, port+k, exteninfo);
                }
                snapshotProcessor.removeSnapshot(clusterId+j,serviceId+i);
                client.lookup(serviceId+i,clusterId+j);
            }
        }

        VintageNamingClientUtils.sleep(snapInterval);

        try{
            for(int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j, serviceId + i);
                    serverValue[num] = VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                    System.out.print("\n snaps[" + num + "]" + snaps[num]);
                    System.out.print("\n serverValue[" + num + "]" + serverValue[num]);
                    signServer[num] = VintageNamingWebUtils.getsign(serviceId+i,clusterId+j);
                    signSnap[num] = JsonHelper.parserStringToJsonNode(snaps[num]).getFieldValue("sign").toString().substring(1,33);
                    num++;
                }
            }

            //server端取消注册全部service全部node
            for(int i = 0 ; i<2;i++) {
                for(int j = 0;j<2;j++) {
                    for (int k=0;k<3;k++) {
                        VintageNamingWebUtils.unregister(serviceId + i, clusterId + j, localIP, port + k);
                    }
                }
            }

            VintageNamingClientUtils.sleep(snapInterval);

            for(int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    snaps[num] = snapshotProcessor.getConfigInfoFromSnapshot(clusterId + j, serviceId + i);
                    serverValue[num] = VintageNamingWebUtils.lookup(serviceId + i, clusterId + j);
                    System.out.print("\n snaps[" + num + "]" + snaps[num]);
                    System.out.print("\n serverValue[" + num + "]" + serverValue[num]);
                    signServer[num] = VintageNamingWebUtils.getsign(serviceId+i,clusterId+j);
                    signSnap[num] = JsonHelper.parserStringToJsonNode(snaps[num]).getFieldValue("sign").toString().substring(1,33);
                    num++;
                }
            }

            for(int n = 0;n<4;n++) {
                assertEquals(signSnap[n+4],"c21f969b5f03d33d43e04f8f136e7682");
                assertNotSame(signSnap[n],signSnap[n+4]);
                assertEquals(signServer[n],signSnap[n]);
                assertEquals(signServer[n+4],signSnap[n+4]);
            }
        }catch (IOException e) {
            System.out.print("IOEXCEPTION");
        }finally {
            for (int i = 0;i < 2;i++) {
                for(int j = 0;j < 2;j++) {
                    for(int k = 0;k <3;k++) {
                        VintageNamingClientUtils.unregister(client,serviceId + i,clusterId + j,localIP,port+k);
                    }
                }
            }
        }
    }
}












