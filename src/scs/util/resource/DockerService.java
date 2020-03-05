package scs.util.resource;

import java.io.BufferedReader;
import java.io.IOException; 
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList; 
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;
import scs.pojo.ContainerBean;
import scs.pojo.ContainerResourceUsageBean; 
import scs.util.repository.Repository; 
/**
 * docker调用接口
 * @author yanan
 *
 */
public class DockerService {
	private NumberFormat percentInstance; 

	
	private static DockerService service=null;
	private DockerService(){
		this.percentInstance = NumberFormat.getPercentInstance();
	}
	public synchronized static DockerService getInstance() {
		if (service == null) {  
			service = new DockerService();
		}  
		return service;
	} 
	/**
	 * 获取容器资源信息
	 * 包括cpu mem pid等信息
	 * @param containerName
	 * @return
	 */
	private JSONObject getContainerConfig(String containerName){
		JSONObject obj=null;
		try { 
			String[] cmd = {"/bin/sh","-c","docker inspect --format='{{json .HostConfig}}' "+containerName}; 
			String line = null,err;
			Process process = Runtime.getRuntime().exec(cmd); 
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			InputStreamReader isr = new InputStreamReader(process.getInputStream());
			LineNumberReader input = new LineNumberReader(isr); 
			while (((err = br.readLine()) != null||(line = input.readLine()) != null)) {
				if(err==null){   
					obj=JSONObject.fromObject(line);
				}else{
					System.out.println(err);
				}
			}  
			cmd[2] = "docker inspect --format='{{.State.Pid}}' "+containerName; 
			process = Runtime.getRuntime().exec(cmd); 
			br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			isr = new InputStreamReader(process.getInputStream());
			input = new LineNumberReader(isr); 
			while (((err = br.readLine()) != null||(line = input.readLine()) != null)) {
				if(err==null){   
					obj.put("Pid",line);
				}else{
					System.out.println(err);
				}
			}  
		}catch (IOException e) { 
			e.printStackTrace();
		} 
		return obj; 
	} 
	/**
	 * 获取容器资源使用率
	 * 包括cpu mem利用率 
	 * @return
	 */
	public Map<String,ContainerResourceUsageBean> getContainerResourceUsage(String containerNameStr){
		Map<String,ContainerResourceUsageBean> map = new HashMap<String,ContainerResourceUsageBean>();
		try {  
			String[] cmd = {"/bin/sh","-c","docker stats "+containerNameStr+" --no-stream --format '{{.Name}}:{{.CPUPerc}}:{{.MemPerc}}'"}; 
			String line = null,err;
			Process process = Runtime.getRuntime().exec(cmd); 
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			InputStreamReader isr = new InputStreamReader(process.getInputStream());
			LineNumberReader input = new LineNumberReader(isr); 
			while (((err = br.readLine()) != null||(line = input.readLine()) != null)) {
				if(err==null){   
					ContainerResourceUsageBean usageBean = new ContainerResourceUsageBean(); 
					String[] split = line.split(":");
					usageBean.setCpuUsagePerc(percentInstance.parse(split[1]).floatValue()*100.0f);
					usageBean.setMemUsagePerc(percentInstance.parse(split[2]).floatValue()*100.0f);
					//usageBean.setMemLimit((int)Float.parseFloat(split[3].split("/")[1].trim().replace("GiB",""))*1024);
					map.put(split[0],usageBean);
				}else{
					System.out.println(err);
				}
			}  

		}catch (IOException e) { 
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} 
		return map; 
	} 

