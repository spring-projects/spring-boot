/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.example.scan.sub;

import org.springframework.boot.SpringBootConfiguration;

/**
 * Example config used in {@code AnnotatedClassFinderTests}. Should not be found since
 * scanner should only search upwards.
 *
 * @author Phillip Webb
 */
@SpringBootConfiguration
public class SubExampleConfig {

}
