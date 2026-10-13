/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.util.function.ThrowingConsumer;

/**
 * Class to work with the native-image argfile.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @since 3.0.0
 */
public final class NativeImageArgFile {

	/**
	 * Location of the argfile.
	 */
	public static final String LOCATION = "META-INF/native-image/argfile";

	private final List<String> excludes;

	/**
	 * Constructs a new instance with the given excludes.
	 * @param excludes dependencies for which the reachability metadata should be excluded
	 */
	public NativeImageArgFile(Collection<String> excludes) {
		this.excludes = List.copyOf(excludes);
	}

	/**
	 * Write the arguments file if it is necessary.
	 * @param writer consumer that should write the contents
	 */
	public void writeIfNecessary(ThrowingConsumer<List<String>> writer) {
		if (this.excludes.isEmpty()) {
			return;
		}
		List<String> lines = new ArrayList<>();
		for (String exclude : this.excludes) {
			int lastSlash = exclude.lastIndexOf('/');
			String jar = (lastSlash != -1) ? exclude.substring(lastSlash + 1) : exclude;
			lines.add("--exclude-config");
			lines.add(Pattern.quote(jar));
			lines.add("^/META-INF/native-image/.*");
		}
		writer.accept(lines);
	}

}