	private int updateContainerResource(String containerName,String command){  
		String[] cmd = {"/bin/sh","-c","docker update "+command+" "+containerName}; 
		//System.out.println("docker update "+command+" "+containerName);
		try { 
			String err;
			Process process = Runtime.getRuntime().exec(cmd); 
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			InputStreamReader isr = new InputStreamReader(process.getInputStream());
			LineNumberReader input = new LineNumberReader(isr); 
			while (((err = br.readLine()) != null||(input.readLine()) != null)) {
				if(err==null){
					//System.out.println(line);
				}else{
					System.out.println(err);
				}
			}

		}catch (IOException e) { 
			e.printStackTrace();
			return 0;
		}  
		return 1;
	} 
	/**
	 * 获取容器的各项信息
	 * pid 网卡名称 CPU绑定 内存大小
	 * @param containerNamesList 容器名称数组
	 * @return 封装的实体类数组
	 */
	public List<ContainerBean> getContainerInfo(String[] containerNamesList,String taskType){
		List<ContainerBean> containerList=new ArrayList<ContainerBean>();
		JSONObject jsonObject;
		for(int i=0;i<containerNamesList.length;i++){
			jsonObject = getContainerConfig(containerNamesList[i]);//查询容器资源使用信息,返回json格式
			ContainerBean bean=new ContainerBean();
			bean.setTaskType(taskType);
			bean.setContainerName(containerNamesList[i]);
			bean.setVirtualNetCard("veth1pl"+jsonObject.get("Pid"));
			bean.setLogicCoreList(jsonObject.get("CpusetCpus").toString()); 
			bean.setMemoryLimit((int)(Long.parseLong(jsonObject.get("Memory").toString())>>20));
			containerList.add(bean);
		}
		return containerList;
	}
	/**
	 * 刷新所有容器的Cpu资源设置,使之生效
	 * @param taskType "BE" 只刷新BE; "LC" 只刷新LC; 其它 都刷新
	 * @return 操作状态
	 */
	public int refreshContainerCpuResource(String taskType){
		if(taskType!=null&&taskType.equals("BE")){ 
			String[] containerNameList=Repository.BE_ContainerNameStr.split(",");
			for(String containerName:containerNameList){
				updateContainerResource(containerName,"--cpuset-cpus="+Repository.BE_TASK.getBindLogicCoreListStr());//更新BE的核心绑定
			}
		}else if(taskType!=null&&taskType.equals("LC")){
			String[] containerNameList=Repository.LC_ContainerNameStr.split(",");
			for(String containerName:containerNameList){
				updateContainerResource(containerName,"--cpuset-cpus="+Repository.LC_TASK.getBindLogicCoreListStr());//更新LC的核心绑定
			}
		}else{
			String[] containerNameList=Repository.BE_ContainerNameStr.split(",");
			for(String containerName:containerNameList){
				updateContainerResource(containerName,"--cpuset-cpus="+Repository.BE_TASK.getBindLogicCoreListStr());//更新BE的核心绑定
			}
			containerNameList=Repository.LC_ContainerNameStr.split(",");
			for(String containerName:containerNameList){
				updateContainerResource(containerName,"--cpuset-cpus="+Repository.LC_TASK.getBindLogicCoreListStr());//更新LC的核心绑定
			}
		}


		return 1;
	}
	/**
	 * 刷新所有容器的Mem资源设置,使之生效
	 * @param containerList 容器列表
	 * @return
	 */
	public int refreshContainerMemResource(List<ContainerBean> containerList){
		for(ContainerBean container:containerList){
			updateContainerResource(container.getContainerName(),"-m "+container.getMemoryLimit()+"m");//更新容器的内存限制
		}
		return 1;
	}
	/**
	 * 刷新所有容器的Mem资源设置,使之生效
	 * @param container 容器
	 * @return
	 */
	public int refreshContainerMemResource(ContainerBean container){
		updateContainerResource(container.getContainerName(),"-m "+container.getMemoryLimit()+"m");//更新容器的内存限制
		return 1;
	}
	/**
	 * docker exec执行容器里的命令
	 * @param containerName 容器名称
	 * @param command 执行的命令
	 * @return 无返回值
	 */
	public void execVoidCommand(String containerName,String command){
		String[] cmd = {"/bin/sh","-c","docker exec -d "+containerName+" "+command}; 
		String err;
		try {
			Process process = Runtime.getRuntime().exec(cmd); 
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			InputStreamReader isr = new InputStreamReader(process.getInputStream());
			LineNumberReader input = new LineNumberReader(isr); 
			while (((err = br.readLine()) != null||(input.readLine()) != null)) {
				if(err==null){
					//System.out.println(line);
				}else{
					System.out.println(err);
				}
			}

		}catch (IOException e) { 
			e.printStackTrace();
		}  
	}
	/**
	 * docker exec执行容器里的命令
	 * @param containerName 容器名称
	 * @param command 执行的命令
	 * @return 返回执行的结果字符串
	 */
	public String execCommand(String containerName,String command){
		String result="";
        String[] cmd = {"/bin/sh","-c","docker exec -t "+containerName+" "+command};
        String err = null,line = null;
        try {
                Process process = Runtime.getRuntime().exec(cmd);
                BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                InputStreamReader isr = new InputStreamReader(process.getInputStream());
                LineNumberReader input = new LineNumberReader(isr);
                while (((err = br.readLine()) != null||(line = input.readLine()) != null)) {
                        if(line==null){
                                result=err;
                        }else{
                                result=line;
                        }
                }
        }catch (IOException e) {
                e.printStackTrace();
        }
        return result;
	}
	/**
	 * 暂停容器执行
	 * @param containerName 容器名称
	 * @return 执行状态
	 */
	public int pauseContainer(String containerName){ 
		String[] cmd = {"/bin/sh","-c","docker pause "+containerName}; 
		//System.out.println("docker pause "+containerName);
		String err;
		try { 
			Process process = Runtime.getRuntime().exec(cmd); 
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			InputStreamReader isr = new InputStreamReader(process.getInputStream());
			LineNumberReader input = new LineNumberReader(isr); 
			while (((err = br.readLine()) != null||(input.readLine()) != null)) {
				if(err==null){
					//System.out.println(line);
				}else{
					System.out.println(err);
				}
			}

		}catch (IOException e) { 
			e.printStackTrace();
			return 0;
		}  
		return 1;
	}
	/**
	 * 继续容器的执行
	 * @param containerName 容器名称
	 * @return 执行状态
	 */
	public int unPauseContainer(String containerName){

		String[] cmd = {"/bin/sh","-c","docker unpause "+containerName}; 
		//System.out.println("docker unpause "+containerName);
		String err;
		try { 
			Process process = Runtime.getRuntime().exec(cmd); 
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			InputStreamReader isr = new InputStreamReader(process.getInputStream());
			LineNumberReader input = new LineNumberReader(isr); 
			while (((err = br.readLine()) != null||(input.readLine()) != null)) {
				if(err==null){
					//System.out.println(line);
				}else{
					System.out.println(err);
				}
			}

		}catch (IOException e) { 
			e.printStackTrace();
			return 0;
		} 
		return 1;
	}
//	/**
//	 * 计算任务的进程数量
//	 * @param command 任务的进程名称字符串 查询命令
//	 * @return 正在运行的数量
//	 */
//	public int getTaskInstanceCount(String command){
//		int result=0; 
//		try {   
//			String line = null,err;
//			Process process = Runtime.getRuntime().exec(command); 
//			BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
//			InputStreamReader isr = new InputStreamReader(process.getInputStream());
//			LineNumberReader input = new LineNumberReader(isr); 
//			while (((err = br.readLine()) != null||(line = input.readLine()) != null)) {
//				if(err==null){  
//					result=Integer.parseInt(line.trim());
//				}else{
//					System.out.println(err);
//				}
//			}   
//		}catch (IOException e) { 
//			e.printStackTrace();
//		} 
//		return result; 
//	
//	}
//	/**
//	 * 计算BE任务的产出量
//	 * @param containerName 容器名称
//	 * @param command 任务的进程名称字符串 查询命令
//	 * @return 正在运行的数量
//	 */
//	public int getTaskProduceCount1(String containerName,String command){ 
//		int result=0; 
//		try {  
//			String[] cmd = {"/bin/sh","-c","docker exec -it "+containerName+" "+command}; 
//			String line = null,err;
//			Process process = Runtime.getRuntime().exec(cmd); 
//			BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
//			InputStreamReader isr = new InputStreamReader(process.getInputStream());
//			LineNumberReader input = new LineNumberReader(isr); 
//			while (((err = br.readLine()) != null||(line = input.readLine()) != null)) {
//				if(err==null){   
//					result=Integer.parseInt(line.trim());
//				}else{
//					System.out.println(err);
//				}
//			}   
//		}catch (IOException e) { 
//			e.printStackTrace();
//		} 
//		return result; 
//	
//	}
}
