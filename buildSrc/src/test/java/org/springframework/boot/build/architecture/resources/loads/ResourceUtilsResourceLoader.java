/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.resources.loads;

import java.io.FileNotFoundException;

import org.springframework.util.ResourceUtils;

public class ResourceUtilsResourceLoader {

	void getResource() throws FileNotFoundException {
		ResourceUtils.getURL("gradle.properties");
	}

}
