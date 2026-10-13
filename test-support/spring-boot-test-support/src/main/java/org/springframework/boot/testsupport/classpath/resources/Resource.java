/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.classpath.resources;

import java.nio.file.Path;

/**
 * A resource that is to be made available in tests.
 *
 * @param path the path of the resource
 * @param additional whether the resource should be made available in addition to those
 * that already exist elsewhere
 * @author Andy Wilkinson
 */
record Resource(Path path, boolean additional) {

}
