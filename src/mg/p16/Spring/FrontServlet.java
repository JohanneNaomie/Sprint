package mg.p16.Spring;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
// javac -cp "C:\Program Files\Apache Software Foundation\Tomcat 10.1\webapps\Spring\WEB-INF\lib\*" FrontServlet.java
public class FrontServlet extends HttpServlet {
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        // Extract the requested JSP page name from the URL
        String requestedPage = request.getPathInfo();
        
        // Forward the request to the appropriate JSP page
        // request.getRequestDispatcher("/web/page" + requestedPage).forward(request, response);
        out.println("/WEB-INF/views/jsp" + requestedPage);
        out.close();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "FrontServlet";
    }
}
