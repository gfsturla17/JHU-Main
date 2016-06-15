package servlets;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;

import services.MailService;


public class EmailServlet extends HttpServlet{
	 /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	 String email = request.getParameter("userEmail");
	     PrintWriter out = response.getWriter();
	 
	     MailService mailService = new MailService();
	     
	     File file = new File("/home/ubuntu/Tercio/JHU");       
			Collection<File> files = FileUtils.listFiles(file, null, true);
			File finalFile = null;
			FileTime latestFileTime = FileTime.from(0,TimeUnit.MILLISECONDS);
			
			for(File file2 : files){
				Path path= file2.toPath();
				BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
				FileTime creationTime = attributes.creationTime();
				
				int compVal = latestFileTime.compareTo(creationTime);
				if(compVal < 0){
					latestFileTime = creationTime;
					finalFile = file2;
				}
				
			} 
	     mailService.sendEmail(finalFile.getPath(), email);
    }
    
 
 

}

