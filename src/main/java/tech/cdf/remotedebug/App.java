package tech.cdf.remotedebug;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import org.java_websocket.enums.ReadyState;

import com.google.gson.Gson;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import tech.cdf.remotedebug.model.RunTarget;
import tech.cdf.remotedebug.model.RunTarget.RunMode;

public class App {
	private static PrintStream ps = System.out;
	private static Client wsc;
	private static Server wss;
	public static Gson gson;

	public static void main(String[] args) throws Exception {
		gson = new Gson();
		if (args.length > 0) {
			if (args[0].startsWith("-remoterun")) {
				try {
					runClient(args);
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
				runServer(args);
			} else {
				help();
			}
		} else {
			help();
		}
		ps.println("Process Exit.");
	}

	public static void runClient(String[] args) throws Exception {
		wsc = new Client(new URI(args[1]));
		wsc.connect();
		ps.print("Connecting");
		while (wsc.getReadyState() == ReadyState.NOT_YET_CONNECTED) {
			ps.print('-');
			Thread.sleep(500);
		}
		if (wsc.getReadyState() == ReadyState.OPEN) {
			ps.println("\nConnected to Server.");
			String jarfile = args[3], clsname = args[4], params = "";
			String[] files = args[2].split("\\|");
			String zipdata = Base64.encode(Zip.zipFiles(files));
			int len = args.length;
			if (len > 6)
				for (int i = 6; i < len; i++)
					params += args[i] + " ";
			RunTarget rt = new RunTarget(jarfile, clsname, params.trim(), zipdata, RunMode.valueOf(args[5]));
			wsc.send(gson.toJson(rt));
			while (wsc.getReadyState() != ReadyState.CLOSED)
				;
			wsc.close();
		}
	}

	public static void runServer(String[] args) {
		ps.println("Server started...");
		try {
			int port = Integer.parseInt(args[1]);
			wss = new Server(port);
			wss.start();
			File flag = new File("/var/remotedebug/stop.flag");
			while (!flag.exists())
				Thread.sleep(100);
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
	}

	public static void help() {
		ps.println("Usage:");
		ps.println("-remoterun <URL> <UploadFilePath1|UploadFilePath2|...>"
				+ " <JarFileName> <Package> <RUN|DEBUG> <StringArguments[]>");
		ps.println("-server <Port>");
		ps.println();
	}

	public static String GetTrace(Exception e) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		e.printStackTrace(ps);
		return baos.toString();
	}
}
