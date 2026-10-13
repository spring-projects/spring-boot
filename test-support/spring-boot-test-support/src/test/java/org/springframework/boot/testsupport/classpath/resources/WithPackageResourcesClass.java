/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.classpath.resources;

/**
 * Class to test the use of {@link WithPackageResources} on a super-class.
 *
 * @author Andy Wilkinson
 */
@WithPackageResources({ "resource-1.txt", "resource-2.txt", "sub/resource-3.txt" })
class WithPackageResourcesClass {

}
