package scs.util.resource.cpu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader; 
import java.util.ArrayList; 
import java.util.List;

import scs.pojo.CpuBean;
import scs.pojo.TwoTuple;
import scs.util.repository.Repository;  

public class CpuMonitor {
	private static CpuMonitor cpuMonitor=null;
	private CpuMonitor(){}
	public synchronized static CpuMonitor getInstance() {
		if (cpuMonitor == null) {  
			cpuMonitor = new CpuMonitor();
		}  
		return cpuMonitor;
	} 

	/**
	 * 获取系统的CPU信息
	 * @return 封装的实体类 
	 */
	public CpuBean getCpuInfo(){
		CpuBean bean=new CpuBean();
		 
		List<List<TwoTuple<Integer,Integer>>> cpuInfoList=new ArrayList<List<TwoTuple<Integer,Integer>>>();

		String[] cmd = {"/bin/sh","-c","pqos -s | grep Co"}; 
		try { 
			Process process = Runtime.getRuntime().exec(cmd); 
			InputStreamReader isr = new InputStreamReader(process.getInputStream());
			LineNumberReader input = new LineNumberReader(isr); 
			String line = input.readLine();
			while (true) { 
				if(line==null){
					break;
				}
				if(line!=null&&line.contains(":")){
					List<TwoTuple<Integer,Integer>> logicCoreList=new ArrayList<TwoTuple<Integer,Integer>>();
					List<Integer> tempCoreList=new ArrayList<Integer>();
					while((line=input.readLine())!=null&&(!line.contains(":"))){
						tempCoreList.add(Integer.parseInt(line.trim().split(",")[0].replace("Core ","")));
					}
					/**
					 * 区分超线程
					 */
					if(Repository.threadPerCore==2){ 
						int middleIndex=tempCoreList.size()>>1;
						for(int i=0;i<middleIndex;i++){
							//	System.out.println(tempCoreList.get(i)+" "+tempCoreList.get(middleIndex+i));
							logicCoreList.add(new TwoTuple<Integer,Integer>(tempCoreList.get(i),tempCoreList.get(middleIndex+i)));
						}
					}else{ 
						int size=tempCoreList.size();
						for(int i=0;i<size;i++){
							logicCoreList.add(new TwoTuple<Integer,Integer>(tempCoreList.get(i),tempCoreList.get(i)));//没有超线程就把first和second都设置为同一个core
						}
					} 
					cpuInfoList.add(logicCoreList); 
				} 
			}
			bean.setLogicCoreList(cpuInfoList);
			bean.setSocketNum(cpuInfoList.size());
			if(bean.getSocketNum()>0){
				bean.setLogicCoreNumsPerSocket(cpuInfoList.get(0).size());
			}else{
				bean.setLogicCoreNumsPerSocket(0);
			}

		}catch (IOException e) { 
			e.printStackTrace();
		}

		return bean;
	}
	/**
	 * 把所有核心的字符串生成出来
	 * @param socketIndex socker索引  -1 生成所有socket的cpus字符串 否则生成对应socket的cpu字符串
	 * @return "0-79" or "" or "0,10,1,1,2,12"
	 */
	public String getAllLogicCoreStr(int socketIndex){
		StringBuilder cpuStr=new StringBuilder();
		if(socketIndex==-1){
			cpuStr.append("0-").append((Repository.CPU_INFO.getLogicCoreNumsPerSocket()*Repository.CPU_INFO.getSocketNum()-1));
		}else{
			if(socketIndex>=0&&socketIndex<Repository.CPU_INFO.getSocketNum()){
				List<TwoTuple<Integer, Integer>> cpusCurSocket=Repository.CPU_INFO.getLogicCoreList().get(socketIndex);
				int size=cpusCurSocket.size()-1;
				for(int i=0;i<size;i++){
					cpuStr.append(cpusCurSocket.get(i).first).append(",").append(cpusCurSocket.get(i).second).append(",");
				}
				cpuStr.append(cpusCurSocket.get(size).first).append(",").append(cpusCurSocket.get(size).second);
			}else{
				//return "";
			}
		}
		return cpuStr.toString();
	}
	/**
	 * 监控指定cores的llc和内存带宽使用
	 * @param cores [0-31] | [1,2,5] 中括号代表分组
	 * @return float[5]={IPC   MISSES    LLC[KB]  MBL[MB/s]  MBR[MB/s]}
	 */
	public float[] getMemBwUsage(String cores) {
		float[] cacheRamBwUsage=new float[5];
		String dataStr="";
		try {
			String line = null,err;
			Process process = Runtime.getRuntime().exec("pqos -t 1 -m all:"+cores);
			//System.out.println("pqos -t 1 -m all:"+cores);
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			InputStreamReader isr = new InputStreamReader(process.getInputStream());
			LineNumberReader input = new LineNumberReader(isr); 
			while (((err = br.readLine()) != null||(line = input.readLine()) != null)) {
				if(err==null){ 
					dataStr=line;
				}else{
					System.out.println(err);
				}
			}  
			String[] datas=dataStr.trim().split("\\s+"); 
			cacheRamBwUsage[0]=Float.parseFloat(datas[1]); 
			//cacheRamBwUsage[1]=Float.parseFloat(datas[2].replace("k",""));
			//cacheRamBwUsage[2]=Float.parseFloat(datas[3]);
			cacheRamBwUsage[3]=Float.parseFloat(datas[4]);
			cacheRamBwUsage[4]=Float.parseFloat(datas[5]);

		}catch (IOException e) { 
			e.printStackTrace();
		} 
		return cacheRamBwUsage;
	}

}
