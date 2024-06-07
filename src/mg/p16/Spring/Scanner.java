package mg.p16.Spring;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scanner {
    public static   Map<String, Mapping>  scanControllersAndMapUrls(String packageName, Map<String, Mapping> urlMappings) throws PackageNotFoundException, MultipleMethodsException {
     List<Class<?>> controllerClasses = scanControllers(packageName);
        for (Class<?> controllerClass : controllerClasses) {
            Method[] methods = controllerClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(GET.class)) {
                    GET getAnnotation = method.getAnnotation(GET.class);
                    String urlPath = getAnnotation.value();
                    if (urlMappings.containsKey(urlPath)) {
                        throw new MultipleMethodsException("Multiple methods with the same URL: " + urlPath);
                    }
                    urlMappings.put(urlPath, new Mapping(controllerClass.getName(), method.getName()));
                } 
            }
        }
        return urlMappings;
    }


    public static List<Class<?>> scanControllers(String packageName) throws PackageNotFoundException {
        List<Class<?>> controllerClasses = new ArrayList<>();
        try {
            ClassLoader classLoader = FrontServlet.class.getClassLoader();
            String packagePath = packageName.replace('.', '/');
            java.net.URL resource = classLoader.getResource(packagePath);
            if (resource == null) {
                throw new PackageNotFoundException("Package not found: " + packageName);
            }
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
        } catch (URISyntaxException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return controllerClasses;
    }
}
