package com.dbexport.util;

import com.kmood.utils.StringUtil;
import org.apache.commons.io.output.FileWriterWithEncoding;

import java.io.*;

public class FileUtils {


	public static byte[] readToBytesByFilepath(String filePath)throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		FileInputStream reader  =  null;
		try {
			File file = new File(filePath);
			reader = new FileInputStream(file);
			int len = 0;
			byte[] buffer = new byte[1024];
			while ((len = reader.read(buffer)) != -1){
				output.write(buffer, 0, len);
			}
		} finally {
			if (reader != null)
				reader.close();
		}
		return output.toByteArray();
	}
	/**
	 * 
	 * @Title readToStringByFilepath
	 * @Description TODO
	 * @param filePath
	 * @return
	 * @throws Exception
	 * @author SunBC
	 * @time 2018年10月16日 下午6:53:31
	 */
	public static String readToStringByFilepath(String filePath)throws Exception{
		String data ="";
		Reader reader  =  null;
		try {
			File file = new File(filePath);
			reader = new FileReader(file);
			char [] c = new char [1024];
			int len = 0;
			while ((len = reader.read(c)) != -1){
				data += String.copyValueOf(c,0,len);
			}
		} catch (Exception e) {
			throw e;
		}finally {
			reader.close();
		}
		return data;
	}
	/**
	 * 
	 * @Title generatorFileByType
	 * @Description TODO
	 * @param fileType 文件类型 如 pdf
	 * @param fileDirPath 文件所在目录
	 * @param filePrefix 导出文件名的前缀
	 * @param data 数据
	 * @return
	 * @throws IOException
	 * @author SunBC
	 * @time 2018年10月27日 上午11:24:33
	 */
	public static  String generatorFileByType(String fileType, String fileDirPath, String filePrefix, String data)
			throws IOException {
		File dir = new File(fileDirPath); 
		if(!dir.exists()) dir.mkdirs();
		String filename = filePrefix+System.currentTimeMillis()+"."+fileType;
		String shpJsonFilePath = fileDirPath + File.separator +filename;
		File shpJsonFile = new File(shpJsonFilePath);
		if(!shpJsonFile.exists()) shpJsonFile.createNewFile();
		FileWriterWithEncoding jsonFileWriter = null;
		jsonFileWriter = new FileWriterWithEncoding(shpJsonFile, "utf-8", false);
		jsonFileWriter.write(data);
		jsonFileWriter.flush();
		String relativePath = File.separator +"resources"+StringUtil.substringAfter(shpJsonFilePath, "resources");
		return relativePath;
	}
}
