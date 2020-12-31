package tech.cdf.remotedebug;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.java_websocket.WebSocket;

public class InvokeThread {
	private WebSocket ws;
	private String cmd;
	private RunTarget.RunMode rm;
	private String err;

	public InvokeThread(WebSocket arg0, String cmd, RunTarget.RunMode rm) throws Exception {
		this.ws = arg0;
		this.cmd = cmd;
		this.rm = rm;
		this.err = "\033[31m\n";
	}

	private void AppendErr(char ch) {
		this.err += String.valueOf(ch);
	}

	public void Invoke() throws Exception {
		final Process p = Runtime.getRuntime().exec(this.cmd);
		final InputStream stdin = p.getInputStream();
		final InputStream errin = p.getErrorStream();
		sendstr("\033[7;32m=====TARGET APP IS EXECUTING ON REMOTE=====\033[0m\n");
		new Thread() {
			public void run() {
				try {
					File flag = new File("/var/remotedebug/stoprun.flag");
					while (p.isAlive()) {
						if (flag.exists()) {
							p.destroy();
							flag.delete();
							ws.send("RUN\n");
							ws.send("ERRThe process was forced to terminate by the server.");
						}
						Thread.sleep(100);
					}
				} catch (Exception e) {
				}
			}
		}.start();
		Thread StdThread = new Thread() {
			public void run() {
				BufferedReader brstd = new BufferedReader(new InputStreamReader(stdin));
				try {
					int i = -1;
					do {
						i = brstd.read();
						if (i != -1)
							sendstr(String.valueOf((char) i));
					} while (i != -1);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try {
						stdin.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		Thread ErrThread = new Thread() {
			public void run() {
				BufferedReader brerr = new BufferedReader(new InputStreamReader(errin));
				try {
					int i = -1;
					do {
						i = brerr.read();
						if (i != -1)
							AppendErr((char) i);
					} while (i != -1);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try {
						errin.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		StdThread.start();
		ErrThread.start();
		p.waitFor();
		while (StdThread.isAlive() || ErrThread.isAlive())
			;
		System.out.println("Target App Exit.");
		ws.send("RUN\n\n");
		ws.send("MSGTarget app exit,exit value=" + p.exitValue());
		sendstr(RunTarget.RunMode.EXCEPTION, "\033[7;31m=====TARGET APP RETURNED ERROR MESSAGE=====\033[0m" + this.err);
		ws.send("RUN\n");
		p.destroy();
	}

	private void sendstr(RunTarget.RunMode m, String s) {
		SimpleDateFormat sdf = new SimpleDateFormat("[MM-dd hh:mm:ss]");
		String send = "RUN";
		if (s.length() > 0) {
			for (char ch : s.toCharArray())
				if (ch == '\n')
					send += "\n[REMOTE][" + m.name() + "]" + sdf.format(new Date());
				else
					send += ch;
			ws.send(send);
		}
	}

	private void sendstr(String s) {
		sendstr(this.rm, s);
	}
}
