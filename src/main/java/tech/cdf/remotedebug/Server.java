package tech.cdf.remotedebug;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import java.io.File;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class Server extends WebSocketServer {
	private final String WorkingPath = "/var/remotedebug/";
	private int dbgport;

	public Server(int port, int dbgp) throws Exception {
		super(new InetSocketAddress(port));
		this.dbgport = dbgp;
	}

	public void onClose(WebSocket arg0, int arg1, String arg2, boolean arg3) {
		System.out.println("[WEBSOCKET]closed.");
	}

	public void onError(WebSocket arg0, Exception arg1) {
		System.out.println("[WEBSOCKET]error:\n");
		arg1.printStackTrace();
	}

	public void onMessage(WebSocket arg0, String arg1) {
		try {
			Clean();
			RunTarget rt = (RunTarget) App.gson.fromJson(arg1, RunTarget.class);
			UnZIP(Base64.decode(rt.Data));
			switch (rt.Runmode) {
			case RUN:
				Run(arg0, "/var/remotedebug/app/" + rt.JarFile, rt.Args);
				break;
			case DEBUG:
				Debug(arg0, "/var/remotedebug/app/" + rt.JarFile, rt.Args);
				break;
			default:
				break;
			}
			arg0.send("END");
		} catch (Exception e) {
			arg0.send("ERRException While Running:\n" + App.GetTrace(e));
			arg0.send("END");
		}
	}

	public void onOpen(WebSocket arg0, ClientHandshake arg1) {
		System.out.println("[WEBSOCKET]opened.");
	}

	public void onStart() {
		System.out.println("[WEBSOCKET]started.");
	}

	private void Clean() {
		File appdir = new File("/var/remotedebug/app");
		File temp = new File("/var/remotedebug/temp");
		if (!temp.exists())
			temp.mkdir();
		if (!appdir.exists()) {
			appdir.mkdir();
		} else {
			byte b;
			int i;
			String[] arrayOfString;
			for (i = (arrayOfString = appdir.list()).length, b = 0; b < i;) {
				String child = arrayOfString[b];
				File f = new File(child);
				f.delete();
				b++;
			}
		}
	}

	private void UnZIP(byte[] zipdata) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
		String zippath = "/var/remotedebug/temp/zip" + sdf.format(new Date()) + ".tmp";
		File zipfile = new File(zippath);
		zipfile.createNewFile();
		FileOutputStream fis = new FileOutputStream(zipfile);
		fis.write(zipdata);
		fis.close();
		Zip.UnZipFiles(zippath, "/var/remotedebug/app");
	}

	private void Run(WebSocket arg0, String mainjar, String args) throws Exception {
		String cmd = "/usr/bin/java -jar " + mainjar;
		cmd = String.valueOf(cmd) + " " + args;
		arg0.send("MSGRun command line:" + cmd);
		InvokeThread it = new InvokeThread(arg0, cmd, RunTarget.RunMode.RUN);
		it.Invoke();
	}

	private void Debug(WebSocket arg0, String mainjar, String args) throws Exception {
		String cmd = "/usr/bin/java -Xdebug -Xnoagent -Djava.compiler=NONE ";
		cmd = String.valueOf(cmd) + "-Xrunjdwp:transport=dt_socket,address=" + this.dbgport;
		cmd = String.valueOf(cmd) + ",server=y,suspend=y -jar " + mainjar + " " + args;
		arg0.send("MSGDebug command line:\n" + cmd);
		InvokeThread it = new InvokeThread(arg0, cmd, RunTarget.RunMode.DEBUG);
		it.Invoke();
	}
}
