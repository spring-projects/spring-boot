/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.task;

import java.util.Map;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

/**
 * Builder to provide a consistent way to access and use the {@code applicationTaskExecutor} 
 * across different Spring integrations.
 *
 * @author Phillip Webb
 * @since 3.4.0
 */
public final class ApplicationTaskExecutorBuilder {

    private final BeanFactory beanFactory;

    /**
     * Create a new {@link ApplicationTaskExecutorBuilder} instance.
     * @param beanFactory the bean factory
     */
    public ApplicationTaskExecutorBuilder(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    /**
     * Get the application's task executor as a plain {@link Executor}.
     * @return the application task executor
     */
    public Executor getExecutor() {
        return getTaskExecutor(Executor.class);
    }

    /**
     * Get the application's task executor as an {@link AsyncTaskExecutor}.
     * This is suitable for Spring MVC, WebFlux, WebSocket, and most integrations.
     * @return the application task executor
     * @throws IllegalStateException if the application task executor is not an {@link AsyncTaskExecutor}
     */
    public AsyncTaskExecutor getAsyncTaskExecutor() {
        Executor executor = getExecutor();
        Assert.state(executor instanceof AsyncTaskExecutor, 
            "The application task executor must be an instance of AsyncTaskExecutor");
        return (AsyncTaskExecutor) executor;
    }

    /**
     * Get the application's task executor as a {@link TaskExecutor}.
     * This is suitable for integrations that don't require async-specific methods.
     * @return the application task executor
     * @throws IllegalStateException if the application task executor is not a {@link TaskExecutor}
     */
    public TaskExecutor getTaskExecutor() {
        Executor executor = getExecutor();
        Assert.state(executor instanceof TaskExecutor,
            "The application task executor must be an instance of TaskExecutor");
        return (TaskExecutor) executor;
    }

    /**
     * Get the application's task executor checking if it's also a specific type.
     * @param <T> the type of executor
     * @param type the type of executor
     * @return the application task executor cast to the requested type
     * @throws IllegalStateException if the application task executor is not of the required type
     */
    public <T extends Executor> T getTaskExecutor(Class<T> type) {
        try {
            Executor executor = this.beanFactory.getBean(
                TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME, Executor.class);
            Assert.state(type.isInstance(executor), 
                "The application task executor bean is not of the required type " + type.getName());
            return type.cast(executor);
        }
        catch (NoSuchBeanDefinitionException ex) {
            throw new IllegalStateException("No application task executor bean found", ex);
        }
    }

    /**
     * Check if an {@link Executor} with the name 'applicationTaskExecutor' exists.
     * @param beanFactory the bean factory
     * @return {@code true} if an application task executor exists
     */
    public static boolean hasApplicationTaskExecutor(BeanFactory beanFactory) {
        return beanFactory.containsBean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME);
    }

    /**
     * Determine the AsyncTaskExecutor to use when multiple are available.
     * @param taskExecutors the map of available task executors
     * @return the application task executor
     */
    public static AsyncTaskExecutor determineAsyncTaskExecutor(Map<String, AsyncTaskExecutor> taskExecutors) {
        if (taskExecutors.size() == 1) {
            return taskExecutors.values().iterator().next();
        }
        return taskExecutors.get(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME);
    }

    /**
     * Get the application task executor from an ObjectProvider, checking for proper type.
     * @param executorProvider the object provider for the executor
     * @return the application task executor or null if none is available
     */
    public static AsyncTaskExecutor getAsyncTaskExecutorIfAvailable(ObjectProvider<Executor> executorProvider) {
        Executor executor = executorProvider.getIfAvailable();
        return (executor instanceof AsyncTaskExecutor asyncTaskExecutor) ? asyncTaskExecutor : null;
    }
} 