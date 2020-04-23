package de.scrum_master.util;

import org.junit.Test;

import java.lang.annotation.Documented;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.core.annotation.AnnotationUtils.isInJavaLangAnnotationPackage;

@MyAnnotation
public class AnnotationUtilsTest {
  @Test
  public void test() {
    assertTrue(isInJavaLangAnnotationPackage("java.lang.annotation.Inherited"));
    assertFalse(isInJavaLangAnnotationPackage("java.lang.Override"));
    assertFalse(isInJavaLangAnnotationPackage("does.not.Exist"));
    assertFalse(isInJavaLangAnnotationPackage(AnnotationUtilsTest.class.getDeclaredAnnotation(MyAnnotation.class)));
    assertTrue(isInJavaLangAnnotationPackage(MyAnnotation.class.getDeclaredAnnotation(Documented.class)));
  }
}
