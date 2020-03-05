package scs.controller.cpu; 

import scs.controller.ControlDriver;
import scs.controller.ControlDriver.Enum_BE_Tolerance;
import scs.controller.ControlDriver.Enum_State; 
import scs.util.repository.Repository;  

/**
 * CPU 控制器代码
 * 分配CPU核心和Cache way
 * 启动或者暂停BE作业
 * @author yanan
 * @date 2019-02-02
 */
public class CpuController extends Thread{

	private ControlDriver controlDriver=ControlDriver.getInstance();
	private final int SLEEP_TIME=2;//5s
	@Override
	public void run(){
		int SLEEP_TIME_MS=SLEEP_TIME*1000;
		Enum_State state=null;
		float[] cacheMemBwUsage=new float[5];
		float overageMemBwUsage=0.0f; 
		float overageCoreNum=0.0f;
		float overageLLcWay=0.0f;
		float MEM_BW_LIMIT_SIZE=Repository.systemMemBandWidthSize*Repository.memBandWidthLimitRate;
		while(Repository.SYSTEM_RUN_FLAG){
			/**
			 * 必须算的量
			 */
			cacheMemBwUsage=controlDriver.getAllMemBwUsage();//查询当前系统的所有核心内存带宽使用
			//	//System.out.println("cpuControl: "+cacheMemBwUsage[0]+" "+cacheMemBwUsage[1]+" "+cacheMemBwUsage[2]+" "+cacheMemBwUsage[3]+" "+cacheMemBwUsage[4]);
			controlDriver.calAvgTaskResourceUsageRate();//计算BE任务的当前资源利用率
			/**
			 * 其它人要用
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
			/**
			 * 判断内存带宽是否超限,是则减少BE核心绑定,否则判断是否可以增加BE资源
			 */
			if(cacheMemBwUsage[3]>MEM_BW_LIMIT_SIZE){
				overageMemBwUsage=Repository.systemMemBandWidthSize-cacheMemBwUsage[3]*Repository.memBandWidthLimitRate;//计算内存带宽过量值
				overageCoreNum=overageMemBwUsage/controlDriver.calBeBwPerLogicCore();
				overageLLcWay=Repository.BE_TASK.getLlcWayNums()*(overageCoreNum/Repository.BE_TASK.getBindLogicCoreNum());
				//System.out.println("cpuControl: 检查到内存带宽超限 change BE core"+(-((int)Math.ceil(overageCoreNum))));
				controlDriver.changeBeTaskLogicCore(-((int)Math.ceil(overageCoreNum)));
				//System.out.println("cpuControl: 检查到内存带宽超限 change BE LLC"+(-((int)Math.ceil(overageLLcWay))));
				controlDriver.changeBeTaskLLC(-((int)Math.ceil(overageLLcWay)));
				continue;
			}else{
				//往下走
			}

			/**
			 * 确保BE能充分利用资源,防止资源浪费
			 * 直到BE的利用率提升上去,才可以考虑增加 LLC和core
			 */
			@SuppressWarnings("unused")
			float predictValue=0;
			
			if(Repository.BE_TASK.getAvgCpuUsageRate()<Repository.cpuUsageLimitRate){ //如果BE没有充分利用cpu,则增加BE副本数量
				state=Enum_State.GROW_COPY;
				//System.out.println("BE 利用率:"+Repository.BE_TASK.getAvgCpuUsageRate()+"不高 state=Enum_State.GROW_COPY;");
				if((predictValue=controlDriver.predictedTotalBw(state))<MEM_BW_LIMIT_SIZE){
					//System.out.println("GROW_COPY状态 预测内存带宽="+predictValue+"不超限 增加BE 1个副本");
					if(controlDriver.getUnstopedBeProcessCount()<Repository.BE_TASK.getBindLogicCoreNum()){
						controlDriver.addBeTaskCopy();
					} 
					//这一步结束之后 不要着急转向增加LLC和core 要继续保持GROW_COPY状态,保证BE充分利用后才可以增加CORE和LLC
				}else{
					//System.out.println("GROW_COPY状态 预测内存带宽="+predictValue+"超限 不增加BE 副本数量");
					state=Enum_State.GROW_LLC;
				}
			}else{
				//System.out.println("BE 利用率:"+Repository.BE_TASK.getAvgCpuUsageRate()+"达到指标 state=Enum_State.GROW_LLC;");
				state=Enum_State.GROW_LLC;
			}
			/**
			 * 进入增加llc状态
			 */
			if(state==Enum_State.GROW_LLC){
				if((predictValue=controlDriver.predictedTotalBw(state))<MEM_BW_LIMIT_SIZE){
					//System.out.println("GROW_LLC状态 预测内存带宽="+predictValue+"不超限 增加BE 1路带宽");
					controlDriver.changeBeTaskLLC(1);
				}else{
					//System.out.println("GROW_LLC状态 预测内存带宽="+predictValue+"超限 转向增加CORE");
				}
				state=Enum_State.GROW_CORE;
			} 
			/**
			 * 进入增加Core状态
			 */
			if(state==Enum_State.GROW_CORE){
				if((predictValue=controlDriver.predictedTotalBw(state))<MEM_BW_LIMIT_SIZE){//if(slack>0.1){addCore()} 
					//System.out.println("GROW_CORE状态 预测内存带宽="+predictValue+"不超限 增加BE 2 core");
					controlDriver.changeBeTaskLogicCore(Repository.threadPerCore);
				}else{
					//System.out.println("GROW_CORE状态 预测内存带宽="+predictValue+"超限  转向增加LLC");
				}
				state=Enum_State.GROW_LLC;
			}
			try {
				Thread.sleep(SLEEP_TIME_MS);
			} catch (InterruptedException e) { 
				e.printStackTrace();
			}
		}
	}

}
