package scs.util.experiment;

import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import scs.util.repository.Repository;
import scs.util.rmi.Main;
import scs.util.tools.DateFormats;
import scs.util.tools.RandomString;

public class AutoExp {
	private AgentRMIService agentRMIService=null; 

	private static AutoExp autoExp=null;

	private AutoExp(){ 
		agentRMIService=AgentRMIService.getInstance();
	}
	public synchronized static AutoExp getInstance() {
		if (autoExp == null) {
			autoExp = new AutoExp();
		}
		return autoExp;
	}  
 
	
	public void autoStartExp(String[] args) throws RemoteException{
		try {
			if(Repository.curAgentId==0){
				agentRMIService.connRemoteAgent();
				
				String[] socket0args=args.clone();
				String[] socket1args=args.clone();
				String[] socket2args=args.clone();
				socket0args[2]+="0";
				socket1args[2]+="1";
				socket2args[2]+="2";
				socket1args[3]="fo";
				socket2args[3]="fo";

				//"[qpsStr(0/1#2#3#) maxQps nodeName nodeType(leader/fo) isSameThreshold(same/diff) beName isProduct(0/1)" //0-6
				//" level(0-5) execTime(ms) maxBeCount intervalTime(ms) isHttp(0 redis |1 http |2 wrk) serviceType]"

				ExecutorService executor = Executors.newFixedThreadPool(1);
				executor.execute(new AutoThread(1,socket1args));
				executor.execute(new AutoThread(2,socket2args));
				executor.shutdown();//停止提交任务

				new Main().getHebeExperResult(socket0args);//同步模式启动本地节点

				//检测全部的线程是否都已经运行结束
				while(!executor.isTerminated()){
					try {
						Thread.sleep(2000);
					} catch(InterruptedException e){
						e.printStackTrace();
					}
				}  
				System.exit(0);
			}else{
				agentRMIService.openLocalAgent();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	class AutoThread extends Thread{
		private int agentIndex;
		private String[] args;
		/**
		 * 初始化方法
		 */ 	
		public AutoThread(int agentIndex,String[] args){
			this.agentIndex=agentIndex;
			this.args=args;
		}

		@Override
		public void run(){ 
			System.out.println(Thread.currentThread().getName()+" autoExperiment start");
			try {
				AgentRMIService.getInstance().agentInterfaceList[agentIndex].autoStart(args);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			System.out.println(Thread.currentThread().getName()+" autoExperiment end");
		}
	}





}
