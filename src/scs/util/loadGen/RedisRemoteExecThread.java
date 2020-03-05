package scs.util.loadGen;
 
import java.rmi.RemoteException;

import scs.util.repository.Repository;
import scs.util.rmi.LoadInterface;  
/**
 * 异步执行redis的线程
 * @author yanan
 *
 */
public class RedisRemoteExecThread extends Thread{

	LoadInterface loader;
	/**
	 * 初始化方法
	 */ 	
	public RedisRemoteExecThread(LoadInterface loader,int qps){
		 this.loader=loader;
	}
	@Override
	public void run(){
		try {
			loader.execRedisLoader(1,Repository.serviceType);
		} catch (RemoteException e) {
			e.printStackTrace();
		}

	}
}


