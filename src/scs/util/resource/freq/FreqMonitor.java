package scs.util.resource.freq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;

public class FreqMonitor {
	private int CPU_MIN_FREQ=0;//最小频率 单位:HZ
	private int CPU_MAX_FREQ=0;//最大频率 单位:HZ
	private int CPU_MAX_POWER=0;//最大功率
	private ArrayList<Integer> AVAILABLE_CPU_FREQ=new ArrayList<Integer>();
	
	private static FreqMonitor cpuMonitor=null;
	private FreqMonitor(){
		 getCpuFreqRange(); //构造对象的时候 查询cpuFreq的上限和下限制
		 getCpuPowerRange();//查询 cpuPower的最大值
	}
	public synchronized static FreqMonitor getInstance() {
		if (cpuMonitor == null) {
			cpuMonitor = new FreqMonitor();
		}  
		return cpuMonitor;
	}
	public int getCpuMaxFreq(){
		return CPU_MAX_FREQ;
	}
	public int getCpuMinFreq(){
		return CPU_MIN_FREQ;
	}
	public int getCpuMaxPower(){
		return CPU_MAX_POWER;
	}
	/**
	 * 查询单个cpu的频率
	 * @param cpuId cpu的逻辑编号
	 * @return 频率 单位:HZ
	 */
	public int getCpuFreq(int cpuId){
		int freq=0;
		String[] cmd = {"/bin/sh","-c","cat /sys/devices/system/cpu/cpu"+cpuId+"/cpufreq/scaling_cur_freq"};
		try {
			String line = null,err;
			Process process = Runtime.getRuntime().exec(cmd); 
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			InputStreamReader isr = new InputStreamReader(process.getInputStream());
			LineNumberReader input = new LineNumberReader(isr); 
			while (((err = br.readLine()) != null||(line = input.readLine()) != null)) {
				if(err==null){ 
					freq=Integer.parseInt(line); 
				}else{
					System.out.println(err);
				}
			}   
		}catch (IOException e) { 
			e.printStackTrace();
		} 
	 	return freq;
	}
	/**
	 * 获取cpu的最大最小频率,可用的频率
	 */
	private void getCpuFreqRange(){  
		String[] cmd1 = {"/bin/sh","-c","cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq"};
		try {
			String line = null,err;
			Process process = Runtime.getRuntime().exec(cmd1); 
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			InputStreamReader isr = new InputStreamReader(process.getInputStream());
			LineNumberReader input = new LineNumberReader(isr);
			while (((err = br.readLine()) != null||(line = input.readLine()) != null)) {
				if(err==null){ 
					CPU_MIN_FREQ=Integer.parseInt(line);
				}else{
					System.out.println(err);
				}
			}   
		}catch (IOException e) {
			e.printStackTrace();
		} 
		String[] cmd2 = {"/bin/sh","-c","cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq"};
		try {
			String line = null,err;
			Process process = Runtime.getRuntime().exec(cmd2); 
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			InputStreamReader isr = new InputStreamReader(process.getInputStream());
			LineNumberReader input = new LineNumberReader(isr); 
			while (((err = br.readLine()) != null||(line = input.readLine()) != null)) {
				if(err==null){ 
					CPU_MAX_FREQ=Integer.parseInt(line); 
				}else{
					System.out.println(err);
				}
			}   
		}catch (IOException e) { 
			e.printStackTrace();
		}  
		String[] cmd3 = {"/bin/sh","-c","cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies"};
		try { 
			String line = null,err;
			Process process = Runtime.getRuntime().exec(cmd3); 
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			InputStreamReader isr = new InputStreamReader(process.getInputStream());
			LineNumberReader input = new LineNumberReader(isr); 
			while (((err = br.readLine()) != null||(line = input.readLine()) != null)) {
				if(err==null){ 
					String[] split=line.trim().split("\\s+");
					for(String item:split){
						AVAILABLE_CPU_FREQ.add(Integer.parseInt(item));
					}
				}else{
					System.out.println(err);
				}
			}   
		}catch (IOException e) { 
			e.printStackTrace();
		}  
	}
	/**
	 * 查询单个cpu的当前功耗 
	 * @return 热量 单位:焦耳
	 */
	public long getCpuCurPower(){
		long power=0;
		String[] cmd = {"/bin/sh","-c","cat /sys/class/powercap/intel-rapl/intel-rapl:0/energy_uj"};
		try {
			String line = null,err;
			Process process = Runtime.getRuntime().exec(cmd); 
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			InputStreamReader isr = new InputStreamReader(process.getInputStream());
			LineNumberReader input = new LineNumberReader(isr); 
			while (((err = br.readLine()) != null||(line = input.readLine()) != null)) {
				if(err==null){ 
					power=Long.parseLong(line); 
				}else{
					System.out.println(err);
				}
			}   
		}catch (IOException e) { 
			e.printStackTrace();
		}
	 	return power;
	}
	private void getCpuPowerRange(){  
		String[] cmd = {"/bin/sh","-c","cat /sys/class/powercap/intel-rapl/intel-rapl:0/constraint_0_max_power_uw"};
		try {
			String line = null,err;
			Process process = Runtime.getRuntime().exec(cmd); 
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			InputStreamReader isr = new InputStreamReader(process.getInputStream());
			LineNumberReader input = new LineNumberReader(isr);
			while (((err = br.readLine()) != null||(line = input.readLine()) != null)) {
				if(err==null){ 
					CPU_MAX_POWER=Integer.parseInt(line);
				}else{
					System.out.println(err);
				}
			}   
		}catch (IOException e) {
			e.printStackTrace();
		}  
	}
 
}
