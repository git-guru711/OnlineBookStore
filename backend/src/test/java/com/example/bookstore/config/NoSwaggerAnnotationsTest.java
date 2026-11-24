package com.example.bookstore.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.util.ClassUtils;

/** All API docs live in OpenApiDoc: controllers and models carry no io.swagger annotations. */
class NoSwaggerAnnotationsTest {

    private static final List<String> PACKAGES =
            List.of("com.example.bookstore.controller", "com.example.bookstore.model");

    @Test
    void controllersAndModelsHaveNoSwaggerAnnotations() throws Exception {
        List<String> found = new ArrayList<>();
        for (Class<?> type : classesIn(PACKAGES)) {
            List<AnnotatedElement> elements = new ArrayList<>();
            elements.add(type);
            elements.addAll(List.of(type.getDeclaredFields()));
            elements.addAll(List.of(type.getDeclaredMethods()));
            elements.addAll(List.of(type.getDeclaredConstructors()));
            if (type.isRecord()) {
                elements.addAll(List.of(type.getRecordComponents()));
            }
            for (AnnotatedElement element : elements) {
                swaggerAnnotations(element).forEach(a -> found.add(type.getSimpleName() + " " + element + ": " + a));
                if (element instanceof Executable executable) {
                    Stream.of(executable.getParameters())
                            .forEach(p -> swaggerAnnotations(p)
                                    .forEach(a -> found.add(type.getSimpleName() + " " + executable.getName() + "("
                                            + p.getName() + "): " + a)));
                }
            }
        }
        assertThat(found).as("Swagger annotations belong in OpenApiDoc").isEmpty();
    }

    private static Stream<String> swaggerAnnotations(AnnotatedElement element) {
        return Stream.of(element.getDeclaredAnnotations())
                .map(Annotation::annotationType)
                .filter(t -> t.getPackageName().startsWith("io.swagger"))
                .map(Class::getSimpleName);
    }

    private static List<Class<?>> classesIn(List<String> packages) throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition definition) {
                return true; // every type, not only Spring components
            }
        };
        scanner.addIncludeFilter((reader, factory) -> true);
        List<Class<?>> classes = new ArrayList<>();
        for (String pkg : packages) {
            for (BeanDefinition definition : scanner.findCandidateComponents(pkg)) {
                classes.add(ClassUtils.forName(
                        definition.getBeanClassName(), NoSwaggerAnnotationsTest.class.getClassLoader()));
            }
        }
        assertThat(classes).as("classes found in " + packages).hasSizeGreaterThan(10);
        return classes;
    }
}
