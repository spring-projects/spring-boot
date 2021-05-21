package org.springframework.boot.env;

import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Strategy to load '.conf' files in
 * <a href="https://github.com/typesafehub/config/blob/master/HOCON.md">HOCON</a>
 * format into a {@link PropertySource}.
 */
public class HoconPropertySourceLoader implements PropertySourceLoader {

	@Override
	public String[] getFileExtensions() {
		return new String[]{"conf", "hocon"};
	}

	@Override
	public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
		if (!ClassUtils.isPresent("com.typesafe.config.ConfigFactory", null)) {
			throw new IllegalStateException(
					"Attempted to load " + name + " but lightbend's typesafe config was not found on the classpath");
		}
		return Collections.singletonList(new HoconPropertySourceFactory()
				.createPropertySource(name, resource instanceof EncodedResource ? (EncodedResource) resource : new EncodedResource(resource)));
	}
}
