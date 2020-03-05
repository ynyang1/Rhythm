package scs.pojo;
/**
 * slo打破的时候lc的配置
 * @author yanan
 *
 */
public class SLOViolateConfigBean {
	private int beCopy;
	private int lcLlcWayNum;
	private int lcCpuCore;  
	private int beCpuCore;  
	private float memBwUsagePerc;
	private float lcAvgCpuUsagePerc;
	private float sysAvgCpuUsagePerc;
	private int beNetBwLimit;
	private int lcQps;
	
	public int getBeCopy() {
		return beCopy;
	}
	public void setBeCopy(int beCopy) {
		this.beCopy = beCopy;
	}
	public int getLcLlcWayNum() {
		return lcLlcWayNum;
	}
	public void setLcLlcWayNum(int lcLlcWayNum) {
		this.lcLlcWayNum = lcLlcWayNum;
	}
	public int getLcCpuCore() {
		return lcCpuCore;
	}
	public void setLcCpuCore(int lcCpuCore) {
		this.lcCpuCore = lcCpuCore;
	}
	public float getMemBwUsagePerc() {
		return memBwUsagePerc;
	}
	public void setMemBwUsagePerc(float memBwUsagePerc) {
		this.memBwUsagePerc = memBwUsagePerc;
	}
	public float getLcAvgCpuUsagePerc() {
		return lcAvgCpuUsagePerc;
	}
	public void setLcAvgCpuUsagePerc(float lcAvgCpuUsagePerc) {
		this.lcAvgCpuUsagePerc = lcAvgCpuUsagePerc;
	}
	public int getBeNetBwLimit() {
		return beNetBwLimit;
	}
	public void setBeNetBwLimit(int beNetBwLimit) {
		this.beNetBwLimit = beNetBwLimit;
	}
	public int getLcQps() {
		return lcQps;
	}
	public void setLcQps(int lcQps) {
		this.lcQps = lcQps;
	}
	public int getBeCpuCore() {
		return beCpuCore;
	}
	public void setBeCpuCore(int beCpuCore) {
		this.beCpuCore = beCpuCore;
	}
	public float getSysAvgCpuUsagePerc() {
		return sysAvgCpuUsagePerc;
	}
	public void setSysAvgCpuUsagePerc(float sysAvgCpuUsagePerc) {
		this.sysAvgCpuUsagePerc = sysAvgCpuUsagePerc;
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SLOViolateConfigBean [beCopy=");
		builder.append(beCopy);
		builder.append(", lcLlcWayNum=");
		builder.append(lcLlcWayNum);
		builder.append(", lcCpuCore=");
		builder.append(lcCpuCore);
		builder.append(", beCpuCore=");
		builder.append(beCpuCore);
		builder.append(", memBwUsagePerc=");
		builder.append(memBwUsagePerc);
		builder.append(", lcAvgCpuUsagePerc=");
		builder.append(lcAvgCpuUsagePerc);
		builder.append(", sysAvgCpuUsagePerc=");
		builder.append(sysAvgCpuUsagePerc);
		builder.append(", beNetBwLimit=");
		builder.append(beNetBwLimit);
		builder.append(", lcQps=");
		builder.append(lcQps);
		builder.append("]");
		return builder.toString();
	}
	
}
