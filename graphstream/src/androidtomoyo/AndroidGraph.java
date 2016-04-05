package androidtomoyo;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.graphstream.algorithm.BetweennessCentrality;
import org.graphstream.algorithm.Prim;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.file.FileSinkDGS;
import org.graphstream.stream.file.FileSinkImages;
import org.graphstream.stream.file.FileSinkImages.LayoutPolicy;
import org.graphstream.stream.file.FileSinkImages.OutputPolicy;
import org.graphstream.stream.file.FileSinkImages.OutputType;
import org.graphstream.stream.file.FileSinkImages.Quality;
import org.graphstream.stream.file.FileSinkImages.RendererType;
import org.graphstream.stream.file.FileSinkImages.Resolutions;
import org.graphstream.stream.file.FileSourceDGS;
import org.graphstream.ui.swingViewer.View;
import org.graphstream.ui.swingViewer.Viewer;
import org.graphstream.ui.swingViewer.util.Camera;

public class AndroidGraph implements MouseWheelListener {
	
	private static int base = 8083; 
	private Camera cam;
	private MultiGraph graph;
	private FileSinkDGS dgs;
	
	public void start(String id) throws Exception {
		start(id, true);
	}
	
	public void start(String id, boolean rubyfeed) throws Exception {
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		
		graph = new MultiGraph(id);
		graph.addAttribute("ui.stylesheet", "url('file://" + System.getProperty("user.dir") + "/graphstyle.css')");
		graph.addAttribute("ui.default.title", id);
		graph.addAttribute("ui.quality");
		graph.addAttribute("ui.antialiasing");
		
		Viewer viewer  = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
		viewer.enableAutoLayout();
		//viewer.disableAutoLayout();
		View view = viewer.addDefaultView(true);
		view.addMouseWheelListener(this);
		cam = view.getCamera();
		
		if (rubyfeed) {
			dgs = new FileSinkDGS();
			dgs.begin("android_binder.dgs");
			HTTPSourceExtended hs = new HTTPSourceExtended(id, base++, this);
			hs.addSink(graph);
			hs.addSink(dgs);
			hs.start();
		} else { /* DGS Feed */
			FileSourceDGS sdgs = new FileSourceDGS();
			FileSinkImages fsi = new FileSinkImages(OutputType.PNG, Resolutions.HD720);
			fsi.setRenderer(RendererType.SCALA);
			fsi.setStyleSheet("url('file://" + System.getProperty("user.dir") + "/graphstyle.css')");
			// COMPUTED_FULLY_AT_NEW_IMAGE -- SCATTA TROPPO
			// COMPUTED_IN_LAYOUT_RUNNER -- OK
			fsi.setLayoutPolicy(LayoutPolicy.COMPUTED_ONCE_AT_NEW_IMAGE);
			fsi.setQuality(Quality.HIGH);
			fsi.setOutputPolicy(OutputPolicy.BY_EDGE_ADDED_REMOVED);
			fsi.begin("/mnt/data/tmp/gs_");
			sdgs.addSink(graph);
			graph.addSink(fsi);
			sdgs.readAll("android_binder.dgs");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			fsi.setOutputPolicy(OutputPolicy.BY_EVENT);
			graph.stepBegins(0);
			System.out.println("\n***Centrality...");
			//br.readLine();
			Thread.sleep(10000);
			doCentrality();
			graph.stepBegins(1);
			System.out.println("\n***Edge Coloring...");
			//br.readLine();
			doEdgeColoring();
			graph.stepBegins(2);
			System.out.println("\n***Spanning Tree...");
			//br.readLine();
			doSpanTree();
			graph.stepBegins(3);
			fsi.end();
		}
	}
	
	public void mouseWheelMoved(MouseWheelEvent e) {
		cam.setViewPercent(cam.getViewPercent() + e.getUnitsToScroll() * 0.01);
	}
	
	private float color_normalize(float n) {
		float nn = n * 15;
		if (nn > 1) return 1;
		return nn;
	}
	
	private float size_normalize(float n) {
		int nn = (int) (n * 12);
		if (nn == 0) return 1;
		return nn;
	}

	public void doCentrality() {
		BetweennessCentrality bcb = new BetweennessCentrality();
		//bcb.setWeightAttributeName("weight");
		bcb.init(graph);
		bcb.compute();
		float max = 0;
		for (Node n : graph) {
			float cur = Float.parseFloat(n.getAttribute("Cb").toString());
			if (cur > max) max = cur;
		}
		for (Node n : graph) {
			float cur = Float.parseFloat(n.getAttribute("Cb").toString());
			n.setAttribute("ui.color", color_normalize(cur/max));
		}
	}

	public void doSpanTree() {
		int count = 0;
		for (Edge e : graph.getEachEdge())
			count++;
		System.out.println("Aristas antes: " + count);
		Prim prim = new Prim("ui.class", "intree", "notintree");
		prim.init(graph);
		prim.compute();
		count = 0;
		for (Edge e : graph.getEachEdge())
			if (e.getAttribute("ui.class").toString().equals("intree"))
				count++;
		System.out.println("Aristas despues: " + count);
	}

	public void doEdgeColoring() {
		float max = 0;
		for (Edge e : graph.getEachEdge()) {
			float cur = Float.parseFloat(e.getAttribute("weight").toString());
			if (cur > max) max = cur;
		}
		for (Edge e : graph.getEachEdge()) {
			float cur = Float.parseFloat(e.getAttribute("weight").toString());
			e.setAttribute("ui.color", color_normalize(cur/max));
			e.setAttribute("ui.size", size_normalize(cur/max));
		}
	}
	
	public void doDGSFlush() throws IOException {
			dgs.flush();
			dgs.end();
	}
}
