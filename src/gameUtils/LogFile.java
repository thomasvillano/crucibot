package gameUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class LogFile {
	
	public static OutputStreamWriter log_file;
	
	public enum Severity {
		INFO,
		WARNING,
		ERROR,
		MESSAGE,
	}
	
	public static void WriteLog(Severity sev, String message) {
		try {
			var now = new SimpleDateFormat("dd/HH:mm:ss::SS").format(new Date());
			var f_message = now + "\t\t" + sev.name() + "\t\t" + message + "\n";
			log_file.write(f_message);
			System.out.println(f_message);
			log_file.flush();
		} catch(Exception e) {
			System.out.println("Error while writing on " + sev.name() + " file");
		}
		
	}
	
	static {
		try {
			Path path = Paths.get("./log/");
			Files.createDirectories(path);
			log_file = new OutputStreamWriter(new FileOutputStream("./log/file_log.txt"), StandardCharsets.UTF_8);
		} catch(Exception e) {
			System.out.println("Unable to open log file ");
		}
		
	}
}
