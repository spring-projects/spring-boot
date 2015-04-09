/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.logging.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.ConsoleAppender;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DefaultLogbackConfiguration}.
 *
 * @author Sylvere Richard
 */
public class DefaultLogbackConfigurationTest {

    DefaultLogbackConfiguration impl;

    @Mock
    LogbackConfigurator config;

    @Mock
    LoggerContext context;

    @Captor
    ArgumentCaptor<ConsoleAppender> appenderCpt;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        impl = new DefaultLogbackConfiguration(null);

        doReturn(new Object()).when(config).getConfigurationLock();
        doReturn(context).when(config).getContext();
    }

    @Test
    public void testApply() throws Exception {
        impl.apply(config);
        verify(config).logger("", Level.ERROR);
        verify(config).appender(eq("CONSOLE"), appenderCpt.capture());

        Assert.assertEquals(impl.detectJansi(), appenderCpt.getValue().isWithJansi());
    }
}