package scs.pojo;
 
import java.util.List;
/**
 * Cpu信息实体类
 * @author yanan
 *
 */
public class CpuBean {
	private List<List<TwoTuple<Integer,Integer>>> logicCoreList; //逻辑核编号数组,socket为第一维度,TwoTuple存储同一个物理核的所有逻辑线程
	private int socketNum; //socket数量
	private int logicCoreNumsPerSocket; //每个socket的逻辑核数量
	
	public List<List<TwoTuple<Integer, Integer>>> getLogicCoreList() {
		return logicCoreList;
	}

	public void setLogicCoreList(List<List<TwoTuple<Integer, Integer>>> logicCoreList) {
		this.logicCoreList = logicCoreList;
	}

	public int getSocketNum() {
		return socketNum;
	}

	public void setSocketNum(int socketNum) {
		this.socketNum = socketNum;
	}
	
	public int getLogicCoreNumsPerSocket() {
		return logicCoreNumsPerSocket;
	}

	public void setLogicCoreNumsPerSocket(int logicCoreNumsPerSocket) {
		this.logicCoreNumsPerSocket = logicCoreNumsPerSocket;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CpuBean [logicCoreList=");
		builder.append(logicCoreList);
		builder.append(", socketNum=");
		builder.append(socketNum);
		builder.append(", logicCoreNumsPerSocket=");
		builder.append(logicCoreNumsPerSocket);
		builder.append("]");
		return builder.toString();
	}




 
 
 
 
}
