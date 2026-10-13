/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.using.autoconfiguration.disablingspecific;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class MyApplication {

}
