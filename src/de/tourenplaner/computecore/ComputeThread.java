/**
 * $$\\ToureNPlaner\\$$
 */
package de.tourenplaner.computecore;

import de.tourenplaner.algorithms.Algorithm;
import de.tourenplaner.algorithms.ComputeException;
import de.tourenplaner.config.ConfigManager;
import de.tourenplaner.database.DatabaseManager;
import de.tourenplaner.database.DatabasePool;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import java.beans.PropertyVetoException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A ComputeThread computes the results of ComputeRequests it gets from the
 * queue of it's associated ComputeCore using Algorithms known to it's
 * AlgorithmManager
 * 
 * @author Niklas Schnelle, Peter Vollmer, Sascha Meusel
 * 
 */
public class ComputeThread extends Thread {
    
    private static Logger log = Logger.getLogger("tourenplaner");

	private final AlgorithmManager alm;
	private final BlockingQueue<ComputeRequest> reqQueue;
	private boolean isPrivate;
	private ThreadMXBean threadMXBean;
	private DatabaseManager dbm;
    private double costPerMillisecond;

	/**
	 * Constructs a new ComputeThread using the given AlgorithmManager and
	 * RequestQueue
	 * 
	 * @param am AlgorithmManager
	 * @param rq BlockingQueue&lt;ComputeRequest&gt;
	 */
	public ComputeThread(AlgorithmManager am, BlockingQueue<ComputeRequest> rq) {
		alm = am;
		reqQueue = rq;
		ConfigManager cm = ConfigManager.getInstance();
		isPrivate = cm.getEntryBool("private", false);
		threadMXBean = ManagementFactory.getThreadMXBean();
		if (isPrivate) {
            try {
                int costPerTimeUnit = cm.getEntryInt("costpertimeunit", 10);
                if (costPerTimeUnit < 0) {
                    costPerTimeUnit = 0;
                }
                // size of time unit is in milliseconds
                int timeUnitSize = cm.getEntryInt("timeunitsize", 1000);
                if (timeUnitSize <= 0) {
                    timeUnitSize = 1;
                }
                costPerMillisecond = ((double) costPerTimeUnit) / ((double) timeUnitSize);

                this.dbm = DatabasePool.getDatabaseManager(
                        cm.getEntryString("dburi","jdbc:mysql://localhost:3306/tourenplaner?autoReconnect=true"),
                        cm.getEntryString("dbuser","tnpuser"),
                        cm.getEntryString("dbpw","toureNPlaner"),
                        cm.getEntryString("dbdriverclass","com.mysql.jdbc.Driver"));
            } catch (PropertyVetoException e) {
                log.log(Level.SEVERE, "Couldn't establish de.tourenplaner.database connection", e);
                System.exit(1);
            }

        }
		this.setDaemon(true);
	}

