package mg.p16.Spring;

import mg.p16.Spring.Rest_Api;

import jakarta.servlet.http.HttpSession;
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

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;


import jakarta.servlet.http.Part;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequestWrapper;
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
    
    private void scanControllers(String packageName) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        URL resource = classLoader.getResource(path);

        if (resource == null) {
            // 500 Internal Server Error - Package not found
            throw new Exception("</p> 500 Internal Server Error: Specified package does not exist: " + packageName+ "</p>");
        }
        //added
        // Get allowed roles from context-param in web.xml
        String allowedRolesFromConfig = getServletContext().getInitParameter("allowedRoles");
        Set<String> configRoles = new HashSet<>(Arrays.asList(allowedRolesFromConfig.split(",")));

        Path classPath = Paths.get(resource.toURI());
        Files.walk(classPath)
                .filter(f -> f.toString().endsWith(".class"))
                .forEach(f -> {
                    String className = packageName + "." + f.getFileName().toString().replace(".class", "");
                    try {
                        Class<?> clazz = Class.forName(className);
                        if (clazz.isAnnotationPresent(ControllerAnnotation.class)&& !Modifier.isAbstract(clazz.getModifiers())) {
                            // if(clazz.isAnnotationPresent(Authorization.class))
                            controllerNames.add(clazz.getSimpleName());
                            Method[] methods = clazz.getMethods();
                            VerbAction vb=null;
                            for (Method method : methods) {
                                if (method.isAnnotationPresent(GET.class)) {
                                    vb = new VerbAction(method.getName(), "GET");
                                    Mapping map = new Mapping(className, vb);
                                    String valeur = method.getAnnotation(GET.class).value();
                                    if (urlMapping.containsKey(valeur)) {
                                        // 500 Internal Server Error - Duplicate URL mapping for GET
                                        throw new Exception("<p> 500 Internal Server Error: Duplicate URL mapping for GET:"+valeur+"</p>");
                                    } else {
                                        urlMapping.put(valeur, map);
                                    }
                                } else if (method.isAnnotationPresent(AnnotationPost.class)) {
                                    vb = new VerbAction(method.getName(), "POST");
                                    Mapping map = new Mapping(className, vb);
                                    String valeur = method.getAnnotation(AnnotationPost.class).value();
                                    if (urlMapping.containsKey(valeur)) {
                                        // 500 Internal Server Error - Duplicate URL mapping for POST
                                        throw new Exception("<p> 500 Internal Server Error: Duplicate URL mapping for POST: "+valeur+" </p>");
                                    } else {
                                        urlMapping.put(valeur, map);
                                    }
                                }
                                if (method.isAnnotationPresent(Authorization.class)) {
                                    Authorization auth = method.getAnnotation(Authorization.class);
                                    String []allowedRoles = auth.roles(); // Values from the annotation

                                    // Ensure all roles in the annotation are valid
                                    for (String role : allowedRoles) {
                                        if (!configRoles.contains(role)) {
                                            throw new Exception("<p> 500 Internal Server Error: Unauthorized role found in method "
                                                    + method.getName() + ": " + role + "</p>");
                                        }
                                    }

                                    //add to the verbAction
                                    vb.setAllowedRoles(allowedRoles);
                                }
                            }
                        }else{
                            throw new Exception("<p> 500 Internal Server Error: controller not annoted </p>");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        
                    }
                });
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException,Exception{
        
    
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
                String[] allowed = mapping.getVerbAction().getAllowedRoles();
                String requestMethod = request.getMethod();


                // Get user's role from session
                if(allowed!=null){ //annoted Authorization 
                
                    try{
                        HttpSession session = request.getSession();
                        MySession mysess=new MySession(session);
                        String expressionRole = getServletContext().getInitParameter("roleSessionAttribute");
                        System.out.println(expressionRole);
                        String userRoleSession=(String)mysess.get(expressionRole); //in session
                        if (userRoleSession==null) {
                            throw new Exception("<p> 500 Internal Server Error:role in session non-existant </p>");
                        }
                        String publi="PUBLIC";
                        boolean match=Arrays.stream(allowed).noneMatch(publi::equals); //tsisy public

                        if(match){ //public tsy mentionned mila checkena
                            // Check if userRole is in the allowed roles
                            if (Arrays.stream(allowed).noneMatch(userRoleSession::equals)) {
                                throw new Exception("<p> 403 Forbidden: User role " + userRoleSession + " is not authorized </p>");
                            }
                        }

                        
                    }catch(Exception e){
                        e.printStackTrace();
                        throw e;
                    }
                }
                

                if (!requestMethod.equalsIgnoreCase(methodVerb)) {
                    response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED); // 405
                    throw new Exception("<p>405 Method Not Allowed: Expected " + methodVerb + " but got " + requestMethod + "</p>");
                }

                Class<?> clazz = Class.forName(mapping.getClassName());
                Object object = clazz.getDeclaredConstructor().newInstance();
                injectMySession(object,request.getSession());
                Method method = findMethod(clazz, requestMethod, mapping.getVerbAction().getAction());

                if (method == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404
                    response.getWriter().println("<p>404 Not Found: No matching method found in the controller class.</p>");
                    return;
                }
                

                Object[] parameters = getMethodParameters(method, request, response, isMultipart);

                Object returnValue = method.invoke(object, parameters);//error
                

                handleReturnValue(returnValue,request, response);
            }catch (MyValidationException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
                PrintWriter writer = response.getWriter();
                writer.println("<p>400 Bad Request: Validation failed.</p>");
                for (MyExceptions ex : e.getValidationErrors()) {
                    writer.println("<p>Field: " + ex.getField() + " | Error: " + ex.getError() + " | Solution: " + ex.getSolution() + "</p>");
                }
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
            List<MyExceptions> errors = (List<MyExceptions>) request.getAttribute("errors");
            String lien="";

            String requestMethod = request.getMethod();
            HttpServletRequest wrapped = new HttpServletRequestWrapper(request) {
                    @Override
                    public String getMethod() {
                        return "GET";
                    }
                };

            if (errors != null && !errors.isEmpty()) {
                RequestDispatcher dispatcher = wrapped.getRequestDispatcher(modelView.getError());
                dispatcher.forward(wrapped, response);
                return; // Ensure to return here to avoid forwarding twice
            }

            RequestDispatcher dispatcher = wrapped.getRequestDispatcher(modelView.getUrl());
            dispatcher.forward(wrapped, response);

        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500
            response.getWriter().println("<p>500 Internal Server Error: Data type not recognized.</p>");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        PrintWriter out = response.getWriter();
       try{
            processRequest(request, response);
       }catch(Exception e){
            out.println(e.getMessage());
       }
        out.close();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        PrintWriter out = response.getWriter();
       try{
            processRequest(request, response);
       }catch(Exception e){
            out.println(e.getMessage());
       }
        out.close();
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
                // Instantiate exactly the type expected by the controller parameter
                Class<?> paramType = parameter.getType();                 // e.g., objet.Reservation
                Object instance = paramType.getDeclaredConstructor().newInstance();
                parameterValues[i] = instance;

                String simple = paramType.getSimpleName();               // "Reservation" or "ReservationMere"
                // Iterate over all request parameters (do NOT consume the original Enumeration)
                for (String paramName : request.getParameterMap().keySet()) {
                    if (!paramName.startsWith(simple + ".")) continue;   // only this object's fields

                    String raw = request.getParameter(paramName);
                    String attributeName = paramName.substring(simple.length() + 1); // after "Reservation."
                    String setterName = "set" + attributeName.substring(0,1).toUpperCase() + attributeName.substring(1);

                    // Prefer String setter if available (your model provides String setters)
                    Method setter = null;
                    try {
                        setter = paramType.getMethod(setterName, String.class);
                        setter.invoke(instance, raw);
                    } catch (NoSuchMethodException nsme) {
                        // Fallback: try to find a typed setter using the declared field type
                        try {
                            Field f = paramType.getDeclaredField(attributeName);
                            Class<?> ftype = f.getType(); // e.g., int, Integer, Date, Timestamp...
                            Method typedSetter = paramType.getMethod(setterName, ftype);
                            Object converted = convert(raw, ftype); // implement minimal converters if needed
                            typedSetter.invoke(instance, converted);
                        } catch (NoSuchFieldException | NoSuchMethodException ignore) {
                            // Unknown field/setter: skip or log
                        }
                    }
                }

                // (optional) validate
                ValidationResult val = validate(instance);
                if (!val.getErrors().isEmpty()) {
                    request.setAttribute("errors", val.getErrors());
                    request.setAttribute("fieldValues", val.getFieldValues());
                }
            }

            // else if (parameter.getType().equals(MySession.class)) {
            //     parameterValues[i] = new MySession(request.getSession());
            // } 
            // else if (parameter.getType().equals(Part.class)) {
            //     if (isMultipart) {
            //         parameterValues[i] = request.getPart(parameter.getName());
            //     }
            // }
        }
        return parameterValues;
    }

    private Object convert(String raw, Class<?> type) {
        if (raw == null) return null;
        if (type.equals(String.class)) return raw;
        if (type.equals(int.class) || type.equals(Integer.class)) return Integer.valueOf(raw);
        if (type.equals(double.class) || type.equals(Double.class)) return Double.valueOf(raw);
        if (type.equals(java.sql.Date.class)) return java.sql.Date.valueOf(raw); // yyyy-MM-dd
        if (type.equals(java.sql.Timestamp.class)) return java.sql.Timestamp.valueOf(raw); // yyyy-MM-dd HH:mm:ss[.fff]
        return raw; // last resort
    }


    public static ValidationResult validate(Object obj) throws Exception {
        List<MyExceptions> errors = new ArrayList<>();
        Map<String, String> fieldValues = new HashMap<>();
        Field[] fields = obj.getClass().getDeclaredFields();

        // Loop through each field and validate
        for (Field field : fields) {
            field.setAccessible(true);

            try {
                Object value = field.get(obj);
                String fieldValue = value != null ? value.toString() : "";

                // Store the field value
                fieldValues.put(field.getName(), fieldValue);

                // Validate @Contains annotation
                Contains contains = field.getAnnotation(Contains.class);
                if (contains != null && value instanceof String) {
                    String strValue = (String) value;
                    if (!strValue.contains(contains.value())) {
                        errors.add(new MyExceptions(field.getName(),
                                contains.message(),
                                "Provide a value containing '" + contains.value() + "'.",
                                fieldValue));
                    }
                }

                // Validate other annotations
                Min min = field.getAnnotation(Min.class);
                if (min != null && value instanceof Integer) {
                    int intValue = (Integer) value;
                    if (intValue < min.value()) {
                        errors.add(new MyExceptions(field.getName(),
                                "Value is less than minimum allowed.",
                                "Provide a value >= " + min.value(),
                                fieldValue));
                    }
                }

                Max max = field.getAnnotation(Max.class);
                if (max != null && value instanceof Integer) {
                    int intValue = (Integer) value;
                    if (intValue > max.value()) {
                        errors.add(new MyExceptions(field.getName(),
                                "Value is greater than maximum allowed.",
                                "Provide a value <= " + max.value(),
                                fieldValue));
                    }
                }

                StartsWith startsWith = field.getAnnotation(StartsWith.class);
                if (startsWith != null && value instanceof String) {
                    String strValue = (String) value;
                    if (!strValue.startsWith(startsWith.value())) {
                        errors.add(new MyExceptions(field.getName(),
                                "Value does not start with the required prefix.",
                                "Provide a value starting with '" + startsWith.value() + "'",
                                fieldValue));
                    }
                }

            } catch (IllegalAccessException e) {
                errors.add(new MyExceptions(field.getName(),
                        "Unable to access field value.",
                        "Ensure the field is accessible.",
                        ""));
            }
        }

        return new ValidationResult(errors, fieldValues);
    }

    private void injectMySession(Object controllerInstance, HttpSession httpSession) {
        Field[] fields = controllerInstance.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getType().isAssignableFrom(MySession.class)) {
                try {
                    field.setAccessible(true);
                    MySession mySession = (MySession) field.get(controllerInstance);
                    if (mySession == null) {
                        mySession = new MySession(httpSession);
                        field.set(controllerInstance, mySession);
                    } else {
                        mySession.setSession(httpSession);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
