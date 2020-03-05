package scs.util.rmi;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import scs.util.experiment.AgentRMIService;
import scs.util.experiment.ExecDriver;
import scs.util.repository.Repository; 
/**
 * rmi远程调用接口类 server端实现该类 客户端不用实现,只负责调用该接口
 * 该接口被Agent的节点上的rmi程序调用
 * @author yanan
 *
 */
public class AgentInterfaceImpl extends UnicastRemoteObject implements AgentInterface{  
 
	private static final long serialVersionUID = 1L;
	
	public AgentInterfaceImpl() throws RemoteException {
		super();
		// TODO Auto-generated constructor stub
	}

	@Override
	public float setSlackLimit(float slackLimit) throws RemoteException {
		// TODO Auto-generated method stub
		return Repository.slackLimit=slackLimit;
		
	}

	@Override
	public void evaluation(String[] args) throws RemoteException {
		// TODO Auto-generated method stub
		try {
			new Main().getHebeExperResult(args);
		} catch (NumberFormatException | InterruptedException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void exit() throws RemoteException {
		// TODO Auto-generated method stub
		AgentRMIService.getInstance().shutDown();
	}

	@Override
	public void autoStart(String[] args) throws RemoteException {
		try { 
			new Main().getHebeExperResult(args);
		} catch (NumberFormatException | InterruptedException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
