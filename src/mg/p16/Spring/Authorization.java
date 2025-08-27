package mg.p16.Spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Authorization annotation to specify allowed roles for a method.
 */
@Retention(RetentionPolicy.RUNTIME) // Retained at runtime for reflection
@Target(ElementType.METHOD) // Can only be applied to methods
public @interface Authorization {
    String[] roles() default {}; // Array of roles allowed to access the method
}
