package scs.util.experiment;

import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import scs.util.repository.Repository;
import scs.util.tools.DateFormats;
import scs.util.tools.RandomString;

public class ThresholdSet {
	private AgentRMIService agentRMIService=null;
	private final float[] MAX={0.2750f,0.1534f,0.4466f};
	private final float MIN=0.0f;
	private float[] curSlackLimitValueArray=new float[Repository.agentIPortList.size()];
	private float[] stepSizeArray=new float[Repository.agentIPortList.size()];

	private static ThresholdSet thresholdSet=null;

	private ThresholdSet(){
		initSlackLimit();
		calStepSize();
		agentRMIService=AgentRMIService.getInstance();
	}
	public synchronized static ThresholdSet getInstance() {
		if (thresholdSet == null) {
			thresholdSet = new ThresholdSet();
		}
		return thresholdSet;
	}  

	public void iterateThreshold() throws RemoteException{
		DateFormats dateFormats=DateFormats.getInstance();
		try {

			if(Repository.curAgentId==0){
				FileWriter writer = new FileWriter("/home/tank/sdcloud/result/microService/slackLimit.txt");
				//"[qpsStr(0/1#2#3#) maxQps nodeName nodeType(leader/fo) isSameThreshold(same/diff) beName isProduct(0/1)" //0-6
				//" level(0-5) execTime(ms) maxBeCount intervalTime(ms) isHttp(0 redis |1 http |2 wrk) serviceType]"
				String[] leaderArgs={"1500","2000","eva","leader","diff","word","1","2","600000","26","1000","1","5"};
				String[] foArgs={"1500","2000","eva","fo","diff","word","1","2","600000","26","1000","1","5"};
				int maxStepSizeAgentIndex=this.getMaxStepSizeAgentIndex();
				while(true){
					for(int i=0;i<curSlackLimitValueArray.length;i++){
						curSlackLimitValueArray[i]-=stepSizeArray[i];
					}
					if(curSlackLimitValueArray[maxStepSizeAgentIndex]>MIN){ //只有当阈值大于MIN才有效
						for(int i=0;i<curSlackLimitValueArray.length;i++){
							agentRMIService.agentInterfaceList[i].setSlackLimit(curSlackLimitValueArray[i]); //rmi给每个节点设置阈值
							if(i!=Repository.curAgentId){//异步模式启动远程节点
								Thread thread=new ThresholdThread(i,foArgs);
								thread.start();
							}
						}
						agentRMIService.agentInterfaceList[Repository.curAgentId].evaluation(leaderArgs);//同步模式启动本地节点
						/*if(Repository.sloViolateCounter>0){
					printResult(0);
				} */


						System.out.println("######################################"+recordResult());
						writer.write(aa.toString());
						writer.flush();
					}else{//阈值已经得到了结果
						break;
					}
				}
				writer.close();
				for(int i=curSlackLimitValueArray.length-1;i>=0;i--){
					agentRMIService.agentInterfaceList[i].exit(); //退出
				}

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void initSlackLimit(){
		for(int i=0;i<curSlackLimitValueArray.length;i++){
			curSlackLimitValueArray[i]=MAX[i];
		}
	}
	private void calStepSize(){
		float[] size={0.0213f,0.0249f,0.0163f};
		float sumContriValue=0.0f;
		for(float item:Repository.agentContribValueArray){
			sumContriValue+=item;
		}
		for(int i=0;i<stepSizeArray.length;i++){
			//stepSizeArray[i]=Repository.agentContribValueArray.get(i)/sumContriValue/20;
			// a=[贡献度1,2,3] a=e^a b=1./a c=b./sum(b) step=c./8
			stepSizeArray[i]=size[i]; 
		}
	}
	private int getMaxStepSizeAgentIndex(){
		int index=0;
		float tempMax=0.0f;
		for(int i=0;i<stepSizeArray.length;i++){
			if(stepSizeArray[i]>tempMax){
				tempMax=stepSizeArray[i];
				index=i;
			}
		}
		return index;
	}
	StringBuilder aa=new StringBuilder();
	private String recordResult(){
		aa.setLength(0);
		for(int i=0;i<curSlackLimitValueArray.length;i++){
			aa.append(curSlackLimitValueArray[i]).append("\t");
			//System.out.print(curSlackLimitValueArray[i]+" ");
		}
		aa.append(Repository.sloViolateCounter).append("\r\n");

		return aa.toString();
	}
}
class ThresholdThread extends Thread{
	private int agentIndex;
	private String[] args;
	/**
	 * 初始化方法
	 */ 	
	public ThresholdThread(int agentIndex,String[] args){
		this.agentIndex=agentIndex;
		this.args=args;
	}

	@Override
	public void run(){ 
		System.out.println(Thread.currentThread().getName()+" evaluation start");
		try {
			AgentRMIService.getInstance().agentInterfaceList[agentIndex].evaluation(args);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		System.out.println(Thread.currentThread().getName()+" evaluation end");
	}
}
