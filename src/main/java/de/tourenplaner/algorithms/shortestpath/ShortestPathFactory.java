/*
 * Copyright 2012 ToureNPlaner
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package de.tourenplaner.algorithms.shortestpath;

import de.tourenplaner.algorithms.Algorithm;
import de.tourenplaner.algorithms.DijkstraStructs;
import de.tourenplaner.algorithms.SharingAlgorithmFactory;
import de.tourenplaner.graphrep.GraphRep;

import java.util.*;

/**
 * Used to create instances of ShortestPath algorithm
 *
 * @author Christoph Haag, Sascha Meusel, Niklas Schnelle, Peter Vollmer
 */
public class ShortestPathFactory extends SharingAlgorithmFactory {
    private final Map<String, Object> details;
    private final List<Map<String, Object>> constraints;
    private final List<Map<String, Object>> pointConstraints;


    public ShortestPathFactory(GraphRep graph) {
		super(graph);
        constraints = new ArrayList<Map<String, Object>>(0);
        pointConstraints = new ArrayList<Map<String, Object>>(0);
        details = new HashMap<String, Object>(3);
        details.put("hidden", this.isHidden());
        details.put("minpoints", 1);
        details.put("sourceistarget", false);

    }

    @Override
    public String getDescription() {
        return "Calculates the shortest path visiting the given points in the order they are supplied";
    }


    @Override
    public Map<String, Object> getDetails() {
        return details;
    }
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.tourenplaner.algorithms.AlgorithmFactory#createAlgorithm()
	 */
	@Override
	public Algorithm createAlgorithm(DijkstraStructs rs) {
		return new ShortestPathNoCH(graph, rs);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.tourenplaner.algorithms.AlgorithmFactory#createAlgorithm()
	 */
	@Override
	public Algorithm createAlgorithm() {
		return new ShortestPathNoCH(graph, new DijkstraStructs(
				graph.getNodeCount(), graph.getEdgeCount()));
	}

	@Override
	public String getURLSuffix() {
		return "sps";
	}

	@Override
	public String getAlgName() {
		return "Shortest Path Simple";
	}

	@Override
	public int getVersion() {
		return 1;
	}

	@Override
	public List<Map<String, Object>> getPointConstraints() {
		return pointConstraints;
	}

	@Override
	public List<Map<String, Object>> getConstraints() {
		return constraints;
	}

	@Override
	public boolean isHidden() {
		return false;
	}

}
