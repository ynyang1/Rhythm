package scs.util.loadGen;

import java.io.IOException;
import java.util.List;

import scs.util.repository.Repository;
import scs.util.tools.FileOperation;

public class GenSimQpsDriver {
	private static GenSimQpsDriver driver=null;
	
	private GenSimQpsDriver(){}
	
	public synchronized static GenSimQpsDriver getInstance() {
		if (driver == null) {  
			driver = new GenSimQpsDriver();
		}  
		return driver;
	}
	/**
	 * 根据真实的负载换算成对应的模拟负载 并储存在Repository类中
	 * @param maxSimQPS 模拟负载的最大QPS
	 * @param filePath 真实负载数据的路径
	 * @param maxRealLoadRate 真实负载的最大负载率 0-1
	 * @throws IOException
	 */
	public void genSimRPSList(int maxSimQPS,String filePath,double maxRealLoadRate) throws IOException{
		List<Integer> realRPSList=this.getRealRPSList(filePath);
		int maxRealRPS=0;
		for(int item:realRPSList){
			maxRealRPS=maxRealRPS>item?maxRealRPS:item;//计算负载中最大值
		}
		maxRealRPS=(int)(maxRealRPS/maxRealLoadRate);//根据负载最大比例进行换算 一般规定最大值为最大负载的0.9
		int size=realRPSList.size();
		for(int i=0;i<size;i++){
			realRPSList.set(i,(int)(realRPSList.get(i)*1.0/maxRealRPS*maxSimQPS));
		}
		Repository.simRPSList.addAll(realRPSList);
		if(Repository.simRPSList.size()>0){
			System.out.println(Thread.currentThread().getName()+" 模拟真实负载生成完毕");
		}
	}
	/**
	 * 读取文件 获取真实的rps负载数据
	 * @param filePath
	 * @return
	 * @throws IOException
	 */
	private List<Integer> getRealRPSList(String filePath) throws IOException{
		FileOperation fo=new FileOperation();
		return fo.readIntFile(filePath);
	}
}
