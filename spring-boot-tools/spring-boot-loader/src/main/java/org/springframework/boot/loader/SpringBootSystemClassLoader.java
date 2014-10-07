package org.springframework.boot.loader;

import java.util.Arrays;
import java.util.List;

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
    private ClassLoader systemParentClassLoader;
    private ClassLoader springClassLoader;

    public SpringBootSystemClassLoader(ClassLoader systemParentClassLoader) {
        super(systemParentClassLoader);
        this.systemParentClassLoader = systemParentClassLoader;
        init();
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (name == null) {
            throw new ClassNotFoundException("Class name can not be NULL!!!");
        }
        try {
            return super.loadClass(name);
        } catch (Exception e) {
            // ClassNotFoundException thrown if class not found
            // from the non-null system parent class loader
        }
        if (!isSpecialCase(name) && this.springClassLoader != null) {
            return springClassLoader.loadClass(name);
        }
        throw new ClassNotFoundException(name);
    }

    // Special cases include calls from URL class to retrieve protocol handlers (null.*), as well as anything in the
    //  org.springframework.boot.loader.* package, as those should always be attempted to load from real System CL
    private static final List<String> specialCaseNames = Arrays.asList("null.file.Handler", "null.jar.Handler");
    private boolean isSpecialCase(String name) {
        return specialCaseNames.contains(name)
                || name.startsWith("org.springframework.boot.loader")
                || name.startsWith("null.");
    }

    private void init() {
        if (this.systemParentClassLoader != null) {
            JarLauncher jarLauncher = new JarLauncher();
            try {
                springClassLoader = jarLauncher.createClassLoader(jarLauncher.getClassPathArchives());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected ClassLoader getSpringClassLoader() {
        return this.springClassLoader;
    }
}
