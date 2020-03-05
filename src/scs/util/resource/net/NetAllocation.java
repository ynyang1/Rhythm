package scs.util.resource.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.List; 
import scs.pojo.ContainerBean;
import scs.util.repository.Repository; 
/**
 * 网络流量分配类
 * @author yanan
 *
 */
public class NetAllocation {
	private static NetAllocation cpuAlloc=null;
	private NetAllocation(){}
	public synchronized static NetAllocation getInstance() {
		if (cpuAlloc == null) {  
			cpuAlloc = new NetAllocation();
		}  
		return cpuAlloc;
	} 
	/**
	 * 重置网络流量限制,取消所有BE节点网卡网络流控
	 */
	public void resetNetTxFlow(){  
		try {
			List<ContainerBean> list=Repository.BE_TASK.getContainerList();
			for(int i=0;i<list.size();i++){
				String[] cmd = {"/bin/sh","-c","tc qdisc del dev "+list.get(i).getVirtualNetCard()+" root"}; 
				System.out.println("tc qdisc del dev "+list.get(i).getVirtualNetCard()+" root");
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
			}

			list=Repository.LC_TASK.getContainerList();
			for(int i=0;i<list.size();i++){
				String[] cmd = {"/bin/sh","-c","tc qdisc del dev "+list.get(i).getVirtualNetCard()+" root"}; 
				System.out.println("tc qdisc del dev "+list.get(i).getVirtualNetCard()+" root");
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
			}
		}catch (IOException e) { 
			e.printStackTrace();
		}
	} 
	/**
	 * 设置网络流控 
	 * @param taskType "all" 都刷新;"BE" 只刷新BE "LC" 只刷新LC
	 */
	public void refreshNetTxFlow(String taskType){

		try { 
			int latency=50;
			int burst=15;
			if(taskType!=null&&taskType.equals("BE")){
				List<ContainerBean> list=Repository.BE_TASK.getContainerList();
				for(int i=0;i<list.size();i++){
					String[] cmd = {"/bin/sh","-c","tc qdisc replace dev "+list.get(i).getVirtualNetCard()+" root tbf rate "+Repository.BE_TASK.getNetBandWidthLimit()+"kbit latency "+latency+"ms burst "+burst+"kb"}; 
					//System.out.println("tc qdisc replace dev "+list.get(i).getVirtualNetCard()+" root tbf rate "+Repository.BE_TASK.getNetBandWidthLimit()+"kbit latency "+latency+"ms burst "+burst+"kb");
					String err;
					Process process = Runtime.getRuntime().exec(cmd); 
					BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
					InputStreamReader isr = new InputStreamReader(process.getInputStream());
					LineNumberReader input = new LineNumberReader(isr); 
					while (((err = br.readLine()) != null||(input.readLine()) != null)) {
						if(err==null){
						//	System.out.println(line);
						}else{
							System.out.println(err);
						}
					}
				} 
			}else if(taskType!=null&&taskType.equals("LC")){
				List<ContainerBean> list=Repository.LC_TASK.getContainerList();
				for(int i=0;i<list.size();i++){
					String[] cmd = {"/bin/sh","-c","tc qdisc replace dev "+list.get(i).getVirtualNetCard()+" root tbf rate "+Repository.LC_TASK.getNetBandWidthLimit()+"kbit latency "+latency+"ms burst "+burst+"kb"}; 
					//System.out.println("tc qdisc replace dev "+list.get(i).getVirtualNetCard()+" root tbf rate "+Repository.LC_TASK.getNetBandWidthLimit()+"kbit latency "+latency+"ms burst "+burst+"kb");
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
				}
			}else{
				List<ContainerBean> list=Repository.BE_TASK.getContainerList();
				for(int i=0;i<list.size();i++){
					String[] cmd = {"/bin/sh","-c","tc qdisc replace dev "+list.get(i).getVirtualNetCard()+" root tbf rate "+Repository.BE_TASK.getNetBandWidthLimit()+"kbit latency "+latency+"ms burst "+burst+"kb"}; 
					//System.out.println("tc qdisc replace dev "+list.get(i).getVirtualNetCard()+" root tbf rate "+Repository.BE_TASK.getNetBandWidthLimit()+"kbit latency "+latency+"ms burst "+burst+"kb");
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
				}
				list=Repository.LC_TASK.getContainerList();
				for(int i=0;i<list.size();i++){
					String[] cmd2 = {"/bin/sh","-c","tc qdisc replace dev "+list.get(i).getVirtualNetCard()+" root tbf rate "+Repository.LC_TASK.getNetBandWidthLimit()+"kbit latency "+latency+"ms burst "+burst+"kb"}; 
					//System.out.println("tc qdisc replace dev "+list.get(i).getVirtualNetCard()+" root tbf rate "+Repository.LC_TASK.getNetBandWidthLimit()+"kbit latency "+latency+"ms burst "+burst+"kb");
					String err;
					Process process = Runtime.getRuntime().exec(cmd2); 
					BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
					InputStreamReader isr = new InputStreamReader(process.getInputStream());
					LineNumberReader input = new LineNumberReader(isr); 
					while (((err = br.readLine()) != null||(input.readLine()) != null)) {
						if(err==null){
							//	System.out.println(line);
						}else{
							System.out.println(err);
						}
					}
				}
			}

		}catch (IOException e) { 
			e.printStackTrace();
		}
	}
}
