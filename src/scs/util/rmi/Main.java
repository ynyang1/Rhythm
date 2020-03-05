package scs.util.rmi; 

import java.io.IOException;
import java.rmi.RemoteException;

import scs.util.experiment.AutoExp;
import scs.util.experiment.ExecDriver;
import scs.util.loadGen.GenSimQpsDriver;
import scs.util.repository.Repository; 

public class Main {
	public static void main(String[] args) throws InterruptedException, IOException {
 
		//new Main().getEFRAResult(args);
		//new Main().getWebServerRealLoadExperResult(args);
		//new Main().getRedisExperResult(args);


		new Main().getHebeExperResult(args);

		//AutoExp.getInstance().autoStartExp(args);
		//ControlDriver 540 line
		//AgentRMIService.getInstance().setUp();
		//ThresholdSet.getInstance().iterateThreshold();

	} 

	/**
	 * 获取实验结果
	 * @param args
	 * @param intervalTime 真实负载每个档次的持续时长
	 * @throws InterruptedException
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public void getHebeExperResult(String[] args)throws InterruptedException, NumberFormatException, IOException {
		if(args.length!=13){
			System.out.println(args.length);
			System.out.println("[qpsStr(0/1#2#3#) maxQps nodeName nodeType(leader/fo) isSameThreshold(same/diff) beName isProduct(0/1)" //0-6
					+ " level(0-5) execTime(ms) maxBeCount intervalTime(ms) isHttp(0 redis/1 http/2 wrk) serviceType]"); //7-10
		}else{
			String qpsStr=args[0].trim();
			Repository.maxRequestIntensity=Integer.parseInt(args[1].trim());//用于计算EMU
			Repository.nodeName=args[2].trim(); 
			Repository.nodeType=args[3].trim();
			Repository.isSameThreshold=args[4].trim();
			Repository.beName=args[5].trim();
			Repository.isProduct=args[6].trim();
			int controlLevel=Integer.parseInt(args[7].trim());
			int execTime=Integer.parseInt(args[8].trim());
			int maxBeCount=Integer.parseInt(args[9].trim());
			int intervalTime=Integer.parseInt(args[10].trim());
			String isHttp=args[11].trim();
			Repository.serviceType=Integer.parseInt(args[12].trim());
			try {
				GenSimQpsDriver.getInstance().genSimRPSList(Repository.maxRequestIntensity,"/home/tank/sdcloud/result/rps.txt",0.9);
				if(isHttp.equals("0")){ //redis负载生成器引擎 0
					if(qpsStr.equals("0")){
						new ExecDriver().redisRealLoadMixed(controlLevel,execTime,maxBeCount,intervalTime);
					}else{
						String[] qpsList=qpsStr.split("#");
						new ExecDriver().redisFixedLoadMixed(qpsList,controlLevel,execTime,maxBeCount);
					}
				}else if(isHttp.equals("1")){ //http负载生成器引擎 1
					if(qpsStr.equals("0")){
						new ExecDriver().webServerRealLoadMixed(controlLevel,execTime,maxBeCount,intervalTime);
					}else{
						String[] qpsList=qpsStr.split("#");
						new ExecDriver().webServerFixedLoadMixed(qpsList,controlLevel,execTime,maxBeCount);
					}
				}
				/*else{ //wrk负载生成器引擎 2
					if(qpsStr.equals("0")){
						new ExecDriver().wrkRealLoadMixed(controlLevel,execTime,maxBeCount,intervalTime);
					}else{
						String[] qpsList=qpsStr.split("#");
						new ExecDriver().wrkFixedLoadMixed(qpsList,controlLevel,execTime,maxBeCount);
					}
				} */

			} catch (IOException e) {
				e.printStackTrace();
			} 
		} 
	}

	/**
	 * 获取干扰表征数据
	 * @param args args[0] 重复计算次数
	 * @throws InterruptedException
	 */
	//	private void getInterferTableRedis(String[] args)throws InterruptedException {
	//		try {
	//			new ExecDriver().getInterferTableWebServer(Integer.parseInt(args[0]));
	//		} catch (NumberFormatException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//
	//		} catch (IOException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		}
	//	}
	private void test(){ 
		
		try {
			System.out.println("start call begin"); 
			System.out.println("start call end");
			System.out.println("stop call begin");
			Repository.loader.execStopHttpLoader(5);
			System.out.println("stop call end");
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("stop");
	}


}
