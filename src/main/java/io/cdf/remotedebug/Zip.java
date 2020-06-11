package io.cdf.remotedebug;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Zip {
	public static byte[] zipFiles(String[] files) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ZipOutputStream out = new ZipOutputStream(baos);
		for (String f : files)
			compress(new File(f), out);
		System.out.println("[ZIP]Compress successful.");
		try {
			if (out != null)
				out.close();
		} catch (Exception e) {
		}
		return baos.toByteArray();
	}

	private static void compress(File srcfile, ZipOutputStream out) throws Exception {
		if (!srcfile.exists())
			return;
		byte[] buf = new byte[1024];
		int len = 0;
		FileInputStream in = new FileInputStream(srcfile);
		out.putNextEntry(new ZipEntry(srcfile.getName()));
		while ((len = in.read(buf)) > 0)
			out.write(buf, 0, len);
		try {
			if (out != null)
				out.closeEntry();
			if (in != null)
				in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void unZipFiles(String zippath, String dir) throws Exception {
		File zipfile = new File(zippath);
		ZipFile zip = new ZipFile(zipfile, Charset.forName("GBK"));
		Enumeration<?> entries = zip.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) entries.nextElement();
			if (entry.isDirectory())
				zipfile.mkdir();
			else {
				File file = new File(dir + "/" + entry.getName());
				if (!file.getParentFile().exists())
					file.getParentFile().mkdirs();
				if (file.exists())
					file.delete();
				file.createNewFile();
				InputStream is = zip.getInputStream(entry);
				FileOutputStream fos = new FileOutputStream(file);
				int len;
				byte[] buf = new byte[1024];
				while ((len = is.read(buf)) != -1) {
					fos.write(buf, 0, len);
				}
				fos.close();
				is.close();
			}
		}
		zip.close();
		zipfile.delete();
		System.out.println("[ZIP]Decompress successful.");
	}
}
