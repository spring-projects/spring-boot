/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.tangled;

import org.springframework.boot.build.architecture.tangled.sub.TangledTwo;

public final class TangledOne {

	public static final String ID = TangledTwo.class.getName() + "One";

	private TangledOne() {

	}

}