	/**
	 * Runs computations taking new ones from the Queue
	 * 
	 */
	@Override
	public void run() {
		ComputeRequest work;
		Algorithm alg;

        checkThreadMXBeanSupport();

        while (!Thread.interrupted()) {
			long cpuTime;
			int requestID = -1;
			ByteArrayOutputStream baOutputStream;
			
			try {
				work = reqQueue.take();

                // workIsPrivate should only be true if ComputeThread is private,
                // else there is no de.tourenplaner.database connection available
                boolean workIsPrivate = false;
                if (this.isPrivate) {
                    workIsPrivate = work.isPrivate();
                }

                // check needed if availability of de.tourenplaner.algorithms changes
				alg = alm.getAlgByURLSuffix(work.getAlgorithmURLSuffix());
				if (alg != null) {
					try {

                        // if de.tourenplaner.server is not private, work.getRequestID() should return -1
                        requestID = work.getRequestID();

                        cpuTime = startTimeMeasurement(workIsPrivate);

                        alg.compute(work);

                        cpuTime = finishTimeMeasurement(cpuTime, workIsPrivate);

                        log.fine("Algorithm "+ work.getAlgorithmURLSuffix() + " successfully computed.");
						
						try {
							baOutputStream = work.getResponder().writeComputeResult(work,
									HttpResponseStatus.OK);

						} catch (IOException e) {
                            log.log(Level.WARNING, "There was an IOException", e);
                            // TODO define error and write to protocol specification
                            String errorMessage = work.getResponder().writeAndReturnErrorMessage("ECOMPUTE",
                                    "The de.tourenplaner.server could not send and not store the compute result", "",
                                    HttpResponseStatus.INTERNAL_SERVER_ERROR);

                            writeIntoDatabase(requestID, errorMessage, "IOException", workIsPrivate);

                            throw e;
						}
						if (workIsPrivate) {

							try {
                                int cost = (int) Math.ceil(((double) cpuTime) * costPerMillisecond);

                                // baOutputStream is not null because else writeComputeResult
                                // would throw an IOException
								dbm.updateRequestWithComputeResult(requestID,
                                        baOutputStream.toByteArray(), cost, cpuTime);
							} catch (SQLException sqlE) {
								log.log(Level.WARNING, "Could not log ComputeResult into DB", sqlE);
							} 
						}
					} catch (ComputeException e) {
						log.log(Level.WARNING, "There was a ComputeException", e);
                        //TODO maybe wrong response status (is algorithm responsible for exception or bad user parameter input?)
                        String errorMessage = work.getResponder().writeAndReturnErrorMessage("ECOMPUTE",
								e.getMessage(), "", HttpResponseStatus.INTERNAL_SERVER_ERROR);

                        writeIntoDatabase(requestID, errorMessage, "ComputeException", workIsPrivate);
					}
				} else {
					log.warning("Unsupported algorithm " + work.getAlgorithmURLSuffix() + " requested");
					String errorMessage = work.getResponder().writeAndReturnErrorMessage("EUNKNOWNALG",
							"An unknown algorithm was requested", null, HttpResponseStatus.NOT_FOUND);

                    writeIntoDatabase(requestID, errorMessage, "EUNKNOWNALG", workIsPrivate);
				}

			} catch (InterruptedException e) {
				log.warning("ComputeThread interrupted");
				return;
			} catch (Exception e) {
				log.log(Level.WARNING ,"An exception occurred", e);
			}
		}
	}

    private void writeIntoDatabase(int requestID, String errorMessage, String errorName, boolean workIsPrivate) {
        if (workIsPrivate) {
            try {
                // TODO change failDescription to user friendly message?
                // TODO maybe a better method should be used to convert a string to a byte array
                dbm.updateRequestAsFailed(requestID, errorMessage.getBytes());
            } catch (SQLException sqlE) {
                log.log(Level.WARNING, "Could not log " + errorName + " into DB ", sqlE);
            }
        }
    }

    private long startTimeMeasurement(boolean workIsPrivate) {
        long cpuTime = 0;
        if (workIsPrivate) {
            cpuTime = threadMXBean != null ? threadMXBean.getCurrentThreadCpuTime() : System.nanoTime();
        }
        return cpuTime;
    }

    private long finishTimeMeasurement(long cpuStartTime, boolean workIsPrivate) {
        if (workIsPrivate) {
            cpuStartTime =
                    threadMXBean != null ?
                    threadMXBean.getCurrentThreadCpuTime() - cpuStartTime :
                    System.nanoTime() - cpuStartTime;
            // convert to milliseconds
            cpuStartTime = Math.round(cpuStartTime / 1000000);
            if (cpuStartTime == 0) {
                cpuStartTime = 1;
            }
        }
        return cpuStartTime;
    }

    /**
     * Checks if thread CPU time measurements works correctly. If not, this method will set this.threadMXBean = null
     */
    private void checkThreadMXBeanSupport() {
        if (threadMXBean != null) {
            try {
                if (threadMXBean.getCurrentThreadCpuTime() < 0) {
                    threadMXBean = null;
                }
            } catch (UnsupportedOperationException e) {
                threadMXBean = null;
            }
        }
    }
}