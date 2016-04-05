package androidtomoyo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.LinkedList;

import org.graphstream.stream.SourceBase;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class HTTPSourceExtended extends SourceBase {

	protected final HttpServer server;
	
	private AndroidGraph graph;

	public HTTPSourceExtended(String graphId, int port, AndroidGraph graph) throws IOException {
		super(String.format("http://%s", graphId));

		server = HttpServer.create(new InetSocketAddress(port), 4);
		server.createContext(String.format("/%s/edit", graphId),
				new EditHandler());
		
		this.graph = graph;

	}

	public void start() {
		server.start();
	}

	public void stop() {
		server.stop(0);
	}

	private class EditHandler implements HttpHandler {

		public void handle(HttpExchange ex) throws IOException {
			HashMap<String, Object> get = GET(ex);
			Action a;

			try {
				a = Action.valueOf(get.get("q").toString().toUpperCase());
			} catch (Exception e) {
				error(ex, "invalid action");
				return;
			}

			switch (a) {
			case AN:
				HTTPSourceExtended.this.sendNodeAdded(sourceId, get.get("id")
						.toString());
				break;
			case CN:
				break;
			case ANA:
				HTTPSourceExtended.this.sendNodeAttributeAdded(sourceId, get.get("id")
						.toString(), get.get("key").toString(), get.get("value"));
				break;
			case AEA:
				HTTPSourceExtended.this.sendEdgeAttributeAdded(sourceId, get.get("id")
						.toString(), get.get("key").toString(), get.get("value"));
				break;
			case DN:
				HTTPSourceExtended.this.sendNodeRemoved(sourceId, get.get("id")
						.toString());
				break;
			case AE:
				HTTPSourceExtended.this.sendEdgeAdded(sourceId, get.get("id")
						.toString(), get.get("from").toString(), get.get("to")
						.toString(), get.containsKey("directed"));
				break;
			case CE:
				break;
			case DE:
				HTTPSourceExtended.this.sendEdgeRemoved(sourceId, get.get("id")
						.toString());
				break;
			case CG:
				break;
			case ST:
				HTTPSourceExtended.this.sendStepBegins(sourceId, Double.valueOf(get
						.get("step").toString()));
				break;
			case CENTRALITY:
				graph.doCentrality();
				break;
			case SPANTREE:
				graph.doSpanTree();
				break;
			case EDGECOLORING:
				graph.doEdgeColoring();
				break;
			case DGSFLUSH:
				graph.doDGSFlush();
				break;
			}

			ex.sendResponseHeaders(200, 0);
			ex.getResponseBody().close();
		}
	}
	
	protected static void error(HttpExchange ex, String message)
			throws IOException {
		byte[] data = message.getBytes();

		ex.sendResponseHeaders(400, data.length);
		ex.getResponseBody().write(data);
		ex.getResponseBody().close();
	}

	@SuppressWarnings("unchecked")
	protected static HashMap<String, Object> GET(HttpExchange ex) {
		HashMap<String, Object> get = new HashMap<String, Object>();
		String[] args = ex.getRequestURI().getRawQuery().split("[&]");

		for (String arg : args) {
			String[] kv = arg.split("[=]");
			String k, v;

			k = null;
			v = null;

			try {
				if (kv.length > 0)
					k = URLDecoder.decode(kv[0], System
							.getProperty("file.encoding"));

				if (kv.length > 1)
					v = URLDecoder.decode(kv[1], System
							.getProperty("file.encoding"));

				if (get.containsKey(k)) {
					Object o = get.get(k);

					if (o instanceof LinkedList<?>)
						((LinkedList<Object>) o).add(v);
					else {
						LinkedList<Object> l = new LinkedList<Object>();
						l.add(o);
						l.add(v);
						get.put(k, l);
					}
				} else {
					get.put(k, v);
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}

		return get;
	}
	
	static enum Action {
		AN, CN, DN, AE, CE, DE, CG, ST, ANA, AEA, CLEAR,
		CENTRALITY, SPANTREE, EDGECOLORING, DGSFLUSH
	}
}

