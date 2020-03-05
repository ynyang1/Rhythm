package scs.util.resource.freq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

public class FreqAllocation {

	private static FreqAllocation freqAlloc=null;
	private FreqAllocation(){}
	public synchronized static FreqAllocation getInstance() {
		if (freqAlloc == null) {  
			freqAlloc = new FreqAllocation();
		}  
		return freqAlloc;
	} 

	/**
	 * 设置多个cpu的频率
	 * @param cpuIdStr cpu的逻辑编号字符串 支持,-格式
	 * @return 频率 单位:KHZ
	 */
	public void setCpuFreq(String cpuIdStr,int cpuFreq){
		String[] cmd = {"/bin/sh","-c","cpufreq-set -c "+cpuIdStr+" -f "+cpuFreq+"KHZ"};
		try {
			String err;
			Process process = Runtime.getRuntime().exec(cmd);
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			InputStreamReader isr = new InputStreamReader(process.getInputStream());
			LineNumberReader input = new LineNumberReader(isr);
			while (((err = br.readLine()) != null||(input.readLine()) != null)) {
				if(err==null){
				}else{
					System.out.println(err);
				}
			}
		}catch (IOException e) {
			e.printStackTrace();
		} 
	}
	/**
	 * 查询单个cpu的频率
	 * @param cpuIdStr cpu的逻辑编号字符串 支持,- all
	 * @return 频率 单位:HZ
	 */
	public void setCpuGovernors(String cpuIdStr,String governor){
		String[] cmd = {"/bin/sh","-c","cpufreq-set -c "+cpuIdStr+" -g "+governor+""};
		try {
			String err;
			Process process = Runtime.getRuntime().exec(cmd);
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			InputStreamReader isr = new InputStreamReader(process.getInputStream());
			LineNumberReader input = new LineNumberReader(isr); 
			while (((err = br.readLine()) != null||(input.readLine()) != null)) {
				if(err==null){
					//
				}else{
					System.out.println(err);
				}
			}   
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
}
