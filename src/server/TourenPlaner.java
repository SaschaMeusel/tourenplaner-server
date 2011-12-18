/**
 * $$\\ToureNPlaner\\$$
 */
package server;

import graphrep.GraphRep;
import graphrep.GraphRepDumpReader;
import graphrep.GraphRepTextReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

import algorithms.AlgorithmFactory;
import algorithms.GraphAlgorithmFactory;
import algorithms.NNSearchFactory;
import algorithms.ShortestPathCHFactory;
import algorithms.ShortestPathFactory;

import computecore.AlgorithmManagerFactory;
import computecore.AlgorithmRegistry;
import computecore.ComputeCore;
import computecore.SharingAMFactory;

import config.ConfigManager;

public class TourenPlaner {
	/**
	 * @param graphName
	 *            Original file name of the graph
	 * @return The filename of the dumped graph
	 */
	private static String dumpName(String graphName) {
		return graphName + ".dat";
	}

	private static Map<String, Object> getServerInfo(AlgorithmRegistry reg) {
		Map<String, Object> info = new HashMap<String, Object>(4);
		info.put("version", new Float(0.1));
		info.put(
				"servertype",
				ConfigManager.getInstance().getEntryBool("private", false) ? "private"
						: "public");
		info.put("sslport",
				ConfigManager.getInstance().getEntryInt("sslport", 8081));
		// Enumerate Algorithms
		Collection<AlgorithmFactory> algs = reg.getAlgorithms();
		Map<String, Object> algInfo;
		List<Map<String, Object>> algList = new ArrayList<Map<String, Object>>();
		for (AlgorithmFactory alg : algs) {
			algInfo = new HashMap<String, Object>(5);
			algInfo.put("version", alg.getVersion());
			algInfo.put("name", alg.getAlgName());
			algInfo.put("urlsuffix", alg.getURLSuffix());
			algInfo.put("hidden", alg.hidden());
			if (alg instanceof GraphAlgorithmFactory) {
				algInfo.put("pointconstraints",
						((GraphAlgorithmFactory) alg).getPointConstraints());
				algInfo.put("constraints",
						((GraphAlgorithmFactory) alg).getConstraints());
			}
			algList.add(algInfo);
		}
		info.put("algorithms", algList);

		return info;
	}

	/**
	 * This is the main class of ToureNPlaner. It passes CLI parameters to the
	 * handler and creates the httpserver
	 */
	public static void main(String[] args) {
		// Create globally shared ObjectMapper so we reuse it's data structures
		ObjectMapper mapper = new ObjectMapper();

		/**
		 * inits config manager if config file is provided; also prints usage
		 * information if necessary
		 */
		CLIHandler handler = new CLIHandler(mapper, args);

		// if serialize, then ignore whether to read text or dump and read
		// text graph since it wouldn't make sense to read a serialized
		// graph just to serialize it. Also do this before anything server
		// related gets actually started
		if (handler.serializegraph()) {
			System.out.println("Dumping Graph");
			GraphRep graph = null;
			String graphfilename = ConfigManager.getInstance().getEntryString(
					"graphfilepath",
					System.getProperty("user.home") + "/germany-ch.txt");
			try {
				graph = new GraphRepTextReader()
						.createGraphRep(new FileInputStream(graphfilename));
				utils.GraphSerializer.serialize(new FileOutputStream(
						dumpName(graphfilename)), graph);
				System.exit(0);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// uses defaults if it was not initialized by the CLIHandler
		ConfigManager cm = ConfigManager.getInstance();
		// Load the Graph
		GraphRep graph = null;
		String graphfilename = cm.getEntryString("graphfilepath",
				System.getProperty("user.home") + "/germany-ch.txt");
		try {
			if (handler.loadTextGraph()) {
				graph = new GraphRepTextReader()
						.createGraphRep(new FileInputStream(graphfilename));
			} else {
				try {
					graph = new GraphRepDumpReader()
							.createGraphRep(new FileInputStream(graphfilename
									+ ".dat"));
				} catch (InvalidClassException e) {
					// TODO: unify default options?
					String textGraphFilename = ConfigManager.getInstance()
							.getEntryString(
									"graphfilepath",
									System.getProperty("user.home")
											+ "/germany-ch.txt");
					System.err
							.println("Dumped Graph version does not match the required version: "
									+ e.getMessage());
					System.out
							.println("Falling back to text reading from file: "
									+ textGraphFilename
									+ " (path provided by config file) ...");
					graph = new GraphRepTextReader()
							.createGraphRep(new FileInputStream(
									textGraphFilename));
					System.out
							.println("Graph successfully read. Now replacing old dumped graph ...");

					if (new File(dumpName(graphfilename)).delete()) {
						utils.GraphSerializer.serialize(new FileOutputStream(
								dumpName(graphfilename)), graph);
						System.out
								.println("... success. Now running server ...");
					} else {
						System.out
								.println("... failed. Now running server ...");
					}
				} catch (IOException e) {
					String textGraphFilename = ConfigManager.getInstance()
							.getEntryString(
									"graphfilepath",
									System.getProperty("user.home")
											+ "/germany-ch.txt");
					System.err.println("Dumped Graph does not exist: "
							+ e.getMessage());
					System.out
							.println("Falling back to text reading from file: "
									+ textGraphFilename
									+ " (path provided by config file) ...");
					graph = new GraphRepTextReader()
							.createGraphRep(new FileInputStream(
									textGraphFilename));
					System.out
							.println("Graph successfully read. Now dumping graph ...");
					utils.GraphSerializer.serialize(new FileOutputStream(
							dumpName(graphfilename)), graph);
					System.out.println("... success. Now running server ...");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (graph == null) {
			System.out
					.println("Graph reading FAILED! Only non graph related services will be available!");
		}

		// Register Algorithms
		AlgorithmRegistry reg = new AlgorithmRegistry();
		reg.registerAlgorithm(new ShortestPathFactory(graph));
		reg.registerAlgorithm(new ShortestPathCHFactory(graph));
		reg.registerAlgorithm(new NNSearchFactory(graph));

		// Create our ComputeCore that manages all ComputeThreads
		ComputeCore comCore = new ComputeCore(reg,
				cm.getEntryInt("threads", 16),
				cm.getEntryInt("queuelength", 32));
		try {
			AlgorithmManagerFactory amFac = new SharingAMFactory(graph);
			comCore.start(amFac);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.err
					.println("\nCould not establish database connections for ComputeThreads.");
			e.printStackTrace();
			// TODO switch from private to public, but ConfigManager doesn't
			// support this yet
		}

		// Create ServerInfo object
		Map<String, Object> serverInfo = getServerInfo(reg);

		new HttpServer(mapper, cm, reg, serverInfo, comCore);
	}
}
