package compatible;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Assert.*;
import compatible.constants.HttpUrls;
import compatible.http.HttpSend;
import compatible.http.Tools;
import net.sf.json.JSONObject;

/**
 * service & cluster register test
* @ClassName: RegisterTest 
* @Description: TODO 
* @author lm
* @date 2017年6月5日 下午3:22:47 
*
 */
public class RegisterTest {
	
	String serviceName="lmservice";
	String clusterName="lmcluster";
	
	/**
	 * addservice-addcluster-update-getcluster-listservice-delcluster-delservice
	* @Title: serviceTest 
	* @return_type:void     
	* @author lm
	 * @throws Exception 
	 */
	@Test
	public void flowTest() throws Exception{
	
		assertTrue(Tools.check(HttpUrls.ADD_SERVICE_P_OLD, "type=statics&threshold=0.1&service="+serviceName, "POST"));
		//-------------------------------------		
		assertTrue(Tools.check(HttpUrls.ADD_CLUSTER_P_OLD, String.format("&service=%s&cluster=%s",serviceName,clusterName), "POST"));
		//-------------------------------------
		assertTrue(Tools.check(HttpUrls.UPDATE_SERVICE_P_OLD, "type=statics&threshold=0.5&service="+serviceName, "POST"));
		//------------
		assertTrue(Tools.check(HttpUrls.GET_CLUSTER,"&service="+serviceName,"GET"));
		//-------
		
		
		
	}
	/**
	 * getservice,delservice,addcluster 
	 * getcluster,delcluster
	 * - service exist - getcluster,delcluster
	* @Title: normalTest 
	* @return_type:void     
	* @author lm
	 */
	public void normalTest(){
		
	}
	
	public static void main(String[] args) {
		String s1=new String("{\"a\":\"b\"}");
		String s2=new String("{\"a\":\"b\"}");
		JSONObject j1=JSONObject.fromObject(s1);
		JSONObject j2=JSONObject.fromObject(s2);
		System.out.println(j1.compareTo(j2));
	}
}
