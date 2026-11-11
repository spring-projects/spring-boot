/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ansi;

/**
 * An ANSI encodable element.
 *
 * @author Phillip Webb
 * @since 1.0.0
 */
public interface AnsiElement {

	/**
	 * Return the ANSI escape code.
	 * @return the ANSI escape code
	 */
	@Override
	String toString();

}
