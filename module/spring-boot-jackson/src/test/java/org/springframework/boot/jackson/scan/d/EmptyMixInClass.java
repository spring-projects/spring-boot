/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson.scan.d;

import org.springframework.boot.jackson.JacksonMixin;
import org.springframework.boot.jackson.types.Name;
import org.springframework.boot.jackson.types.NameAndAge;

@JacksonMixin(type = { Name.class, NameAndAge.class })
public class EmptyMixInClass {

}
