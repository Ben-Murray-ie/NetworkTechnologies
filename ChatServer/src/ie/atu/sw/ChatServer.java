package ie.atu.sw;

//For references see design document attached.

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

// ChatServer class, manages server, creating new client connections.
public class ChatServer {

	// Chat server socket listens on designated port for new connections.
	private ServerSocket chatServerSocket;

	// Constructor for new instances of chat server.
	public ChatServer(ServerSocket serverSocket) {
		this.chatServerSocket = serverSocket;
	}

	/*
	 * Method passes newly connected clients to client managing class, each assigned
	 * a new thread.
	 */
	public void createClientConnection() {
		try {
			while (!chatServerSocket.isClosed()) {
				// When a new connection is made, announce in console and create new object to
				// handle client connection.
				Socket socket = chatServerSocket.accept();
				System.out.println("A new client has entered the chat.");
				ClientManager clientManager = new ClientManager(socket);

				// Start a new thread to handle each individual client.
				Thread thread = new Thread(clientManager);
				thread.start();
			}
			closeServerSocket();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// Utility method. When session is finished, close serverSocket.
	public void closeServerSocket() {
		try {
			if (chatServerSocket != null) {
				chatServerSocket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Main method, runs server program, allows user to specify port as command line
	 * parameter, or defaults to hardcoded port. Allows port to be reused
	 * immediately after closing server socket.
	 */
	public static void main(String[] args) throws IOException {

		int port = args.length > 0 ? Integer.parseInt(args[0]) : 1025;

		ServerSocket serverSocket = new ServerSocket(port);
		serverSocket.setReuseAddress(true);
		if (!serverSocket.isBound()) {
			System.out.println("[ERROR] Server Socket not bound. Please try again.");
		}
		System.out.println("[SERVER] Online. Listening on port: " + port);
		ChatServer chatServer = new ChatServer(serverSocket);
		chatServer.createClientConnection();
	}

	/*
	 * Client manager inner class oversees client input/output (message read/write),
	 * tracks connected clients and closes connections and all associated I/O.
	 */
	private class ClientManager implements Runnable {

		/*
		 * Instance variables for each client. List of connected clients (for
		 * broadcasting messages to all users), socket for client side connection,
		 * buffered reader/writer for input and output and username to identify each
		 * client and add to connected clients list.
		 */
		public static ArrayList<ClientManager> connectedClients = new ArrayList<>();
		private Socket socket;
		private BufferedReader input;
		private BufferedWriter output;
		private String username;

		/*
		 * Constructor for ClientManager, creates new instances of client related
		 * variables, gets client username, declares client entered chat.
		 */
		public ClientManager(Socket socket) {
			try {
				this.socket = socket;
				this.output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				this.username = input.readLine();
				connectedClients.add(this);
				relayMessage("has entered the chat.");
			} catch (IOException e) {
				closeAll(socket, input, output);
			}
		}

		/*
		 * Inherited run() method from Runnable, broadcasts client messages across
		 * server as long as connection is active.
		 */
		@Override
		public void run() {
			String clientMessage;
			while (socket.isConnected()) {
				try {
					clientMessage = input.readLine();
					if (clientMessage.equalsIgnoreCase("!quit")) {
						removeClientManager();
						break;
					}
					relayMessage(clientMessage);
				} catch (IOException e) {
					closeAll(socket, input, output);
					break;
				}
			}
		}

		/*
		 * Method to broadcast messages to all connected client (current client
		 * excluded). If keyword is used, client is removed from chat and open I/O is
		 * closed.
		 */
		public void relayMessage(String message) {
			for (ClientManager clientManager : connectedClients) {
				try {
					if (!clientManager.username.equals(username)) {
						clientManager.output.write(username + ": " + message);
						clientManager.output.newLine();
						clientManager.output.flush();
					}
				} catch (IOException e) {
					closeAll(socket, input, output);
				}
			}
		}

		// Remove client from chat and announce user has left.
		public void removeClientManager() {
			connectedClients.remove(this);
			relayMessage("has left the chat.");
		}

		// Close open socket and reader/writer associated with this client manager.
		public void closeAll(Socket socket, BufferedReader input, BufferedWriter output) {
			removeClientManager();
			try {
				if (input != null) {
					input.close();
				}
				if (output != null) {
					output.close();
				}
				if (socket != null) {
					socket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
