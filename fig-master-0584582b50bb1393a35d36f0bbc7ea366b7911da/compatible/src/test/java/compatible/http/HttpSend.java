package compatible.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import compatible.constants.HttpUrls;

public class HttpSend {
	static String[] hosts=null;
	static String hostv1=null;
	static int i=0;
	private static void init() throws Exception{
		//http://
		Properties p=GetConfig.getConfig("config.properties");
		hosts=p.getProperty("hosts").split(",");
		for(int i=0;i<hosts.length;i++){
			if(!hosts[i].startsWith("http://"))
				hosts[i]="http://"+hosts[i];
		}
		hostv1=p.getProperty("hostv1");
		if(!hostv1.startsWith("http://"))
			hostv1="http://"+hostv1;
	}
	//send 
	//POST
	public static String postV1(Header[] headers,String relativeUrl , String paras) throws Exception{
		return basePost(headers, hostv1+relativeUrl, paras);
	}
	public static String post(Header[] headers,String relativeUrl , String paras) throws Exception{
		String host=before(relativeUrl);
		return basePost(headers, host+relativeUrl, paras);
	}
	//@Deprecated
	private static String basePost(Header[] headers,String url ,String paras) throws Exception{
		HttpPost post = new HttpPost(url);
		if(headers != null){
			for(Header header : headers)
				post.addHeader(header);
		}
		StringEntity se =new StringEntity(paras,"UTF-8");
		se.setContentType("application/x-www-form-urlencoded");
		post.setEntity(se);
		CloseableHttpClient client = HttpClients.createDefault();
		CloseableHttpResponse sp=client.execute(post, HttpClientContext.create());
		String result=null;
		if (null!=(result=Redirect(sp,paras,"POST")))
			return result;
		return EntityUtils.toString(sp.getEntity(),"UTF-8");
	}
	
	//GET
	public static String getV1(Header[] headers,String relativeUrl , String paras) throws Exception{
		return baseGet(headers,hostv1+relativeUrl+paras);
	}
	public static String get(Header[] headers,String relativeUrl , String paras) throws Exception{
		String host=before(relativeUrl);
		if(!paras.startsWith("?")){
			paras="?"+paras;
		}
		return baseGet(headers,host+relativeUrl+paras);
	}
	
	private static String baseGet(Header[] headers,String url) throws Exception{
		HttpGet get = new HttpGet(url);
		if(headers != null){
			for(Header header : headers)
				get.addHeader(header);
		}
		CloseableHttpClient client = HttpClients.createDefault();
		CloseableHttpResponse sp=client.execute(get, HttpClientContext.create());
		String result=null;
		if (null!=(result=Redirect(sp,null,"GET")))
			return result;
		return EntityUtils.toString(sp.getEntity(),"UTF-8");
		
	}
	
	//DEL
	//PUT
		
	//redirect
	private static String Redirect(CloseableHttpResponse sp,String paras,String method) throws Exception{
		//307
		if(sp.getStatusLine().getStatusCode()==307){
			Header[] hs=sp.getHeaders("Location");
			String redirectUrl=hs[0].getValue();
			if(method=="POST"){
				System.out.println("--------redirect-------");
				return basePost(null, redirectUrl, paras);
			}
			else if(method=="GET"){
				System.out.println("--------redirect-------");
				return baseGet(null, redirectUrl);
			}
		}
		
		//308
		
		return null;
	}
	
	private static String before(String relativeUrl) throws Exception{
		if (hosts==null)
			init();
		if (!relativeUrl.startsWith("/"))
			relativeUrl = "/"+relativeUrl;
		return hosts[i%hosts.length];
	}
	
	public static void main(String[] args) throws Exception {
		HttpSend.post(null, HttpUrls.DEL_CLUSTER_P_OLD, "service=testservice&cluster=testcluster");
	}
}
