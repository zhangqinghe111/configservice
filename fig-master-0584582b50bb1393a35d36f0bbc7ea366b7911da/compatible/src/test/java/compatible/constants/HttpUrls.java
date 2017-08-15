package compatible.constants;

public class HttpUrls {
	//action=addservice,type= , threshold=
	public static String ADD_SERVICE_P_OLD="/naming/admin?action=addservice";
	public static String ADD_SERVICE_P_NEW="/naming/admin";
	
	//action=deleteservice,service=
	public static String DEL_SERVICE_D_NEW="/naming/admin?action=deleteservice&service=";
	public static String DEL_SERVICE_P_OLD="/naming/admin?action=deleteservice";
	
	//action=updateservice,type= , threshold=
	public static String UPDATE_SERVICE_P_OLD="/naming/admin?action=updateservice";
	public static String UPDATE_SERVICE_PUT_NEW="/naming/admin?action=updateservice&service=";
	
	public static String LIST_SERVICE="/naming/admin?action=getservice";
	
	//action=addcluster,&service=testservice&cluster=testcluster
	public static String ADD_CLUSTER_P_OLD="/naming/admin?action=addcluster";
	public static String ADD_CLUSTER_P_NEW="/naming/admin";
	
	//action=deletecluster&service=testservice&cluster=testcluster
	public static String DEL_CLUSTER_D_NEW="/naming/admin";
	public static String DEL_CLUSTER_P_OLD="/naming/admin?action=deletecluster";
	
	//&service=testservice
	public static String GET_CLUSTER="/naming/admin?action=getcluster";
	
	//action=add&service=testservice&node=127.0.0.1
	public static String ADD_WHITELIST_P_OLD="/naming/whitelist?action=add";
	public static String ADD_WHITELIST_P_NEW="/naming/whitelist";
	
	//"action=delete&service=testservice&node=127.0.0.1"
	public static String DEL_WHITELIST_D_NEW="/naming/whitelist";
	public static String DEL_WHITELIST_P_OLD="/naming/whitelist?action=delete";
	
	//"?action=update&service=testservice&node=127.0.0.1"
	//TODO : ?? no new 
	public static String UPDATE_WHITELIST_P_OLD="/naming/whitelist?action=update";
	
	public static String GET_WHITELIST="/naming/whitelist?action=get&service=testservice";
	
	//&service=testservice&node=127.0.0.1
	public static String EXIST_WHITELIST="/naming/whitelist?action=exists";
	
	//action=register&service=testservice&cluster=testcluster&node=127.0.0.1:8888&extInfo=interface description
	public static String ADD_NODE_P_OLD="/naming/service?action=register";
	public static String ADD_NODE_P_NEW="/naming/service";
	
	//service=yf-rpc-test&cluster=user-pool1&node=127.0.0.1:8881&extInfo=interfacelllllll
	public static String DEL_NODE_D_NEW="/naming/service?action=unregister";
	public static String DEL_NODE_P_OLD="/naming/service?action=unregister";
	
	public static String GET_NODESERVICE="/naming/service?action=getnodeservice&ip=";
	public static String GET_SERVICE_HEART="/naming/service?action=heartbeat";
	
	///naming/service?action=getsign&service=yf-rpc-test&cluster=user-pool1" 
	public static String GET_SIGN="/naming/service?action=getsign&service=";
	
	//??curl -d "action=batchunregister&node=127.0.0.1:8888&service=testservice” “http://ip:port/naming/service"
	public static String BATCH_UNREGIST="/naming/service";
	
	
			
}
