package io.cdf.remotedebug;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.java_websocket.WebSocket;

public class SocketPrintStream extends PrintStream {
	private WebSocket ws;

	public SocketPrintStream(WebSocket arg0) {
		super(System.out);
		ws = arg0;
	}

	@Override
	public void print(boolean b) {
		sendstr(b ? "true" : "false");
	}

	@Override
	public void print(char c) {
		sendstr(String.valueOf(c));
	}

	@Override
	public void print(int i) {
		sendstr(String.valueOf(i));
	}

	@Override
	public void print(long l) {
		sendstr(String.valueOf(l));
	}

	@Override
	public void print(float f) {
		sendstr(String.valueOf(f));
	}

	@Override
	public void print(double d) {
		sendstr(String.valueOf(d));
	}

	@Override
	public void print(char s[]) {
		sendstr(String.valueOf(s));
	}

	@Override
	public void print(String s) {
		if (s == null) {
			s = "null";
		}
		sendstr(s);
	}

	@Override
	public void print(Object obj) {
		sendstr(String.valueOf(obj));
	}

	@Override
	public void println() {
		sendstr("\n");
	}

	@Override
	public void println(boolean b) {
		sendstr(b ? "true" : "false" + "\n");
	}

	@Override
	public void println(char c) {
		sendstr(String.valueOf(c) + "\n");
	}

	@Override
	public void println(int i) {
		sendstr(String.valueOf(i) + "\n");
	}

	@Override
	public void println(long l) {
		sendstr(String.valueOf(l) + "\n");
	}

	@Override
	public void println(float f) {
		sendstr(String.valueOf(f) + "\n");
	}

	@Override
	public void println(double d) {
		sendstr(String.valueOf(d) + "\n");
	}

	@Override
	public void println(char s[]) {
		sendstr(String.valueOf(s) + "\n");
	}

	@Override
	public void println(String s) {
		if (s == null) {
			s = "null";
		}
		sendstr(s + "\n");
	}

	@Override
	public void println(Object obj) {
		sendstr(String.valueOf(obj) + "\n");
	}

	@Override
	public PrintStream printf(String format, Object... args) {
		sendstr(String.format(format, args));
		return System.out;
	}

	@Override
	public PrintStream printf(Locale l, String format, Object... args) {
		sendstr(String.format(l, format, args));
		return System.out;
	}

	private void sendstr(String s) {
		SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd hh:mm:ss]");
		String send = "RUN";
		if (s.length() > 0) {
			for (char ch : s.toCharArray())
				if (ch == '\n')
					send += "\n[INVOKE]" + sdf.format(new Date());
				else
					send += ch;
		}
		ws.send(send);
	}
}