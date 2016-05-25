package main;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.websocket.Session;

import org.apache.commons.io.FileUtils;

import optimization.HillClimber;
import components.Person;
import components.Service;
import parsingCode.HopkinsParser;
import services.MailService;


public class HopkinsMain {
	Session session;
	
	public HopkinsMain(Session session){
		this.session = session;
	}
	
	public void run(String args[]) throws IOException{
		System.out.println("started running");
		String directory;
		String name = "Intern_input";
//		if (args.length == 0){
//			directory = "";//"C:\\Users\\Matthew\\Desktop\\Scheduling of Residents\\";
////			name = "JAR_SAR_Input";
//			name = "Intern_Input";
//		} else {
//			directory = args[0];
//			name = args[1];
//		}
//		String file = directory+name + ".xls";
//		String file = "C:\\Users\\Giovanni\\Desktop\\Intern_input.xls";
		File file = findLatestUploadedFile();
		String fileName = file.getAbsolutePath();

		//Create the parser and get all of the information from the file.
		HopkinsParser parser = new HopkinsParser(session);
		parser.parseFile(fileName);
		ArrayList<String> blockNames = parser.getBlockNames();
		ArrayList<Person> people = parser.getPeople();
		ArrayList<Service> services = parser.getServices();
		double[] multipliers = parser.getMultipliers();

		HillClimber solver = new HillClimber(services,people,blockNames,multipliers,name, session);
		solver.solve();
		emailSchedule();
		

	}
	
	public void emailSchedule() throws IOException{
		File file = new File("C:\\Users\\Giovanni\\Documents\\JHU\\");       
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
	}
	
	private File findLatestUploadedFile() throws IOException{
		File file = new File("C:\\Users\\Giovanni\\Documents\\apache-tomcat-8.0.18\\webapps\\data\\");       
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
		return finalFile;
	}
}
