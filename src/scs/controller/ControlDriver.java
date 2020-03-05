package scs.controller;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import scs.controller.cpu.CpuController;
import scs.controller.freq.FreqController;
import scs.controller.mem.MemController;
import scs.controller.net.NetController;
import scs.pojo.ContainerBean;
import scs.pojo.TaskBean;
import scs.pojo.TwoTuple;
import scs.util.experiment.TimerThread;
import scs.util.repository.Repository;
import scs.util.resource.DockerService;
import scs.util.resource.cpu.CpuAllocation;
import scs.util.resource.cpu.CpuMonitor;
import scs.util.resource.freq.FreqAllocation;
import scs.util.resource.freq.FreqMonitor;
import scs.util.resource.mem.MemAllocation;
import scs.util.resource.mem.MemMonitor;
import scs.util.resource.net.NetAllocation;
import scs.util.resource.net.NetMonitor;
import scs.util.utilitization.LatencyRecordController;
import scs.util.utilitization.UtilityController; 
/**
 * 所有资源的控制驱动
 * 被各个维度资源的监控线程所调用
 * 同时负责合法性校验,防止资源分配错误
 * @author yanan
 *
 */
public class ControlDriver {
	private Random random=new Random(); 

	private CpuMonitor cpuMonitor;
	private CpuAllocation cpuAlloc;

	private NetMonitor netMonitor;
	private NetAllocation netAlloc;

	private MemMonitor memMonitor;
	private MemAllocation memAlloc;

	private FreqMonitor freqMonitor; 
	private FreqAllocation freqAllocation; 

	private DockerService dockerService;

	private String allCoreStr;

