package scs.util.loadGen;
 
import java.rmi.RemoteException;

import scs.util.repository.Repository;
import scs.util.rmi.LoadInterface;  
/**
 * 生成真实负载的线程
 * @author yanan
 *
 */
public class RedisRealLoadThread extends Thread{

	LoadInterface loader;
	int intervalTime;
	/**
	 * 初始化方法
	 */ 	
	public RedisRealLoadThread(LoadInterface loader,int intervalTime){
		 this.loader=loader;
		 this.intervalTime=intervalTime;
	}
	@Override
	public void run(){ 
		try {
			System.out.println("--------真实负载开始--------- "+Repository.simRPSList.size());
			int i=0;
			int size=Repository.simRPSList.size();
			while(Repository.SYSTEM_RUN_FLAG){
				if(Repository.nodeType.equals("leader")){
					loader.execRedisLoader(0,Repository.serviceType);
					loader.setIntensity(Repository.simRPSList.get(i%size),Repository.serviceType);
					RedisRemoteExecThread execRedisThread=new RedisRemoteExecThread(loader,Repository.simRPSList.get(i%size));
					execRedisThread.start();//此处为异步开启 不然程序不会往下走
					Repository.setRequestIntensity=Repository.simRPSList.get(i%size);
					Thread.sleep(intervalTime);//睡眠30秒钟等待生效
				}else{
					Repository.setRequestIntensity=Repository.simRPSList.get(i%size);
					Thread.sleep(intervalTime);//睡眠30秒钟等待生效
				}
				i++;
			}
			/**
			 * 实验结束,leader节点 将负载生成器关闭
			 */
			if(Repository.nodeType.equals("leader")){
				loader.execRedisLoader(0,Repository.serviceType);
			} 
			System.out.println(Thread.currentThread().getName()+"--------真实负载结束---------");
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
}


