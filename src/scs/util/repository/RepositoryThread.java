package scs.util.repository; 

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import scs.pojo.ContainerResourceUsageBean;
import scs.util.resource.DockerService; 

/**
 * 仓库伴随线程类
 * 负责实时查询容器的资源使用情况,并更新到对应的仓库存储数组中
 * @author yanan
 *
 */
public class RepositoryThread extends Thread{

	private DockerService dockerService;
	private Repository repository;
	private int interval;
	public RepositoryThread(int interval){ 
		this.dockerService=DockerService.getInstance();
		this.repository=Repository.getInstance();
		this.interval=interval;
	}

	@Override
	public void run(){
		Map<String,ContainerResourceUsageBean> tempMap=null;
		Set<String> keySet=new HashSet<String>();
		String type="";
		String lcContainerNameStr=Repository.LC_ContainerNameStr.replaceAll(","," ");
		String beContainerNameStr=Repository.BE_ContainerNameStr.replaceAll(","," ");
		String containerNameStr=lcContainerNameStr+" "+beContainerNameStr;
		while(Repository.SYSTEM_RUN_FLAG){
			/**
			 * 实时监控LC和BE的CPU和内存使用率
			 */
			tempMap=dockerService.getContainerResourceUsage(containerNameStr);
			if(tempMap!=null){
				keySet=tempMap.keySet();
				for(String key:keySet){
					type=repository.identityContainerType(key);
					if(type!=null&&type.equals("LC")){
						Repository.LC_TASK.updateContainerResource(key,tempMap.get(key));
					}else if(type!=null&&type.equals("BE")){
						Repository.BE_TASK.updateContainerResource(key,tempMap.get(key));
					}
				} 
			}  
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}


}
