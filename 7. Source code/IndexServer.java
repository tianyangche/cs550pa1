/*
 *
 * Copyright 2013 Yifan Liu & Tianyang Che.
 * @author Tianyang Che & Yifan Liu
 */

package myp2p;

import java.io.*;
import java.net.*;
import java.util.*;

import com.google.gson.Gson;

public class IndexServer implements Runnable {
	// member description:
	// Socket: store current socket to a connected client
	// ServerSocket: store the listening server socket
	// localFileList: a hash map used to store temp file list of a client.
	// globalFileList: a hash map used to store file lists of all clients.
	// dos & dis: input and out stream
	// gson: a tool used to send message.

	public Socket socket;
	public ServerSocket ss;
	public HashMap<Integer, String> localFileList = new HashMap<Integer, String>();
	public static HashMap<Integer, String> globalFileList = new HashMap<Integer, String>();
	public DataOutputStream dos;
	public DataInputStream dis;
	public Gson gson;

	public IndexServer() {
	}

	public IndexServer(Socket s) {
		this.socket = s;
	}

	// initialize index server
	public void createIndexServer() {
		try {
			ss = new ServerSocket(8101);
			socket = ss.accept();
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
			gson = new Gson();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// create file list
	public void getFileList() {
		try {
			// convert received message (String ) to a hash map objective
			localFileList = gson.fromJson(dis.readUTF(),
					localFileList.getClass());

			// insert global file list here.
			int tempPeer;
			String tempList;

			// for loop used to get each single hash map entry,
			// and then put them in the globalfilelist
			for (Map.Entry me : localFileList.entrySet()) {
				tempPeer = Integer.parseInt(me.getKey().toString());
				tempList = me.getValue().toString();
				globalFileList.put(tempPeer, tempList);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//	return peer's ID who has a given file
	public String returnSearchResult() {
		String result = "";
		String searchFileName = "";
		try {
			searchFileName = gson.fromJson(dis.readUTF(),
					searchFileName.getClass());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		//	for loop uses to search each entry in the hash map to find which peer
		//	has the given file
		String tempItem = "";
		for (Map.Entry me : globalFileList.entrySet()) {
			tempItem = me.getValue().toString();
			tempItem = tempItem.substring(1, tempItem.length() - 1);
			List<String> myList = new ArrayList<String>(Arrays.asList(tempItem
					.split(", ")));
			//	inner for loop is to parse each value to an array, and compare each
			//	element in the array with a given file, if matches, store the corresponding
			//	key
			for (int i = 0; i < myList.size(); i++) {
				if (myList.get(i).equals(searchFileName) == true) {
					System.out.println("Find target...");
					result = result + me.getKey() + "-";
				}
			}
		}
		System.out.println("Perrs who contain this file : " + result);

		try {
			String sendBuffer = gson.toJson(result);
			dos.writeUTF(sendBuffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	// display file list
	public void displayFileList() {
		for (Map.Entry me : globalFileList.entrySet()) {
			System.out.println(me.getKey() + "   " + me.getValue());
		}
	}

	@Override
	// 	override the run function. This is to fork a thread, 
	//	and handle the request from a new connected client.
	public void run() {
		gson = new Gson();
		try {
			System.out.println("Index Server New Thread");
			String commandIndexString = "";
			int commandIndex;
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());
			localFileList = new HashMap<Integer, String>();
			
			
			// choose operation according to the input command index
			do {
				commandIndexString = dis.readUTF();
				commandIndex = Integer.parseInt(commandIndexString);
				switch (commandIndex) {
				case 1:
					getFileList();
					displayFileList();
					break;
				case 2:
					returnSearchResult();
					break;
				case 3:
					returnSearchResult();
					break;
				}
			} while (commandIndex != 4);

			if (socket != null)
				socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean closeSocket() {
		try {
			socket.close();
			ss.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	public static void main(String[] args) {
		try {

			// Set server socket listening port. we put it as 8101
			ServerSocket ss = new ServerSocket(8101);
			System.out.println("Index Server is Listening...");
			System.out.println("on port 8101");

			// a while loop to create a thread whenever 8101 port got a request.
			while (true) {
				new Thread(new IndexServer(ss.accept())).start();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
