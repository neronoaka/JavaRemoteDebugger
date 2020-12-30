package tech.cdf.remotedebug;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class Client extends WebSocketClient {

	public Client(URI serverUri) {
		super(serverUri);
	}

	@Override
	public void onClose(int arg0, String arg1, boolean arg2) {
		System.out.println("[WEBSOCKET]closed.");
	}

	@Override
	public void onError(Exception arg0) {
		System.out.println("[WEBSOCKET]error:\n");
		arg0.printStackTrace();
	}

	@Override
	public void onMessage(String arg0) {
		String actually = arg0.substring(3, arg0.length());
		if (arg0.startsWith("RUN"))
			System.out.print(actually);
		if (arg0.startsWith("MSG"))
			System.out.println("[REMOTE][INFO]" + actually);
		else if (arg0.startsWith("ERR"))
			System.out.println("[REMOTE][ERR]" + actually + "\n");
		else if (arg0.startsWith("END"))
			this.close();
	}

	@Override
	public void onOpen(ServerHandshake arg0) {
		System.out.println("[WEBSOCKET]opened.");
	}

}
