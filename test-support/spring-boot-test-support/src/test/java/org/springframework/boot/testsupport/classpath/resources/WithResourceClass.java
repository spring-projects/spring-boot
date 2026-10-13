/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.classpath.resources;

/**
 * Class to test the use of {@link WithResource} on a super-class
 *
 * @author Andy Wilkinson
 */
@WithResource(name = "on-super-class", content = "super-class content")
class WithResourceClass {

}
