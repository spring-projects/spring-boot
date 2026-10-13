/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.resources.noloads;

import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.util.ResourceUtils;

public class ResourceUtilsWithoutLoading {

	void inspectResourceLocation() throws MalformedURLException {
		ResourceUtils.isUrl("gradle.properties");
		ResourceUtils.isFileURL(new URL("gradle.properties"));
		"test".startsWith(ResourceUtils.FILE_URL_PREFIX);
	}

}
