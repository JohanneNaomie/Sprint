package mg.p16.Spring;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// @Target(ElementType.METHOD)
// @Retention(RetentionPolicy.RUNTIME)
// public @interface AnnotationPost {
//     String value();
// }

@Retention(RetentionPolicy.RUNTIME)
public @interface AnnotationPost {
    String value();
}

