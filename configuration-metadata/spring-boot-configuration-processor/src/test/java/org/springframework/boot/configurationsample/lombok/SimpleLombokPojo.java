/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.lombok;

import lombok.Data;

/**
 * Lombok POJO for use with samples.
 *
 * @author Stephane Nicoll
 */
@Data
@SuppressWarnings("unused")
public class SimpleLombokPojo {

	private int value;

}
