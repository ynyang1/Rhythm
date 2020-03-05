package scs.util.experiment;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry; 
import scs.pojo.TwoTuple;
import scs.util.repository.Repository;
import scs.util.rmi.AgentInterface;
import scs.util.rmi.AgentInterfaceImpl;

public class AgentRMIService {
	private boolean[] agentRMIConnFlag=new boolean[Repository.agentIPortList.size()];
	public AgentInterface[] agentInterfaceList=new AgentInterface[Repository.agentIPortList.size()];
	private TwoTuple<String,Integer> localIPort=Repository.agentIPortList.get(Repository.curAgentId);

	private static AgentRMIService service=null;
	private AgentRMIService(){}
	public synchronized static AgentRMIService getInstance() {
		if (service == null) {
			service = new AgentRMIService();
		}
		return service;
	}  
	/**
	 * 开启本地rmi agent服务并连接所有agent节点
	 */
	public void setUp(){
		this.openLocal(localIPort.first,localIPort.second);
		this.connRemote(0);
	}
	public void shutDown(){
		this.closeLocal(localIPort.first,localIPort.second);
	}
	
	
	/**
	 * autoStart
	 * @param ip
	 * @param port
	 */
	public void openLocalAgent(){
		this.openLocal(localIPort.first,localIPort.second);
	}
	public void connRemoteAgent(){
		this.connRemote(1);
	} 
	private void openLocal(String ip,int port) {
		try {
			System.setProperty("java.rmi.server.hostname",ip);
			LocateRegistry.createRegistry(port);
			AgentInterface agent = new AgentInterfaceImpl();  
			Naming.rebind("rmi://"+ip+":"+port+"/agent", agent);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		System.out.println("agent "+ip+":"+port+" rmi 服务端开启 ");
	}
	private void closeLocal(String ip,int port){
		try {
			Naming.unbind("rmi://"+ip+":"+port+"/agent");
		} catch (RemoteException | MalformedURLException | NotBoundException e) {
			e.printStackTrace();
		}
		System.out.println("agent "+ip+":"+port+" rmi 服务端关闭 ");
		System.exit(0);
	}
	/**
	 * 
	 * @param startAgentId 0 包括自身 1不包括自身 从1开始
	 */
	private void connRemote(int startAgentId){
		for(int i=startAgentId;i<Repository.agentIPortList.size();i++){
			if(agentRMIConnFlag[i]==false){
				while(true){
					try {
						agentInterfaceList[i]=(AgentInterface) Naming.lookup("rmi://"+Repository.agentIPortList.get(i).toColonString()+"/agent");
					} catch (MalformedURLException | RemoteException | NotBoundException e) {
						try {Thread.sleep(2000);} catch (InterruptedException e1) {e1.printStackTrace();}
						continue;
					}
					System.out.println(Repository.agentIPortList.get(i).toColonString()+" connected successfully");
					agentRMIConnFlag[i]=true;
					break;
				}
			}
		}
	}

}
