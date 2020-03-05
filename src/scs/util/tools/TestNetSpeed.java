package scs.util.tools;

import scs.controller.ControlDriver;
import scs.util.repository.Repository;

public class TestNetSpeed extends Thread{
	//List<ContainerBean> list=new ArrayList<ContainerBean>();
	private ControlDriver controlDriver=ControlDriver.getInstance();
	private final int SLEEP_TIME=5;//5s
	private static long lastNetInfo[]=new long[5];
	private static long curNetInfo[]=new long[5];
	private static long diffNetInfo[]=new long[5];
	/**
	 * 初始化方法
	 */
	private void init(){
		lastNetInfo=controlDriver.getBeNetPackageInfo(); //读取一次网卡使用信息
	}
	private void calNetSpeed(){
		
		curNetInfo=controlDriver.getBeNetPackageInfo(); //读取一次网卡使用信息
		this.calDiffValue();
		Repository.beAvgNetBwUsagePerc=(diffNetInfo[0]>>7)*100.0f/Repository.systemNetBandWidthSize;
		//System.out.println(diffNetInfo[0]+" byte/s " +(diffNetInfo[0]>>7)+" kbit/s " +(diffNetInfo[0]>>17)+" mbit/s");
	}
	@Override
	public void run(){ 
		init();
		int SLEEP_TIME_MS=SLEEP_TIME*1000;
		try {
			Thread.sleep(SLEEP_TIME_MS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		while(Repository.SYSTEM_RUN_FLAG){
			try {
				Thread.sleep(SLEEP_TIME_MS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			calNetSpeed();
		}
	}
	public TestNetSpeed(){
		//		ContainerBean bean=new ContainerBean();
		//		bean.setContainerName("Redis-server1");
		//		bean.setVirtualNetCard("veth1pl9674");
		//		list.add(bean);
		//		bean=new ContainerBean();
		//		bean.setContainerName("Redis-server2");
		//		bean.setVirtualNetCard("veth1pl9876");
		//		list.add(bean);
		//		bean=new ContainerBean();
		//		bean.setContainerName("Redis-server5");
		//		bean.setVirtualNetCard("veth1pl44374");
		//		list.add(bean);
		//		ContainerBean bean=new ContainerBean();
		//		bean.setContainerName("Redis-server3");
		//		bean.setVirtualNetCard("veth1pl39548");
		//		list.add(bean);
		//		bean=new ContainerBean();
		//		bean.setContainerName("Redis-server4");
		//		bean.setVirtualNetCard("veth1pl39647");
		//		list.add(bean);
		//		bean=new ContainerBean();
		//		bean.setContainerName("Redis-server6");
		//		bean.setVirtualNetCard("veth1pl71582");
		//		list.add(bean);

	}

	private void calDiffValue(){
		for(int i=0;i<curNetInfo.length;i++){ 
			diffNetInfo[i]=(curNetInfo[i]-lastNetInfo[i])/SLEEP_TIME;
			lastNetInfo[i]=curNetInfo[i];
		}
	}
	/**
	 * 查询LC网卡的丢包状态
	 * @param netCard 网卡名称
	 * @return float[3]={droped,overLimit,requeue}
	 */
	/*public long[] getLcNetPackageInfo(){
		long sentByte=0;
		long sentPkt=0;
		long droped=0;
		long overLimit=0;
		long requeue=0;
		long[] singleContainerData=new long[5]; 

		for(ContainerBean container:list){
			singleContainerData=netMonitor.getDropOverLimit(container.getVirtualNetCard());
			sentByte+=singleContainerData[0];
			sentPkt+=singleContainerData[1];
			droped+=singleContainerData[2];
			overLimit+=singleContainerData[3];
			requeue+=singleContainerData[4];
		}
		singleContainerData[0]=sentByte; 
		singleContainerData[1]=sentPkt;
		singleContainerData[2]=droped; 
		singleContainerData[3]=overLimit;
		singleContainerData[4]=requeue;

		return singleContainerData;
	}*/
}

