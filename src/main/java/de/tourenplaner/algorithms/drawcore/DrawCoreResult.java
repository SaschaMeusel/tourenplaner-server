package de.tourenplaner.algorithms.drawcore;

import com.carrotsearch.hppc.IntArrayList;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tourenplaner.algorithms.bbbundle.BBBundleEdge;
import de.tourenplaner.computecore.StreamJsonWriter;
import de.tourenplaner.graphrep.GraphRep;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Created by niklas on 30.03.15.
 */
public class DrawCoreResult implements StreamJsonWriter {

    private final ArrayList<BBBundleEdge> edges;
    private final int coreSize;
    private final GraphRep graph;
    private final IntArrayList edgesToDraw;
    private final IntArrayList verticesToDraw;

    public DrawCoreResult(GraphRep graph, ArrayList<BBBundleEdge> edges, IntArrayList verticesToDraw, IntArrayList edgesToDraw, int coreSize) {
        this.graph = graph;
        this.edges = edges;
        this.coreSize = coreSize;
        this.verticesToDraw = verticesToDraw;
        this.edgesToDraw = edgesToDraw;
    }

    @Override
    public void writeToStream(ObjectMapper mapper, OutputStream stream) throws IOException {
        JsonGenerator gen = mapper.getFactory().createGenerator(stream);
        gen.writeStartObject();
        gen.writeNumberField("nodeCount", coreSize);
        gen.writeNumberField("edgeCount", edges.size());
        // Drawing Data
        // Drawing Data
        gen.writeObjectFieldStart("draw");
        gen.writeArrayFieldStart("vertices");
        for (int i = 0; i < verticesToDraw.size(); ++i) {
            writeDrawVertex(gen, verticesToDraw.get(i));
        }
        gen.writeEndArray();
        gen.writeArrayFieldStart("edges");
        for (int i = 0; i < edgesToDraw.size(); ++i) {
            gen.writeNumber(edgesToDraw.get(i));
        }
        gen.writeEndArray();
        gen.writeEndObject();
        // Edges
        gen.writeArrayFieldStart("edges");
        for (BBBundleEdge e : edges) {
            gen.writeStartObject();
            gen.writeNumberField("src", e.srcId);
            gen.writeNumberField("trgt", e.trgtId);
            gen.writeNumberField("cost", e.cost);
            gen.writeNumberField("edgeId", e.edgeId);
            gen.writeArrayFieldStart("path");
            for (int i = 0; i < e.unpacked.size(); ++i){
                gen.writeNumber(e.unpacked.get(i));
            }
            gen.writeEndArray();
            gen.writeEndObject();
        }
        gen.writeEndArray();
        gen.writeEndObject();
        gen.flush();
    }

    public final void writeDrawVertex(JsonGenerator gen, int nodeId) throws IOException {
        gen.writeNumber(graph.getXPos(nodeId));
        gen.writeNumber(graph.getYPos(nodeId));
    }
}
