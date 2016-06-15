package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginServlet extends HttpServlet{
	private static final long serialVersionUID = 1L;

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	 String username = request.getParameter("username");
    	 String password = request.getParameter("password");
	     PrintWriter out = response.getWriter();
	     System.out.println(username + ".");
	     System.out.println(password + ".");
	     
	     if(username.contains("admin") && password.contains("admin")){
	    	 out.print("Success");
	    	 response.sendRedirect("index.html");
	    	 System.out.println("success");

	     }else{
	    	 out.print("Fail");
	    	 System.out.println("fail");
	     }
    }
}
