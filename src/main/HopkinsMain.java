package main;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
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
	boolean fastSchedule;
	
	public HopkinsMain(Session session, boolean fastSchedule){
		this.session = session;
		this.fastSchedule = fastSchedule;
	}
	
	public void run() throws IOException, ParseException{
		System.out.println("started running");
		String directory;
		String name = "Intern_input";

		File file = findLatestUploadedFile();
		String fileName = file.getAbsolutePath();
		
		double bestScheduleSoFar = Double.NEGATIVE_INFINITY;
		ArrayList<Double> scoreList = new ArrayList<Double>();
		long timeOut = 1*24*60*60*1000; // 1 day * 24 hr/day * 60 min/hr * 60 sec/min * 1000 ms/sec 
		long startTime = System.currentTimeMillis();
		while (true){

			//Create the parser and get all of the information from the file.
			HopkinsParser parser = new HopkinsParser(session);
			parser.parseFile(fileName);
			ArrayList<String> blockNames = parser.getBlockNames();
			ArrayList<Person> people = parser.getPeople();
			ArrayList<Service> services = parser.getServices();
			double[] multipliers = parser.getMultipliers();

			
			HillClimber solver = new HillClimber(services,people,blockNames,multipliers,name,fastSchedule,bestScheduleSoFar,session, scoreList);
			scoreList = solver.solve();
		
			if (fastSchedule){
				break;
			} else {
				if (System.currentTimeMillis() - startTime > timeOut){
					break;
				}
			}
		}
		emailSchedule();
		session.getBasicRemote().sendText("Perform 2-opt (2 people x 2 Blocks)~100");

	}
	
	public void emailSchedule() throws IOException{
		File file = new File("/home/ubuntu/Tercio/JHU/");       
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
		File file = new File("/var/lib/tomcat7/webapps/data"); 
//		File file = new File("C:\\Tercio\\Documents\\apache-tomcat-7.0.64\\webapps\\data"); 

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
