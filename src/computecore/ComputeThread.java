/**
 * $$\\ToureNPlaner\\$$
 */
package computecore;


import java.util.concurrent.BlockingQueue;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import algorithms.Algorithm;

/**
 * @author Niklas Schnelle, Peter Vollmer
 *
 */
public class ComputeThread extends Thread {
	
	private AlgorithmManager alm;
	private BlockingQueue<ComputeRequest> reqQueue;
	
	public ComputeThread(AlgorithmManager am, BlockingQueue<ComputeRequest>rq){
		alm = am;
		reqQueue = rq;
		this.setDaemon(true);
	}
	

	
	public void run(){
		ComputeRequest work;
		Algorithm alg;
		ComputeResult res;
		
		
		while(!Thread.interrupted()){
			try {
				work = reqQueue.take();
			    alg = alm.getAlgByURLSuffix(work.getAlgorithmShort());
			    if(alg != null){
			    	alg.setRequest(work);
			    	alg.run();
			    	res = alg.getResult();
				    if(res !=  null){
				    	work.getResponder().writeJSON(res, HttpResponseStatus.OK);
				    } else {
				    	System.err.println("Compute Thread couldn't process: "+work);
				    }
			    } else {
			    	System.err.println("Unsupported algorithm "+work.getAlgorithmShort()+" requested");
			    }
			    
			    
			} catch (InterruptedException e) {
				System.err.println("ComputeThread interrupted");
				return;
			} catch (Exception e){
				System.err.println("Exception in ComputeThread: "+e.getMessage());
				e.printStackTrace();
			}
		}
	}
}