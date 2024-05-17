package mg.p16.Spring;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import mg.p16.Spring.ControllerAnnotation;

public class FrontServlet extends HttpServlet {
    private String controllerPackage;
    private List<String> controllerNames;
    private boolean scanned = false;

    @Override
    public void init() throws ServletException {
        // Retrieve the controller package initialization parameter
        this.controllerPackage = getServletConfig().getInitParameter("controller-package");
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        // Extract the requested JSP page name from the URL
        String requestedPage = request.getPathInfo();
        out.println("The Path to my Front Servlet:");
        out.println("/WEB-INF/views/jsp" + requestedPage);

        out.println("Scanner:");
        out.println("package controlers:"+this.controllerPackage);
        // Check if scanning has been performed
        if (!scanned) {
            out.println("Scanning ...");
            // Scan controllers and set scanned to true
            controllerNames = scanControllers(this.controllerPackage);
            scanned = true;
        }

        // Print the names of the controller classes
        out.println("Controller classes:");
        for (String controllerName : controllerNames) {
            out.println(controllerName);
        }

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

    public static List<String> scanControllers(String packageName) {
        List<String> controllerNames = new ArrayList<>();

        try {
            // Get the class loader for the current class
            ClassLoader classLoader = FrontServlet.class.getClassLoader();

            // Convert package name to directory path
            String packagePath = packageName.replace('.', '/');

            // Get all resources (files and directories) within the package directory
            java.net.URL resource = classLoader.getResource(packagePath);
            if (resource != null) {
                java.nio.file.Path packageDirectory = java.nio.file.Paths.get(resource.toURI());
                java.io.File[] files = packageDirectory.toFile().listFiles();
                
                // Filter out class files and map them to class names
                if (files != null) {
                    controllerNames = java.util.Arrays.stream(files)
                            .filter(file -> file.getName().endsWith(".class"))
                            .map(file -> file.getName().replaceAll(".class$", ""))
                            .collect(Collectors.toList());
                }
            }
        } catch (java.net.URISyntaxException e) {
            e.printStackTrace();
        }

        return controllerNames;
    }

    public static String[] getClassNames(String packageName) {
        // Get the class loader for the current class
        ClassLoader classLoader = FrontServlet.class.getClassLoader();
    
        // Convert package name to directory path
        String packagePath = packageName.replace('.', '/');
    
        // Get all resources (files and directories) within the package directory
        java.net.URL resource = classLoader.getResource(packagePath);
        if (resource != null) {
            try {
                java.nio.file.Path packageDirectory = java.nio.file.Paths.get(resource.toURI());
                java.io.File[] files = packageDirectory.toFile().listFiles();
    
                // Filter out class files and map them to class names
                if (files != null) {
                    List<String> classNames = java.util.Arrays.stream(files)
                            .filter(file -> file.getName().endsWith(".class"))
                            .map(file -> {
                                String fileName = file.getName();
                                return packageName + "." + fileName.substring(0, fileName.lastIndexOf('.'));
                            })
                            .collect(Collectors.toList());
    
                    // Convert List to String array
                    return classNames.toArray(new String[0]);
                }
            } catch (java.net.URISyntaxException e) {
                e.printStackTrace();
            }
        }
    
        return new String[0]; // Return an empty array if no classes found
    }

}
