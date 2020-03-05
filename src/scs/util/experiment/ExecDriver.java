package scs.util.experiment;

import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import scs.controller.ControlDriver;
import scs.util.loadGen.RedisRemoteExecThread;
import scs.util.loadGen.RedisRealLoadThread;
import scs.util.loadGen.HttpRealLoadThread;
import scs.util.repository.Repository;
import scs.util.rmi.LoadInterface;
/**
 * 干扰表征实验
 * @author yanan
 *
 */
public class ExecDriver {
	private ControlDriver controlDriver=null;
	private LoadInterface loader=null;
	/**
	 * 构造方法
	 */
	public ExecDriver(){
		controlDriver=ControlDriver.getInstance();
		loader=Repository.loader;
	} 

	/**
	 * 干扰表征结果记录
	 * 也可以用来求最大QPS
	 * @param num
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void getInterferTableRedis(int num) throws InterruptedException, IOException{
		FileWriter writer=new FileWriter("/home/tank/redis_infer_table_latency.txt");
		FileWriter writer1=new FileWriter("/home/tank/redis_infer_table_qps.txt");
		for(int j=0;j<num;j++){
			System.out.println("第"+j+"遍");
			double maxQps=46;
			int curQps=0; 
			//for(double i=0.05;i<1;i=i+0.05){
			//	curQps=(int)(maxQps*i);
			for(curQps=2;curQps<maxQps;curQps=curQps+2)
			{
				loader.execRedisLoader(0,Repository.serviceType);//关闭查询
				Thread.sleep(3000);//睡眠5秒钟等待生效
				loader.setIntensity(curQps,Repository.serviceType);
				Thread.sleep(5000);//睡眠5秒钟等待生效
				Thread thread=new RedisRemoteExecThread(loader,1);
				thread.start();
				Thread.sleep(20000);//睡眠5秒钟等待生效
				for(int i=0;i<30;i++){
					writer.write(controlDriver.getLcCurLatency(Repository.serviceType)+"\t"); //每隔一秒查询实时延迟 99th size=30
					writer1.write(loader.getRealServiceRate(Repository.serviceType)+"\t"); //每隔一秒查询实时延迟 99th size=30

					writer.flush();
					writer1.flush();
					Thread.sleep(1000);//睡眠1秒钟
				}
				writer.write("\n");
				writer.flush();
				writer1.write("\n");
				writer1.flush();
			}
			writer.write("\n");
			writer.flush();
			writer1.write("\n");
			writer1.flush();
		}
		writer.close();
		writer1.close();
		System.out.println("程序结束");
		/*FileWriter writer=new FileWriter("/home/tank/exper_redis_qps_latency.txt");
		FileWriter writer1=new FileWriter("/home/tank/exper_redis_qps_realqps.txt");
		for(int k=0;k<num;k++){
			System.out.println("第"+k+"遍");
			for(int i=5;i<80;i=i+5){

				loader.execRedisLoader(0);//关闭查询
				Thread.sleep(2000);//睡眠5秒钟等待生效

				loader.setIntensity(i);

				Thread.sleep(2000);//睡眠5秒钟等待生效
				Thread thread=new TestThread(loader,i);
				thread.start();

				Thread.sleep(30000);//睡眠5秒钟等待生效
				for(int j=0;j<30;j++){
					writer.write(loader.getLcCurLatency()+"\t"); //每隔一秒查询实时延迟 99th size=30
					writer1.write(loader.getRealIntensity()+"\t"); //每隔一秒查询实时延迟 99th size=30

					writer.flush();
					writer1.flush();
					Thread.sleep(1000);//睡眠1秒钟
				}
				writer.write("\n");
				writer.flush();
				writer1.write("\n");
				writer1.flush();
			}
			writer.write("\n");
			writer.flush();
			writer1.write("\n");
			writer1.flush();
		}



		writer.close();
		writer1.close();
		System.out.println("程序结束");
		 */
	}

	/**
	 * 干扰表征结果记录
	 * 也可以用来求最大QPS
	 * @param num
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void getInterferTableWebServer(int maxQps,String beName) throws InterruptedException, IOException{
		LoadInterface loader=null;
		try {
			loader=(LoadInterface) Naming.lookup("rmi://192.168.1.128:22222/load");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
		if(loader!=null){
			System.out.println("rmi://192.168.1.128:22222/load 建立连接 success");
		}else{
			System.out.println("rmi://192.168.1.128:22222/load 建立连接 fail");
		}



		FileWriter writer=new FileWriter("/home/tank/sdcloud/result/webserver_infertable_"+beName+"_curlatency.txt");
		FileWriter writer0=new FileWriter("/home/tank/sdcloud/result/webserver_infertable_"+beName+"+_avglatency.txt");
		
		int curQps=0;

		for(double i=0.1;i<1;i=i+0.1){
			curQps=(int)(maxQps*i);
			//for(curQps=1500;curQps<maxQps;curQps=curQps+50){
			System.out.println("load="+i*100+"%");
			loader.setIntensity(curQps,1);
			Thread.sleep(60000);//睡眠30秒钟等待生效
			for(int k=0;k<120;k++){
				writer.write(loader.getLcCurLatency99th(1)+"\t"); //每隔一秒查询实时延迟 99th size=30
				writer0.write(loader.getLcAvgLatency(1)+"\t"); //每隔一秒查询实时延迟 99th size=30

				writer.flush();
				writer0.flush();
				Thread.sleep(1000);//睡眠1秒钟
			}
			writer.write("\n");
			writer.flush();
			writer0.write("\n");
			writer0.flush();
		}
		writer.write("\n");
		writer.flush();
		writer0.write("\n");
		writer0.flush();
		writer.close();
		writer0.close();
		System.out.println("程序结束");
	}

//	public void wrkFixedLoadMixed(String[] qpsList,int controlLevel,int execTime,int BeMaxCount) throws InterruptedException, IOException{
//		for(int i=0;i<qpsList.length;i++){
//			if(Repository.nodeType.equals("leader")){
//				if(i!=0||qpsList.length==1){
//					Repository.init();
//				}
//				loader.execWrkLoader(0,Repository.serviceType);//关闭查询
//				loader.setIntensity(Integer.parseInt(qpsList[i]),Repository.serviceType); 
//				Repository.setRequestIntensity=Integer.parseInt(qpsList[i]);
//				Thread thread=new WrkRemoteExecThread(loader,1);
//				thread.start();
//				Thread.sleep(30000);//睡眠30秒钟等待生效
//
//			}else{
//				if(i!=0||qpsList.length==1){
//					Repository.init();
//				}
//				Repository.setRequestIntensity=Integer.parseInt(qpsList[i]);
//				Thread.sleep(30000);//睡眠30秒钟等待生效
//
//			}
//			ControlDriver.start(controlLevel,execTime,BeMaxCount);
//		}
//		if(Repository.nodeType.equals("leader")){
//			loader.setIntensity(1,Repository.serviceType);
//			//loader.execWrkLoader(0,Repository.serviceType);//关闭查询
//		}
//	}

	/**
	 * webserver在真实负载下的混部控制实验
	 * @param controlLevel
	 * @param execTime
	 * @param BeMaxCount
	 * @throws InterruptedException
	 * @throws IOException
	 */
