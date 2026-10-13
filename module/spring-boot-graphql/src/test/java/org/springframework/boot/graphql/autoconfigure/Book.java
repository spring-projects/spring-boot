/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.graphql.autoconfigure;

import org.springframework.data.annotation.Id;

/**
 * Sample class for
 *
 * @author Brian Clozel
 */
public class Book {

	@Id
	@SuppressWarnings("NullAway.Init")
	String id;

	@SuppressWarnings("NullAway.Init")
	String name;

	@SuppressWarnings("NullAway.Init")
	int pageCount;

	@SuppressWarnings("NullAway.Init")
	String author;

	protected Book() {
	}

	public Book(String id, String name, int pageCount, String author) {
		this.id = id;
		this.name = name;
		this.pageCount = pageCount;
		this.author = author;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPageCount() {
		return this.pageCount;
	}

	public void setPageCount(int pageCount) {
		this.pageCount = pageCount;
	}

	public String getAuthor() {
		return this.author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

}
