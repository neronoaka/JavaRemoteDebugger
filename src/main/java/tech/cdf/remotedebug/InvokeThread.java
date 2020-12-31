package tech.cdf.remotedebug;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.java_websocket.WebSocket;

import tech.cdf.remotedebug.model.RunTarget.RunMode;

public class InvokeThread {
	private WebSocket ws;
	private String cmd;
	private RunMode rm;

	public InvokeThread(WebSocket arg0, String cmd, RunMode rm) throws Exception {
		this.ws = arg0;
		this.cmd = cmd;
		this.rm = rm;
	}

	public void Invoke() throws Exception {
		final Process p = Runtime.getRuntime().exec(cmd);
		try {
			final InputStream stdin = p.getInputStream();
			final InputStream errin = p.getErrorStream();
			sendstr("=====TARGET APP IS DEBUGGING ON REMOTE=====\n");
			new Thread() {
				public void run() {
					BufferedReader brstd = new BufferedReader(new InputStreamReader(stdin));
					try {
						while (p.isAlive())
							sendstr(String.valueOf((char) brstd.read()));
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
			}.start();
			new Thread() {
				public void run() {
					BufferedReader brerr = new BufferedReader(new InputStreamReader(errin));
					try {
						while (p.isAlive())
							sendstr("\033[31m" + String.valueOf((char) brerr.read()));
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
			}.start();
			p.waitFor();
			p.destroy();
		} catch (Exception ex) {
			try {
				p.getErrorStream().close();
				p.getInputStream().close();
				p.getOutputStream().close();
			} finally {
			}
			throw ex;
		}
	}

	private void sendstr(String s) {
		SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd hh:mm:ss]");
		String send = "RUN";
		if (s.length() > 0) {
			for (char ch : s.toCharArray())
				if (ch == '\n')
					send += "\n[REMOTE][" + rm.name() + "]" + sdf.format(new Date());
				else
					send += ch;
		}
		ws.send(send);
	}
}
