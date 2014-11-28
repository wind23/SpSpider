package com.wind23.spspider;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;


public class SpSpider {
	static String website;
	static Queue<String> urlQueue;
	static Set<String> completeSet;
	static Set<String> errorSet;
	
	public static void main(String[] args) {
		System.out.print("Website:\t");
		Scanner stdin = new Scanner(System.in);
		website = stdin.nextLine();
		stdin.close();
		urlQueue = new LinkedBlockingQueue<String>();
		urlQueue.add("http://"+website+"/");
		completeSet = new HashSet<String>();
		errorSet = new HashSet<String>();
		while(!urlQueue.isEmpty()) {
			String urlString = urlQueue.poll();
			if(completeSet.contains(urlString)||errorSet.contains(urlString)) {
				continue;
			}
			System.out.print(urlString);
			try {
				URL url = new URL(urlString);
				HttpURLConnection conn=(HttpURLConnection)url.openConnection();
				conn.setConnectTimeout(10*1000);
				conn.setRequestMethod("GET");
				conn.setReadTimeout(10*1000);
				conn.setDoOutput(true);
				conn.setDoInput(true);
				conn.setUseCaches(false);
				conn.setInstanceFollowRedirects(true);
				conn.connect();
				InputStream is = conn.getInputStream();
				byte buffer[] = new byte[4096];
				String readString = "";
				int prefixLength = 0;
				if(urlString.startsWith("http:")) {
					prefixLength += 7;
				}
				else if(urlString.startsWith("https:")) {
					prefixLength += 8;
				}
				else if(urlString.startsWith("ftp:")) {
					prefixLength += 6;
				}
				String fileName = urlString.substring(prefixLength);
				String [] filePath = fileName.split("/");
				for(int i=0;i<filePath.length;i++) {
					filePath[i] = URLDecoder.decode(filePath[i],"utf-8");
					String curString = "";
					for(int j=0;j<filePath[i].length();j++) {
						if(filePath[i].charAt(j)=='\\'||filePath[i].charAt(j)=='/'||filePath[i].charAt(j)==':'||filePath[i].charAt(j)=='*'||filePath[i].charAt(j)=='?'||filePath[i].charAt(j)=='\"'||filePath[i].charAt(j)=='<'||filePath[i].charAt(j)=='>'||filePath[i].charAt(j)=='|') {
							curString += URLEncoder.encode(filePath[i].substring(j,j+1),"utf-8");
						}
						else {
							curString += filePath[i].charAt(j);
						}
					}
					filePath[i] = curString;
				}
				String path = "output";
				File outDic = new File(path);
				outDic.mkdirs();
				if(filePath.length==1 || (filePath.length==2 && filePath[1].equals(""))) {
					String [] filePath1 = new String [2];
					filePath1[0] = filePath[0];
					filePath1[1] = "index.html";
					filePath = filePath1;
				}
				if(!filePath[filePath.length-1].contains(".")) {
					filePath[filePath.length-1] += ".html";
				}
				
				for(int i=0;i<filePath.length-1;i++) {
					path += "\\" + filePath[i];
					outDic = new File(path);
					outDic.mkdirs();						
				}
				path += "\\" + filePath[filePath.length-1];
				
				FileOutputStream fos = new FileOutputStream(path,false);
				while(true) {
					int readLength = is.read(buffer,0,4096);
					if(readLength>0) {
						fos.write(buffer,0,readLength);
						readString += new String(buffer,0,readLength,"utf-8");
					}
					else {
						break;
					}
				}

				try {
					is.close();
					fos.close();
				} catch (IOException e1) {
					System.out.println("\tFail.");
				}
				
				Pattern pattern = Pattern.compile("\\s(?:src|href)=(?:|'|\")([^'|\"|>|\\s]+)(?:'|\"|>|\\s)");
				Matcher matcher = pattern.matcher(readString);

				while(matcher.find()) {
					String matchString = matcher.group(1);
					matchString = Jsoup.parse(matchString).text();
					String convertString = "";
					for(int i=0;i<matchString.length();i++) {
						String currentChar = matchString.substring(i, i+1);
						if(currentChar.charAt(0)>=256) {
							convertString += URLEncoder.encode(currentChar,"utf-8");
						}
						else {
							convertString += currentChar;
						}
					}
					matchString = convertString;
					String enqueueUrl = null;

					if(matchString.startsWith("http://")) {
						enqueueUrl = matchString;
					}
					else if(matchString.startsWith("https://")) {
						enqueueUrl = matchString;
					}
					else if(matchString.startsWith("ftp://")) {
						enqueueUrl = matchString;
					}
					else if(matchString.startsWith("//")) {
						enqueueUrl = "http:"+matchString;
					}
					else if(matchString.startsWith("/")) {
						enqueueUrl = "http://"+website+matchString;
					}
					else if(matchString.startsWith("javascript:")) {
					}
					else if(matchString.startsWith("mailto:")) {
					}
					else {
						enqueueUrl = "http://"+website+"/"+matchString;
					}
					if(enqueueUrl!=null) {
						if(!completeSet.contains(enqueueUrl)&&!errorSet.contains(enqueueUrl)) {
							int prefixLength1 = 0;
							if(urlString.startsWith("http:")) {
								prefixLength1 = 7;
							}
							else if(urlString.startsWith("https:")) {
								prefixLength1 = 8;
							}
							else if(urlString.startsWith("ftp:")) {
								prefixLength1 = 6;
							}
							String url1 = enqueueUrl.substring(prefixLength1);
							if(url1.startsWith(website)) {
								urlQueue.add(enqueueUrl);
							}
						}
					}
				}
				
				System.out.println("\t--> "+path);
				completeSet.add(urlString);
			}
			catch(Exception e) {
				e.printStackTrace();
				try {
					errorSet.add(urlString);
					FileOutputStream fos = new FileOutputStream("error.log",true);
					OutputStreamWriter osw = new OutputStreamWriter(fos);
					BufferedWriter bw = new BufferedWriter(osw);
					try {
						bw.write(urlString+"\r\n");
						bw.close();
					} catch (IOException e1) {
						System.out.println("\tFail.");
					}
				} catch (FileNotFoundException e1) {
					System.out.println("\tFail.");
				}
			}
		}
	}
}
