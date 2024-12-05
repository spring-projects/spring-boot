package org.springframework.boot.actuate.autoconfigure.scheduling;


import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.scheduling.ThreadPoolTaskExecutorsEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@AutoConfiguration
@ConditionalOnAvailableEndpoint(ThreadPoolTaskExecutorsEndpoint.class)
public class ThreadPoolTaskExecutorsEndpointAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ThreadPoolTaskExecutorsEndpoint threadPoolTaskExecutorEndpoint(ObjectProvider<ThreadPoolTaskExecutor> holders) {
        return new ThreadPoolTaskExecutorsEndpoint(holders.orderedStream().toList());
    }

}
