/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.bootstrap.cli.template;

import groovy.text.GStringTemplateEngine;
import groovy.text.Template;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import org.codehaus.groovy.control.CompilationFailedException;

/**
 * @author Dave Syer
 */
public class GroovyTemplate {

	// FIXME is this used?

	public static String template(String name) throws IOException,
			CompilationFailedException, ClassNotFoundException {
		return template(name, Collections.<String, Object> emptyMap());
	}

	public static String template(String name, Map<String, ?> model) throws IOException,
			CompilationFailedException, ClassNotFoundException {
		GStringTemplateEngine engine = new GStringTemplateEngine();
		File file = new File("templates", name);
		URL resource = GroovyTemplate.class.getClassLoader().getResource(
				"templates/" + name);
		Template template;
		if (file.exists()) {
			template = engine.createTemplate(file);
		} else {
			if (resource != null) {
				template = engine.createTemplate(resource);
			} else {
				template = engine.createTemplate(name);
			}
		}
		return template.make(model).toString();
	}

}
