package io.cdf.remotedebug;

import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import org.java_websocket.enums.ReadyState;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

public class App {
	private static PrintStream ps = System.out;
	private static Client wsc;
	private static Server wss;

	public static void main(String[] args) {
		run(args);
	}

	public static void run(String[] args) {
		if (args.length > 0) {
			if (args[0].startsWith("-remoterun")) {
				try {
					wsc = new Client(new URI(args[1]));
					wsc.connect();
					ps.print("Connecting");
					while (wsc.getReadyState() == ReadyState.NOT_YET_CONNECTED) {
						ps.print('-');
						Thread.sleep(500);
					}
					if (wsc.getReadyState() == ReadyState.OPEN) {
						ps.println("\nConnected to Server.");
						String[] files = args[2].split(":");
						String zipdata = Base64.encode(Zip.zipFiles(files));
						String jarfile = args[3], clsname = args[4], params = "";
						int len = args.length;
						if (len > 5) {
							for (int i = 5; i < len; i++)
								params += args[i] + ';';
							params = params.substring(0, params.length() - 1);
						}
						String xml = "<DebugTarget>";
						xml += String.format("<JarFile>%s</JarFile>", jarfile);
						xml += String.format("<ClassName>%s</ClassName>", clsname);
						xml += String.format("<Args>%s</Args>", params);
						xml += String.format("<Data>%s</Data>", zipdata);
						xml += "</DebugTarget>";
						wsc.send(xml);
						while (wsc.getReadyState() != ReadyState.CLOSED)
							;
						wsc.close();
					}
				} catch (Exception e) {
					ps.println("Exception While Upload Messages!");
					e.printStackTrace();
				} finally {
					try {
						wsc.close();
					} catch (Exception e) {
					}
				}
			} else if (args[0].startsWith("-server")) {
				ps.println("Server started...");
				try {
					int port = Integer.parseInt(args[1]);
					wss = new Server(port);
					wss.start();
					File flag = new File("/var/remotedebug/stop.flag");
					while (!flag.exists())
						Thread.sleep(1000);
					flag.delete();
					wss.stop();
				} catch (Exception e) {
					ps.println("Exception While Starting Server!");
					e.printStackTrace();
				} finally {
					try {
						wss.stop();
					} catch (Exception e) {
					}
				}
			} else {
				help();
			}
		} else {
			help();
		}
		ps.println("Process Exit.");
	}

	public static void help() {
		ps.println("Usage:");
		ps.println(
				"-remoterun <URL> <UploadFiles1;UploadFiles2> <JarFileName> <Package.ClassName> <StringArguments[]>");
		ps.println("-server <Port>");
		ps.println();
	}
}
