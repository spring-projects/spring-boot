/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.autoconfigure;

import java.io.File;
import java.io.FileFilter;

import org.springframework.util.Assert;

/**
 * {@link FileFilter} that accepts only a specific "trigger" file.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class TriggerFileFilter implements FileFilter {

	private final String name;

	public TriggerFileFilter(String name) {
		Assert.notNull(name, "'name' must not be null");
		this.name = name;
	}

	@Override
	public boolean accept(File file) {
		return file.getName().equals(this.name);
	}

}
