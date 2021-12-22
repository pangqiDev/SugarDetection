package com.example.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Net {
    private URL url;

    public String DownLoad(String urlstr) {
        StringBuffer buf = new StringBuffer();
        String line = null;
        BufferedReader buffer = null;
        try {
            url = new URL(urlstr);
            HttpURLConnection urlcon = (HttpURLConnection) url.openConnection();
            buffer = new BufferedReader(new InputStreamReader(urlcon.getInputStream()));
            while ((line = buffer.readLine()) != null) {
                buf.append(line);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                buffer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return buf.toString();
    }

    /**
     *   * 读取文件，并保存起来
     *   * @param urlStr
     *   * @param path
     *   * @param fileName
     *   * @return -1:下载出错  0:成功    1: 文件已经存在
     *  
     */
    /*public int downFile(String urlStr,String path,String fileName){
		int result=-1;
		InputStream inputStream=null;
		FileUtils fileUtils=new FileUtils();
		try {
			if(fileUtils.isFileExist(path+fileName)){
				return 1;
			}else{
				inputStream=getInputStreamFromUrl(urlStr);
				File resultFile=fileUtils.write2SDFromInput(path, fileName, inputStream);
				if(resultFile==null){
					return 1;
					}
				}
			} catch (IOException e) {
		e.printStackTrace();
		return -1;
	}finally{
		try {
			inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	return 0;
	}*/
    public InputStream getInputStreamFromUrl(String urlStr) throws IOException {
        url = new URL(urlStr);
        HttpURLConnection urlCon = (HttpURLConnection) url.openConnection();
        InputStream inputStream = urlCon.getInputStream();
        return inputStream;
    }
}

