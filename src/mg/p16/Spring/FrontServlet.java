package mg.p16.Spring;

import mg.p16.Spring.Rest_Api;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import jakarta.servlet.http.Part;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
//clarify 

@MultipartConfig()
public class FrontServlet extends HttpServlet {
    private String packageName;
    private static List<String> controllerNames = new ArrayList<>();
    private HashMap<String, Mapping> urlMapping = new HashMap<>();
    String error = "";

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        packageName = config.getInitParameter("packageControllerName");
        try {
            if (packageName == null || packageName.isEmpty()) {
                // 500 Internal Server Error - Package name not specified
                throw new Exception("<p>500 Internal Server Error: Package name not specified in the servlet configuration.</p>");
            }
            scanControllers(packageName);
        } catch (Exception e) {
            error = e.getMessage();
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // // Print the mapping details
        // PrintWriter out = response.getWriter();
        // out.println("<h3>URL Mappings:</h3>");
        // for (Map.Entry<String, Mapping> entry : urlMapping.entrySet()) {
        //     String url = entry.getKey();
        //     Mapping mapping = entry.getValue();
        //     String className = mapping.getClassName();
        //     String methodName = mapping.getVerbAction().getAction();
        //     String httpMethod = mapping.getVerbAction().getVerb();
            
        //     out.println("<p>URL: " + url + "</p>");
        //     out.println("<p>Controller Class: " + className + "</p>");
        //     out.println("<p>Method: " + methodName + " (" + httpMethod.toUpperCase() + ")</p>");
        //     out.println("<hr>");
        // }
        // out.close();

        // Check if the request is a multipart request
        boolean isMultipart = request.getContentType() != null && request.getContentType().startsWith("multipart/form-data");
        
        if (isMultipart) {
            // Handle multipart requests
            request.setAttribute("jakarta.servlet.upload", true); // Indicate that this is a multipart request
        }

        StringBuffer requestURL = request.getRequestURL();
        String[] requestUrlSplitted = requestURL.toString().split("/");
        String controllerSearched = requestUrlSplitted[requestUrlSplitted.length - 1];

        if (!error.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500
            response.getWriter().println(error);
        } else if (!urlMapping.containsKey(controllerSearched)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404
            response.getWriter().println("<p>404 Not Found: No method related to the requested URL.</p>");
        } else {
            try {
                Mapping mapping = urlMapping.get(controllerSearched);
                String methodVerb = mapping.getVerbAction().getVerb();
                String requestMethod = request.getMethod();
                

                if (!requestMethod.equalsIgnoreCase(methodVerb)) {
                    response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED); // 405
                    throw new Exception("<p>405 Method Not Allowed: Expected " + methodVerb + " but got " + requestMethod + "</p>");
                }

                Class<?> clazz = Class.forName(mapping.getClassName());
                Object object = clazz.getDeclaredConstructor().newInstance();
                Method method = findMethod(clazz, requestMethod, mapping.getVerbAction().getAction());

                if (method == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404
                    response.getWriter().println("<p>404 Not Found: No matching method found in the controller class.</p>");
                    return;
                }
                

                Object[] parameters = getMethodParameters(method, request, response, isMultipart);

                // for (int i = 0; i < parameters.length; i++) {
                //     out.println("Parameter " + i + ": " + parameters[i] + " (" + parameter[i].getName() + ")");
                // }


                Object returnValue = method.invoke(object, parameters);//error
                

                handleReturnValue(returnValue,request, response);
            } catch (Exception e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500
                response.getWriter().println("<p>500 Internal Server Error: " + e.getMessage() + "</p>");
            }
        }
    }

    private Method findMethod(Class<?> clazz, String requestMethod, String action) throws NoSuchMethodException {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(action)) {
                if (requestMethod.equalsIgnoreCase("GET") && m.isAnnotationPresent(GET.class)) {
                    return m;
                } else if (requestMethod.equalsIgnoreCase("POST") && m.isAnnotationPresent(AnnotationPost.class)) {
                    return m;
                }
            }
        }
        return null;
    }

    private void handleReturnValue(Object returnValue,HttpServletRequest request, HttpServletResponse response) throws IOException,ServletException {
        if (returnValue instanceof String) {
            response.getWriter().println("Method found in " + (String) returnValue);
        } else if (returnValue instanceof ModelView) {
            ModelView modelView = (ModelView) returnValue;
            for (Map.Entry<String, Object> entry : modelView.getData().entrySet()) {
                request.setAttribute(entry.getKey(), entry.getValue());
            }
            RequestDispatcher dispatcher = request.getRequestDispatcher(modelView.getUrl());
            dispatcher.forward(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500
            response.getWriter().println("<p>500 Internal Server Error: Data type not recognized.</p>");
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

    private void scanControllers(String packageName) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        URL resource = classLoader.getResource(path);

        if (resource == null) {
            // 500 Internal Server Error - Package not found
            throw new Exception("</p> 500 Internal Server Error: Specified package does not exist: " + packageName+ "</p>");
        }

        Path classPath = Paths.get(resource.toURI());
        Files.walk(classPath)
                .filter(f -> f.toString().endsWith(".class"))
                .forEach(f -> {
                    String className = packageName + "." + f.getFileName().toString().replace(".class", "");
                    try {
                        Class<?> clazz = Class.forName(className);
                        if (clazz.isAnnotationPresent(ControllerAnnotation.class)
                                && !Modifier.isAbstract(clazz.getModifiers())) {
                            controllerNames.add(clazz.getSimpleName());
                            Method[] methods = clazz.getMethods();

                            for (Method method : methods) {
                                if (method.isAnnotationPresent(GET.class)) {
                                    VerbAction vb = new VerbAction(method.getName(), "GET");
                                    Mapping map = new Mapping(className, vb);
                                    String valeur = method.getAnnotation(GET.class).value();
                                    if (urlMapping.containsKey(valeur)) {
                                        // 500 Internal Server Error - Duplicate URL mapping for GET
                                        throw new Exception("<p> 500 Internal Server Error: Duplicate URL mapping for GET:"+valeur+"</p>");
                                    } else {
                                        urlMapping.put(valeur, map);
                                    }
                                } else if (method.isAnnotationPresent(AnnotationPost.class)) {
                                    VerbAction vb = new VerbAction(method.getName(), "POST");
                                    Mapping map = new Mapping(className, vb);
                                    String valeur = method.getAnnotation(AnnotationPost.class).value();
                                    if (urlMapping.containsKey(valeur)) {
                                        // 500 Internal Server Error - Duplicate URL mapping for POST
                                        throw new Exception("<p> 500 Internal Server Error: Duplicate URL mapping for POST: "+valeur+" </p>");
                                    } else {
                                        urlMapping.put(valeur, map);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    public Object[] getMethodParameters(Method method, HttpServletRequest request, HttpServletResponse response, boolean isMultipart) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] parameterValues = new Object[parameters.length];
        Map<String, Object> objectInstances = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];

            if (parameter.isAnnotationPresent(Parametre.class)) {
                Parametre param = parameter.getAnnotation(Parametre.class);
                String paramName = param.value();
                if (isMultipart && parameter.getType().equals(Part.class)) {
                    // Retrieve file Part for file upload handling
                    Part filePart=request.getPart(paramName);
                    parameterValues[i] = filePart;
                } else {
                    // Regular parameter
                    
                    parameterValues[i] = request.getParameter(paramName);
                }

            } else if (parameter.isAnnotationPresent(RequestObject.class)) {
                while (parameterNames.hasMoreElements()) {
                    String paramName = parameterNames.nextElement();
                    if (paramName.contains(".")) {
                        String[] parts = paramName.split("\\.");
                        String className = parts[0];
                        String attributeName = parts[1];

                        String fullClassName = "mg.p16.models." + className;
                        Object instance = objectInstances.get(fullClassName);
                        if (instance == null) {
                            Class<?> clazz = Class.forName(fullClassName);
                            instance = clazz.getDeclaredConstructor().newInstance();
                            objectInstances.put(fullClassName, instance);
                        }

                        String attribute = attributeName.substring(0, 1).toUpperCase() + attributeName.substring(1);
                        String methodName = "set" + attribute;

                        Class<?> clazz = instance.getClass();
                        Method setterMethod = clazz.getMethod(methodName, String.class);
                        setterMethod.invoke(instance, request.getParameter(paramName));

                        parameterValues[i] = instance;
                    }
                }
            } else if (parameter.getType().equals(MySession.class)) {
                parameterValues[i] = new MySession(request.getSession());
            } else if (parameter.getType().equals(Part.class)) {
                if (isMultipart) {
                    parameterValues[i] = request.getPart(parameter.getName());
                }
            }
        }
        return parameterValues;
    }

}
