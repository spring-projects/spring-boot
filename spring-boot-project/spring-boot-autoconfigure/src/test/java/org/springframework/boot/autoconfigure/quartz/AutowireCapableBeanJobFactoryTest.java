/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.quartz;

import java.lang.reflect.Proxy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.spi.TriggerFiredBundle;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class AutowireCapableBeanJobFactoryTest {

	@Mock
	private TriggerFiredBundle mockedTriggerFireBundle;

	@Test
	public void createNoWrappedInstance() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(BaseConfiguration.class);
		AutowireCapableBeanJobFactory jobFactory = ctx.getBean(AutowireCapableBeanJobFactory.class);

		JobDetail jobDetail = JobBuilder.newJob(TestJob.class).build();
		given(this.mockedTriggerFireBundle.getJobDetail()).willReturn(jobDetail);

		Object jobInstance = jobFactory.createJobInstance(this.mockedTriggerFireBundle);
		assertThat(jobInstance).isInstanceOf(TestJob.class);
		assertThat(Proxy.isProxyClass(jobInstance.getClass())).isFalse();
		assertThat(ReflectionTestUtils.getField(jobInstance, "env")).isNotNull();
	}

	@Test
	public void createWrappedInstance() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(WrappingJobConfiguration.class);
		AutowireCapableBeanJobFactory jobFactory = ctx.getBean(AutowireCapableBeanJobFactory.class);

		JobDetail jobDetail = JobBuilder.newJob(TestJob.class).build();
		given(this.mockedTriggerFireBundle.getJobDetail()).willReturn(jobDetail);

		Object jobInstance = jobFactory.createJobInstance(this.mockedTriggerFireBundle);
		assertThat(jobInstance).isNotInstanceOf(TestJob.class);
		assertThat(Proxy.isProxyClass(jobInstance.getClass())).isTrue();
		assertThat(ReflectionTestUtils.getField(AopProxyUtils.getSingletonTarget(jobInstance), "env")).isNotNull();
	}

	@Configuration
	protected static class BaseConfiguration {

		@Bean
		public AutowireCapableBeanJobFactory jobFactory(ApplicationContext ctx) {
			return new AutowireCapableBeanJobFactory(ctx.getAutowireCapableBeanFactory());
		}
	}

	@Configuration
	protected static class WrappingJobConfiguration extends BaseConfiguration {

		@Bean
		public static BeanPostProcessor jobWrapper() {
			return new BeanPostProcessor() {
				@Override
				public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
					if (bean instanceof TestJob) {
						ProxyFactory proxyFactory = new ProxyFactory(bean);
						proxyFactory.addInterface(Job.class);
						return proxyFactory.getProxy();
					}
					return bean;
				}
			};
		}
	}

	protected static class TestJob extends QuartzJobBean {

		@Autowired
		private Environment env;

		@Override
		protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		}
	}
}
