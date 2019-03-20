/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.mustache;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Mustache.Collector;
import com.samskivert.mustache.Mustache.Compiler;
import com.samskivert.mustache.Mustache.Escaper;
import com.samskivert.mustache.Mustache.Formatter;
import com.samskivert.mustache.Mustache.TemplateLoader;

import org.springframework.beans.factory.FactoryBean;

/**
 * Factory for a Mustache compiler with custom strategies. For building a {@code @Bean}
 * definition in Java it probably doesn't help to use this factory since the underlying
 * fluent API is actually richer.
 *
 * @author Dave Syer
 * @since 1.2.2
 * @see MustacheResourceTemplateLoader
 * @deprecated as of 1.5
 */
@Deprecated
public class MustacheCompilerFactoryBean implements FactoryBean<Mustache.Compiler> {

	private String delims;

	private TemplateLoader templateLoader;

	private Formatter formatter;

	private Escaper escaper;

	private Collector collector;

	private Compiler compiler;

	private String defaultValue;

	private Boolean emptyStringIsFalse;

	public void setDelims(String delims) {
		this.delims = delims;
	}

	public void setTemplateLoader(TemplateLoader templateLoader) {
		this.templateLoader = templateLoader;
	}

	public void setFormatter(Formatter formatter) {
		this.formatter = formatter;
	}

	public void setEscaper(Escaper escaper) {
		this.escaper = escaper;
	}

	public void setCollector(Collector collector) {
		this.collector = collector;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public void setEmptyStringIsFalse(Boolean emptyStringIsFalse) {
		this.emptyStringIsFalse = emptyStringIsFalse;
	}

	@Override
	public Mustache.Compiler getObject() throws Exception {
		this.compiler = Mustache.compiler();
		if (this.delims != null) {
			this.compiler = this.compiler.withDelims(this.delims);
		}
		if (this.templateLoader != null) {
			this.compiler = this.compiler.withLoader(this.templateLoader);
		}
		if (this.formatter != null) {
			this.compiler = this.compiler.withFormatter(this.formatter);
		}
		if (this.escaper != null) {
			this.compiler = this.compiler.withEscaper(this.escaper);
		}
		if (this.collector != null) {
			this.compiler = this.compiler.withCollector(this.collector);
		}
		if (this.defaultValue != null) {
			this.compiler = this.compiler.defaultValue(this.defaultValue);
		}
		if (this.emptyStringIsFalse != null) {
			this.compiler = this.compiler.emptyStringIsFalse(this.emptyStringIsFalse);
		}
		return this.compiler;
	}

	@Override
	public Class<?> getObjectType() {
		return Mustache.Compiler.class;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

}
