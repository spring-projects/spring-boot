/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.untangled;

import org.springframework.boot.build.architecture.untangled.sub.UntangledTwo;

public final class UntangledOne {

	public static final String ID = UntangledTwo.class.getName() + "One";

	private UntangledOne() {

	}

}
