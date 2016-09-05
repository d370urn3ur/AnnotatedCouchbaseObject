package the.autarch.android.annotatedcouchbaseobject.compiler;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.google.common.collect.Sets;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;

import the.autarch.android.annotatedcouchbaseobject.api.AnnotatedCouchbaseField;

/**
 * Created by jpierce on 05/09/16.
 */
public class AnnotatedCouchbaseObjectAnnotatedClass {

    private static final String SUFFIX = "Schema";

    private TypeElement couchbaseObject;
    private String qualifiedSuperClassName;
    private String simpleTypeName;
    private String parseClassName;
    private Set<VariableElement> couchbaseObjectFields = Sets.newHashSet();

    public AnnotatedCouchbaseObjectAnnotatedClass(TypeElement classElement) throws IllegalArgumentException {

        couchbaseObject = classElement;
        DeclaredType classTypeMirror = (DeclaredType) couchbaseObject.getSuperclass();
        TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
        qualifiedSuperClassName = classTypeElement.getQualifiedName().toString();
        simpleTypeName = classTypeElement.getSimpleName().toString();

        addFields(couchbaseObject.getEnclosedElements());
    }

    private void addFields(List<? extends Element> fields) {
        for(Element field : fields) {
            if(field.getKind().isField()) {
                couchbaseObjectFields.add((VariableElement)field);
            }
        }
    }

    public void generateCode(Elements elementUtils, Filer filer) throws IOException {

        TypeElement superClassName = elementUtils.getTypeElement(qualifiedSuperClassName);
        String schemaName = couchbaseObject.getSimpleName().toString();
        String className = schemaName.replace(SUFFIX, ""); // (ex: ArticleSchema become Article)
        PackageElement pkg = elementUtils.getPackageOf(couchbaseObject);
        String packageName = pkg.isUnnamed() ? null : pkg.getQualifiedName().toString();

        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(className)
                .superclass(
                        ParameterizedTypeName.get(
                                ClassName.get(HashMap.class),
                                TypeName.get(String.class),
                                TypeName.get(Object.class)
                        )
                )
                .addModifiers(Modifier.PUBLIC);

        FieldSpec docSpec = FieldSpec.builder(TypeName.get(Document.class), "doc", Modifier.PRIVATE).build();
        FieldSpec dirtySpec = FieldSpec.builder(TypeName.BOOLEAN, "isDirty", Modifier.PRIVATE).build();
        typeSpecBuilder.addFields(Arrays.asList(docSpec, dirtySpec));

        MethodSpec initSpec = MethodSpec.constructorBuilder()
                .addParameter(TypeName.get(Document.class), "doc")
                .addStatement("this.$L = $L", "doc", "doc")
                .addStatement("putAll(doc.getProperties())")
                .build();

        MethodSpec saveSpec = MethodSpec.methodBuilder("save")
                .returns(TypeName.VOID)
                .beginControlFlow("if(isDirty)")
                .beginControlFlow("try")
                .addStatement("doc.putProperties(this)")
                .addStatement("isDirty = false")
                .endControlFlow()
                .beginControlFlow("catch($T e)", TypeName.get(CouchbaseLiteException.class))
                .endControlFlow()
                .endControlFlow()
                .build();

        typeSpecBuilder.addMethods(Arrays.asList(initSpec, saveSpec));

        for(VariableElement field : couchbaseObjectFields) {

            String parseKey;
            if(field.getAnnotation(AnnotatedCouchbaseField.class) != null) {
                parseKey = field.getAnnotation(AnnotatedCouchbaseField.class).value();
            } else {
                parseKey = field.getSimpleName().toString();
            }
            String fieldName = field.getSimpleName().toString();

            MethodSpec getMethodSpec = MethodSpec.methodBuilder("get" + StringUtils.capitalize(fieldName))
                    .addModifiers(Modifier.PUBLIC)
                    .returns(TypeName.get(field.asType()))
                    .addStatement("return ($T)$L.getProperty($S)", TypeName.get(field.asType()), "doc", parseKey)
                    .build();

            MethodSpec setMethodSpec = MethodSpec.methodBuilder("set" + StringUtils.capitalize(fieldName))
                    .addModifiers(Modifier.PUBLIC)
                    .returns(TypeName.VOID)
                    .addParameter(TypeName.get(field.asType()), "value")
                    .addStatement("put($S, value)", parseKey)
                    .addStatement("isDirty = true")
                    .build();

            typeSpecBuilder.addMethods(Arrays.asList(getMethodSpec, setMethodSpec));
        }

        JavaFile.builder(packageName, typeSpecBuilder.build()).build().writeTo(filer);
    }

    public String getParseClassName() {
        return parseClassName;
    }

    public String getQualifiedFactoryGroupName() {
        return qualifiedSuperClassName;
    }

    public String getSimpleFactoryGroupName() {
        return simpleTypeName;
    }

    public TypeElement getTypeElement() {
        return couchbaseObject;
    }
}
