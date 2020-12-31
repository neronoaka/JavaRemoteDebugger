package tech.cdf.remotedebug;

import com.google.gson.Gson;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import org.java_websocket.enums.ReadyState;

public class App {
	private static PrintStream ps = System.out;
	private static Client wsc;
	private static Server wss;
	public static Gson gson;

	public static void main(String[] args) throws Exception {
		gson = new Gson();
		if (args.length > 0) {
			if (args[0].startsWith("-remoterun") && args.length == 6) {
				try {
					RunClient(args);
				} catch (Exception e) {
					ps.println("\033[0;31mException While Upload Messages!");
					e.printStackTrace();
				} finally {
					try {
						wsc.close();
					} catch (Exception exception) {
					}
				}
			} else if (args[0].startsWith("-server") && args.length == 3)
				RunServer(args);
			else
				Help();
		} else
			Help();
		ps.println("\033[0mProcess Exit.");
	}

	public static void RunClient(String[] args) throws Exception {
		wsc = new Client(new URI(args[1]));
		wsc.connect();
		ps.print("Connecting");
		while (wsc.getReadyState() == ReadyState.NOT_YET_CONNECTED) {
			ps.print('-');
			Thread.sleep(500);
		}
		if (wsc.getReadyState() == ReadyState.OPEN) {
			ps.println("\n\033[7;32mConnected to Server.\033[0m");
			String jarfile = args[3], params = "";
			String[] files = args[2].split("\\|");
			String zipdata = Base64.encode(Zip.ZipFiles(files));
			int len = args.length;
			if (len > 5)
				for (int i = 5; i < len; i++)
					params = String.valueOf(params) + args[i] + " ";
			RunTarget rt = new RunTarget(jarfile, params.trim(), zipdata, RunTarget.RunMode.valueOf(args[4]));
			wsc.send(gson.toJson(rt));
			while (wsc.getReadyState() != ReadyState.CLOSED)
				;
			wsc.close();
		}
	}

	public static void RunServer(String[] args) {
		ps.println("Server started...");
		try {
			int port = Integer.parseInt(args[1]);
			int dbgport = Integer.parseInt(args[2]);
			if (dbgport < 0 || dbgport >= 65536)
				throw new Exception("Debug port must between 0-65535");
			wss = new Server(port, dbgport);
			wss.start();
			File flag = new File("/var/remotedebug/stop.flag");
			while (!flag.exists())
				Thread.sleep(100);
			flag.delete();
			wss.stop();
		} catch (Exception ex) {
			ps.println("Exception While Starting Server!");
			ex.printStackTrace();
		} finally {
			try {
				wss.stop();
			} catch (Exception e) {
			}
		}
	}

	private static void Help() {
		ps.println("Usage:");
		ps.println("-remoterun <URL> <File1|File2|...> <JarFileName> <RUN or DEBUG> <StringArguments[]>");
		ps.println("-server <Port> <DebugPort>");
		ps.println();
	}

	public static String GetTrace(Exception e) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		e.printStackTrace(ps);
		return String.valueOf(baos.toString()) + "\n";
	}
}
