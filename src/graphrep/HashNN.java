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

	private static final long serialVersionUID = 1L;
	GraphRep graphRep;
	HashMap<Long, Object> hashMap;
	NNSearcher dumpNN;

	public HashNN(GraphRep graphRep) {
		this.graphRep = graphRep;
		dumpNN = new DumbNN(graphRep);

		hashMap = new HashMap<Long, Object>();
		for (int i = 0; i < graphRep.getNodeCount(); i++) {
			int tempLat = (int) (graphRep.getNodeLat(i) * 1000);
			int tempLon = (int) (graphRep.getNodeLon(i) * 1000);

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
		int keyLat = (int) (lat * 1000);
		int keyLon = (int) (lon * 1000);
		int pos = -1;
		long key = keyLat << 32 | keyLon;
		double dist = Long.MAX_VALUE;
		double tempDist = Long.MAX_VALUE;
		if (hashMap.containsKey(key)) {
			int[] arr = (int[]) hashMap.get(key);
			for (int nodeID : arr) {
				tempDist = (graphRep.getNodeLat(nodeID) - lat)
						* (graphRep.getNodeLat(nodeID) - lat)
						+ (graphRep.getNodeLon(nodeID) - lon)
						* (graphRep.getNodeLon(nodeID) - lon);
				if (tempDist < dist) {
					dist = tempDist;
					pos = nodeID;
				}

			}
		}
		// test ring 1
		long ringkey = 0;
		for (int i = -1; i < 2; i++) {
			for (int j = -1; i < 2; i++) {
				ringkey = keyLat + i << 32 | keyLon + j;
				if (hashMap.containsKey(ringkey)) {
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
				}
			}
		}
		// some parts of ring 2
		for (int j = -1; j < 2; j++) {
			ringkey = keyLat + -2 << 32 | keyLon + j;
			if (hashMap.containsKey(ringkey)) {
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
			}
		}
		for (int j = -1; j < 2; j++) {
			ringkey = keyLat + 2 << 32 | keyLon + j;
			if (hashMap.containsKey(ringkey)) {
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
			}
		}
		for (int i = -1; i < 2; i++) {
			ringkey = keyLat + i << 32 | keyLon + -2;
			if (hashMap.containsKey(ringkey)) {
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
			}
		}
		for (int i = -1; i < 2; i++) {
			ringkey = keyLat + i << 32 | keyLon + 2;
			if (hashMap.containsKey(ringkey)) {
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
			}
		}

		if (pos == -1) {
			pos = dumpNN.getIDForCoordinates(lat, lon);
		}
		return pos;
	}
}
