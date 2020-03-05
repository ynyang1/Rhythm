package scs.controller.mem; 

import java.util.List;
import scs.controller.ControlDriver;
import scs.controller.ControlDriver.Enum_BE_Tolerance;
import scs.controller.mem.MemController;
import scs.pojo.ContainerBean;
import scs.util.repository.Repository; 
/**
 * 内存控制器代码
 * 调整LC和BE的内存使用
 * @author yanan
 * @date 2019-02-02
 */
public class MemController extends Thread{

	private ControlDriver controlDriver=ControlDriver.getInstance();
	private final int MEM_SIZE_PER_ALLOC=128;//每次分配的内存大小 128MB
	private final int SLEEP_TIME=5;//5s
	
	@Override
	public void run(){
		int SLEEP_TIME_MS=SLEEP_TIME*1000;
		List<ContainerBean> memShortList=null;
		int availMemory=0;
		int availMemPerContainer=0;
		while(Repository.SYSTEM_RUN_FLAG){ 
			if(Repository.BE_EXEC_STATUS==Enum_BE_Tolerance.DISABLE_PAUSE){
				try { 
					Thread.sleep(SLEEP_TIME_MS);//BE暂停的状态 不进行内存控制(因为暂停状态 docker无法操作)
					////System.out.println("BE处于暂停状态 无法操作 等待...");
				} catch (InterruptedException e) { 
					e.printStackTrace();
				}
				continue; 
			}else{
				//程序往下执行
			}
			memShortList=controlDriver.getMemShortContainerList("LC");//查询LC中mem资源紧张的节点
			if(memShortList.size()>0){ //说明有LC容器内存资源紧张
				availMemory=Repository.systemMemorySize-Repository.LC_TASK.getTotalMemLimit()-Repository.BE_TASK.getTotalMemLimit();//计算空闲内存
				if(availMemory>0&&(availMemory/memShortList.size()>0)){//如果有空闲的内存而且可以分给每个短缺的容器内存大于0
					//System.out.println("有空闲内存 ="+availMemory);
					if(availMemory>(memShortList.size()<<7)){ //如果可以给每个资源短缺的节点分配128MB内存
						for(ContainerBean bean:memShortList){ 
							bean.setMemoryLimit(bean.getMemoryLimit()+MEM_SIZE_PER_ALLOC);//在这里修改,因为是引用,所以仓库里的数组的实体类也会更改
							//System.out.println(bean.getContainerName()+" 扩展后的内存="+bean.getMemoryLimit());
						}
						Repository.LC_TASK.setTotalMemLimit(Repository.LC_TASK.getTotalMemLimit()+((memShortList.size()<<7)));//更新LC的资源总量
					}else{//如果分配不了128MB,则平均分配可用的资源
						availMemPerContainer=availMemory/memShortList.size();//计算每个容器可以多分得的内存资源
						//如果空闲的资源可以分配给每个急缺的容器,但是每个容器分不到128MB
						//System.out.println("分不到128的内存 每个节点可以分得 ="+availMemPerContainer);
						for(ContainerBean bean:memShortList){
							bean.setMemoryLimit(bean.getMemoryLimit()+availMemPerContainer);//在这里修改,因为是引用,所以仓库里的数组的实体类也会更改
							//System.out.println(bean.getContainerName()+" 扩展后的内存="+bean.getMemoryLimit());
						}
						Repository.LC_TASK.setTotalMemLimit(Repository.LC_TASK.getTotalMemLimit()+((memShortList.size()*availMemPerContainer)));//更新LC的资源总量
					} 
					controlDriver.changeMemoryLimit(memShortList);	////System.out.println("刷新内存 生效");
				}else{//如果没有空闲资源,那么就温和得抢占部分BE的资源
					ContainerBean richestContainer=controlDriver.getMemRichestContainer("BE");//获得资源最富裕的BE容器
					//System.out.println("return ="+richestContainer.hashCode()+" "+richestContainer.toString());
					if(richestContainer.getMemoryLimit()==0){//如果没有富裕的BE容器可以让出资源
						try { 
							//System.out.println("没有资源最富裕的BE节点 等待");
							Thread.sleep(10000);//等待10秒
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						continue;
					}else{
						availMemory=(int)(richestContainer.getMemoryLimit()*(80-richestContainer.getMemUsagePerc())/100.0f);//留出20%的资源给提供者
						availMemPerContainer=availMemory/memShortList.size();//计算每个容器可以多分得的内存资源
						if(availMemPerContainer>0&&(richestContainer.getMemoryLimit()-(memShortList.size()*availMemPerContainer))>=Repository.minMemoryKeepSizePerContainer){ //availMemPerContainer有可能正好为0或者最富有的分了之后不足minKeepSzie
							for(ContainerBean bean:memShortList){
								bean.setMemoryLimit(bean.getMemoryLimit()+availMemPerContainer);//在这里修改,因为是引用,所以仓库里的数组的实体类也会更改
								//System.out.println(bean.getContainerName()+" 扩展后的内存="+bean.getMemoryLimit());
							}
							richestContainer.setMemoryLimit(richestContainer.getMemoryLimit()-(memShortList.size()*availMemPerContainer));
							//System.out.println(richestContainer.getContainerName()+" 打劫后的内存="+richestContainer.getMemoryLimit());
							Repository.LC_TASK.setTotalMemLimit(Repository.LC_TASK.getTotalMemLimit()+((memShortList.size()*availMemPerContainer)));//更新LC的资源总量
							Repository.BE_TASK.setTotalMemLimit(Repository.BE_TASK.getTotalMemLimit()-((memShortList.size()*availMemPerContainer)));//更新BE的资源总量
							controlDriver.changeMemoryLimit(memShortList);
							controlDriver.changeMemoryLimit(richestContainer);
						}else{
							//最富有的BE要被强制打土豪
							availMemory=(int)(richestContainer.getMemoryLimit()-Repository.minMemoryKeepSizePerContainer);//直接把BE容器 除最小keepSize外的内存 分走
							availMemPerContainer=availMemory/memShortList.size();//计算每个容器可以多分得的内存资源
							if(availMemPerContainer>0){ //有可能为0
								for(ContainerBean bean:memShortList){
									bean.setMemoryLimit(bean.getMemoryLimit()+availMemPerContainer);//在这里修改,因为是引用,所以仓库里的数组的实体类也会更改
									//System.out.println(bean.getContainerName()+" 扩展后的内存="+bean.getMemoryLimit());
								}
								Repository.LC_TASK.setTotalMemLimit(Repository.LC_TASK.getTotalMemLimit()+((memShortList.size()*availMemPerContainer)));//更新LC的资源总量
								Repository.BE_TASK.setTotalMemLimit(Repository.BE_TASK.getTotalMemLimit()-((memShortList.size()*availMemPerContainer)));//更新BE的资源总量
							
							/*if(richestContainer.getMemoryLimit()>((memShortList.size()<<7))){
								for(ContainerBean bean:memShortList){
									bean.setMemoryLimit(bean.getMemoryLimit()+MEM_SIZE_PER_ALLOC);//在这里修改,因为是引用,所以仓库里的数组的实体类也会更改
									////System.out.println(bean.getContainerName()+" 扩展后的内存="+bean.getMemoryLimit());
								}
								richestContainer.setMemoryLimit(richestContainer.getMemoryLimit()-(memShortList.size()<<7));
								////System.out.println(richestContainer.getContainerName()+" 打劫后的内存="+richestContainer.getMemoryLimit());
								Repository.LC_TASK.setTotalMemLimit(Repository.LC_TASK.getTotalMemLimit()+((memShortList.size()<<7)));//更新LC的资源总量
								Repository.BE_TASK.setTotalMemLimit(Repository.BE_TASK.getTotalMemLimit()-((memShortList.size()<<7)));//更新BE的资源总量*/
								controlDriver.changeMemoryLimit(memShortList);
								controlDriver.changeMemoryLimit(richestContainer);
							}else{
								//暴力也抢占不了了 内存实在不够了 真的不分了
								////System.out.println("内存实在不够了 真的不分了");
							}
						}
					}
				}
			}else{//否则当LC的资源都很充裕时,可以考虑增加BE的资源
				memShortList=controlDriver.getMemShortContainerList("BE");//查询BE中mem资源紧张的节点
				if(memShortList.size()>0){ //说明有BE容器内存资源达到上限
					availMemory=Repository.systemMemorySize-Repository.LC_TASK.getTotalMemLimit()-Repository.BE_TASK.getTotalMemLimit();//计算空闲内存
					if(availMemory>0&&(availMemory/memShortList.size()>0)){//如果有空闲的内存
						if(availMemory>(memShortList.size()<<7)){ //如果可以给每个资源短缺的分配128MB内存
							for(ContainerBean bean:memShortList){ 
								bean.setMemoryLimit(bean.getMemoryLimit()+MEM_SIZE_PER_ALLOC);//在这里修改,因为是引用,所以仓库里的数组的实体类也会更改
							}
							Repository.BE_TASK.setTotalMemLimit(Repository.BE_TASK.getTotalMemLimit()+((memShortList.size()<<7)));//更新BE的资源总量
						}else{//如果分配不了128MB,则平均分配可用的资源
							availMemPerContainer=availMemory/memShortList.size();//计算每个容器可以多分得的内存资源
							for(ContainerBean bean:memShortList){
								bean.setMemoryLimit(bean.getMemoryLimit()+availMemPerContainer);//在这里修改,因为是引用,所以仓库里的数组的实体类也会更改
							}
							Repository.BE_TASK.setTotalMemLimit(Repository.BE_TASK.getTotalMemLimit()+((memShortList.size()*availMemPerContainer)));//更新BE的资源总量
						}
						controlDriver.changeMemoryLimit(memShortList); 
					}else{//如果空闲资源没有,那么就温和得抢占部分LC的资源,以提高资源利用率
						ContainerBean richestContainer=controlDriver.getMemRichestContainer("LC");//获得资源最富裕的LC容器
						if(richestContainer.getMemoryLimit()==0){//如果没有富裕的LC容器可以让出资源
							try {
								////System.out.println("没有富裕的LC容器可以让出资源 wait");
								Thread.sleep(10000);//等待10秒
							} catch (InterruptedException e) { 
								e.printStackTrace();
							}
						}else{
							availMemory=(int)(richestContainer.getMemoryLimit()*(80-richestContainer.getMemUsagePerc())/100.0f);//留出20%的资源给提供者
							availMemPerContainer=availMemory/memShortList.size();//计算每个容器可以多分得的内存资源
							if(availMemPerContainer>0&&(richestContainer.getMemoryLimit()-(memShortList.size()*availMemPerContainer))>=Repository.minMemoryKeepSizePerContainer){
								for(ContainerBean bean:memShortList){
									bean.setMemoryLimit(bean.getMemoryLimit()+availMemPerContainer);//在这里修改,因为是引用,所以仓库里的数组的实体类也会更改
								}
								richestContainer.setMemoryLimit(richestContainer.getMemoryLimit()-(memShortList.size()*availMemPerContainer));
								Repository.BE_TASK.setTotalMemLimit(Repository.BE_TASK.getTotalMemLimit()+((memShortList.size()*availMemPerContainer)));//更新BE的资源总量
								Repository.LC_TASK.setTotalMemLimit(Repository.LC_TASK.getTotalMemLimit()-((memShortList.size()*availMemPerContainer)));//更新LC的资源总量
								controlDriver.changeMemoryLimit(memShortList);
								controlDriver.changeMemoryLimit(richestContainer);
							}else{ 
								//不可以暴力抢占LC的资源
								////System.out.println("不可以暴力抢占LC的资源");
							}
						}

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
}

