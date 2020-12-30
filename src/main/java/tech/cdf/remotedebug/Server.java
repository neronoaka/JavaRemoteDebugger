package tech.cdf.remotedebug;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import tech.cdf.remotedebug.model.RunTarget;
import tech.cdf.remotedebug.model.SocketPrintStream;

public class Server extends WebSocketServer {
	private final String WorkingPath = "/var/remotedebug/";

	public Server(int port) throws Exception {
		super(new InetSocketAddress(port));
	}

	@Override
	public void onClose(WebSocket arg0, int arg1, String arg2, boolean arg3) {
		System.out.println("[WEBSOCKET]closed.");
	}

	@Override
	public void onError(WebSocket arg0, Exception arg1) {
		System.out.println("[WEBSOCKET]error:\n");
		arg1.printStackTrace();
	}

	@Override
	public void onMessage(WebSocket arg0, String arg1) {
		try {
			Clean();
			RunTarget rt = App.gson.fromJson(arg1, RunTarget.class);
			UnZIP(Base64.decode(rt.Data));
			switch (rt.Runmode) {
			case RUN:
				Run(arg0, rt.JarFile, rt.ClassName, rt.Args);
				break;
			case DEBUG:
				Debug(arg0, WorkingPath + "app/" + rt.JarFile, rt.Args);
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

	@Override
	public void onOpen(WebSocket arg0, ClientHandshake arg1) {
		System.out.println("[WEBSOCKET]opened.");
	}

	@Override
	public void onStart() {
		System.out.println("[WEBSOCKET]started.");
	}

	private void Clean() {
		File appdir = new File(WorkingPath + "app");
		File temp = new File(WorkingPath + "temp");
		if (!temp.exists())
			temp.mkdir();
		if (!appdir.exists())
			appdir.mkdir();
		else
			for (String child : appdir.list()) {
				File f = new File(child);
				f.delete();
			}
	}

	private void UnZIP(byte[] zipdata) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
		String zippath = WorkingPath + "temp/zip" + sdf.format(new Date());
		File zipfile = new File(zippath);
		zipfile.createNewFile();
		FileOutputStream fis = new FileOutputStream(zipfile);
		fis.write(zipdata);
		fis.close();
		Zip.unZipFiles(zippath, WorkingPath + "app");
	}

	private void Run(WebSocket arg0, String jarfile, String clsname, String args) throws Exception {
		SocketPrintStream ps = new SocketPrintStream(arg0, "RUN");
		arg0.send("MSGStarting:" + jarfile + '/' + clsname);
		System.out.println("Running Target App ...");
		LoadLibrary(arg0, WorkingPath + "app", jarfile);
		ps.println("=====TARGET APP IS RUNNING ON REMOTE=====");
		URL url = new URL("file:" + WorkingPath + "app/" + jarfile);
		URLClassLoader loader = new URLClassLoader(new URL[] { url });
		Class<?> cls = loader.loadClass(clsname + ".App");
		loader.close();
		Class<?>[] paramsCls = new Class<?>[] { String[].class, PrintStream.class };
		Object[] params = new Object[] { args.split(" "), ps };
		Method method = cls.getDeclaredMethod("run", paramsCls);
		Object obj = method.invoke(cls.newInstance(), params);
		System.out.println("Target App Exit.");
		arg0.send("RUN\n\n");
		arg0.send("MSGTarget app exit,returned: " + String.valueOf(obj));
	}

	private void Debug(final WebSocket arg0, String mainjar, String args) throws Exception {
		String cmd = "/usr/bin/java -Xdebug -Xnoagent -Djava.compiler=NONE "
				+ "-Xrunjdwp:transport=dt_socket,address=4520,server=y,suspend=y -jar ";
		cmd += mainjar + " " + args;
		final Process p = Runtime.getRuntime().exec(cmd);
		try {
			final SocketPrintStream ps = new SocketPrintStream(arg0, "DEBUG");
			arg0.send("MSGDebug command line:\n" + cmd);
			ps.println("=====TARGET APP IS DEBUGGING ON REMOTE=====");
			final InputStream stdin = p.getInputStream();
			new Thread() {
				public void run() {
					BufferedReader brstd = new BufferedReader(new InputStreamReader(stdin));
					try {
						while (p.isAlive())
							ps.print((char) brstd.read());
					} catch (Exception e) {
						arg0.send("ERR" + App.GetTrace(e));
					} finally {
						try {
							stdin.close();
						} catch (Exception e) {
						}
					}
				}
			}.start();
			p.waitFor();
			p.destroy();
		} catch (Exception e) {
			try {
				p.getErrorStream().close();
				p.getInputStream().close();
				p.getOutputStream().close();
			} catch (Exception ee) {
			}
		}
		System.out.println("Target App Exit.");
		arg0.send("RUN\n\n");
		arg0.send("MSGDebug target exit.");
	}

	private void LoadLibrary(WebSocket arg0, String path, String mainfile) throws Exception {
		ArrayList<File> jars = new ArrayList<File>();
		for (File f : new File(path).listFiles())
			if (!f.getName().endsWith(mainfile))
				jars.add(f);
		if (!jars.isEmpty()) {
			Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			boolean accessible = method.isAccessible();
			try {
				if (accessible == false)
					method.setAccessible(true);
				URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
				for (File file : jars) {
					try {
						method.invoke(classLoader, file.toURI().toURL());
						arg0.send("MSGLoading jar:" + file.getName() + "[SUCCESSFUL]");
					} catch (Exception e) {
						arg0.send("ERRLoading jar:" + file.getName() + "\n" + App.GetTrace(e));
					}
				}
			} catch (Exception e) {
				arg0.send("ERR" + App.GetTrace(e));
			} finally {
				method.setAccessible(accessible);
			}
		}
	}
}