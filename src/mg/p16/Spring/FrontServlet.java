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

import jakarta.servlet.ServletConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
//clarify 
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
                throw new Exception("Package name not specified");
            }
            scanControllers(packageName);
        } catch (Exception e) {
            error = e.getMessage();
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        StringBuffer requestURL = request.getRequestURL();
        String[] requestUrlSplitted = requestURL.toString().split("/");
        String controllerSearched = requestUrlSplitted[requestUrlSplitted.length - 1];

        if (!error.isEmpty()) {
            response.getWriter().println(error);
        } else if (!urlMapping.containsKey(controllerSearched)) {
            response.getWriter().println("<p>No method related .</p>");
        } else {
            try {
                Mapping mapping = urlMapping.get(controllerSearched);
                Class<?> clazz = Class.forName(mapping.getClassName());
                Object object = clazz.getDeclaredConstructor().newInstance();
                Method method = null;

                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.getName().equals(mapping.getMethodeName())) {
                        if (request.getMethod().equalsIgnoreCase("GET") && m.isAnnotationPresent(GET.class)) {
                            method = m;
                            break;
                        } else if (request.getMethod().equalsIgnoreCase("POST") && m.isAnnotationPresent(AnnotationPost.class)) {
                            method = m;
                            break;
                        }
                    }
                }

                if (method == null) {
                    response.getWriter().println("<p>No Method Found</p>");
                    return;
                }
                // response.getWriter().println("<p>get method param</p>");
                Object[] parameters = getMethodParameters(method, request, response);
                // response.getWriter().println("<p>invoke </p>");
                Object returnValue = method.invoke(object, parameters);
                // response.getWriter().println("<p>invoked</p>");
                // Check if the method is a REST API
                if (method.isAnnotationPresent(Rest_Api.class)) {
                    // Set the response content type to JSON
                    response.setContentType("application/json");
                    Gson gson = new Gson();
                    String jsonResponse = gson.toJson(returnValue);
                    response.getWriter().write(jsonResponse);
                }
                else if (returnValue instanceof String) {
                    response.getWriter().println("Méthode trouvée dans " + (String) returnValue);
                } else if (returnValue instanceof ModelView) {
                    ModelView modelView = (ModelView) returnValue;
                    for (Map.Entry<String, Object> entry : modelView.getData().entrySet()) {
                        request.setAttribute(entry.getKey(), entry.getValue());
                    }
                    RequestDispatcher dispatcher = request.getRequestDispatcher(modelView.getUrl());
                    dispatcher.forward(request, response);
                } else {
                    response.getWriter().println("Data type not recognized");
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.getWriter().println("<p>"+e.getMessage() +"</p><p>An exception came up during the transaction .</p>");
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

    private void scanControllers(String packageName) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        URL resource = classLoader.getResource(path);

        if (resource == null) {
            throw new Exception("Package non-existent: " + packageName);
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
                                    Mapping map = new Mapping(className, method.getName());
                                    String valeur = method.getAnnotation(GET.class).value();
                                    if (urlMapping.containsKey(valeur)) {
                                        throw new Exception("doublant url" + valeur);
                                    } else {
                                        urlMapping.put(valeur, map);
                                    }
                                } else if (method.isAnnotationPresent(AnnotationPost.class)) {
                                    Mapping map = new Mapping(className, method.getName());
                                    String valeur = method.getAnnotation(AnnotationPost.class).value();
                                    if (urlMapping.containsKey(valeur)) {
                                        throw new Exception("doublant" + valeur);
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

    public Object[] getMethodParameters(Method method, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] parameterValues = new Object[parameters.length];
        Map<String, Object> objectInstances = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
    
        for (int i = 0; i < parameters.length; i++) { 
            Parameter parameter = parameters[i];
    
            if (parameter.isAnnotationPresent(Parametre.class)) {
                Parametre param = parameter.getAnnotation(Parametre.class);
                String paramName = param.value();
                String paramValue = request.getParameter(paramName);
                parameterValues[i] = paramValue;
    
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
            }else if (parameter.getType().equals(MySession.class)) {
                parameterValues[i] = new MySession(request.getSession());
            }
        }
        return parameterValues;
    }
    
}
