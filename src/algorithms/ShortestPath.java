package algorithms;

import graphrep.GraphRep;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import computecore.ComputeRequest;
import computecore.ComputeResult;

public class ShortestPath extends GraphAlgorithm {

	private ComputeRequest req = null;
	private ComputeResult res = null;

	float srclat;
	float srclon;
	float destlat;
	float destlon;

	int srcid;
	int destid;

	public ShortestPath(GraphRep graph) {
		super(graph);
		dist = new int[graph.getNodeCount()];
		prev = new int[graph.getNodeCount()];

	}

	@Override
	public void setRequest(ComputeRequest req) {
		// reset dists
		for (int i = 0; i < dist.length; i++) {
			dist[i] = Integer.MAX_VALUE;
		}
		this.req = req;
	}

	@Override
	public ComputeResult getResult() {
		return res;
	}

	private final int[] dist;
	private final int[] prev;

	@Override
	public void run() {
		if (req == null) {
			// TODO: some problem here
			// return;
		}
		res = req.getResultObject();

		// TODO: Validation!
		ArrayList<Map<String, Double>> points = (ArrayList<Map<String, Double>>) req
				.get("points");

		srclat = points.get(0).get("lt").floatValue();
		srclon = points.get(0).get("ln").floatValue();
		destlat = points.get(1).get("lt").floatValue();
		destlon = points.get(1).get("ln").floatValue();

		srcid = graph.getIDForCoordinates(srclat, srclon);
		destid = graph.getIDForCoordinates(destlat, destlon);

		dist[srcid] = 0;
		graphrep.Heap h = new graphrep.Heap();

		h.insert(srcid, dist[srcid]);

		int nodeID;
		int nodeDist;
		int outTarget = 0;

		DIJKSTRA: for (;;) {
			if (!h.isEmpty()) {
				nodeID = h.peekMinId();
				nodeDist = h.peekMinDist();
				h.removeMin();
				if (nodeID == destid) {
					break DIJKSTRA;
				} else if (nodeDist > dist[nodeID]) {
					continue;
				}
				for (int i = 0; i < graph.getOutEdgeCount(nodeID); i++) {
					outTarget = graph.getOutTarget(nodeID, i);

					if (dist[nodeID] + graph.getOutDist(nodeID, i) < dist[outTarget]) {
						dist[outTarget] = dist[nodeID]
								+ graph.getOutDist(nodeID, i);
						prev[outTarget] = nodeID;
						h.insert(outTarget, dist[outTarget]);
					}
				}
			} else {
				System.err.println("heap empty");
				// TODO: do something useful
				return;
			}
		}
		System.err.println("found sp with dist = " + dist[outTarget] / 1000
				+ " km");

		int currNode = nodeID;

		ArrayList<ArrayList<Map<String, Float>>> route = new ArrayList<ArrayList<Map<String, Float>>>(
				dist[outTarget] / 50);
		do {
			route.add(0,
					new ArrayList<Map<String, Float>>(dist[outTarget] / 50));
			route.get(0).add(0, new TreeMap<String, Float>());
			route.get(0).get(0).put("lt", graph.getNodeLat(currNode));
			route.get(0).get(0).put("ln", graph.getNodeLat(currNode));
			currNode = prev[currNode];
		} while (currNode != srcid);

		Map<String, Float> misc = new TreeMap<String, Float>();
		misc.put("distance", (float) dist[outTarget]);

		// System.out
		// .println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>");
		// System.out
		// .println("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:gpxx=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3\" xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\" creator=\"Oregon 400t\" version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.garmin.com/xmlschemas/GpxExtensions/v3 http://www.garmin.com/xmlschemas/GpxExtensionsv3.xsd http://www.garmin.com/xmlschemas/TrackPointExtension/v1 http://www.garmin.com/xmlschemas/TrackPointExtensionv1.xsd\">");
		// System.out.println("  <trk>\n"
		// + "    <name>Example GPX Document</name>");
		// System.out.println("<trkseg>");
		// for (ArrayList<Float> l : list) {
		// System.out.println("<trkpt lat=\"" + l.get(0) + "\" lon=\""
		// + l.get(1) + "\"></trkpt>");
		// }
		// System.out.println("</trkseg>\n</trk>\n</gpx>");
		res.put("points", route);
		res.put("misc", misc);
	}
}