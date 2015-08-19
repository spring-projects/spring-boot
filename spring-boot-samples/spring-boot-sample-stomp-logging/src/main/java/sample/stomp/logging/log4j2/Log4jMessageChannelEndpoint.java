package sample.stomp.logging.log4j2;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.springframework.messaging.MessageHandler;

import sample.stomp.logging.AbstractLogMessageChannelEndpoint;

public class Log4jMessageChannelEndpoint extends AbstractLogMessageChannelEndpoint {

    private final Map<String, MessageHandler> subscribed = new HashMap<String, MessageHandler>();

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        LoggerContext ctx = getLoggerContext();
        subscribe(ctx.getConfiguration());
    }

    @Override
    public void destroy() throws Exception {
        LoggerContext ctx = getLoggerContext();
        unsubscribe(ctx.getConfiguration());
        super.destroy();
    }

    private void subscribe(Configuration configuration) {
        for (Appender appender : configuration.getAppenders().values()) {
            String appenderName = appender.getName();
            if (appender instanceof MessageChannelAppender) {
                final MessageChannelAppender channelAppender = (MessageChannelAppender) appender;
                MessageHandler handler = new LogLineMessageHandler(channelAppender.getManager().getChannelName());

                channelAppender.getManager().getChannel().subscribe(handler);
                subscribed.put(appenderName, handler);
            }
        }
    }

    private void unsubscribe(Configuration configuration) {
        for (Map.Entry<String, MessageHandler> subscribedEntry : subscribed.entrySet()) {
            String appenderName = subscribedEntry.getKey();
            Appender appender = configuration.getAppender(appenderName);
            if (appender instanceof MessageChannelAppender) {
                MessageChannelAppender channelAppender = (MessageChannelAppender) appender;
                MessageHandler handler = subscribedEntry.getValue();

                channelAppender.getManager().getChannel().unsubscribe(handler);
            }
        }
        subscribed.clear();
    }

    private static LoggerContext getLoggerContext() {
        return (LoggerContext) LogManager.getContext(false);
    }

}
