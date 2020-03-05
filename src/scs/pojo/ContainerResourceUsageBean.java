package scs.pojo;
/**
 * 容器资源使用类
 * cpu利用率
 * mem利用率
 * mem分配容量
 * @author yanan
 *
 */
public class ContainerResourceUsageBean {
	private float cpuUsagePerc;//cpu使用百分比
	private float memUsagePerc;//内存使用百分比
	//private int memLimit;//分配的内存大小,单位 MB
	
	public float getCpuUsagePerc() {
		return cpuUsagePerc;
	}
	public void setCpuUsagePerc(float cpuUsagePerc) {
		this.cpuUsagePerc = cpuUsagePerc;
	}
	public float getMemUsagePerc() {
		return memUsagePerc;
	}
	public void setMemUsagePerc(float memUsagePerc) {
		this.memUsagePerc = memUsagePerc;
	}
//	public int getMemLimit() {
//		return memLimit;
//	}
//	public void setMemLimit(int memLimit) {
//		this.memLimit = memLimit;
//	}
}
