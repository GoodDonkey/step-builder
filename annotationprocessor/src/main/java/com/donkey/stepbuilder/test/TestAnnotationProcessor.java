package com.donkey.stepbuilder.test;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import lombok.AccessLevel;
import lombok.Setter;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@AutoService(Processor.class)
public class TestAnnotationProcessor extends AbstractProcessor {
    
    private ClassName innerBuilderClassName;
    private String packageName;
    private String className;
    ArrayList<MethodSpec> methodSpecs = new ArrayList<>();
    ArrayList<TypeSpec.Builder> interfaceSpecs = new ArrayList<>();
    ArrayList<FieldSpec> fieldSpecs = new ArrayList<>();
    ArrayList<String> pascalFieldNames = new ArrayList<>();
    ArrayList<String> camelFieldNames = new ArrayList<>();
    ArrayList<Modifier> fieldScopes = new ArrayList<>();
    ArrayList<TypeMirror> fieldTypes = new ArrayList<>();
    ArrayList<Boolean> isFieldFinal = new ArrayList<>();
    TypeSpec builderbuilder = null;
    
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(TestAnnotation.class.getName());
    }
    
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
    
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(TestAnnotation.class);
        for (Element element : annotatedElements) {
            // 패키지 이름 설정
            if (packageName == null) {
                Element e = element;
                while (!(e instanceof PackageElement)) {
                    e = e.getEnclosingElement();
                    System.out.println(e); // package 이름
                }
                packageName = ((PackageElement) e).getQualifiedName().toString();
                innerBuilderClassName = ClassName.get(packageName, "SSomeSBuilder","InnerBuilder");
            }
            TypeElement typeElement = (TypeElement) element;
            String simpleTypeName = typeElement.getSimpleName().toString();
            // 클래스가 맞는지 확인.
            if (element.getKind() != ElementKind.CLASS) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "can't not use @TestAnnotation in other than a class");
                System.out.println("클래스가 아님 element = " + element);
                return false;
            }
    
            extractFieldsInfos(element);
        }
        
        if (roundEnv.processingOver()) {
            try {
                generateAClass(builderbuilder);
                return true;
            } catch (IOException ex) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, ex.toString());
            }
        }
        
        
        System.out.println("그냥 프린트");
        
        return true;
    }
    
    private void extractFieldsInfos(Element element) {
        // 클래스 이름을 설정.
        className = element.getSimpleName().toString();
        System.out.println("클래스 이름 = " + className);
        List<? extends Element> enclosedElements = element.getEnclosedElements();
        
        // 해당 클래스에 있는 element 를 돌린다.
        for (int i = 0; i < enclosedElements.size(); i++) {
            Element enclosedElement = enclosedElements.get(i);
            System.out.println("enclosedElement = " + enclosedElement.getSimpleName());
            if (enclosedElement.getKind() == ElementKind.FIELD) {
        
                VariableElement variableElement = (VariableElement) enclosedElement;
                TypeMirror typeMirror = variableElement.asType();
                Name simpleName = variableElement.getSimpleName();
                String camelName = simpleName.toString();
                String pascalName = asPascalCased(simpleName);
                Set<Modifier> modifiers = variableElement.getModifiers();
                System.out.println("pascalCasedName = " + pascalName);
                
                fieldTypes.add(typeMirror);
                
                pascalFieldNames.add(pascalName);
                camelFieldNames.add(camelName);
                // final 체크
                if (modifiers.contains(Modifier.FINAL)) {
                    isFieldFinal.add(true);
                } else {
                    isFieldFinal.add(false);
                }
                
                // 접근제어자 체크
                for (Modifier modifier : modifiers) {
                    switch (modifier) {
                        case PRIVATE:
                            fieldScopes.add(Modifier.PRIVATE);
                            break;
                        case PUBLIC:
                            fieldScopes.add(Modifier.PUBLIC);
                            break;
                        case PROTECTED:
                            fieldScopes.add(Modifier.PROTECTED);
                            break;
                        case DEFAULT:
                            fieldScopes.add(Modifier.DEFAULT);
                            break;
                        default:
                            break;
                    }
                }
        
                //Todo static 체크
        
                System.out.println("modifiers = " + modifiers);
                System.out.println("typeMirror = " + typeMirror);
            }
        }
        System.out.println("fieldNames = " + pascalFieldNames);
        System.out.println("fieldScopes = " + fieldScopes);
        System.out.println("fieldTypes = " + fieldTypes);
        System.out.println("isFieldFinal = " + isFieldFinal);
        
        for (int i = 0; i < pascalFieldNames.size(); i++) {
            interfaceSpecs.add(buildInterfaceSpecBuilder(i));
        }
        
        try {
            builderbuilder = defineAClass((TypeElement) element, interfaceSpecs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private TypeSpec.Builder buildInterfaceSpecBuilder(int i) {
        String fieldName = pascalFieldNames.get(i);
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(fieldName + "Builder").addModifiers(Modifier.PUBLIC);
        return builder;
    }
    
    private String asPascalCased(Name simpleName) {
        String name = simpleName.toString();
        char first = name.charAt(0);
        char capitalFirst = String.valueOf(first).toUpperCase(Locale.ROOT).charAt(0);
        String substring = name.substring(1);
        return String.valueOf(capitalFirst).concat(substring);
    }
    
    private void generateAClass(TypeSpec builderbuilder) throws IOException {
            JavaFile.builder(packageName, builderbuilder)
                    .build()
                    .writeTo(processingEnv.getFiler());
    
    }
    
    // 필드 정보를 가져와야 함.
    private TypeSpec defineAClass(TypeElement element, ArrayList<TypeSpec.Builder> interfaceSpecs) throws IOException {
        String BuilderClassName = className + "SBuilder";
        TypeSpec.Builder builder = TypeSpec.classBuilder(BuilderClassName).addModifiers(Modifier.PUBLIC);
        

        TypeSpec.Builder type2 = TypeSpec.classBuilder(innerBuilderClassName)
                                 .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                                 .addSuperinterfaces(List.of(ClassName.get(packageName, interfaceSpecs.get(1).build().name),
                                                             ClassName.get(packageName, interfaceSpecs.get(2).build().name),
                                                             ClassName.get(packageName, interfaceSpecs.get(3).build().name)));
        // InnerBuilder noargs
        type2.addMethod(MethodSpec.constructorBuilder().build());
        // InnerBuilder one args
        type2.addMethod(MethodSpec.constructorBuilder()
                                .addParameter(TypeName.get(fieldTypes.get(0)), camelFieldNames.get(0))
                                .addStatement("this.$N = $N", camelFieldNames.get(0), camelFieldNames.get(0)).build());
        
        // InnerBuilder Setters
//        type2.addAnnotation(AnnotationSpec.builder(Setter.class)
//                                    .addMember("value", "$T.PRIVATE", AccessLevel.class).build());
        
        // add fields & setters
        for (int i = 0; i < fieldTypes.size(); i++) {
            builder.addField(FieldSpec.builder(TypeName.get(fieldTypes.get(i)),
                                               camelFieldNames.get(i),
                                               fieldScopes.get(i)).build());
            type2.addField(FieldSpec.builder(TypeName.get(fieldTypes.get(i)),
                                               camelFieldNames.get(i),
                                               fieldScopes.get(i)).build());
            
            // setters
            type2.addMethod(MethodSpec.methodBuilder("set" + pascalFieldNames.get(i))
                                      .returns(void.class)
                                        .addModifiers(Modifier.PRIVATE)
                                      .addParameter(TypeName.get(fieldTypes.get(i)), camelFieldNames.get(i))
                                      .addStatement("this.$N = $N", camelFieldNames.get(i), camelFieldNames.get(i))
                                      .build()).build();
        }
        
        
        int last = interfaceSpecs.size();
    
        for (int i = 1; i < interfaceSpecs.size(); i++) {
            if (i+1 < pascalFieldNames.size()) {
                type2.addMethod(MethodSpec.methodBuilder(camelFieldNames.get(i)).addAnnotation(Override.class)
                                          .addModifiers(Modifier.PUBLIC)
                                          .returns(ClassName.get(packageName, interfaceSpecs.get(i + 1).build().name))
                                          .addParameter(TypeName.get(fieldTypes.get(i)), camelFieldNames.get(i))
                                          .addStatement("this.$N = $N", camelFieldNames.get(i), camelFieldNames.get(i))
                                          .addStatement("return this").build());
            }
        }
        MethodSpec.Builder buildMethodBuilder = MethodSpec.methodBuilder(camelFieldNames.get(last - 1)).addAnnotation(Override.class)
                                          .addModifiers(Modifier.PUBLIC).returns(ClassName.get(packageName, className))
                                          .addParameter(TypeName.get(fieldTypes.get(last - 1)), camelFieldNames.get(last - 1))
                                          .addStatement("this.$N = $N",
                                                        camelFieldNames.get(last - 1),
                                                        camelFieldNames.get(last - 1))
                                           .addStatement("$T obj = new $T()",
                                                         ClassName.get(packageName, className),
                                                         ClassName.get(packageName, className));
    
        for (int i = 0; i < fieldTypes.size(); i++) {
            buildMethodBuilder.addStatement("obj.set$N(this.$N)",
                                            pascalFieldNames.get(i),
                                            camelFieldNames.get(i));
        }
        
        buildMethodBuilder.addStatement("return obj").build();
        
        type2.addMethod(buildMethodBuilder.build());
    
    
        builder.addType(type2.build());

        
        // 그 다음 부터는 인터페이스로 정의
        for (int i = 1; i < interfaceSpecs.size(); i++) {
            TypeSpec.Builder interfaceSpec = interfaceSpecs.get(i);
            if (i+1 < pascalFieldNames.size()) {
                interfaceSpec.addMethod(MethodSpec.methodBuilder(camelFieldNames.get(i))
                                                  .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                                  .returns(ClassName.get(packageName, interfaceSpecs.get(i+1).build().name))
                                                  .addParameter(TypeName.get(fieldTypes.get(i)), camelFieldNames.get(i)).build());
                TypeSpec builtInterface = interfaceSpec.build();
                JavaFile.builder(packageName, builtInterface).build().writeTo(processingEnv.getFiler());
            } else {
                interfaceSpec.addMethod(MethodSpec.methodBuilder(camelFieldNames.get(i))
                                                  .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                                  .returns(ClassName.get(packageName, className))
                                                  .addParameter(TypeName.get(fieldTypes.get(i)), camelFieldNames.get(i)).build());
                TypeSpec builtInterface = interfaceSpec.build();
                JavaFile.builder(packageName, builtInterface).build().writeTo(processingEnv.getFiler());
            }
        }
        
        // 첫번째 : public static propOne 메서드
        MethodSpec.Builder staticMethodBuilder = MethodSpec.methodBuilder(camelFieldNames.get(0))
                                                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                                           .addParameter(TypeName.get(fieldTypes.get(0)), camelFieldNames.get(0))
                                                           .returns(ClassName.get(packageName, interfaceSpecs.get(1).build().name))
                                                           .addStatement("$T $N = new $T()", innerBuilderClassName, "innerBuilder",
                                                                                            innerBuilderClassName);
        // 파라미터 개수만큼 돌리기
        for (int i = 0; i < 1; i++) {
            staticMethodBuilder.addStatement("innerBuilder.set$N($N)",
                                            pascalFieldNames.get(i),
                                            camelFieldNames.get(i));
        }
        staticMethodBuilder.addStatement("return innerBuilder");
        builder.addMethod(staticMethodBuilder.build());
        
        
        
        // 인터페이스를 정의한다.
        return builder.build();
    }
}
