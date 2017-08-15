package com.weibo.dorteam.Bean;

public class NamingServiceInfo {

	private String name;
	private String type;
	private double threshold;
	
//	public NamingServiceInfo(String name, String type, double threshold){
//		this.name = name;
//		this.type = type;
//		this.threshold = threshold;
//	}
	
	public void setName(String name){
		this.name = name;
	}
	public void setType(String type){
		this.type = type;
	}
	public void setThreshold(double threshold){
		this.threshold = threshold;
	}
	
	public String getName(){
		return this.name;
	}
	public String getType(){
		return this.type;
	}
	public double getThreshold(){
		return this.threshold;
	}
	
	public boolean equals(Object obj) {
        if (obj instanceof NamingServiceInfo) {
        	NamingServiceInfo instance = (NamingServiceInfo) obj;
            return (name.equals(instance.name) && type.equals(instance.type) && (threshold == instance.threshold));
        }
        return super.equals(obj);
    }
	
	public int hashCode() {
		NamingServiceInfo instance = (NamingServiceInfo) this;
        return name.hashCode();
    }
	
//	public static void main(String[] args){
//		NamingServiceInfo ins1 = new NamingServiceInfo("liuyu","static", 0.6);
//		NamingServiceInfo ins2 = new NamingServiceInfo("liuyu","static", 0.6);
//		System.out.println(ins1.equals(ins2));
//	}
}
