package scs.util.resource.mem;

import java.util.ArrayList;
import java.util.List;  
import scs.pojo.ContainerBean;
import scs.util.repository.Repository; 

public class MemMonitor { 
	private static MemMonitor memMonitor=null;
	private MemMonitor(){ 
	}
	public synchronized static MemMonitor getInstance() {
		if (memMonitor == null) {  
			memMonitor = new MemMonitor();
		}  
		return memMonitor;
	}
	/**
	 * 获取内存利用率达到限制的容器名称数组
	 * @param taskType 任务类型 "LC"或者"BE"
	 * @return 容器数组
	 */
	public List<ContainerBean> getMemShortContainerList(String taskType){
		List<ContainerBean> result=new ArrayList<ContainerBean>();
		List<ContainerBean> containerList=null;
		if(taskType!=null&&taskType.equals("LC")){
			containerList=Repository.LC_TASK.getContainerList();
		}else{
			containerList=Repository.BE_TASK.getContainerList();
		}
		for(ContainerBean bean:containerList){
			if(bean.getMemUsagePerc()>Repository.memUsageLimitRate*100){
				result.add(bean);
			}
		}
		return result;
	}
	/**
	 * 获得内存资源最富裕的那个容器
	 * @param taskType 任务类型 "LC"或者"BE"
	 * @return containerBean
	 */
	public ContainerBean getMemRichestContainer(String taskType){ 
		List<ContainerBean> containerList=null;
		if(taskType!=null&&taskType.equals("LC")){
			containerList=Repository.LC_TASK.getContainerList(); 
		}else{
			containerList=Repository.BE_TASK.getContainerList(); 
		}
		float maxDonateResource=0.0f;
		float tempValue=0.0f; 
		ContainerBean richestContainer=new ContainerBean();
		for(ContainerBean bean:containerList){
			if(bean.getMemoryLimit()<Repository.minMemoryKeepSizePerContainer){
				continue;
			}
			tempValue=bean.getMemoryLimit()*(80-bean.getMemUsagePerc())/100.0f;//留出20%的资源供自己使用 
			if(tempValue>maxDonateResource){ 
				maxDonateResource=tempValue;
				richestContainer=bean;
			}
		}
		return richestContainer;
	}

	public static void main(String[] args){
	}
}