	private static ControlDriver controlDriver=null;
	private ControlDriver(){
		this.cpuMonitor=CpuMonitor.getInstance();
		this.cpuAlloc=CpuAllocation.getInstance();  
		this.netMonitor=NetMonitor.getInstance();
		this.netAlloc=NetAllocation.getInstance();  
		this.memMonitor=MemMonitor.getInstance();
		this.memAlloc=MemAllocation.getInstance();
		this.freqMonitor=FreqMonitor.getInstance(); 
		this.freqAllocation=FreqAllocation.getInstance();
		this.dockerService=DockerService.getInstance();
		this.allCoreStr=cpuMonitor.getAllLogicCoreStr(Repository.bindSocketId);
		
	}
	public synchronized static ControlDriver getInstance() {
		if(controlDriver == null) {
			controlDriver = new ControlDriver();
		}
		return controlDriver;
	}
	/**
	 * 计算BE任务所有core平均占用的内存带宽
	 */
	public float calBeBwPerLogicCore(){
		float[] BECacheMemBwUsage=cpuMonitor.getMemBwUsage("["+Repository.BE_TASK.getBindLogicCoreListStr()+"]");
		return BECacheMemBwUsage[3]/Repository.BE_TASK.getBindLogicCoreNum();//分母不可能为0,因为至少分1个物理核心
	}
	/**
	 * 计算BE任务所有Copy平均占用的内存带宽
	 */
	public float calBeBwPerCopy(){
		if(Repository.BE_TASK.getCopy()<=0){ //如果当前没有副本数可供参考,则返回0
			return 0.0f;
		}else{
			float[] BECacheMemBwUsage=cpuMonitor.getMemBwUsage("["+Repository.BE_TASK.getBindLogicCoreListStr()+"]");
			return BECacheMemBwUsage[3]/Repository.BE_TASK.getCopy();
		}
	}
	/**
	 * 计算任务的资源利用率
	 * 遍历该类型任务的所有容器 然后计算CPU或者mem
	 * LC任务平均利用率
	 * BE任务平均利用率
	 * system平均利用百分比
	 * 存入Repository中
	 */
	public void calAvgTaskResourceUsageRate(){
		float totalCpuUsagePerc=0.0f;
		float cpuUsagePerc=0.0f;
		//float memUsagePerc=0.0f;
		List<ContainerBean> list=Repository.BE_TASK.getContainerList();
		for(ContainerBean container:list){
			cpuUsagePerc+=container.getCpuUsagePerc();//因为BE任务的所有容器绑核都是相同的,所以要累加
			//memUsagePerc+=container.getMemUsagePerc();
		}  
		totalCpuUsagePerc+=cpuUsagePerc;
		//	System.out.println("BE total CPU perc"+cpuUsagePerc+" 核心数量:"+Repository.BE_TASK.getBindLogicCoreNum());
		Repository.BE_TASK.setAvgCpuUsageRate(cpuUsagePerc/(Repository.BE_TASK.getBindLogicCoreNum()*100.0f));//除以逻辑核心数量再除以100得到一个(0,1)之间的值
		if(Repository.BE_TASK.getAvgCpuUsageRate()>1){
			Repository.BE_TASK.setAvgCpuUsageRate(1);
		}
		//Repository.BE_TASK.setAvgMemUsageRate(memUsagePerc/(Repository.BE_TASK.getContainerList().size()*100.0f));//求均值(0-1)之间的值
		/**
		 * 计算LC的负载
		 */
		cpuUsagePerc=0;//重置为0
		list=Repository.LC_TASK.getContainerList();
		for(ContainerBean container:list){
			cpuUsagePerc+=container.getCpuUsagePerc();//因为BE任务的所有容器绑核都是相同的,所以要累加
			//memUsagePerc+=container.getMemUsagePerc();
		}  
		totalCpuUsagePerc+=cpuUsagePerc;
		Repository.LC_TASK.setAvgCpuUsageRate(cpuUsagePerc/(Repository.LC_TASK.getBindLogicCoreNum()*100.0f));//除以逻辑核心数量再除以100得到一个(0,1)之间的值
		if(Repository.LC_TASK.getAvgCpuUsageRate()>1){
			Repository.LC_TASK.setAvgCpuUsageRate(1);
		}
		//Repository.BE_TASK.setAvgMemUsageRate(memUsagePerc/(Repository.BE_TASK.getContainerList().size()*100.0f));//求均值(0-1)之间的值
		Repository.systemAvgCpuUsagePerc=totalCpuUsagePerc/(Repository.LC_TASK.getBindLogicCoreNum()+Repository.BE_TASK.getBindLogicCoreNum());
	}
	/**
	 * 计算逻辑核心的平均频率
	 * @param logicCoreList
	 * @return 平均频率 单位:HZ
	 */
	private static ArrayList<Integer> cpus=new ArrayList<Integer>();
	public int calAvgCpuFreq(LinkedList<TwoTuple<Integer,Integer>> logicCoreList){
		int sumFreq=0;
		int avgFreq=0;
		if(Repository.threadPerCore==2){ //超线程情况
			cpus.clear();
			for(TwoTuple<Integer,Integer> item:logicCoreList){
				cpus.add(item.first);
				cpus.add(item.second);
			}
			for(int item:cpus){
				sumFreq+=freqMonitor.getCpuFreq(item);
			}
			avgFreq=sumFreq/(logicCoreList.size()<<1);//左移1位 乘以2
		}else{//非超线程情况
			cpus.clear();
			for(TwoTuple<Integer,Integer> item:logicCoreList){
				cpus.add(item.first);
			}
			for(int item:cpus){
				sumFreq+=freqMonitor.getCpuFreq(item); 
			}
			avgFreq=sumFreq/logicCoreList.size();
		}
		return avgFreq;  

	}
	/**
	 * 预测下一步的行动后,内存带宽的总使用量
	 * @param state 下一步的行动
	 * @return 内存带宽总使用量估计值
	 */
	public float predictedTotalBw(Enum_State state){
		float predictedTotalBw=0.0f;
		float[] cacheMemBwUsage=cpuMonitor.getMemBwUsage("["+allCoreStr+"]");
		switch(state) {
		case GROW_COPY:
			float beBwPerCopy=this.calBeBwPerCopy();
			predictedTotalBw=cacheMemBwUsage[3]+beBwPerCopy;//资源估计值=BE和LC已使用的+多开一个BE副本可能会用到的资源量
			break;
		case GROW_LLC:
			float[] lcCacheMemBwUsage=cpuMonitor.getMemBwUsage("["+Repository.LC_TASK.getBindLogicCoreListStr()+"]");
			predictedTotalBw=cacheMemBwUsage[3]+lcCacheMemBwUsage[3]/Repository.LC_TASK.getLlcWayNums();
			break;
		case GROW_CORE:
			lcCacheMemBwUsage=cpuMonitor.getMemBwUsage("["+Repository.LC_TASK.getBindLogicCoreListStr()+"]");
			predictedTotalBw=cacheMemBwUsage[3]+lcCacheMemBwUsage[3]*Repository.threadPerCore/Repository.LC_TASK.getBindLogicCoreNum();
			break;
		default: break;
		}
		return predictedTotalBw;

	}
	/**
	 * 增加BE任务的副本
	 * 脚本中配置每次增加1个
	 */
	public void addBeTaskCopy(){
		List<ContainerBean> list=Repository.BE_TASK.getContainerList();
		for(ContainerBean container:list){
			dockerService.execVoidCommand(container.getContainerName(),Repository.addBeCopyCommand.replace("#","_"+Repository.beName));
			Repository.BE_TASK.setCopy(controlDriver.getUnstopedBeProcessCount());
			System.out.println("BE copy="+Repository.BE_TASK.getCopy());
		} 
	}
	/**
	 * 暂停所有BE任务
	 */
	public void pauseBeTask(){
		List<ContainerBean> list=Repository.BE_TASK.getContainerList();
		for(ContainerBean container:list){
			dockerService.pauseContainer(container.getContainerName()); 
		} 
		Repository.BE_TASK.setPause(true);
	}
	/**
	 * 继续所有BE任务
	 */
	public void unPauseBeTask(){
		List<ContainerBean> list=Repository.BE_TASK.getContainerList();
		for(ContainerBean container:list){
			dockerService.unPauseContainer(container.getContainerName()); 
		} 
		Repository.BE_TASK.setPause(false);
	}
	/**
	 * kill 所有BE任务
	 */
	public void killAllBeTaskCopy(){
		List<ContainerBean> list=Repository.BE_TASK.getContainerList();
		for(ContainerBean container:list){
			dockerService.execCommand(container.getContainerName(),Repository.killAllBeTaskCommand);
		} 
		int beProcess=controlDriver.getUnstopedBeProcessCount();
		if(beProcess>0){
			System.err.println("be未正常全部被杀死");
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			for(ContainerBean container:list){
				dockerService.execCommand(container.getContainerName(),Repository.killAllBeTaskCommand);
			} 
			beProcess=controlDriver.getUnstopedBeProcessCount();
			if(beProcess>0){
				System.err.println("第二次 be未正常全部被杀死");
			}
			
		}else{
			Repository.BE_TASK.setCopy(0);//杀掉BE任务后需要改BE的数量为0
		}

	} 
	public void cleanBeTaskProduct(){
		List<ContainerBean> list=Repository.BE_TASK.getContainerList();
		for(ContainerBean container:list){
			dockerService.execCommand(container.getContainerName(),Repository.cleanResultCommand);
		} 
		int result=0;
		for(ContainerBean container:list){
			result+=Integer.parseInt(dockerService.execCommand(container.getContainerName(),Repository.beTaskCountResultCommand));
		}

		if(result>0){
			System.err.println("be product 未正常清理");
		}else{
			//System.out.println("be product 已清理");;//杀掉BE任务后需要改BE的数量为0
		}

	} 
	/**
	 * 增加BE任务的llc
	 * @param changeValue 变更值,正值增加,负值减少
	 */
	public void changeBeTaskLLC(int changeValue){
		if(changeValue>0){
			int curLcLlcWayNum=Repository.LC_TASK.getLlcWayNums();
			int result=curLcLlcWayNum-changeValue;
			if(result<1){
				result=1;//合法性校验
			}
			Repository.LC_TASK.setLlcWayNums(result);
			Repository.BE_TASK.setLlcWayNums(Repository.llcWaySize-Repository.LC_TASK.getLlcWayNums());
			cpuAlloc.refreshLLC();//刷新生效
		}else{
			int curLcLlcWayNum=Repository.BE_TASK.getLlcWayNums();
			int result=curLcLlcWayNum+changeValue;//注意负值
			if(result<1){
				result=1;//合法性校验
			}
			Repository.BE_TASK.setLlcWayNums(result);
			Repository.LC_TASK.setLlcWayNums(Repository.llcWaySize-Repository.BE_TASK.getLlcWayNums());
			cpuAlloc.refreshLLC();//刷新生效
		}

	}
	/**
	 * 修改BE任务的逻辑核心数量 
	 * @param changeValue 变更值,正值增加,负值减少
	 */
	public void changeBeTaskLogicCore(int changeValue){
		if(changeValue>0){ //为正值,则增加BE的逻辑核心数量
			int curLogicCoreNum=Repository.LC_TASK.getBindLogicCoreNum();
			int result=curLogicCoreNum-changeValue;
			changeValue=result<Repository.threadPerCore?(curLogicCoreNum-Repository.threadPerCore):changeValue;//合法性校验
			cpuAlloc.removeLcLogicCores(changeValue);
		}else{ //为负值,则增加BE的逻辑核心数量
			int curLogicCoreNum=Repository.BE_TASK.getBindLogicCoreNum();
			int result=curLogicCoreNum+changeValue;//负值直接用+号运算
			changeValue=result<Repository.threadPerCore?(curLogicCoreNum-Repository.threadPerCore):changeValue;//合法性校验
			cpuAlloc.removeBeLogicCores(-changeValue);//注意这里要变号

		}

	}
	/**
	 * 修改BE的网络流速度
	 * @param changeValue 变更值,正值增加,负值减少 
	 */
	public void changeBeTaskNetFlowSpeed(int changeValue){
		int curSpeed=Repository.BE_TASK.getNetBandWidthLimit();
		int result=curSpeed+changeValue;
		if(result<Repository.minNetBandWidthKeepSizePerContainer){
			result=Repository.minNetBandWidthKeepSizePerContainer;//合法性校验,如果小于16,则设置为16
		}else if(result>Repository.systemNetBandWidthSize){
			result=Repository.systemNetBandWidthSize;//合法性校验,如果大于上限,则设置为上限
		} 
		Repository.BE_TASK.setNetBandWidthLimit(result);
		netAlloc.refreshNetTxFlow("BE");
	}
	/**
	 * 设置BE的网络流速度
	 * @param result 更新后的值
	 */
	/*public void setBeTaskNetFlowSpeed(int result){
		if(result<Repository.minNetBandWidthKeepSizePerContainer){
			result=Repository.minNetBandWidthKeepSizePerContainer;//合法性校验,如果小于16,则设置为16
		}else if(result>Repository.systemNetBandWidthSize){
			result=Repository.systemNetBandWidthSize;//合法性校验,如果大于上限,则设置为上限
		} 
		Repository.BE_TASK.setNetBandWidthLimit(result);
		netAlloc.refreshNetTxFlow("BE");
	}*/
	/**
	 * 改变BE任务的cpu核心频率
	 * @param changeValue 正值为增,负值为减
	 */
	public void changeBeTaskCpuFreq(int changeValue){
		int result=0;
		List<TwoTuple<Integer,Integer>> BeCpuList=Repository.BE_TASK.getBindLogicCoreList();
		if(changeValue>0){//正值增加BE核心频率情况
			if(Repository.threadPerCore==2){//超线程情况
				cpus.clear();
				for(TwoTuple<Integer,Integer> item:BeCpuList){
					cpus.add(item.first);
					cpus.add(item.second);
				}
				for(int item:cpus){
					result=freqMonitor.getCpuFreq(item)+changeValue;
					result=result>freqMonitor.getCpuMaxFreq()?freqMonitor.getCpuMaxFreq():result;//合法性校验
					freqAllocation.setCpuFreq(Integer.toString(item),result);
				}
			}else{//非超线程情况
				cpus.clear();
				for(TwoTuple<Integer,Integer> item:BeCpuList){
					cpus.add(item.first);
				}
				for(int item:cpus){
					result=freqMonitor.getCpuFreq(item)+changeValue;
					result=result>freqMonitor.getCpuMaxFreq()?freqMonitor.getCpuMaxFreq():result;//合法性校验
					freqAllocation.setCpuFreq(Integer.toString(item),result);
				}
			}
		}else{//负值减少BE核心频率情况
			if(Repository.threadPerCore==2){//超线程情况
				cpus.clear();
				for(TwoTuple<Integer,Integer> item:BeCpuList){
					cpus.add(item.first);
					cpus.add(item.second);
				}
				for(int item:cpus){
					result=freqMonitor.getCpuFreq(item)+changeValue;
					result=result<freqMonitor.getCpuMinFreq()?freqMonitor.getCpuMinFreq():result;//合法性校验
					freqAllocation.setCpuFreq(Integer.toString(item),result);
				}
			}else{//非超线程情况
				cpus.clear();
				for(TwoTuple<Integer,Integer> item:BeCpuList){
					cpus.add(item.first);
				}
				for(int item:cpus){
					result=freqMonitor.getCpuFreq(item)+changeValue;
					result=result<freqMonitor.getCpuMinFreq()?freqMonitor.getCpuMinFreq():result;
					freqAllocation.setCpuFreq(Integer.toString(item),result);
				}
			}
		}


	}
	/**
	 * 修改容器的内存大小限制
	 * @param containerList 要修改的容器列表 
	 */
	public void changeMemoryLimit(List<ContainerBean> containerList){
		int result=0; 
		String type=containerList.get(0).getTaskType();
		TaskBean taskBean=null;
		if(type!=null&&type.equals("LC")){
			taskBean=Repository.LC_TASK;
		}else{
			taskBean=Repository.BE_TASK;
		}
		for(ContainerBean container:containerList){
			result=container.getMemoryLimit();
			if(result>Repository.systemMemorySize){
				result=Repository.systemMemorySize;//合法性校验,如果内存大于系统上限,则设置为系统最大值
				taskBean.setTotalMemLimit(taskBean.getTotalMemLimit()-result+Repository.systemMemorySize);//修正总的内存使用量
			}else if(result<Repository.minMemoryKeepSizePerContainer){
				result=Repository.minMemoryKeepSizePerContainer;//合法性校验,如果内存大小小于最小限制,则设置为最小限制值
				taskBean.setTotalMemLimit(taskBean.getTotalMemLimit()+Repository.minMemoryKeepSizePerContainer-result);//修正总的内存使用量
			}
			container.setMemoryLimit(result);
		}
		memAlloc.setContainerMemLimit(containerList);	
	}

