package tech.cdf.remotedebug;

import java.net.URI;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class Client extends WebSocketClient {
	public Client(URI serverUri) {
		super(serverUri);
	}

	public void onClose(int arg0, String arg1, boolean arg2) {
		System.out.println("\033[0;33m[WEBSOCKET]closed.");
	}

	public void onError(Exception arg0) {
		System.out.println("\033[0;33m[WEBSOCKET]error:\n");
		arg0.printStackTrace();
	}

	public void onMessage(String arg0) {
		if (arg0.length() >= 3) {
			String actually = arg0.substring(3, arg0.length());
			if (arg0.startsWith("RUN"))
				System.out.print(actually);
			if (arg0.startsWith("MSG")) {
				System.out.println("\033[1;32m[REMOTE][INFO]" + actually);
			} else if (arg0.startsWith("ERR")) {
				System.out.println("\033[1;31m[REMOTE][ERR]" + actually + "\n");
			} else if (arg0.startsWith("END")) {
				close();
			}
		}
	}

	public void onOpen(ServerHandshake arg0) {
		System.out.println("\n\033[0;33m[WEBSOCKET]opened.");
	}
}
