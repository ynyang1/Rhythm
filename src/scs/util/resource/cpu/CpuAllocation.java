package scs.util.resource.cpu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.LinkedList;
import java.util.List;

import scs.pojo.TwoTuple;
import scs.util.repository.Repository;
import scs.util.resource.DockerService;

/**
 * CPU core llc和内存带宽 分配类
 * @author yanan
 *
 */
public class CpuAllocation {
	private DockerService dockerService=DockerService.getInstance();

	private static CpuAllocation cpuAlloc=null;
	private CpuAllocation(){}
	public synchronized static CpuAllocation getInstance() {
		if (cpuAlloc == null) {  
			cpuAlloc = new CpuAllocation();
		}  
		return cpuAlloc;
	} 
	/**
	 * 初始化,重置所有LLC配置策略
	 */
	public void resetLLC(){  
		try {
			String err;
			Process process = Runtime.getRuntime().exec("pqos -R"); 
			//	System.out.println("pqos -R");
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
	 * 设置LLC策略
	 * COS0:未分配核心 COS1:LC核心 COS2:BE核心
	 */
	public int refreshLLC(){
		return cpuAlloc.setLLC(Repository.LC_TASK.getBindLogicCoreListStr(),Repository.LC_TASK.getLlcWayNums(),Repository.BE_TASK.getBindLogicCoreListStr(),Repository.BE_TASK.getLlcWayNums());//初始化分配llcWay,LC 19路,BE 1路
	}
	/**
	 * 移除BE的核心到LC上
	 * @param removeLogicCoreNum 移除的逻辑核数量
	 */
	public void removeBeLogicCores(int removeLogicCoreNum){
		//执行remove操作
		for(int i=0;i<removeLogicCoreNum;i=i+Repository.threadPerCore){
			//System.out.println("BE的逻辑线程数为:"+Repository.BE_TASK.getBindLogicCoreNum() +"现在删除最后一个");
			TwoTuple<Integer,Integer> removedLogicCore=Repository.BE_TASK.removeLastLogicCore();//BE让出核心
			Repository.LC_TASK.addLogicCore(removedLogicCore);//把让出来的核心给LC
		}
		//更新核心绑定
		dockerService.refreshContainerCpuResource("all");
	}
	/**
	 * 移除LC的核心到BE上
	 * @param removeLogicCoreNum 移除的逻辑核数量
	 */
	public void removeLcLogicCores(int removeLogicCoreNum){
		//执行remove操作
		for(int i=0;i<removeLogicCoreNum;i=i+Repository.threadPerCore){
			TwoTuple<Integer,Integer> removedLogicCore=Repository.LC_TASK.removeLastLogicCore();//LC让出核心
			Repository.BE_TASK.addLogicCore(removedLogicCore);//把让出来的核心给BE
		}
		//更新核心绑定
		dockerService.refreshContainerCpuResource("all");
	}
	/**
	 * 返回taskType的容器初始化绑定的核心
	 * LC初始化获得除最后一个核心之外的所有逻辑线程
	 * BE初始化获得最后一个核心的所有逻辑线程
	 * @param taskType LC|BE
	 * @return 超线程：linkedList={<0,16>,<1,17>,<2,18>...}  非超线程：linkedList={<0,0>,<1,1>,<2,2>...}  
	 */
	public LinkedList<TwoTuple<Integer,Integer>> getInitBindLogicCores(String taskType){
		LinkedList<TwoTuple<Integer,Integer>> list=new LinkedList<TwoTuple<Integer,Integer>>();
		if(taskType!=null&&taskType.equals("LC")){ //LC任务分配除最后一个核心之外的所有核
			list.addAll(Repository.CPU_INFO.getLogicCoreList().get(Repository.bindSocketId)); 
			list.removeLast();//移除最后一个物理核心的所有逻辑线程
		}else{ //BE任务分配最后一个物理核心的所有逻辑线程
			List<TwoTuple<Integer,Integer>> tempList=Repository.CPU_INFO.getLogicCoreList().get(Repository.bindSocketId);
			list.add(tempList.get(tempList.size()-1));
		}
		return list;
	}
	/**
	 * 设置LLC策略
	 * COS0:未分配核心 COS1:LC核心 COS2:BE核心
	 */
	private int setLLC(String LCcores,int LCways,String BEcores,int BEways){
		String LC_HexStr=genLLCwayHexStr("pre",Repository.llcWaySize,LCways);
		String BE_HexStr=genLLCwayHexStr("lat",Repository.llcWaySize,BEways);
		try {
			String err; 
			String[] cmd = {"/bin/sh","-c","pqos -e 'llc@"+Repository.bindSocketId+":0=0x"+BE_HexStr+";llc@"+Repository.bindSocketId+":1=0x"+LC_HexStr+";llc@"+Repository.bindSocketId+":2=0x"+BE_HexStr+"'"}; 
			//System.out.println("LCcoresWay= "+LCways+" BEcoresWay="+BEways);
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
			String[] cmd2 = {"/bin/sh","-c","pqos -a 'llc:1="+LCcores+";llc:2="+BEcores+"'"}; 
			//System.out.println("pqos -a 'llc:1="+LCcores+";llc:2="+BEcores+"'");
			process = Runtime.getRuntime().exec(cmd2); 
			br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			isr = new InputStreamReader(process.getInputStream());
			input = new LineNumberReader(isr); 
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

		return 1;
	}
	/**
	 * /**
	 * 生成LLC 二进制位 
	 * @param type pre 前n位
	 * @param llcWaySize llcWays总大小
	 * @param allocWaySize 分配的llcWays大小
	 * @return 二进制字符串
	 */
	private String genLLCwayHexStr(String type,int llcWaySize,int allocWaySize){
		StringBuilder str=new StringBuilder();
		if(type!=null&&type.equals("pre")){ //从高位到低位分
			for(int i=0;i<allocWaySize;i++){
				str.append("1");
			} 
			int zeroWaySize=llcWaySize-allocWaySize;
			for(int i=0;i<zeroWaySize;i++){
				str.append("0");
			}
		}else{//从低位到高位分
			int oneWaySize=llcWaySize-allocWaySize;
			for(int i=0;i<oneWaySize;i++){
				str.append("0");
			} 
			for(int i=0;i<allocWaySize;i++){
				str.append("1");
			}
		}  
		String hexStr=Integer.toHexString(Integer.parseInt(str.toString(),2));//2进制转16进制

		return hexStr;
	}
	public static void main(String[] args){ 
	}

}
