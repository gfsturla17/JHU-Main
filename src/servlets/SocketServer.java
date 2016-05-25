package servlets;

import java.io.IOException;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import main.HopkinsMain;



@ServerEndpoint("/actions")
public class SocketServer {
	boolean empty = false;
	
	@OnOpen
	public void open(Session session) throws Exception {

		empty = false;
		String directory = "";//"C:\\Users\\Matthew\\Desktop\\Scheduling of Residents\\";
		String name = "JAR_SAR_Input.xls";
		String[] args = new String[]{directory,name};
		
		HopkinsMain main = new HopkinsMain(session);
		main.run(args);

	}
	
	@OnClose
	public void close(Session session) {
		if(empty){
			System.out.print("Invalid File");

		}else{
			System.out.print("Session " + session.getId() + " has ended");

		}
	}
	@OnMessage
	public void handleMessage(String message, Session session) {
		System.out.print(message);
		try {
			session.getBasicRemote().sendText("");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