	/**
	 * 修改容器的内存大小限制
	 * @param containerList 要修改的容器列表 
	 */
	public void changeMemoryLimit(ContainerBean container){
		int result=0;  
		String type=container.getTaskType();
		TaskBean taskBean=null;
		if(type!=null&&type.equals("LC")){
			taskBean=Repository.LC_TASK;
		}else{
			taskBean=Repository.BE_TASK;
		}
		result=container.getMemoryLimit();
		if(result>Repository.systemMemorySize){
			result=Repository.systemMemorySize;//合法性校验,如果内存大于系统上限,则设置为系统最大值
			taskBean.setTotalMemLimit(taskBean.getTotalMemLimit()-result+Repository.systemMemorySize);//修正总的内存使用量
		}else if(result<Repository.minMemoryKeepSizePerContainer){
			result=Repository.minMemoryKeepSizePerContainer;//合法性校验,如果内存大小小于512,则设置为512MB
			taskBean.setTotalMemLimit(taskBean.getTotalMemLimit()+Repository.minMemoryKeepSizePerContainer-result);//修正总的内存使用量
		}
		container.setMemoryLimit(result); 
		memAlloc.setContainerMemLimit(container);	
	}

	/**
	 * 查询所有核心的LLC和内存带宽使用情况
	 * @return float[5]={IPC MISSES LLC[KB] MBL[MB/s] MBR[MB/s]}
	 */
	public float[] getAllMemBwUsage(){
		float[] result=cpuMonitor.getMemBwUsage("["+allCoreStr+"]");
		Repository.systemAvgMemBwUsageRate=result[3]/Repository.systemMemBandWidthSize;
		return result;
	}
	/**
	 * 查询所有LC核心的LLC和内存带宽使用情况
	 * @return float[5]={IPC MISSES LLC[KB] MBL[MB/s] MBR[MB/s]}
	 */
	public float[] getLcMemBwUsage(){
		float[] result=cpuMonitor.getMemBwUsage("["+Repository.LC_TASK.getBindLogicCoreListStr()+"]");
		Repository.lcAvgMemBwUsagePerc=result[3]/Repository.systemMemBandWidthSize;
		return result;
	}
	/**
	 * 查询LC网卡的丢包状态
	 * @param netCard 网卡名称
	 * @return float[3]={droped,overLimit,requeue}
	 */
	public long[] getLcNetPackageInfo(){
		long sentByte=0;
		long sentPkt=0;
		long droped=0;
		long overLimit=0;
		long requeue=0;
		long[] singleContainerData=new long[5];
		List<ContainerBean> list=Repository.LC_TASK.getContainerList();
		for(ContainerBean container:list){
			singleContainerData=netMonitor.getDropOverLimit(container.getVirtualNetCard());
			sentByte+=singleContainerData[0];
			sentPkt+=singleContainerData[1];
			droped+=singleContainerData[2];
			overLimit+=singleContainerData[3];
			requeue+=singleContainerData[4];
		}  
		singleContainerData[0]=sentByte; 
		singleContainerData[1]=sentPkt;
		singleContainerData[2]=droped; 
		singleContainerData[3]=overLimit;
		singleContainerData[4]=requeue; 
		return singleContainerData;
	}
	/**
	 * 查询BE网卡的丢包状态
	 * @param netCard 网卡名称
	 * @return float[3]={droped,overLimit,requeue}
	 */
	public long[] getBeNetPackageInfo(){
		long sentByte=0;
		long sentPkt=0;
		long droped=0;
		long overLimit=0;
		long requeue=0;
		long[] singleContainerData=new long[5];
		List<ContainerBean> list=Repository.BE_TASK.getContainerList();
		for(ContainerBean container:list){
			singleContainerData=netMonitor.getDropOverLimit(container.getVirtualNetCard());
			sentByte+=singleContainerData[0];
			sentPkt+=singleContainerData[1];
			droped+=singleContainerData[2];
			overLimit+=singleContainerData[3];
			requeue+=singleContainerData[4];
		}  
		singleContainerData[0]=sentByte; 
		singleContainerData[1]=sentPkt;
		singleContainerData[2]=droped; 
		singleContainerData[3]=overLimit;
		singleContainerData[4]=requeue;

		return singleContainerData;
	}
	/**
	 * 查询当前系统的当前功耗
	 * @return 功耗  单位:焦耳
	 */
	public long getSysCurPowerInfo(){
		return freqMonitor.getCpuCurPower();
	}
	/**
	 * 查询LC服务的窗口的实时平均延迟情况
	 * @return 窗口数据的平均延迟
	 * @throws RemoteException 
	 */
	public float getLcAvgLatency(int serviceType) throws RemoteException{
		return Repository.loader.getLcAvgLatency(serviceType);
	}
	/**
	 * 查询LC服务的实时延迟情况
	 * @return 最新的实时延迟
	 * @throws RemoteException 
	 */
	public float getLcCurLatency(int serviceType) throws RemoteException{
		//return random.nextInt(100)+50;
		return Repository.loader.getLcCurLatency99th(serviceType);
	}
	/**
	 * 获取内存利用率达到限制的容器名称数组
	 * @param taskType 任务类型 "LC"或者"BE"
	 * @return 容器数组
	 */
	public List<ContainerBean> getMemShortContainerList(String taskType){
		return memMonitor.getMemShortContainerList(taskType);
	}
	/**
	 * 获得内存资源最富裕的那个容器
	 * @param taskType 任务类型 "LC"或者"BE"
	 * @return containerBean
	 */
	public ContainerBean getMemRichestContainer(String taskType){  
		ContainerBean bean=new ContainerBean();
		bean=memMonitor.getMemRichestContainer(taskType);
		return bean;
	}
	/**
	 * 查询未正常停止的Be 进程数量
	 * @return
	 */
	public int getUnstopedBeProcessCount(){
		int result=0;
		if(Repository.BE_EXEC_STATUS==Enum_BE_Tolerance.DISABLE_PAUSE){//如果容器暂停了,返回0
			result=0;
		}else{
			for(ContainerBean container:Repository.BE_TASK.getContainerList()){
				result+=Integer.parseInt(dockerService.execCommand(container.getContainerName(),Repository.beTaskCountInstanceCommand));//查询每个容器的结果产出
			}
		}
		return result; 
	}
	/**
	 * 只用于utiliThread类进行统计
	 * 计算BE任务所有的所有实例数量
	 * 如果BE处于暂停状态,返回0 否则返回当前
	 */
	public int getBeInstanceCount(){
		int result=0;
		int beCoreNum=0;
		if(Repository.BE_EXEC_STATUS==Enum_BE_Tolerance.DISABLE_PAUSE){//如果容器暂停了,返回0
			result=0;
		}else{
			beCoreNum=Repository.BE_TASK.getBindLogicCoreNum();
			result=getUnstopedBeProcessCount(); 
			result=result>beCoreNum?beCoreNum:result;//如果进程数比BE的逻辑线程数大,则代表BE实例不能全部执行,返回逻辑线程数作为BE的实际执行数
		}
		return result;
	}
	/**
	 * 计算BE任务所有的所有实例数量
	 * 如果BE处于暂停状态,返回0 否则返回当前
	 */
	public int getBeProductCount(){
		int result=0;
		if(Repository.BE_EXEC_STATUS==Enum_BE_Tolerance.DISABLE_PAUSE){//如果容器暂停了,返回0
			result=0;
		}else{
			for(ContainerBean container:Repository.BE_TASK.getContainerList()){
				result+=Integer.parseInt(dockerService.execCommand(container.getContainerName(),Repository.beTaskCountResultCommand));//查询每个容器的结果产出
			}
		}

		return result;
	}
	/**
	 * 资源控制开启函数
	 * @param controlLevel 资源控制等级,1 2 3 4依次开启cpu mem net freq资源控制,后面的级别涵盖前面的级别
	 */
	public static void start(int controlLevel,int execTime,int BeMaxCount){
		if(controlLevel>=0){
			Thread timeThread=new Thread(new TimerThread(execTime));
			timeThread.start();

			Repository.getInstance().updateContainerResourceUsage(1000);//1秒钟一次 刷新一次容器的资源使用情况

			Thread utilityController=new Thread(new UtilityController(BeMaxCount));
			utilityController.start();
			Thread latencyRecordController=new Thread(new LatencyRecordController());
			latencyRecordController.start();
		}
		if(controlLevel>=1){
			Thread topController=new Thread(new TopController());
			topController.start();
		}
		if(controlLevel>=2){
			Thread cpuControlThread=new Thread(new CpuController());
			cpuControlThread.start();
		}
		if(controlLevel>=3){
			Thread freqControlThread=new Thread(new FreqController());
			freqControlThread.start();
		}
		if(controlLevel>=4){
			Thread memControlThread=new Thread(new MemController());
			memControlThread.start();
		}
		if(controlLevel>=5){
			Thread netControlThread=new Thread(new NetController());
			netControlThread.start();
		} 
		
		while(Repository.SYSTEM_RUN_FLAG==true){
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}


	/**
	 * 状态枚举
	 * @author yanan
	 *
	 */
	public enum Enum_State{
		GROW_COPY,//增加BE副本
		GROW_LLC, //增加BE llc大小
		GROW_CORE //增加BE 逻辑核心
	}
	public enum Enum_BE_Tolerance{
		DISABLE_KILL,//kill掉所有BE,并释放BE的所有资源
		DISABLE_PAUSE, //暂停BE的执行,不再新增BE任务,保留BE的资源
		ENABLE_DIS_GROW, //允许BE的运行,不再新增BE任务或增加资源
		ENABLE_ALLOW_GROW //允许BE的运行,可以新增BE任务或者增加资源
	}
}
