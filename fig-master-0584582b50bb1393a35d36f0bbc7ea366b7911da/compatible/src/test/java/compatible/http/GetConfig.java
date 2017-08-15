package compatible.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

public class GetConfig {
	static GetConfig gc=null;
	public static Properties getConfig(String name) throws Exception{
		if(gc==null)
			gc =new GetConfig();
		Properties p =new Properties();
		p.load(new FileInputStream(getFile(name)));
		return p;
	}
	
	public static File getFile(String name)throws IOException{
		if(gc==null)
			gc =new GetConfig();
		URL u=gc.getClass().getResource("/");
//		if(!name.startsWith("/"))
//			name="/"+name;
		File f = new File(u.getPath()+name);
		System.out.println(f.getPath());
		if(!f.exists()){
			System.out.println("文件不存在，创建中 :"+f.getAbsolutePath());
			f.createNewFile();
		}	
		return f;
	}
	
	public static FileReader readFile(String name) throws IOException{
		return new FileReader(getFile(name));
	}
	
	public static FileWriter writeFile(String name) throws IOException{
		return new FileWriter(getFile(name),true);
	}
}
