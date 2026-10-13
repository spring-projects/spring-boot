/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.convert;

import java.io.File;
import java.io.IOException;

import org.springframework.boot.io.ApplicationResourceLoader;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.Resource;

/**
 * {@link Converter} to convert from a {@link String} to a {@link File}. Supports basic
 * file conversion as well as file URLs.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class StringToFileConverter implements Converter<String, File> {

	@Override
	public File convert(String source) {
		return getFile(ApplicationResourceLoader.get().getResource(source));
	}

	private File getFile(Resource resource) {
		try {
			return resource.getFile();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not retrieve file for " + resource + ": " + ex.getMessage());
		}
	}

}
