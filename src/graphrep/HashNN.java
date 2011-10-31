/**
 * 
 */
package graphrep;

import java.util.HashMap;

import com.carrotsearch.hppc.IntArrayList;

/**
 * @author nino
 * 
 */
public class HashNN implements NNSearcher {

	GraphRep graphRep;
	HashMap<Long, Object> hashMap;
	NNSearcher dumpNN;
	private static final int maxHopLimit = 10;

	public HashNN(GraphRep graphRep) {
		this.graphRep = graphRep;
		dumpNN = new DumbNN(graphRep);
		hashMap = new HashMap<Long, Object>();
		for (int i = 0; i < graphRep.getNodeCount(); i++) {
			long tempLat = (long) graphRep.getNodeLat(i) * 1000;
			long tempLon = (long) graphRep.getNodeLon(i) * 1000;

			long key = tempLat << 32 | tempLon;
			IntArrayList tempValues = (IntArrayList) hashMap.get(key);
			if (tempValues == null) {
				tempValues = new IntArrayList(10);
				tempValues.add(i);
				hashMap.put(key, tempValues);
			} else {
				tempValues.add(i);
			}
		}
		for (Long key : hashMap.keySet()) {
			int[] arr = ((IntArrayList) hashMap.get(key)).toArray();
			hashMap.put(key, arr);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see graphrep.NNSearcher#getIDForCoordinates(double, double)
	 */
	@Override
	public int getIDForCoordinates(double lat, double lon) {
		long keyLat = (long) (lat * 1000);
		long keyLon = (long) (lon * 1000);
		int pos = -1;
		long key;
		double dist = Long.MAX_VALUE;
		double tempDist = Long.MAX_VALUE;
		boolean found = false;
		boolean finished = false;
		int hops = 0;
		for (int i = 0; i <= hops; i++) {
			// North
			for (int j = -i; j <= i; j++) {
				key = keyLat + i << 32 | keyLon + j;
				if (hashMap.containsKey(key)) {
					int[] ringArr = (int[]) hashMap.get(key);
					for (int nodeID : ringArr) {
						tempDist = (graphRep.getNodeLat(nodeID) - lat)
								* (graphRep.getNodeLat(nodeID) - lat)
								+ (graphRep.getNodeLon(nodeID) - lon)
								* (graphRep.getNodeLon(nodeID) - lon);
						if (tempDist < dist) {
							dist = tempDist;
							pos = nodeID;
						}

					}
					found = true;
				}
			}
			// East
			for (int j = -i + 1; j <= i - 1; j++) {
				key = keyLat + j << 32 | keyLon - i;
				if (hashMap.containsKey(key)) {
					int[] ringArr = (int[]) hashMap.get(key);
					for (int nodeID : ringArr) {
						tempDist = (graphRep.getNodeLat(nodeID) - lat)
								* (graphRep.getNodeLat(nodeID) - lat)
								+ (graphRep.getNodeLon(nodeID) - lon)
								* (graphRep.getNodeLon(nodeID) - lon);
						if (tempDist < dist) {
							dist = tempDist;
							pos = nodeID;
						}

					}
					found = true;
				}
			}
			// West
			for (int j = -i + 1; j <= i - 1; j++) {
				key = keyLat + j << 32 | keyLon + i;
				if (hashMap.containsKey(key)) {
					int[] ringArr = (int[]) hashMap.get(key);
					for (int nodeID : ringArr) {
						tempDist = (graphRep.getNodeLat(nodeID) - lat)
								* (graphRep.getNodeLat(nodeID) - lat)
								+ (graphRep.getNodeLon(nodeID) - lon)
								* (graphRep.getNodeLon(nodeID) - lon);
						if (tempDist < dist) {
							dist = tempDist;
							pos = nodeID;
						}

					}
					found = true;
				}
			}

			// South
			for (int j = -i; j <= i; j++) {
				key = keyLat - i << 32 | keyLon + j;
				if (hashMap.containsKey(key)) {
					int[] ringArr = (int[]) hashMap.get(key);
					for (int nodeID : ringArr) {
						tempDist = (graphRep.getNodeLat(nodeID) - lat)
								* (graphRep.getNodeLat(nodeID) - lat)
								+ (graphRep.getNodeLon(nodeID) - lon)
								* (graphRep.getNodeLon(nodeID) - lon);
						if (tempDist < dist) {
							dist = tempDist;
							pos = nodeID;
						}

					}
					found = true;
				}
			}
			/*
			 * for (int j = -i; j <= i; j++) { for (int k = -i; k <= i; k++) {
			 * key = keyLat + j << 32 | keyLon + k; if
			 * (hashMap.containsKey(key)) { int[] ringArr = (int[])
			 * hashMap.get(key); for (int nodeID : ringArr) { tempDist =
			 * (graphRep.getNodeLat(nodeID) - lat) (graphRep.getNodeLat(nodeID)
			 * - lat) + (graphRep.getNodeLon(nodeID) - lon)
			 * (graphRep.getNodeLon(nodeID) - lon); if (tempDist < dist) { dist
			 * = tempDist; pos = nodeID; }
			 * 
			 * } found = true; } } }
			 */
			if (found != true && hops <= maxHopLimit) {
				hops++;
			} else if (found && !finished) {
				finished = true;
				hops++;
			}

		}

		if (!found) {
			pos = dumpNN.getIDForCoordinates(lat, lon);
		}
		return pos;
	}
}
