/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.configurationsample.specific;

import java.io.PrintWriter;
import java.io.Writer;

import javax.sql.DataSource;

import org.springframework.boot.configurationsample.ConfigurationProperties;

/**
 * Sample config with types that should not be added to the meta-data as we have no way to
 * bind them from simple strings.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties(prefix = "excluded")
public class ExcludedTypesPojo {

	private String name;

	private ClassLoader classLoader;

	private DataSource dataSource;

	private PrintWriter printWriter;

	private Writer writer;

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ClassLoader getClassLoader() {
		return this.classLoader;
	}

	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public DataSource getDataSource() {
		return this.dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public PrintWriter getPrintWriter() {
		return this.printWriter;
	}

	public void setPrintWriter(PrintWriter printWriter) {
		this.printWriter = printWriter;
	}

	public Writer getWriter() {
		return this.writer;
	}

	public void setWriter(Writer writer) {
		this.writer = writer;
	}

}
