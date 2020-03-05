package scs.util.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * rmi远程调用接口类 server端实现该类 客户端不用实现,只负责调用该接口
 * 该接口被Agent的节点上的rmi程序调用
 * @author yanan
 *
 */
public interface AgentInterface extends Remote{  
	public float setSlackLimit(float slackLimit) throws RemoteException; 
	public void evaluation(String[] args) throws RemoteException; 
	public void exit() throws RemoteException;
	public void autoStart(String[] args) throws RemoteException; 
}
