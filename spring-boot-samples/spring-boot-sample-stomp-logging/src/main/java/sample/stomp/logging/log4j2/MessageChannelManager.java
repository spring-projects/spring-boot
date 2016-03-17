package sample.stomp.logging.log4j2;

import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ExecutorSubscribableChannel;

/**
 * Manager of {@link MessageChannel}s used by {@link MessageChannelAppender}s.
 *
 * @author Vladimir Tsanev
 */
public class MessageChannelManager extends AbstractManager {

    private static MessageChannelManagerFactory FACTORY = new MessageChannelManagerFactory();

    private final ExecutorSubscribableChannel channel;

    private final String channelName;

    protected MessageChannelManager(String name, String channelName) {
        super(name);
        this.channelName = channelName;
        this.channel = new ExecutorSubscribableChannel();
    }

    @Override
    protected void releaseSub() {
    }

    public String getChannelName() {
        return channelName;
    }

    public SubscribableChannel getChannel() {
        return channel;
    }

    /**
     * Creates a Manager.
     *
     * @param name The name of the channel to manage.
     * @return An MessageChannelManager.
     */
    public static <T> MessageChannelManager getManager(final String name) {
        return AbstractManager.getManager("MessageChannel:" + name, FACTORY, name);
    }

    /**
     * Factory to create the MessageChannelManager.
     */
    private static class MessageChannelManagerFactory implements ManagerFactory<MessageChannelManager, String> {

        @Override
        public MessageChannelManager createManager(String name, String channel) {
            return new MessageChannelManager(name, channel);
        }

    }

}
