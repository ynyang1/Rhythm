package scs.pojo;

import java.util.ArrayList; 
import java.util.LinkedList;
import java.util.List;

import scs.util.repository.Repository; 

public class TaskBean {
	private int copy; //副本数量,对BE任务而言
	private String taskType; //LC或者BE 
	private int totalMemLimit; //内存大小限制
	private float avgCpuUsageRate; //cpu利用率 
	private int netBandWidthLimit; //网络流量速度 单位:kbit 
	private int llcWayNums; //末级缓存大小 单位:way
	private boolean isPause;//是否处于暂停状态

	private List<ContainerBean> containerList=new ArrayList<ContainerBean>(); //包含的容器列表
	private LinkedList<TwoTuple<Integer,Integer>> bindLogicCoreList=new LinkedList<TwoTuple<Integer,Integer>>(); //已经绑定的逻辑核心

	public int getCopy() {
		return copy;
	}
	public void setCopy(int copy) {
		this.copy = copy;
	}
	public String getTaskType() {
		return taskType;
	} 
	public void setTaskType(String taskType) {
		this.taskType = taskType;
	}
	public int getTotalMemLimit() {
		return totalMemLimit;
	}
	public void setTotalMemLimit(int totalMemLimit) {
		this.totalMemLimit = totalMemLimit;
	}
	public float getAvgCpuUsageRate() {
		return avgCpuUsageRate;
	}
	public void setAvgCpuUsageRate(float avgCpuUsageRate) {
		this.avgCpuUsageRate = avgCpuUsageRate;
	}

	public int getNetBandWidthLimit() {
		return netBandWidthLimit;
	}
	public void setNetBandWidthLimit(int netBandWidthLimit) {
		this.netBandWidthLimit = netBandWidthLimit;
	} 
	public int getLlcWayNums() {
		return llcWayNums;
	}
	public void setLlcWayNums(int llcWayNums) {
		this.llcWayNums = llcWayNums;
	}
	public List<ContainerBean> getContainerList() {
		return containerList;
	}
	public void setContainerList(List<ContainerBean> containerList) {
		this.containerList = containerList;
	}

	public void setBindLogicCoreList(LinkedList<TwoTuple<Integer,Integer>> bindLogicCoreList) {
		this.bindLogicCoreList = bindLogicCoreList;
	}
	public LinkedList<TwoTuple<Integer, Integer>> getBindLogicCoreList() {
		return bindLogicCoreList;
	}
	/**
	 * 返回当前类型任务绑定的逻辑核心字符串
	 * @return 超线程下:"0,40,1,41,2,42..." 非超线程:"0,1,2,3"
	 */
	public String getBindLogicCoreListStr(){
		StringBuilder str=new StringBuilder();  
		if(Repository.threadPerCore==2){
			for(TwoTuple<Integer,Integer> item:bindLogicCoreList){
				str.append(item.first).append(",").append(item.second).append(",");
			}
		}else{
			for(TwoTuple<Integer,Integer> item:bindLogicCoreList){
				str.append(item.first).append(",");
			}
		}
		return str.substring(0,str.length()-1);
	}
	/**
	 * 返回当前类型任务绑定的逻辑核数量
	 * @return 超线程返回bindLogicCoreList数组大小的2倍 ,非超线程返回数组大小
	 */
	public int getBindLogicCoreNum(){ 
		return (bindLogicCoreList.size()<<(Repository.threadPerCore-1));
	}
	/**
	 * 移除最后一个物理核心的所有逻辑线程并返回该核心
	 * @return 移除掉的物理核心
	 */
	public TwoTuple<Integer,Integer> removeLastLogicCore(){
		return bindLogicCoreList.removeLast(); 
	}
	/**
	 * 添加一个物理核心的所有逻辑线程
	 * @return 操作状态
	 */
	public boolean addLogicCore(TwoTuple<Integer,Integer> logicCore){
		return bindLogicCoreList.add(logicCore); 
	}
	/**
	 * 更新容器的资源使用情况
	 * @param containerName 容器名称
	 * @param usageBean 资源使用封装实体
	 */
	public void updateContainerResource(String containerName,ContainerResourceUsageBean usageBean){
		for(ContainerBean bean:containerList){
			if(bean.getContainerName().equals(containerName)){
				bean.setMemUsagePerc(usageBean.getMemUsagePerc());
				bean.setCpuUsagePerc(usageBean.getCpuUsagePerc());
				break;
			}
		}
	}
	public boolean isPause() {
		return isPause;
	}
	public void setPause(boolean isPause) {
		this.isPause = isPause;
	}

}
