package scs.controller.net;

import scs.controller.ControlDriver;
import scs.controller.ControlDriver.Enum_BE_Tolerance;
import scs.util.repository.Repository;  
/**
 * 网络控制器代码
 * 调整LC和BE使用的网络带宽
 * @author yanan
 * @date 2019-02-02
 */
public class NetController extends Thread{

	private long lastNetInfo[]=new long[5]; 
	private long curNetInfo[]=new long[5];  
	private long diffNetInfo[]=new long[5]; 

	private ControlDriver controlDriver=ControlDriver.getInstance();
	private final int SLEEP_TIME=1;//1s
	/**
	 * 初始化方法
	 */
	private void init(){
		lastNetInfo=controlDriver.getLcNetPackageInfo(); //读取一次网卡使用信息
		
	}
	private void calDiffValue(){
		for(int i=0;i<curNetInfo.length;i++){ 
			diffNetInfo[i]=(curNetInfo[i]-lastNetInfo[i])/SLEEP_TIME;
			lastNetInfo[i]=curNetInfo[i];
		}
	}

	@Override
	public void run(){ 
		
		int SLEEP_TIME_MS=SLEEP_TIME*1000;
		int netBandWidthLcUsed=0;
		int netBandWidthBeCanUse=0;
		int netBandWidthBeCanGrow=0;
		this.init();
		try {
			Thread.sleep(SLEEP_TIME_MS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		while(Repository.SYSTEM_RUN_FLAG){
			/**
			 * 必须要算的量
			 */
			curNetInfo=controlDriver.getLcNetPackageInfo();
			this.calDiffValue();//计算上一秒的网络状况 
			Repository.lcAvgNetBwUsagePerc=(diffNetInfo[0]>>7)*100.0f/Repository.systemNetBandWidthSize;
			////System.out.println(diffNetInfo[0]+"byte/s " +(diffNetInfo[0]>>7)+" kbit/s " +(diffNetInfo[0]>>17)+"mbit/s");
			
			if(diffNetInfo[2]>0){ //如果LC有丢包,那么立即减少BE的带宽50% kbit 
				//System.out.println("丢包改变 "+(Repository.BE_TASK.getNetBandWidthLimit()>>1));
				controlDriver.changeBeTaskNetFlowSpeed(-(Repository.BE_TASK.getNetBandWidthLimit()>>1));//该函数会同时更新仓库类BE_TASK的网络带宽使用量
				//controlDriver.setBeTaskNetFlowSpeed(-(Repository.BE_TASK.getNetBandWidthLimit()>>1));//该函数会同时更新仓库类BE_TASK的网络带宽使用量
			}else if(diffNetInfo[3]>0){//如果LC overLimit,那么立即减少BE的带宽12.5% kbit 
				//System.out.println("over limit改变 "+(Repository.BE_TASK.getNetBandWidthLimit()>>2));
 			controlDriver.changeBeTaskNetFlowSpeed(-(Repository.BE_TASK.getNetBandWidthLimit()>>2));//该函数会同时更新仓库类BE_TASK的网络带宽使用量
			//	controlDriver.setBeTaskNetFlowSpeed(Repository.BE_TASK.getNetBandWidthLimit()-(Repository.BE_TASK.getNetBandWidthLimit()>>3));//该函数会同时更新仓库类BE_TASK的网络带宽使用量
			}else{//否则,则尝试增加BE的带宽  
				/**
				 * 做出决策
				 */
				if(Repository.BE_EXEC_STATUS!=Enum_BE_Tolerance.ENABLE_ALLOW_GROW){
					try {
						Thread.sleep(SLEEP_TIME_MS); //如果不是可以增加BE资源的状态,不进行操作(因为下面的操作目的是增加BE的资源)
						//System.out.println("不可以增加BE 资源 等待...");
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					continue; 
				}else{
					//程序往下执行
				}
				netBandWidthLcUsed=(int)(diffNetInfo[0]>>7);//计算LC任务使用的网络带宽,由byte*8/1024换算成kbit
				netBandWidthBeCanUse=(int)(Repository.systemNetBandWidthSize-netBandWidthLcUsed*1.5);//计算BE可以用的带宽总额 20%带宽作为LC预留带宽
				if(netBandWidthBeCanUse>0){
					netBandWidthBeCanGrow=netBandWidthBeCanUse-Repository.BE_TASK.getNetBandWidthLimit();//计算BE可以用的带宽和当前已分配的带宽差值
					if(netBandWidthBeCanGrow>0){
						//System.out.println("改变BE "+(netBandWidthBeCanGrow>>2));
						controlDriver.changeBeTaskNetFlowSpeed(netBandWidthBeCanGrow>>2);//该函数会同时更新仓库类BE_TASK的网络带宽使用量
					}else{
						//System.out.println("改变BE "+(netBandWidthBeCanGrow<<2));
						controlDriver.changeBeTaskNetFlowSpeed(netBandWidthBeCanGrow<<2);//该函数会同时更新仓库类BE_TASK的网络带宽使用量
					}
					//if((int)(netBandWidthBeCanGrow>>1)>0){//二分法增长
						
				//	} 
				//	//System.out.println("设置BE"+(netBandWidthBeCanUse));
				//	controlDriver.setBeTaskNetFlowSpeed(netBandWidthBeCanUse);//该函数会同时更新仓库类BE_TASK的网络带宽使用量
				}
			}
			try {
				Thread.sleep(SLEEP_TIME_MS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	 
}
