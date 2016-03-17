package sample.stomp.logging.log4j2;

import java.io.Serializable;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Booleans;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;

/**
 * Appender that sends log messages to a {@link MessageChannel}.
 *
 * @author Vladimir Tsanev
 */
@Plugin(name = "MessageChannel", category = "Core", elementType = "appender", printObject = true)
public class MessageChannelAppender extends AbstractAppender {

    private static final long serialVersionUID = 1L;

    private final MessageChannelManager manager;

    private final MessageChannel channel;

    private MessageChannelAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, MessageChannelManager manager) {
        super(name, filter, layout, ignoreExceptions);
        this.manager = manager;
        this.channel = manager.getChannel();
    }

    @PluginFactory
    public static MessageChannelAppender createAppender(@PluginElement("Layout") Layout<? extends Serializable> layout,
                                                        @PluginElement("Filter") final Filter filter, @PluginAttribute("channel") String channel,
                                                        @PluginAttribute("name") final String name,
                                                        @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) final String ignore) {
        if (name == null) {
            LOGGER.error("No name provided for MessageChannelAppender");
            return null;
        }
        if (channel == null) {
            LOGGER.error("No channel provided for MessageChannelAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        MessageChannelManager manager = MessageChannelManager.getManager(channel);
        return new MessageChannelAppender(name, filter, layout, ignoreExceptions, manager);
    }

    public MessageChannelManager getManager() {
        return manager;
    }

    @Override
    public void append(LogEvent event) {
        try {
            channel.send(createMessage(event));
        } catch (final MessagingException e) {
            throw new AppenderLoggingException(e);
        }
    }

    private Message<byte[]> createMessage(LogEvent event) {
        return new GenericMessage<byte[]>(getLayout().toByteArray(event));
    }

    @Override
    public void stop() {
        super.stop();
        manager.release();
    }

}
