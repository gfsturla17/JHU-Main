package services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;

import org.apache.commons.io.FileUtils;

public class MailService {

/**
 * @param args
 * @throws IOException 
 */
public static void main(String[] args) throws IOException {
    // TODO Auto-generated method stub
    MailService emailer = new MailService();
    //the domains of these email addresses should be valid,
    //or the example will fail:
    
//    emailer.sendEmail("<h1>hey</h1>");
    emailSchedule();
}

/**
  * Send a single email.
  */
	public void sendEmail(String filePath, String userEmail){
		Session mailSession = createSmtpSession();
		mailSession.setDebug (true);

		try {
			Transport transport = mailSession.getTransport ();

			MimeMessage message = new MimeMessage (mailSession);

			message.setSubject ("AUTO EMAIL: New JHU Schedule");
			message.setFrom (new InternetAddress ("gfsturla17@terciosolutions.com"));
			message.addRecipient (Message.RecipientType.TO, new InternetAddress ("gfsturla17@terciosolutions.com"));
			message.addRecipient (Message.RecipientType.TO, new InternetAddress (userEmail));

			
			// Create the message part 
	         BodyPart messageBodyPart = new MimeBodyPart();
	 
	         // Fill the message
	         messageBodyPart.setText("New Schedule Attached Below");
	         
	         // Create a multipar message
	         Multipart multipart = new MimeMultipart();
	 
	         // Set text message part
	         multipart.addBodyPart(messageBodyPart);
	 
	         // Part two is attachment
	         messageBodyPart = new MimeBodyPart();
	         DataSource source = new FileDataSource(filePath);
	         messageBodyPart.setDataHandler(new DataHandler(source));
	         messageBodyPart.setFileName("Schedule.csv");
	         multipart.addBodyPart(messageBodyPart);
	 
	         // Send the complete message parts
	         message.setContent(multipart );
	 
	         // Send message
			transport.connect ();
			transport.sendMessage (message, message.getRecipients (Message.RecipientType.TO));  
		}
		catch (MessagingException e) {
			System.err.println("Cannot Send email");
			e.printStackTrace();
		}
	}

	public static void emailSchedule() throws IOException{
		File file = new File("C:\\Users\\Giovanni\\Documents\\JHU\\");       
		Collection<File> files = FileUtils.listFiles(file, null, true);
		File finalFile = null;
		FileTime latestFileTime = FileTime.from(0,TimeUnit.MILLISECONDS);
		
		for(File file2 : files){
			Path path= file2.toPath();
			BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
			FileTime creationTime = attributes.creationTime();
			
			int compVal = latestFileTime.compareTo(creationTime);
			if(compVal > 0){
				latestFileTime = creationTime;
				finalFile = file2;
			}
			
		    System.out.println(finalFile.getName());            
		} 
	}
	
	private Session createSmtpSession() {
		final Properties props = new Properties();
		props.setProperty ("mail.host", "smtp.gmail.com");
		props.setProperty("mail.smtp.auth", "true");
		props.setProperty("mail.smtp.port", "" + 587);
		props.setProperty("mail.smtp.starttls.enable", "true");
		props.setProperty ("mail.transport.protocol", "smtp");
		// props.setProperty("mail.debug", "true");

		return Session.getInstance(props, new javax.mail.Authenticator() {
  
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("gfsturla17@terciosolutions.com", "12giova34");
			}
		});
	}
}
