package scs.util.repository;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import scs.controller.ControlDriver; 
import scs.controller.ControlDriver.Enum_BE_Tolerance;
import scs.pojo.ContainerBean;
import scs.pojo.CpuBean;
import scs.pojo.TaskBean;
import scs.pojo.TwoTuple;
import scs.util.resource.DockerService;
import scs.util.resource.cpu.CpuAllocation;
import scs.util.resource.cpu.CpuMonitor;
import scs.util.resource.freq.FreqAllocation;
import scs.util.resource.freq.FreqMonitor;
import scs.util.resource.net.NetAllocation;
import scs.util.rmi.LoadInterface; 

/**
 * 系统静态仓库类
 * 通过静态变量的形式为系统运行中需要用到的数据提供内存型存储
 * 包括一些系统参数，应用运行数据，控制标志等
 * @author yanan
 *
 */
public class Repository{
	private static DockerService dockerService=DockerService.getInstance();
	private static CpuAllocation cpuAlloc=CpuAllocation.getInstance();
	private static NetAllocation netAlloc=NetAllocation.getInstance();
	private static FreqAllocation freqAlloc=FreqAllocation.getInstance();
	private static FreqMonitor freqMonitor=FreqMonitor.getInstance();
	private static ControlDriver controlDriver=null;
	private static Repository repository=null;
	public static LoadInterface loader=null;
	
	private Repository(){}
	
	public synchronized static Repository getInstance() {
		if (repository == null) {
			repository = new Repository();
		}
		return repository;
	}  


	/**
	 * 控制信号
	 */
	public static boolean SYSTEM_RUN_FLAG=true;//控制系统各个线程的标志 
	public static Enum_BE_Tolerance BE_EXEC_STATUS=Enum_BE_Tolerance.DISABLE_KILL;

	/**
	 * 系统信息
	 */
	public static CpuBean CPU_INFO=new CpuBean();
	public static int systemMemBandWidthSize=51200; //系统内存带宽速度上限 MB/s
	public static int systemMemorySize=32768; //系统内存大小限制  MB
	public static int systemNetBandWidthSize=819200; //系统网络带宽速度上限 kbit
	public static int minMemoryKeepSizePerContainer=512;//每个容器的最低内存保有量 MB
	public static int minNetBandWidthKeepSizePerContainer=128; //每个容器的最低网络带宽保有量 kbit
	public static int llcWaySize=20;
	public static int threadPerCore=2;
	public static int bindSocketId=0;

	public static String loaderRmiUrl;
	public static String addBeCopyCommand;
	public static String killAllBeTaskCommand;
	public static String cleanResultCommand;
	public static String beTaskCountInstanceCommand;
	public static String beTaskCountResultCommand;
	public static String resultFilePath;//数据采集结果的存放目录

	//命令行输入的参数
	public static String nodeType;//leader or follower
	public static String nodeName; 
	public static String beName;
	public static String isProduct;
	public static String isSameThreshold;
	public static float loadLimit;
	public static float slackLimit;
	public static int serviceType;
	//
	/**
	 * 运行阈值
	 */
	public static float memBandWidthLimitRate=0.95f;
	public static float cpuUsageLimitRate=0.95f;
	public static float cpuPowerLimitRate=0.95f;
	public static float memUsageLimitRate=0.9f;
	public static float lcCpuFreqGuaranteed=0.0f;//lc的cpu保证频率 单位:GHZ
	public static int lcLatencyTarget=0; //SLO
	public static int maxRequestIntensity=30; //在线每秒钟请求数最大QPS

