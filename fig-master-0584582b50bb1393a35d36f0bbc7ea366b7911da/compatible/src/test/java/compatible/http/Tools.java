package compatible.http;

import compatible.constants.HttpUrls;
import net.sf.json.JSONObject;

public class Tools {

	public static boolean JSONCompare(String[] ss){
		JSONObject j1= JSONObject.fromObject(ss[0]);
		JSONObject j2= JSONObject.fromObject(ss[1]);
		return (j1.compareTo(j2)==1);
	}
	
	public static String[] getOld(String oldUrl,String paras,String method) throws Exception{
		String[] ss= new String[2];
		if (method .equals("POST")){
			ss[0]=HttpSend.post(null, oldUrl, paras);
			ss[1]=HttpSend.postV1(null, oldUrl, paras);
		}
		else if (method.equals("GET")){
			ss[0]=HttpSend.get(null, oldUrl, paras);
			ss[1]=HttpSend.getV1(null, oldUrl, paras);
		}
		return ss;
	}
	
	public static boolean check(String oldUrl,String paras,String method) throws Exception{
		return Tools.JSONCompare(Tools.getOld(oldUrl,paras, method));
	}
}
