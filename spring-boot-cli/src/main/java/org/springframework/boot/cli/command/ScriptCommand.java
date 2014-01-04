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

package org.springframework.boot.cli.command;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;

import java.util.Collection;
import java.util.Collections;

import org.springframework.boot.cli.Command;
import org.springframework.boot.cli.OptionHelp;

/**
 * {@link Command} to run a Groovy script.
 * 
 * @author Dave Syer
 */
public class ScriptCommand implements Command {

	private Object main;

	private String defaultName;

	public ScriptCommand(String name, Object main) {
		this.main = main;
		this.defaultName = name;
	}

	@Override
	public String getName() {
		if (this.main instanceof Command) {
			return ((Command) this.main).getName();
		}
		else if (this.main instanceof GroovyObject) {
			GroovyObject object = (GroovyObject) this.main;
			if (object.getMetaClass().hasProperty(object, "name") != null) {
				return (String) object.getProperty("name");
			}
		}

		return this.defaultName;
	}

	@Override
	public boolean isOptionCommand() {
		return false;
	}

	@Override
	public String getDescription() {
		if (this.main instanceof Command) {
			return ((Command) this.main).getDescription();
		}
		return this.defaultName;
	}

	@Override
	public String getHelp() {
		if (this.main instanceof OptionHandler) {
			return ((OptionHandler) this.main).getHelp();
		}
		if (this.main instanceof Command) {
			return ((Command) this.main).getHelp();
		}
		return null;
	}

	@Override
	public Collection<OptionHelp> getOptionsHelp() {
		if (this.main instanceof OptionHandler) {
			return ((OptionHandler) this.main).getOptionsHelp();
		}
		if (this.main instanceof Command) {
			return ((Command) this.main).getOptionsHelp();
		}
		return Collections.emptyList();
	}

	@Override
	public void run(String... args) throws Exception {
		if (this.main instanceof Command) {
			((Command) this.main).run(args);
		}
		else if (this.main instanceof OptionHandler) {
			((OptionHandler) this.main).run(args);
		}
		else if (this.main instanceof Closure) {
			((Closure<?>) this.main).call((Object[]) args);
		}
	}

	@Override
	public String getUsageHelp() {
		if (this.main instanceof Command) {
			return ((Command) this.main).getDescription();
		}
		return "[options] <args>";
	}

	public static ScriptCommand command(Class<?> type) {

		Object main = null;
		try {
			main = type.newInstance();
		}
		catch (Exception ex) {
			// Inner classes and closures will not be instantiatable
			return null;
		}
		if (main instanceof Command) {
			return new ScriptCommand(type.getSimpleName(), main);
		}
		else if (main instanceof OptionHandler) {
			((OptionHandler) main).options();
			return new ScriptCommand(type.getSimpleName(), main);
		}
		return null;

	}

}
