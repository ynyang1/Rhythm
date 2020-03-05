package scs.util.resource.mem;

import java.util.List; 
import scs.pojo.ContainerBean; 
import scs.util.resource.DockerService; 

public class MemAllocation {
	private DockerService dockerService=DockerService.getInstance();

	private static MemAllocation memAlloc=null;
	private MemAllocation(){}
	public synchronized static MemAllocation getInstance() {
		if (memAlloc == null) {  
			memAlloc = new MemAllocation();
		}  
		return memAlloc;
	} 

	/**
	 * 修改容器的内存大小限制
	 * @param containerList 要修改的容器列表 
	 */
	public void setContainerMemLimit(List<ContainerBean> containerList){
		dockerService.refreshContainerMemResource(containerList);
	}

	/**
	 * 修改容器的内存大小限制
	 * @param container 要修改的容器 
	 */
	public void setContainerMemLimit(ContainerBean container){
		dockerService.refreshContainerMemResource(container);
	}
}