	public static int setRequestIntensity=10; //在线每秒钟请求数真实 QPS 理论值,不需要读取文件
	public static int realRequestIntensity=10; //在线每秒钟请求数真实 QPS 实际值 不需要读取文件
	/**
	 * 运行变量
	 */ 
	public static String LC_ContainerNameStr;
	public static String BE_ContainerNameStr;
	private static Map<String,String> containerTypeMap=new HashMap<String,String>();
	public static TaskBean LC_TASK=new TaskBean();
	public static TaskBean BE_TASK=new TaskBean();
	public static List<Integer> simRPSList=new ArrayList<Integer>();
	public static List<TwoTuple<String,Integer>> agentIPortList=new ArrayList<TwoTuple<String,Integer>>();
	private static String agentIPortStr;
	public static int curAgentId=0;
	private static String agentContribValueStr;
	public static List<Float> agentContribValueArray=new ArrayList<Float>();
	/**
	 * 系统运行结果指标采集
	 */
	public static float systemAvgCpuUsagePerc=0;//系统平均CPU使用百分比
	public static float systemAvgMemBwUsageRate=0;//系统平均内存带宽使用百分比
	public static float lcAvgMemBwUsagePerc=0;//lc平均内存带宽使用百分比
	public static float systemTDPUsagePerc=0;//系统平均cpu能耗使用百分比
	public static float lcAvgNetBwUsagePerc=0;//lc平均net带宽使用百分比
	public static float beAvgNetBwUsagePerc=0;//be平均net带宽使用百分比
	public static int sloViolateCounter=0; //打破SLO的次数
	public static int productCount=0;
 
	/**
	 * 静态块
	 */
	static {
		readProperties();// 读取配置文件
		init(); //初始化工作,配置文件 只读取一次
		setupRmiConnection();
	}

	/**
	 * 获取容器的类型
	 * @param containerName 容器名称
	 * @return 容器类型 "LC"或者"BE"
	 */
	public String identityContainerType(String containerName){
		if(containerTypeMap.containsKey(containerName))
			return containerTypeMap.get(containerName);
		else
			return null;
	}
	/**
	 * 读取配置文件的参数
	 */
	private static void readProperties(){
		Properties prop = new Properties();
		//InputStream is = Repository.class.getResourceAsStream("/sys.properties");
		try {
			InputStream is = new FileInputStream(new File("/home/tank/sdcloud/result/sys.properties"));
			prop.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		} 
		Repository.LC_ContainerNameStr=prop.getProperty("LC_ContainerNameList").trim();//读取LC容器名称列表  
		Repository.BE_ContainerNameStr=prop.getProperty("BE_ContainerNameList").trim();//读取BE容器名称列表

		Repository.systemMemBandWidthSize=Integer.parseInt(prop.getProperty("systemMemBandWidthSize").trim());//读取内存带宽速度上限 MB/s
		Repository.systemNetBandWidthSize=Integer.parseInt(prop.getProperty("systemNetBandWidthSize").trim());//读取网络带宽速度上限 Mbit
		Repository.systemMemorySize=Integer.parseInt(prop.getProperty("systemMemorySize").trim());//读取内存大小 MB
		Repository.minMemoryKeepSizePerContainer=Integer.parseInt(prop.getProperty("minMemoryKeepSizePerContainer").trim());//读取每个容器的最低内存保有量 MB
		Repository.minNetBandWidthKeepSizePerContainer=Integer.parseInt(prop.getProperty("minNetBandWidthKeepSizePerContainer").trim());//读取每个容器的最低网络带宽 mbit

		Repository.memBandWidthLimitRate=Float.parseFloat(prop.getProperty("memBandWidthLimitRate").trim());//读取内存带宽limit比例
		Repository.cpuUsageLimitRate=Float.parseFloat(prop.getProperty("cpuUsageLimitRate").trim());//读取cpu使用率limit值
		Repository.cpuPowerLimitRate=Float.parseFloat(prop.getProperty("cpuPowerLimitRate").trim());//读取cpu功耗limit值
		Repository.memUsageLimitRate=Float.parseFloat(prop.getProperty("memUsageLimitRate").trim());//读取内存使用率limit值
		Repository.loadLimit=Float.parseFloat(prop.getProperty("loadLimit").trim());//读取内存使用率limit值
		Repository.slackLimit=Float.parseFloat(prop.getProperty("slackLimit").trim());//读取内存使用率limit值

		Repository.addBeCopyCommand=prop.getProperty("addBeCopyCommand").trim();//增加BE任务副本的命令
		Repository.killAllBeTaskCommand=prop.getProperty("killAllBeTaskCommand").trim();//杀掉所有BE任务的命令
		Repository.cleanResultCommand=prop.getProperty("cleanResultCommand").trim();//清除掉所有BE的产出
		
		Repository.loaderRmiUrl=prop.getProperty("loaderRmiUrl").trim();//系统负载生成器的rmi调用url
		Repository.beTaskCountInstanceCommand=prop.getProperty("beTaskCountInstanceCommand").trim();//查看BE进程数量的命令
		Repository.beTaskCountResultCommand=prop.getProperty("beTaskCountResultCommand").trim();//查看BE进程产出文件的命令

		Repository.threadPerCore=Integer.parseInt(prop.getProperty("threadPerCore").trim());//读取每个物理核心的逻辑线程数量
		Repository.llcWaySize=Integer.parseInt(prop.getProperty("llcWayNum").trim());//读取llc总路数 0xfffff 代表20路
		Repository.bindSocketId=Integer.parseInt(prop.getProperty("bindSocketId").trim());//socket num list 逗号分隔

		Repository.resultFilePath=prop.getProperty("resultFilePath").trim();//读取结果的输出路径
		Repository.lcCpuFreqGuaranteed=Float.parseFloat(prop.getProperty("lcCpuFreqGuaranteed").trim());//读取在线请求的最大qps

		Repository.lcLatencyTarget=Integer.parseInt(prop.getProperty("lcLatencyTarget").trim());//SLA
		Repository.agentIPortStr=prop.getProperty("agentIPortStr").trim();
		Repository.curAgentId=Integer.parseInt(prop.getProperty("curAgentId").trim());//curAgentId
		Repository.agentContribValueStr=prop.getProperty("agentContribValueStr").trim();
		
	}

