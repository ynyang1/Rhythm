package scs.util.loadGen;

import java.rmi.RemoteException;
import scs.util.repository.Repository;
import scs.util.rmi.LoadInterface;  
/**
 * 生成真实负载的线程
 * @author yanan
 *
 */
public class HttpRealLoadThread extends Thread{

	LoadInterface loader;
	int intervalTime;
	/**
	 * 初始化方法
	 */ 	
	public HttpRealLoadThread(LoadInterface loader,int intervalTime){
		this.loader=loader;
		this.intervalTime=intervalTime;
	}
	@Override
	public void run(){ 
		try {
			System.out.println(Thread.currentThread().getName()+"--------真实负载开始--------- "+Repository.simRPSList.size());
			int i=0;
			int size=Repository.simRPSList.size();
			if(Repository.nodeType.equals("leader")){
				Repository.loader.execStopHttpLoader(Repository.serviceType);
				Thread.sleep(8000);//睡眠5秒钟等待生效
				Thread thread=new Thread(new StartHttpThread(Repository.serviceType));
				thread.start();
				Thread.sleep(7000);//睡眠5秒钟等待生效
			}else{
				Thread.sleep(15000);//睡眠5秒钟等待生效
			}
			if(Repository.nodeType.equals("leader")){
				while(Repository.SYSTEM_RUN_FLAG){
					loader.setIntensity(Repository.simRPSList.get(i%size),Repository.serviceType);
					Repository.setRequestIntensity=Repository.simRPSList.get(i%size);
					Thread.sleep(intervalTime);//睡眠30秒钟等待生效
					i++;
				}
			}else{
				while(Repository.SYSTEM_RUN_FLAG){
					Repository.setRequestIntensity=loader.getRealRequestIntensity(Repository.serviceType);
					Thread.sleep(1000);//睡眠30秒钟等待生效
				}
			}

			/**
			 * 实验结束,leader节点 将负载生成器设置为QPS=1
			 */
			if(Repository.nodeType.equals("leader")){
				Repository.loader.execStopHttpLoader(Repository.serviceType);
			} 
			System.out.println(Thread.currentThread().getName()+"--------真实负载结束---------");
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	class StartHttpThread extends Thread{
		private int serviceType;

		/**
		 * 初始化方法
		 */ 	
		public StartHttpThread(int serviceType){
			this.serviceType=serviceType;
		}
		@Override
		public void run(){ 
			try {
				Repository.loader.execStartHttpLoader(serviceType);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}


