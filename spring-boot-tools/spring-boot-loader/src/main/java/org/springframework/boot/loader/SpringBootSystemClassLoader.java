package org.springframework.boot.loader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This class provides ability to specify a custom JVM System Class Loader to enable loading classes provided inside
 *  nested jars of the Spring Boot uber-jar for use cases such as setting custom
 *  'policy.provider' or 'java.security.manager' or 'java.rmi.server.RMIClassLoaderSpi'.
 *
 *  All those classes are being loaded by JVM from the System Class Loader, so this enables packaging custom implementations
 *      inside the distribution jar as nested artifact dependencies.
 *
 *  {@code -Djava.system.class.loader=org.springframework.boot.loader.SpringBootSystemClassLoader} can be provided as
 *      a startup argument to enable use of custom system class loader
 *
 * @author Alex Antonov
 */
public class SpringBootSystemClassLoader extends ClassLoader {
    public static final String ORG_SPRINGFRAMEWORK_BOOT_LOADER_PREFIX = "org.springframework.boot.loader";
    public static final String NULL_PREFIX = "null.";
    public static final String DELEGATE_ORDER_PROPERTY = "spring.boot.classloader.delegate";
    public static final String CLASS_EXCLUSION_REGEX_PROPERTY = "spring.boot.classloader.exclude";

    private final ClassLoader systemParentClassLoader;
    private final ClassLoader springClassLoader;

    public SpringBootSystemClassLoader(ClassLoader systemParentClassLoader) {
        super(systemParentClassLoader);
        this.systemParentClassLoader = systemParentClassLoader;
        this.springClassLoader = initSpringClassLoader();
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (name == null) {
            throw new ClassNotFoundException("Class name can not be NULL!!!");
        }
        if ("first".equalsIgnoreCase(System.getProperty(DELEGATE_ORDER_PROPERTY, "first"))) {
            return loadClassFromSystemFirst(name);
        } else {
            return loadClassFromSystemLast(name);
        }
    }

    private Class<?> loadClassFromSystemFirst(String name) throws ClassNotFoundException {
        try {
            return super.loadClass(name);
        } catch (Exception e) {
            // ClassNotFoundException thrown if class not found
            // from the non-null system parent class loader
        }
        return loadClassFromSpring(name);
    }

    private Class<?> loadClassFromSystemLast(String name) throws ClassNotFoundException {
        try {
            return loadClassFromSpring(name);
        } catch (Exception e) {
            // ClassNotFoundException thrown if class not found
            // from the spring class loader
        }
        return super.loadClass(name);
    }

    private Class<?> loadClassFromSpring(String name) throws ClassNotFoundException {
        if (!isSpecialCase(name) && this.springClassLoader != null) {
            return this.springClassLoader.loadClass(name);
        }
        throw new ClassNotFoundException(name);
    }

    // Special cases include calls from URL class to retrieve protocol handlers (null.*), as well as anything in the
    //  org.springframework.boot.loader.* package, as those should always be attempted to load from real System CL
    private static final List<Pattern> specialCaseNames = new ArrayList<Pattern>();
    private boolean isSpecialCase(String name) {
        for (Pattern p : specialCaseNames) {
            if (p.matcher(name).matches()) {
                return true;
            }
        }
        return name.startsWith(ORG_SPRINGFRAMEWORK_BOOT_LOADER_PREFIX)
                || name.startsWith(NULL_PREFIX);
    }

    private ClassLoader initSpringClassLoader() {
        initExclusionRules();
        if (this.systemParentClassLoader != null) {
            JarLauncher jarLauncher = new JarLauncher();
            try {
                return jarLauncher.createClassLoader(jarLauncher.getClassPathArchives());
            } catch (Exception e) {
                throw new RuntimeException("Unable to create Spring Boot Class Loader", e);
            }
        }
        return null;
    }

    protected ClassLoader getSpringClassLoader() {
        return this.springClassLoader;
    }

    private void initExclusionRules() {
        String exclusionRulesRegex = System.getProperty(CLASS_EXCLUSION_REGEX_PROPERTY);
        if (exclusionRulesRegex != null) {
            specialCaseNames.add(Pattern.compile(exclusionRulesRegex));
        }
    }
}
