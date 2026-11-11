/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hibernate.autoconfigure.mapping;

/**
 * A non annotated entity that is handled by a custom "mapping-file".
 *
 * @author Stephane Nicoll
 */
public class NonAnnotatedEntity {

	@SuppressWarnings("NullAway.Init")
	private Long id;

	@SuppressWarnings("NullAway.Init")
	private String item;

	protected NonAnnotatedEntity() {
	}

	public NonAnnotatedEntity(String item) {
		this.item = item;
	}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getItem() {
		return this.item;
	}

	public void setItem(String value) {
		this.item = value;
	}

}
