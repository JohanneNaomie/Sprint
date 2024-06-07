package mg.p16.Spring;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mg.p16.Spring.ResourceNotFoundException;
import mg.p16.Spring.Scanner;
public class FrontServlet extends HttpServlet {
    private String controllerPackage;
    private Map<String, Mapping> urlMappings;

    @Override
    public void init() throws ServletException {
        this.controllerPackage = getServletConfig().getInitParameter("controller-package");
        this.urlMappings = new HashMap<>();

    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try {
            this.urlMappings=Scanner.scanControllersAndMapUrls(this.controllerPackage, urlMappings);
        } catch (PackageNotFoundException | MultipleMethodsException e) {
            handleException(request, response, "ERROR: " + e.getMessage());
            return;
        }
        
        // Printing all mappings in urlMappings
        System.out.println("URL Mappings:");
        for (Map.Entry<String, Mapping> entry : urlMappings.entrySet()) {
            System.out.println("URL: " + entry.getKey() + " -> Class: " + entry.getValue().getClassName() + ", Method: " + entry.getValue().getMethodName());
        }

        try (PrintWriter out = response.getWriter()) {
            String requestedPath = request.getPathInfo();
            System.out.println("Requested Path: " + requestedPath);

            Mapping mapping = urlMappings.get(requestedPath);
            if (mapping == null) {
                throw new ResourceNotFoundException("No Method: " + requestedPath);
            }

            out.println("Requested URL Path: " + requestedPath);
            out.println("Mapped to Class: " + mapping.getClassName());
            out.println("Mapped to Method: " + mapping.getMethodName());

            try {
                // Load the class
                Class<?> clazz = Class.forName(mapping.getClassName());

                // Create an instance of the class
                Object instance = clazz.getDeclaredConstructor().newInstance();

                // Get the method to invoke
                Method method = clazz.getDeclaredMethod(mapping.getMethodName());

                // Invoke the method and get the result
                Object result = method.invoke(instance);

                if (result instanceof String) {
                    // If the result is a String, print it
                    out.println("Result from invoked method: " + result);
                } else if (result instanceof ModelView) {
                    // If the result is a ModelView, forward to the specified URL
                    ModelView mv = (ModelView) result;
                    for (Map.Entry<String, Object> entry : mv.getData().entrySet()) {
                        request.setAttribute(entry.getKey(), entry.getValue());
                    }
                    RequestDispatcher dispatcher = request.getRequestDispatcher(mv.getUrl());
                    dispatcher.forward(request, response);
                    return;
                }
            } catch (ClassNotFoundException e) {
                handleException(request, response, "Method not found: " + e.getMessage());
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException  | NoSuchMethodException e) {
                handleException(request, response, "Error while invoking method: " + e.getMessage());
            }
        } catch (Exception e) {
            handleException(request, response, "No method associated with the path: " + e.getMessage());
        }
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

    public void handleException(HttpServletRequest request, HttpServletResponse response, String errorMessage) throws ServletException, IOException {
        request.setAttribute("error", errorMessage);
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/views/error.jsp");
        dispatcher.forward(request, response);
    }
}
