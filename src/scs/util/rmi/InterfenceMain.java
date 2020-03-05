package scs.util.rmi;

import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;


public class InterfenceMain {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if(args.length!=2){
			System.out.println("[maxQps beName]");
		}else{
			try {
				new InterfenceMain().getInterferTableWebServer(Integer.parseInt(args[0]),args[1]);
			} catch (NumberFormatException | InterruptedException | IOException e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * 干扰表征结果记录
	 * 也可以用来求最大QPS
	 * @param num
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void getInterferTableWebServer(int maxQps,String beName) throws InterruptedException, IOException{
		LoadInterface loader=null;
		try {
			loader=(LoadInterface) Naming.lookup("rmi://192.168.1.128:22222/load");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
		if(loader!=null){
			System.out.println("rmi://192.168.1.128:22222/load 建立连接 success");
		}else{
			System.out.println("rmi://192.168.1.128:22222/load 建立连接 fail");
		}



		FileWriter writer=new FileWriter("/home/tank/sdcloud/result/inferfenceTable/webserver_infertable_"+beName+"_curlatency.txt");
		FileWriter writer0=new FileWriter("/home/tank/sdcloud/result/inferfenceTable/webserver_infertable_"+beName+"+_avglatency.txt");
		
		int curQps=0;

		for(double i=0.1;i<1;i=i+0.1){
			curQps=(int)(maxQps*i);
			//for(curQps=1500;curQps<maxQps;curQps=curQps+50){
			System.out.println("load="+i*100+"%");
			loader.setIntensity(curQps,1);
			Thread.sleep(60000);//睡眠30秒钟等待生效
			for(int k=0;k<120;k++){
				writer.write(loader.getLcCurLatency99th(1)+"\t"); //每隔一秒查询实时延迟 99th size=30
				writer0.write(loader.getLcAvgLatency(1)+"\t"); //每隔一秒查询实时延迟 99th size=30

				writer.flush();
				writer0.flush();
				Thread.sleep(1000);//睡眠1秒钟
			}
			writer.write("\n");
			writer.flush();
			writer0.write("\n");
			writer0.flush();
		}
		writer.write("\n");
		writer.flush();
		writer0.write("\n");
		writer0.flush();
		writer.close();
		writer0.close();
		System.out.println("程序结束");
	}
}
