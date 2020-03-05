package scs.controller;
import java.rmi.RemoteException;

import scs.controller.ControlDriver.Enum_BE_Tolerance;
import scs.util.repository.Repository;

/**
 * TopController类 
 * @author yanan
 * @date 2019-02-02
 */
public class TopController extends Thread{
	private ControlDriver controlDriver=ControlDriver.getInstance();
	private final int SLEEP_TIME=2;//10s
	

	@Override
	public void run(){
		int SLEEP_TIME_MS=SLEEP_TIME*1000;
		float lcLoad=0.0f;
		float lcLatency=0.0f;
		float slack=0.0f;
		float halfSlackLimit=Repository.slackLimit/2;
		
		float lcLatencyTarget=Repository.lcLatencyTarget;
		
		while(Repository.SYSTEM_RUN_FLAG){
			lcLoad=Repository.setRequestIntensity*1.0f/Repository.maxRequestIntensity;
			try {
				lcLatency=controlDriver.getLcAvgLatency(Repository.serviceType);
			} catch (RemoteException e1) {
				e1.printStackTrace();
			}
			slack=(lcLatencyTarget-lcLatency)/lcLatencyTarget;
		
			if(slack<0){
				Repository.sloViolateCounter++; 
				this.killBE();//已经打破SLO,立即停止BE任务，并且释放资源
			}else{
				if(lcLoad>Repository.loadLimit){
					//System.out.println("slack="+slack+ " load="+lcLoad+" pause BE");
					this.pauseBE();//即将打破SLO,暂停BE任务 
				}else if(lcLoad<Repository.loadLimit-0.05){//0.05的缓冲区
					if(slack<halfSlackLimit){//小 0-0.15
						//System.out.println("slack="+slack+ " load="+lcLoad+" remove core BE "+(2-Repository.BE_TASK.getBindLogicCoreNum()));
						controlDriver.changeBeTaskLogicCore(Repository.threadPerCore-Repository.BE_TASK.getBindLogicCoreNum());
					}else if(slack<Repository.slackLimit){ //大 0.5-0.3
						//System.out.println("slack="+slack+ " load="+lcLoad+" disAllowGrowBE BE");
						this.disAllowGrowBE();//不允许继续混部BE任务，但已有BE可以继续运行
					}else{
						//System.out.println("slack="+slack+ " load="+lcLoad+" allowGrowBE BE");
						this.allowGrowBE();//允许BE增长
					}
				}

			}

			try {
				Thread.sleep(SLEEP_TIME_MS);
			} catch (InterruptedException e) { 
				e.printStackTrace();
			}
		}

	}
	private void killBE(){
		Repository.BE_EXEC_STATUS=Enum_BE_Tolerance.DISABLE_KILL; 
		if(Repository.BE_TASK.isPause()==true){
			controlDriver.unPauseBeTask();//暂停的容器先解除暂停,不然没法进行操作
		}
		controlDriver.killAllBeTaskCopy(); //top控制器杀掉所有BE释放资源
	
		controlDriver.changeBeTaskLogicCore(Repository.threadPerCore-
				Repository.BE_TASK.getBindLogicCoreNum());//sub控制器释放BE占用的核心给LC
		controlDriver.changeBeTaskLLC(1-Repository.llcWaySize);//sub控制器释放BE的llc带宽给LC任务
		controlDriver.changeBeTaskNetFlowSpeed(
				-(Repository.BE_TASK.getNetBandWidthLimit()>>1));
	}
	private void pauseBE(){
		Repository.BE_EXEC_STATUS=Enum_BE_Tolerance.DISABLE_PAUSE;
		if(Repository.BE_TASK.isPause()==false){
			controlDriver.pauseBeTask();//暂停所有BE
		} 
	}
	private void disAllowGrowBE(){
		if(Repository.BE_TASK.isPause()==true){
			controlDriver.unPauseBeTask();//暂停的容器先解除暂停,不然没法进行操作
		}
		Repository.BE_EXEC_STATUS=Enum_BE_Tolerance.ENABLE_DIS_GROW;
	}
	private void allowGrowBE(){
		if(Repository.BE_TASK.isPause()==true){
			controlDriver.unPauseBeTask();//暂停的容器先解除暂停,不然没法进行操作
		}
		Repository.BE_EXEC_STATUS=Enum_BE_Tolerance.ENABLE_ALLOW_GROW;
	}


}
