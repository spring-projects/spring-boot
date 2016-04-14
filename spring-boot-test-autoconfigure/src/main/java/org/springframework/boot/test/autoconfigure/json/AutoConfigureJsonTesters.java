/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.autoconfigure.json;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.json.GsonTester;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;

/**
 * Annotation that can be applied to a test class to enable and configure
 * auto-configuration of JSON testers.
 * <p>
 * NOTE: {@code @AutoConfigureJsonTesters} works in conjunction with
 * {@link JsonTesterInitializationTestExecutionListener}. If you declare your own
 * {@link TestExecutionListeners @TestExecutionListeners} and don't
 * {@link MergeMode#MERGE_WITH_DEFAULTS merge with defaults} you must include
 * {@link JsonTesterInitializationTestExecutionListener} to use this annotation.
 *
 * @author Phillip Webb
 * @see JsonTesterInitializationTestExecutionListener
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface AutoConfigureJsonTesters {

	/**
	 * If {@link BasicJsonTester}, {@link JacksonTester} and {@link GsonTester} fields
	 * should be initialized using marshallers from the {@link ApplicationContext}.
	 * @return if JSON tester fields should be initialized
	 */
	boolean initFields() default true;

}
