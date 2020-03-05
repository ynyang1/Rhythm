package scs.controller.freq; 

import scs.controller.ControlDriver; 
import scs.controller.freq.FreqController;
import scs.util.repository.Repository;
import scs.util.resource.freq.FreqMonitor; 

/**
 * 能耗控制器类
 * 控制调节CPU的频率
 * @author yanan
 * @date 2019-02-02
 */
public class FreqController extends Thread{

	private ControlDriver controlDriver=ControlDriver.getInstance();
	private final static int FREQ_CHANGE_STEP=100000;//每次改变的频率步长
	private final int SLEEP_TIME=5;//1000ms

	private long lastSysPower=0;
	private long curSysPower=0;
	private long diffSysPower=0;
	/**
	 * 初始化方法
	 */
	private void init(){
		lastSysPower=controlDriver.getSysCurPowerInfo(); //读取一次
	}
	private void calDiffValue(){
		diffSysPower=(curSysPower-lastSysPower)/SLEEP_TIME;
		lastSysPower=curSysPower;
	}
	@Override
	public void run(){
		int SLEEP_TIME_MS=SLEEP_TIME*1000;
		this.init();
		int cpuPower=FreqMonitor.getInstance().getCpuMaxPower();
		float curPowerRate=0.0f;
		float lcAvgCpuFreq=0.0f;
		while(Repository.SYSTEM_RUN_FLAG){
			try {
				Thread.sleep(SLEEP_TIME_MS);
				curSysPower=controlDriver.getSysCurPowerInfo();
				this.calDiffValue();
				Repository.systemTDPUsagePerc=(float) (diffSysPower*100.0/cpuPower);
			//	System.out.println(diffSysPower*100.0/cpuPower+"% power");

				curPowerRate=diffSysPower*1.0f/cpuPower;
				lcAvgCpuFreq=controlDriver.calAvgCpuFreq(Repository.LC_TASK.getBindLogicCoreList());
				//System.out.println("lc avg freq="+lcAvgCpuFreq);
				if(curPowerRate<=Repository.cpuPowerLimitRate&&lcAvgCpuFreq>=Repository.lcCpuFreqGuaranteed){
					controlDriver.changeBeTaskCpuFreq(FREQ_CHANGE_STEP);
				}else if(curPowerRate>Repository.cpuPowerLimitRate&&lcAvgCpuFreq<Repository.lcCpuFreqGuaranteed){
					controlDriver.changeBeTaskCpuFreq(-FREQ_CHANGE_STEP);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

}
