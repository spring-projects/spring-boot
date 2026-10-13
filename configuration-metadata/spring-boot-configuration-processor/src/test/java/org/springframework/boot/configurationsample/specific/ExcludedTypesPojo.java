/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.specific;

import java.io.PrintWriter;
import java.io.Writer;

import javax.sql.DataSource;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Sample config with types that should not be added to the meta-data as we have no way to
 * bind them from simple strings.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("excluded")
public class ExcludedTypesPojo {

	private String name;

	private ClassLoader classLoader;

	private DataSource dataSource;

	private PrintWriter printWriter;

	private Writer writer;

	private Writer[] writerArray;

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

	public Writer[] getWriterArray() {
		return this.writerArray;
	}

	public void setWriterArray(Writer[] writerArray) {
		this.writerArray = writerArray;
	}

}
