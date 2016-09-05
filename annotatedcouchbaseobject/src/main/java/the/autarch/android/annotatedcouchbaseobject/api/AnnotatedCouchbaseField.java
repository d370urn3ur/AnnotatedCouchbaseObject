package the.autarch.android.annotatedcouchbaseobject.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by jpierce on 05/09/16.
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface AnnotatedCouchbaseField {
    String value();
}
