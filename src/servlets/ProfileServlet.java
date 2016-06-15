/*
 * COPYRIGHT (C) 2015 Tercio Solutions. All Rights Reserved.u00A9"
 */
package servlets;

import java.io.IOException;  
import java.io.PrintWriter;  
import javax.servlet.ServletException;  
import javax.servlet.http.HttpServlet;  
import javax.servlet.http.HttpServletRequest;  
import javax.servlet.http.HttpServletResponse;  
import javax.servlet.http.HttpSession;  
public class ProfileServlet extends HttpServlet {  
    protected void doGet(HttpServletRequest request, HttpServletResponse response)  
                      throws ServletException, IOException {  
        response.setContentType("text/html");  
        PrintWriter out=response.getWriter();  
        request.getRequestDispatcher("link.html").include(request, response);  
          
        HttpSession session=request.getSession(false);  
        if(session!=null){  
        String name=(String)session.getAttribute("name");  
          
        out.print("<!DOCTYPE html> <html lang=\"en\"> <head> <meta charset=\"utf-8\"> <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\"> <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"> <title>John Hopkins Scheduling Software</title> <script src=\"js/jquery-1.11.2.min.js\"></script> <script src=\"js/jquery-ui.min.js\"></script> <script src=\"js/jquery.ajaxfileupload.js\"></script> <script src=\"js/d3.min.js\"></script> <script src=\"js/nv.d3.min.js\"></script> <script src=\"js/linegraph.js\"></script> <script src=\"js/main.js\"></script> <link rel=\"Stylesheet\" href=\"css/nv.d3.min.css\" type=\"text/css\" /> <link href=\"css/style.css\" rel=\"stylesheet\"> </head> <body> <div class=\"logo\"> <img src=\"images/tercio_logo.jpg\" alt=\"Tercio Logo\"> </div> <div id = \"main\" class=\"main-container\"> <div class=\"title\"> <p>John Hopkins Scheduling Software</p> </div> <div class=\"functional-container\"> <div class=\"left\"> <div class=left-container> <div class=\"upload-container\"> <form action=\"UploadServlet\" method=\"post\" enctype=\"multipart/form-data\"> <input id=\"file\" class=\"uploadButton\" type=\"file\" name=\"datafile\" /> <br /> <button class=\"submitButton\" id=\"uploadButton\" type=\"button\">Upload File</button> </form> </div> <div class=\"radio-container\"> <form> <input id=\"quick\" class=\"quick\" type=\"radio\" name=\"search\" value=\"quick\">Quick Search <input id =\"thorough\" class=\"thorough\" type=\"radio\" name=\"search\" value=\"thorough\">Thorough Search </form> </div> <button class=\"runSched\" id=\"runSched\" type=\"button\">Run Schedule</button> </div> </div> <div class=\"right\"> <div id=\"spinner\" class=\"cssload-container\"> <div class=\"cssload-progress cssload-float cssload-shadow\"> <div class=\"cssload-progress-item\"></div> </div> </div> <div class=\"chart\" id=\"chart\"> <svg></svg> </div> </div> </div> <div class=\"email-container\"> <input class=\"email\" id=\"userEmail\" type=\"text\" name=\"email\" value=\"email\"> <button class=\"emailButton\" id=\"emailSched\" type=\"button\">Send email</button> </div> </div> <p class=\"copyright\"> Tercio Solutions</p> </body> </html>");
        }  
        else{  
            out.print("Please login first");  
            request.getRequestDispatcher("login2.html").include(request, response);  
        }  
        out.close();  
    }  
}  