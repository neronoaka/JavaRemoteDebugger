package io.cdf.remotedebug;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.DocumentBuilderFactory;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

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
			if (arg1.startsWith("<DebugTarget>")) {
				String[] target = GetXML(arg1);
				String jarfile = WorkingPath + "files/" + target[0];
				String clsname = target[1];
				String[] arguments = target[2].split(" ");
				byte[] zipdata = Base64.decode(target[3]);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
				String zippath = WorkingPath + "temp/zip" + sdf.format(new Date());
				File zipfile = new File(zippath);
				zipfile.createNewFile();
				FileOutputStream fis = new FileOutputStream(zipfile);
				fis.write(zipdata);
				fis.close();
				Zip.unZipFiles(zippath, "/var/remotedebug/files");
				SocketPrintStream ps = new SocketPrintStream(arg0);
				arg0.send("MSGStarting:" + jarfile + '/' + clsname);
				System.out.println("Running Target App...");
				LoadLibrary(WorkingPath + "files", target[0], arg0);
				ps.println("\n");
				Object obj = Invoke(jarfile, clsname + ".App", "run",
						new Class<?>[] { String[].class, PrintStream.class }, new Object[] { arguments, ps });
				System.out.println("Target App Exit.");
				arg0.send("RUN\n\n");
				arg0.send("MSGTarget app Exit,returned: " + String.valueOf(obj));
				arg0.send("END");
			}
		} catch (Exception e) {
			arg0.send("ERRException While Running:\n" + GetTrace(e));
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

	private String[] GetXML(String arg0) throws Exception {
		String[] result = new String[4];
		InputStream is = new ByteArrayInputStream(arg0.getBytes());
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document doc = dbf.newDocumentBuilder().parse(is);
		doc.getDocumentElement().normalize();
		NodeList list = doc.getElementsByTagName("DebugTarget");
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element elem = (Element) node;
				result[0] = elem.getElementsByTagName("JarFile").item(0).getTextContent();
				result[1] = elem.getElementsByTagName("ClassName").item(0).getTextContent();
				result[2] = elem.getElementsByTagName("Args").item(0).getTextContent();
				result[3] = elem.getElementsByTagName("Data").item(0).getTextContent();
			}
		}
		return result;
	}

	private Object Invoke(String clspath, String clsname, String methodname, Class<?>[] paramsCls, Object[] params)
			throws Exception {
		URL url = new URL("file:" + clspath);
		URLClassLoader loader = new URLClassLoader(new URL[] { url });
		Class<?> cls = loader.loadClass(clsname);
		loader.close();
		Method method = cls.getDeclaredMethod(methodname, paramsCls);
		return method.invoke(cls.newInstance(), params);
	}

	private String GetTrace(Exception e) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		e.printStackTrace(ps);
		return baos.toString();
	}

	private void LoadLibrary(String path, String mainfile, WebSocket msg) throws Exception {
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
						msg.send("MSGLoad jar:" + file.getName() + " SUCCESSFUL");
					} catch (Exception e) {
						msg.send("ERRLoad jar:" + file.getName() + "ERROR\n" + GetTrace(e));
					}
				}
			} catch (Exception e) {
				msg.send("ERR" + GetTrace(e));
			} finally {
				method.setAccessible(accessible);
			}
		}
	}
}