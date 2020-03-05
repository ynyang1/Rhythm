package scs.util.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException; 
/**
 * rmi远程调用接口类 server端实现该类 客户端不用实现,只负责调用该接口
 * 该接口被Agent的节点上的rmi程序调用
 * @author yanan
 *
 */
public interface LoadInterface extends Remote{  
	public float getLcAvgLatency(int serviceType) throws RemoteException; //获取windowsize长度的延迟99分位数的平均值
	public float getLcCurLatency95th(int serviceType) throws RemoteException; //获取实时的延迟95分位数
	public float getLcCurLatency99th(int serviceType) throws RemoteException; //获取实时的延迟99分位数
	public float getLcCurLatency999th(int serviceType) throws RemoteException; //获取实时的延迟99.9分位数
	public int setIntensity(int intensity,int serviceType) throws RemoteException; //设置请求密度 rps
	public int getRealQueryIntensity(int serviceType) throws RemoteException; //查询实时的处理速度 qps
	public int getRealRequestIntensity(int serviceType) throws RemoteException; //查询实时的处理速度 rps
	public float getRealServiceRate(int serviceType) throws RemoteException; //查询实时的服务率 %
	public void execRedisLoader(int flag,int serviceType) throws RemoteException; //调用redis的启动/停止标志
	public void execWrkLoader(int flag,int serviceType) throws RemoteException; //调用wrk的启动/停止标志
	public void execStartHttpLoader(int serviceType) throws RemoteException; //调用Http的启动/停止标志
	public void execStopHttpLoader(int serviceType) throws RemoteException; //调用Http的启动/停止标志
}
