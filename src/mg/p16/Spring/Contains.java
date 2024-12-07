package mg.p16.Spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Annotation definition
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Contains {
    String value();  // The substring that must be contained
    String message() default "Value does not contain the required substring."; // Default error message
}