	/**
	 * 初始化函数
	 */
	public static void init(){
		Repository.CPU_INFO=CpuMonitor.getInstance().getCpuInfo();//读取系统的Cpu各项参数信息(逻辑线程,socket数等)

		/**
		 * 建立LC和BE容器的标识数组
		 */
		String[] containerNames=LC_ContainerNameStr.split(",");
		for(int i=0;i<containerNames.length;i++)
			containerTypeMap.put(containerNames[i],"LC");
		containerNames=BE_ContainerNameStr.split(",");
		for(int i=0;i<containerNames.length;i++)
			containerTypeMap.put(containerNames[i],"BE");
		/**
		 * 初始化并读取LC和BE容器的各项信息,网卡名称,粗粒度资源使用情况等(绑核)
		 */
		Repository.LC_TASK.setTaskType("LC");//LC容器 
		Repository.LC_TASK.setBindLogicCoreList(cpuAlloc.getInitBindLogicCores(LC_TASK.getTaskType()));
		dockerService.refreshContainerCpuResource(LC_TASK.getTaskType());//刷新绑核,使LC cpu分配生效
		Repository.LC_TASK.setContainerList(dockerService.getContainerInfo(LC_ContainerNameStr.split(","),LC_TASK.getTaskType()));

		Repository.BE_TASK.setTaskType("BE");//BE容器
		Repository.BE_TASK.setBindLogicCoreList(cpuAlloc.getInitBindLogicCores(BE_TASK.getTaskType()));
		dockerService.refreshContainerCpuResource(BE_TASK.getTaskType());//刷新绑核,使BE cpu分配生效
		Repository.BE_TASK.setContainerList(dockerService.getContainerInfo(BE_ContainerNameStr.split(","),BE_TASK.getTaskType()));

		controlDriver=ControlDriver.getInstance();//初始化controlDriver的对象
		controlDriver.unPauseBeTask();//首先解除所有BE的暂停
		controlDriver.killAllBeTaskCopy();//再杀掉所有BE任务
		controlDriver.cleanBeTaskProduct();//清除所有BE的产出结果文件
		Repository.productCount=0;
		/**
		 * 初始化LC和BE细粒度资源使用情况等(末级缓存)
		 */
		//cpuAlloc.resetLLC();//重置所有LLC分配策略 谨慎使用！！
		Repository.LC_TASK.setLlcWayNums(llcWaySize-1);
		Repository.BE_TASK.setLlcWayNums(llcWaySize-LC_TASK.getLlcWayNums());
		cpuAlloc.refreshLLC();//刷新LLC分配策略,使分配生效 
		/**
		 * 初始化LC和BE的网络流量速度限制
		 */
//		netAlloc.resetNetTxFlow();//重置BE任务所有流量限制策略
//		Repository.LC_TASK.setNetBandWidthLimit(Repository.systemNetBandWidthSize);
//		Repository.BE_TASK.setNetBandWidthLimit(Repository.minNetBandWidthKeepSizePerContainer);
//		netAlloc.refreshNetTxFlow("all");//刷新net HTB策略,使网络分配生效 
		/**
		 * 初始化LC和BE的内存使用限制
		 */
		//计算LC的内存使用量
		List<ContainerBean> list=LC_TASK.getContainerList();
		int totalMemMBLimit=0;
		for(ContainerBean bean:list){totalMemMBLimit+=bean.getMemoryLimit();}
		Repository.LC_TASK.setTotalMemLimit(totalMemMBLimit);
		System.out.println("LC内存总量="+Repository.LC_TASK.getTotalMemLimit());
		//计算BE的内存使用量
		list=BE_TASK.getContainerList();
		totalMemMBLimit=0;
		for(ContainerBean bean:list){totalMemMBLimit+=bean.getMemoryLimit();}
		Repository.BE_TASK.setTotalMemLimit(totalMemMBLimit); 
		System.out.println("BE内存总量="+Repository.BE_TASK.getTotalMemLimit());
		/**
		 * 初始化频率
		 */
		freqAlloc.setCpuGovernors("all","userspace");//设置cpu核心为用户自定义频率
		freqAlloc.setCpuFreq(Repository.LC_TASK.getBindLogicCoreListStr(),freqMonitor.getCpuMaxFreq());//为LC核心赋予最大化频率
		freqAlloc.setCpuFreq(Repository.BE_TASK.getBindLogicCoreListStr(),freqMonitor.getCpuMinFreq());//为BE核心赋予最小频率

		Repository.sloViolateCounter=0;//初始化
		
		String[] tempStr=agentIPortStr.split("#");
		for(int i=0;i<tempStr.length;i++){
			String[] iPort=tempStr[i].split(":");
			agentIPortList.add(new TwoTuple<String,Integer>(iPort[0],Integer.parseInt(iPort[1])));
		}
		tempStr=agentContribValueStr.split("#");
		for(int i=0;i<tempStr.length;i++){
			agentContribValueArray.add(Float.parseFloat(tempStr[i]));
		}
	} 

	/**
	 * 更新LC和BE各个容器的内存及cpu利用率
	 * 同时更新LC和BE的总
	 * @param interval
	 */
	public void updateContainerResourceUsage(int interval){
		Thread thread=new RepositoryThread(interval);
		thread.start();
	}
	/**
	 * 建立loader与agent直接的rmi连接
	 */
	private static void setupRmiConnection(){
		try {
			Repository.loader=(LoadInterface) Naming.lookup(Repository.loaderRmiUrl);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
		if(loader!=null){
			System.out.println(Repository.loaderRmiUrl +"建立连接 success");
		}else{
			System.out.println(Repository.loaderRmiUrl +"建立连接 fail");
		}
	
	}

}