//	public void wrkRealLoadMixed(int controlLevel,int execTime,int BeMaxCount,int intervalTime) throws InterruptedException, IOException{
//		Thread realLoadThread=new Thread(new WrkRealLoadThread(loader,intervalTime));
//		realLoadThread.start();
//		ControlDriver.start(controlLevel,execTime,BeMaxCount);
//	}


	public void redisFixedLoadMixed(String[] qpsList,int controlLevel,int execTime,int BeMaxCount) throws InterruptedException, IOException{
		for(int i=0;i<qpsList.length;i++){
			if(Repository.nodeType.equals("leader")){
				if(i!=0||qpsList.length==1){
					Repository.init();
				}
				loader.execRedisLoader(0,Repository.serviceType);//关闭查询
				loader.setIntensity(Integer.parseInt(qpsList[i]),Repository.serviceType); 
				Repository.setRequestIntensity=Integer.parseInt(qpsList[i]);
				Thread thread=new RedisRemoteExecThread(loader,1);
				thread.start();
				Thread.sleep(30000);//睡眠30秒钟等待生效

			}else{
				if(i!=0||qpsList.length==1){
					Repository.init();
				}
				Repository.setRequestIntensity=Integer.parseInt(qpsList[i]);
				Thread.sleep(30000);//睡眠30秒钟等待生效

			}
			ControlDriver.start(controlLevel,execTime,BeMaxCount);
		}
		if(Repository.nodeType.equals("leader")){
			loader.execRedisLoader(0,Repository.serviceType);//关闭查询.
		}
		System.out.println(Thread.currentThread().getName()+" 程序结束");
	}
	/**
	 * webserver在真实负载下的混部控制实验
	 * @param controlLevel
	 * @param execTime
	 * @param BeMaxCount
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void redisRealLoadMixed(int controlLevel,int execTime,int BeMaxCount,int intervalTime) throws InterruptedException, IOException{
		Thread realLoadThread=new Thread(new RedisRealLoadThread(loader,intervalTime));
		realLoadThread.start();
		ControlDriver.start(controlLevel,execTime,BeMaxCount);
	}

	public void webServerFixedLoadMixed(String[] qpsList,int controlLevel,int execTime,int BeMaxCount) throws InterruptedException, IOException{
		for(int i=0;i<qpsList.length;i++){
			if(Repository.nodeType.equals("leader")){
				if(i!=0||qpsList.length==1){
					Repository.init();
				}
				Repository.loader.execStopHttpLoader(Repository.serviceType);
				Thread.sleep(8000);//睡眠5秒钟等待生效
				Thread thread=new Thread(new StartHttpThread(Repository.serviceType));
				thread.start();
				Thread.sleep(7000);//睡眠5秒钟等待生效
				loader.setIntensity(Integer.parseInt(qpsList[i]),Repository.serviceType);
				Repository.setRequestIntensity=Integer.parseInt(qpsList[i]);
				Thread.sleep(50000);//睡眠30秒钟等待生效
			}else{
				if(i!=0||qpsList.length==1){
					Repository.init();
				}
				Thread.sleep(5000);//睡眠30秒钟等待生效
				Repository.setRequestIntensity=Integer.parseInt(qpsList[i]);
				Thread.sleep(60000);//睡眠30秒钟等待生效
			}
			ControlDriver.start(controlLevel,execTime,BeMaxCount);
		}
		if(Repository.nodeType.equals("leader")){
			Repository.loader.execStopHttpLoader(Repository.serviceType);
		}
		System.out.println(Thread.currentThread().getName()+" 程序结束");
	}
	/**
	 * webserver在真实负载下的混部控制实验
	 * @param controlLevel
	 * @param execTime
	 * @param BeMaxCount
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void webServerRealLoadMixed(int controlLevel,int execTime,int BeMaxCount,int intervalTime) throws InterruptedException, IOException{
		Thread realLoadThread=new Thread(new HttpRealLoadThread(loader,intervalTime)); //线程内部睡了15秒
		realLoadThread.start();
		Thread.sleep(15000);//睡眠15秒钟等待生效
		ControlDriver.start(controlLevel,execTime,BeMaxCount);

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
