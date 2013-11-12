package myp2p;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.vfs2.FileChangeEvent;
import org.apache.commons.vfs2.FileListener;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileMonitor;
import org.apache.commons.io.FileUtils;

public class ClientClass implements Runnable {
	public int clientId;
	public ServerSocket ss;
	public Socket socket;
	public HashMap<Integer, String> fileList;
	public DataOutputStream dos;
	public DataInputStream dis;
	public Gson gson;

	public ClientClass() {
	}

	public ClientClass(int ci) {
		clientId = ci;
	}

	public ClientClass(Socket s) {
		socket = s;
	}

	// initialize a peer
	public void createPeer() {
		try {
			// connect to the index server on port 8101.
			socket = new Socket("127.0.0.1", 8101);
			int socketPort = socket.getLocalPort();
			System.out.println("Peer ID : " + socketPort);
			clientId = socketPort;

			// create a directory named by the clientId, i.e, the connected
			// port.
			String peerPath = "/Users/yangkklt/cs550demo/"
					+ Integer.toString(clientId);
			File file = new File(peerPath);
			if (file.exists()) {
				System.out.println("Already exists.");
			} else {
				file.mkdirs();
			}

			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
			gson = new Gson();

			// initFileMonitor is to automatically update each directory's
			// condition.
			initFileMonitor(peerPath);
			// create a new thread to set up a server socket
			new Thread(new ClientClass(clientId + 2000)).start();

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// get file list in a given input path
	public void getFileList(String inputpath) {
		GetFileList gfl1 = new GetFileList();
		ArrayList al1 = gfl1.getFileList(inputpath);
		fileList = new HashMap<Integer, String>();
		String temp = al1.toString();
		temp = temp.substring(1, temp.length() - 1);
		fileList.put(clientId, al1.toString());
	}

	// Before registry, search and obtain, the client needs to send the
	// corresponding
	// commandindex to the index server.
	public void sendCommandIndex(int command) {
		try {
			dos.writeUTF(Integer.toString(command));
			getFileList("/Users/yangkklt/cs550demo/"
					+ Integer.toString(clientId));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// register the file list to the index server.
	public boolean registry() {
		sendCommandIndex(1);
		try {
			String sendBuffer = gson.toJson(fileList);
			dos.writeUTF(sendBuffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	// search for a given file
	public boolean search() {
		try {

			System.out
					.println("Please input the name of the file you are looking for.");
			Scanner input = null;
			String fileString = "";
			input = new Scanner(System.in);
			String fileName = input.nextLine();
			fileString = fileName;
			String sendBuffer = gson.toJson(fileName);
			dos.writeUTF(sendBuffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		String peerList = "";
		try {
			peerList = gson.fromJson(dis.readUTF(), peerList.getClass());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Peer List is : " + peerList);

		return true;
	}

	// closeSocket
	public boolean closeSocket() {
		try {
			socket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	
	//	obtain firstly execute basic search action, 
	//	and then choose a peer to download the desired file.
	public void obtain() {
		try {
			
			//	input and send the name of the file to the server
			System.out
					.println("Please input the name of the file you are looking for.");
			Scanner input = null;
			String fileString = "";
			input = new Scanner(System.in);
			String fileName = input.nextLine();
			fileString = fileName;
			String sendBuffer = gson.toJson(fileName);
			dos.writeUTF(sendBuffer);

			//	get the peer list
			String peerList = "";
			peerList = gson.fromJson(dis.readUTF(), peerList.getClass());
			System.out.println("Peers who has the given file: ");
			System.out.println(peerList);
			System.out
					.println("Please input the id of the client you want to download from : ");
			input = new Scanner(System.in);
			fileName = input.nextLine();
			int choosePeerId = Integer.parseInt(fileName);
			choosePeerId += 2000;
			Socket peerToPeerSocket = new Socket("127.0.0.1", choosePeerId);

			// send file name
			DataInputStream p2pIn = new DataInputStream(
					peerToPeerSocket.getInputStream());
			DataOutputStream p2pOut = new DataOutputStream(
					peerToPeerSocket.getOutputStream());
			p2pOut.writeUTF(fileString);

			// get file content
			String result = p2pIn.readUTF();
			result = result + "\0";

			// write to local file
			FileWriter fw = new FileWriter("/Users/yangkklt/cs550demo/"
					+ Integer.toString(clientId) + "/" + fileString);

			fw.write(result, 0, result.length());
			fw.flush();
			fw.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	//	this function uses vfs2 to monitor the change of a given directory.
	//	It based on event-driven. file created, delete, change can trigger an event
	//	that is synchronized void updateRegister. And I put the registry in this function
	//	By doing this, each time an event is triggered, the client can do a registry.
	public void initFileMonitor(String filePath) {
		try {
			FileSystemManager fsManager = VFS.getManager();
			FileObject listendir = fsManager.resolveFile(filePath);

			DefaultFileMonitor fm = new DefaultFileMonitor(new FileListener() {
				private synchronized void updateRegister() {
					getFileList("/Users/yangkklt/cs550demo/"
							+ Integer.toString(clientId));
					registry();
				}

				@Override
				public void fileCreated(FileChangeEvent fce) throws Exception {
					this.updateRegister();
				}

				@Override
				public void fileDeleted(FileChangeEvent fce) throws Exception {
					this.updateRegister();
				}

				@Override
				public void fileChanged(FileChangeEvent fce) throws Exception {
					this.updateRegister();
				}
			});
			fm.setRecursive(false);
			fm.addFile(listendir);
			fm.start();
		} catch (FileSystemException ex) {
			Logger.getLogger(ClientClass.class.getName()).log(Level.SEVERE,
					null, ex);
		}

	}

	
	//	this is the way to fork a thread. A new generated thread is responsible to
	//	handle the download request.
	@Override
	public void run() {
		gson = new Gson();
		try {
			System.out.println("Monitoring on Port : " + clientId);
			ss = new ServerSocket(clientId);
			while (true) {
				//	if serversocket gets a request, then continue on.
				socket = ss.accept();
				dis = new DataInputStream(socket.getInputStream());
				dos = new DataOutputStream(socket.getOutputStream());

				// get File Name
				String downloadFileName = dis.readUTF();
				System.out.println("Download file is : " + downloadFileName);

				//	get the localfile location
				String filePath = "/Users/yangkklt/cs550demo/"
						+ Integer.toString(clientId - 2000) + "/"
						+ downloadFileName;

				File file = new File(filePath);


				FileInputStream in = new FileInputStream(filePath);
				File fileTemp = new File("/Users/yangkklt/tempfile");
				FileOutputStream out = new FileOutputStream(fileTemp);
				int c;
				byte buffer[] = new byte[10240];
				//	read the file to a buffer
				int textLength = 0;
				while ((c = in.read(buffer)) != -1) {
					for (int i = 0; i < c; i++) {
						out.write(buffer[i]);
						textLength = i;
					}
				}
				String str = new String(buffer, "UTF-8");
				str = str.substring(0, textLength);
				//	send the buffer to the target.
				dos.writeUTF(str);

			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) throws IOException {
		ClientClass client1 = new ClientClass();
		client1.createPeer();
		client1.getFileList("/Users/yangkklt/cs550demo/"
				+ Integer.toString(client1.clientId));


		Scanner input = null;
		int commandIndex;

		do {

			// user interface
			System.out.println("Please input operation index:");
			System.out.println("1: Registry");
			System.out.println("2: Search");
			System.out.println("3: Obtain");
			System.out.println("4: Quit");
			input = new Scanner(System.in);
			commandIndex = input.nextInt();
			switch (commandIndex) {
			case 1:
				client1.registry();
				break;
			case 2:
				client1.sendCommandIndex(2);
				client1.search();
				break;
			case 3:
				client1.sendCommandIndex(2);
				client1.obtain();
				break;
			}
		} while (commandIndex != 4);
		client1.closeSocket();
	}
}
