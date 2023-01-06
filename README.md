# Spring annotation utilities (self-contained)

This is basically a re-packaged version of Spring's annotation utilities with classes such as `AnnotationUtils` and
`MergedAnnotations`. They have the same package and class names and can be used as drop-in replacements for the
originals based on Spring version `5.2.5.RELEASE`. They have been extracted from Spring Core and recompiled to run
outside of Spring infrastructure. You can use them in any POJO project. The dependency on Spring's JCL logging bridge
has also been removed. The library uses the Java logging framework instead.

There are two variants:
  * The `master` branch keeps the dependency on the Kotlin core and reflection libraries, utilising their capabilities
    for retaining the up-stream library's Kotlin support for
      * optionals, nullable Kotlin types,
      * determining generic return types (with support of suspending functions) and
      * normal method return types (also with support of suspending functions).
  * The `no-kotlin` branch completely removes Kotlin dependencies along with the special capabilities mentioned above.

Both variants retain support for JSR 305 `@Nonnull` when determining (non-) nullability. Thus there still is a
dependency on the Google Findbugs JSR 305 annotation library.

## How I built this library (template for your own project)

### Preface

Lately someone mentioned Spring's `AnnotationUtils` on StackOverflow, mentioning how to conveniently get access to
annotations from a class, even including annotations from all implemented interfaces and meta annotations
(annotations on annotations). As I do not use Spring but wanted to use this utility class, I had this idea of just
copying its source code into my project so as not to make the project depend on Spring Core where the utility class
resides.

Unfortunately, `AnnotationUtils` itself recursively uses a bunch of other classes, and my first try of copying them one
by one proved to be tedious. So I tried to find a way to at least semi-automatically do that. Unfortunately I found no
good tool creating a report of which class uses which other classes. JBoss Tattletale can do it, but not recursively.

The closest idea I got was to create a minimised JAR with Maven Shade, but I wanted the source files, not the class
files. So I solved the problem by some shell magic (Git Bash on Windows, standard Bash on Linux would work too), i.e.
listing the minimised JAR contents, using the result as an input for extracting the corresponding Java files from the
dependency's source JAR and finally moving them into my project.

You may wonder: Why the effort and not just be happy with the minimised JAR? Well, I wanted to be able to edit the 
source code, for example prune it further down to my needs, possibly removing unneeded features, minimising the list of
dependency classes to what I really need for my purposes. For example:

* If I eliminate Spring's usage of JCL (Jakarta Commons Logging) classes and either remove all logging code or replace
  it by `System.out.println()` or Java logging framework instead, I can eliminate the Spring JCL bridge dependency.
* There is some code to handle Kotlin annotations. If I decide that I do not need that because I program in Java and
  write tests in Groovy (I love Spock and Geb), I can get rid of those lines of code and eliminate the dependency on the
  Kotlin standard library and Kotlin reflection tools.
* The same is true if I do not need information about `Nullable` classes or can live without the Spring classes 
  themselves using that annotation. Then I can also get rid of the Google Findbugs JSR 305 dependency.

So here is what I did. You can use it as a template for your own project, which is why I am describing it in the first
place:

### Create project with dummy class using dependency classes of interest

First add dependency of interest to your project's POM:

```xml
<dependency>
  <groupId>org.springframework</groupId>
  <artifactId>spring-core</artifactId>
  <version>5.2.5.RELEASE</version>
</dependency>
``` 

Now add a dummy class using the dependency classes of interest, such as Spring's `AnnotationUtils`:

```java
import org.springframework.core.annotation.AnnotationUtils;

public class Foo {
  public static void main(String[] args) {
    AnnotationUtils.isInJavaLangAnnotationPackage("Foo");
  }
}
```

### Create minimised binary JAR

Then make sure you create a minimised JAR like this:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-shade-plugin</artifactId>
  <version>3.2.1</version>
  <executions>
    <execution>
      <phase>package</phase>
      <goals>
        <goal>shade</goal>
      </goals>
      <configuration>
        <minimizeJar>true</minimizeJar>
      </configuration>
    </execution>
  </executions>
</plugin>
```

Optionally, it might be a good idea to check if the minimised JAR can be used instead of the original dependency (or
dependencies) from another project without problems. If the compiler complains about missing classes there, chances are
that you need to add transitive dependencies to that project or extract additional classes from them. 

### Find source files for classes from minimised JAR

Now copy the minimised JAR as well as the source JAR(s) to a work directory and unpack all source files corresponding to
the class files in the minimised JAR:

```
unzip -l annotation-utils-1.0-SNAPSHOT.jar | grep -E '\.class' | grep -Ev 'package-info|\$|scrum_master' | sed -E 's/.* (.*)\.class/\1.java/' | xargs unzip spring-core-5.2.5.RELEASE-sources.jar

Archive:  spring-core-5.2.5.RELEASE-sources.jar
  inflating: org/springframework/core/GraalDetector.java
  inflating: org/springframework/core/annotation/AnnotationTypeMappings.java
  (...)
  inflating: org/springframework/util/ClassUtils.java
  inflating: org/springframework/util/MultiValueMap.java
caution: filename not matched:  org/apache/commons/logging/LogAdapter.java
caution: filename not matched:  org/apache/commons/logging/LogFactory.java
caution: filename not matched:  org/apache/commons/logging/Log.java
```

For each `filename not matched` warning you
* either need to "rinse & repeat" by extracting the missing file from another source JAR
* or make sure your project has a Maven dependency on the corresponding binary JAR.

What you want do in this case mostly depends on how self-contained you want your new JAR to be. 

### Move sources into own project and make it compile

Finally
* move all unpacked source files into your project's source tree,
* deactivate the Maven Shade minimiser in your POM as it is no longer needed (instead of `<phase>package</phase>` you
  can just use `<phase>none</phase>`),
* deactivate the maven dependency on the original JAR because we want to find out if the project still compiles without
  it. If it does not, maybe you forgot to take care of the `filename not matched` stuff mentioned above. Go double-check
  until the project compiles.

If after that you want to do some clean-up work in the source files in order to remove unneeded functionality and maybe
get rid of some more dependency classes or JARs is up to you.

### Final remarks

Of course this method is neither fully automated nor perfect. If e.g. the minimised JAR is incomplete because some
classes are not imported directly but loaded via reflection magic, you might have to do additional manual work, such as
add a reference to missing classes to your dummy class and create a new minimised JAR, hoping that next time the project
compiles and runs after you moved all relevant source code into your project and deactivated the Maven dependency (or
dependencies, depending on how complex your situation is). But even an iterative approach to this semi-automated process
should be significantly faster than doing everything manually.
