package mg.p16.Spring;

import java.io.IOException;
import java.io.PrintWriter;
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

public class FrontServlet extends HttpServlet {
    private String controllerPackage;
    private Map<String, Mapping> urlMappings;
    private Map<String, Mapping> unannotatedMethods;

    @Override
    public void init() throws ServletException {
        this.controllerPackage = getServletConfig().getInitParameter("controller-package");
        this.urlMappings = new HashMap<>();
        this.unannotatedMethods = new HashMap<>();
        scanControllersAndMapUrls(this.controllerPackage);

        // Printing all mappings in urlMappings
        System.out.println("URL Mappings:");
        for (Map.Entry<String, Mapping> entry : urlMappings.entrySet()) {
            System.out.println("URL: " + entry.getKey() + " -> Class: " + entry.getValue().getClassName() + ", Method: " + entry.getValue().getMethodName());
        }

        // Printing all methods in unannotatedMethods
        System.out.println("Unannotated Methods:");
        for (Map.Entry<String, Mapping> entry : unannotatedMethods.entrySet()) {
            System.out.println("Class: " + entry.getKey() + " -> Methods: " + entry.getValue());
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        
        try (PrintWriter out = response.getWriter()) {
            String requestedPath = request.getPathInfo();

            Mapping mapping = urlMappings.get(requestedPath);
            if (mapping != null) {
<<<<<<< Updated upstream
                out.println("Requested URL Path: " + requestedPath + "\n");
                out.println("Mapped to Class: " + mapping.getClassName()+ "\n");
                out.println("Mapped to Method: " + mapping.getMethodName()+ "\n");
=======
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
                } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    out.println("Error while invoking method: " + e.getMessage());
                    e.printStackTrace(out);
                }
>>>>>>> Stashed changes
            } else {
                Mapping unannotatedMapping = unannotatedMethods.get(requestedPath);
                if (unannotatedMapping != null) {
                    out.println("Requested URL Path: " + requestedPath+ "\n");
                    out.println("Mapped to Unannotated Method in Class: " + unannotatedMapping.getClassName()+ "\n");
                    out.println("Mapped to Method: " + unannotatedMapping.getMethodName()+ "\n");
                } else {
                    out.println("No method associated with the path: " + requestedPath+ "\n");
                }
            }
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

    private void scanControllersAndMapUrls(String packageName) {
        List<Class<?>> controllerClasses = scanControllers(packageName);
        for (Class<?> controllerClass : controllerClasses) {
            Method[] methods = controllerClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(GET.class)) {
                    GET getAnnotation = method.getAnnotation(GET.class);
                    String urlPath = getAnnotation.value();
                    urlMappings.put(urlPath, new Mapping(controllerClass.getName(), method.getName()));
                } else {
                    // Assume the method name will be used as a URL path for unannotated methods
                    String urlPath = "/" + method.getName();
                    unannotatedMethods.put(urlPath, new Mapping(controllerClass.getName(), method.getName()));
                }
            }
        }
    }

    public static List<Class<?>> scanControllers(String packageName) {
        List<Class<?>> controllerClasses = new ArrayList<>();
        try {
            ClassLoader classLoader = FrontServlet.class.getClassLoader();
            String packagePath = packageName.replace('.', '/');
            java.net.URL resource = classLoader.getResource(packagePath);
            if (resource != null) {
                Path packageDirectory = Paths.get(resource.toURI());
                List<String> classNames = Files.walk(packageDirectory)
                        .filter(Files::isRegularFile)
                        .filter(file -> file.getFileName().toString().endsWith(".class"))
                        .map(file -> packageName + "." + file.getFileName().toString().replace(".class", ""))
                        .collect(Collectors.toList());
                for (String className : classNames) {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(ControllerAnnotation.class)) {
                        controllerClasses.add(clazz);
                    }
                }
            }
        } catch (URISyntaxException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return controllerClasses;
    }
}
