package the.autarch.android.annotatedcouchbaseobject;

import the.autarch.android.annotatedcouchbaseobject.api.AnnotatedCouchbaseField;
import the.autarch.android.annotatedcouchbaseobject.api.AnnotatedCouchbaseObject;

/**
 * Created by jpierce on 05/09/16.
 */
@AnnotatedCouchbaseObject
public class ArticleSchema {

    String title;

    @AnnotatedCouchbaseField("text")
    String content;

}
