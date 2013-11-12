package myp2p;

import java.util.*;
import java.io.*;




public class GetFileList {
	public GetFileList() {
	}

	public ArrayList getFileList(String path) {
		ArrayList list = new ArrayList();
		try {
			File file = new File(path);
			String[] filelist = file.list();
			for (int i = 0; i < filelist.length; i++) {
				list.add(filelist[i]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	
	
	
}
