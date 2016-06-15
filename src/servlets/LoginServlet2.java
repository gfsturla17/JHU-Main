package servlets;

import java.io.IOException;  
import java.io.PrintWriter;  
  
import javax.servlet.ServletException;  
import javax.servlet.http.HttpServlet;  
import javax.servlet.http.HttpServletRequest;  
import javax.servlet.http.HttpServletResponse;  
import javax.servlet.http.HttpSession;  
public class LoginServlet2 extends HttpServlet {  
    protected void doPost(HttpServletRequest request, HttpServletResponse response)  
                    throws ServletException, IOException {  
        response.setContentType("text/html");  
        PrintWriter out=response.getWriter();  
//        request.getRequestDispatcher("index.html").include(request, response);  
          
        String name=request.getParameter("name");  
        String password=request.getParameter("password");  
          
        if(password.equals("admin1") && name.equals("admin")){  
        HttpSession session=request.getSession();  
        session.setAttribute("name",name);
        	out.print("<!DOCTYPE html> <html lang=\"en\"> <head> <meta charset=\"utf-8\"> <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\"> <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"> <title>John Hopkins Scheduling Software</title> <script src=\"js/jquery-1.11.2.min.js\"></script> <script src=\"js/jquery-ui.min.js\"></script> <script src=\"js/jquery.ajaxfileupload.js\"></script> <script src=\"js/d3.min.js\"></script> <script src=\"js/nv.d3.min.js\"></script> <script src=\"js/linegraph.js\"></script> <script src=\"js/main.js\"></script> <link rel=\"Stylesheet\" href=\"css/nv.d3.min.css\" type=\"text/css\" /> <link href=\"css/style.css\" rel=\"stylesheet\"> </head> <body> <div class=\"logo\"> <img src=\"images/tercio_logo.jpg\" alt=\"Tercio Logo\"> </div> <div id = \"main\" class=\"main-container\"> <div class=\"title\"> <p>Johns Hopkins University <br/> Internal Medicine Residency Program <br/> Scheduling Algorithm</p> </div> <div class=\"functional-container\"> <div class=\"left\"> <div class=left-container> <div class=\"upload-container\"> <form action=\"UploadServlet\" method=\"post\" enctype=\"multipart/form-data\"> <input id=\"file\" class=\"uploadButton\" type=\"file\" name=\"datafile\" /> </form> <form> <input id=\"quick\" class=\"quick\" type=\"radio\" name=\"search\" value=\"quick\" checked=\"checked\">Quick Search <input id =\"thorough\" class=\"thorough\" type=\"radio\" name=\"search\" value=\"thorough\">Thorough Search </form> <button class=\"runSched\" id=\"runSched\" type=\"button\">Run Schedule</button> <div class=\"email-container\"> <input class=\"email\" id=\"userEmail\" type=\"text\" name=\"email\" value=\"email\"> <button class=\"emailButton\" id=\"emailSched\" type=\"button\">Send email</button> </div> </div> </div> </div> <div class=\"right\"> <div id=\"spinner\" class=\"cssload-container\"> <div class=\"cssload-progress cssload-float cssload-shadow\"> <div class=\"cssload-progress-item\"></div> </div> </div> <h3 id=\"chartTitle\" class=\"chartTitle\" >Algorithm Process</h3> <div class=\"chart\" id=\"chart\"> <h1></h1> <svg></svg> <h3 id=\"scheduleScore\" class =\"scheduleScore\"></h3> </div> </div> </div> </div> <p class=\"copyright\">Copyright &copy; 2016 Tercio &reg; Solutions LLC</p> </body> </html>");
        }  
        else{  
            out.print("<html lang=\"en\"> <head> <meta charset=\"utf-8\"> <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge,chrome=1\"> <title>Login Form</title> <link rel=\"stylesheet\" href=\"css/login.css\"> <script src=\"js/jquery-1.11.2.min.js\"></script> <script src=\"js/jquery-ui.min.js\"></script> </head> <body> <div class=\"logo\\\"> <img src=\"images/tercio_logo.jpg\" alt=\"Tercio Logo\"> </div> <h1 class=\"portal\"> Portal</h1> <section class=\"container\"> <div class=\"login\"> <form action=\"LoginServlet2\" method=\"post\"> <p> <input id=\"username\" type=\"text\" name=\"name\" value=\"\" placeholder=\"Username\"> </p> <p> <input id=\"password\" type=\"password\" name=\"password\" value=\"\" placeholder=\"Password\"> </p> <p class=\"error\"> Sorry, incorrect username or password error!</p> <input id=\"submit\" value=\"login\" type=\"submit\"> </form> </div> </section> <footer> <p class=\"copyright\">Copyright &copy; 2016 Tercio &reg; Solutions LLC</p> </footer> </body> </html>");  
        }  
        out.close();  
    }  
}  