package ie.atu.sw;

//For references see design document attached.

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Scanner;

/*ChatClient class, manages per-client I/O simultaneously. Uses separate thread to listen for messages 
 * as both sending and receiving messages are blocking operations.
 */
public class ChatClient {

	/*
	 * Instance variables for chat clients (socket for connection to server,
	 * buffered reader/writer for reading and writing messages and username for
	 * identifying clients).
	 */
	private Socket clientSocket;
	private BufferedReader clientInput;
	private BufferedWriter clientOutput;
	private String clientUsername;
	private static boolean keepRunning = true;

	/*
	 * Constructor for chat clients, creates new instances of variables for each
	 * client.
	 */
	public ChatClient(Socket socket, String username) {
		try {
			this.clientSocket = socket;
			this.clientOutput = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			this.clientInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			this.clientUsername = username;
		} catch (IOException e) {
			closeAll(clientSocket, clientInput, clientOutput);
		}
	}

	/*
	 * Main method for clients, sets up socket connection. Hostname and port can be
	 * passed as command line parameters or default to hardcoded values. Prompts
	 * client to enter username. Creates new client object with socket connection
	 * and username. Starts client transmitting and receiving messages. If initial
	 * connection is unsuccessful, client retries (default 3) times connection,
	 * before successfully connecting or abandoning connection attempt.
	 */
	public static void main(String[] args) throws UnknownHostException, IOException {
		String host = args.length > 0 ? args[0] : "localhost";
		int port = args.length > 1 ? Integer.parseInt(args[1]) : 1025;
		Socket mainSocket = null;

		int reAttemptTries = 3;
		int currentTry = 0;

		while (currentTry < reAttemptTries && keepRunning) {
			try {
				mainSocket = new Socket(host, port);
			} catch (ConnectException ce) {
				System.out.println("[ERROR] Connection unsuccessful. Retrying...");
				try {
					Thread.sleep(Duration.ofSeconds(3));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				currentTry++;
			}
			if (mainSocket == null) {
				System.out.println("[ERROR] Failed to Connect.");
			} else {
				System.out.println("[ALERT] Connected.");
				try (Scanner mainScanner = new Scanner(System.in)) {
					System.out.println("Enter a username to start the chat.");
					String mainUsername = mainScanner.nextLine();
					ChatClient currentClient = new ChatClient(mainSocket, mainUsername);
					currentClient.receiveMessages();
					currentClient.transmitMessages();

				}
			}
		}
	}

	/*
	 * Client output method, initially sends client username to add client to list
	 * of connected clients and make connection. As long as connection is
	 * maintained, client output is taken from scanner and written to socket
	 * connection, where it is relayed to other clients.
	 */
	public void transmitMessages() {
		try {
			clientOutput.write(clientUsername);
			clientOutput.newLine();
			clientOutput.flush();
			try (Scanner clientScanner = new Scanner(System.in)) {
				while (clientSocket.isConnected()) {
					String clientMessage = clientScanner.nextLine();
					if (clientMessage.equalsIgnoreCase("!quit")) {
						clientOutput.write(clientMessage);
						clientOutput.newLine();
						clientOutput.flush();
						closeAll(clientSocket, clientInput, clientOutput);
						keepRunning = false;
						break;
					}
					clientOutput.write(clientMessage);
					clientOutput.newLine();
					clientOutput.flush();
				}
			}
		} catch (IOException e) {
			closeAll(clientSocket, clientInput, clientOutput);
		}
	}

	/*
	 * Listening method uses separate thread so messages can be sent and received
	 * simultaneously. Creates new thread of anonymous Runnable class which listens
	 * for messages from server as long as connection is maintained.
	 */
	public void receiveMessages() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				String chatMessage;
				while (clientSocket.isConnected()) {
					try {
						chatMessage = clientInput.readLine();
						System.out.println(chatMessage);
					} catch (IOException e) {
						closeAll(clientSocket, clientInput, clientOutput);
					}
				}
			}
		}).start();
	}

	/*
	 * Close open socket and reader/writer associated with this client.
	 * Try-with-resources used for scanners.
	 */
	public void closeAll(Socket socket, BufferedReader clientInput, BufferedWriter clientOutput) {
		try {
			if (clientInput != null) {
				clientInput.close();
			}
			if (clientOutput != null) {
				clientOutput.close();
			}
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
