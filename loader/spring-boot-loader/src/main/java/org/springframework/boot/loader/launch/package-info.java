/*
 * Copyright 2012-present the original author or authors.
 */

/**
 * System that allows self-contained JAR/WAR archives to be launched using
 * {@code java -jar}. Archives can include nested packaged dependency JARs (there is no
 * need to create shade style jars) and are executed without unpacking. The only
 * constraint is that nested JARs must be stored in the archive uncompressed.
 *
 * @see org.springframework.boot.loader.launch.JarLauncher
 * @see org.springframework.boot.loader.launch.WarLauncher
 */
package org.springframework.boot.loader.launch;
