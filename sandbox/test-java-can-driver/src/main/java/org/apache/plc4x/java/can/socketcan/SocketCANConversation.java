package org.apache.plc4x.java.can.socketcan;

import org.apache.plc4x.java.can.api.conversation.canopen.CANConversation;
import org.apache.plc4x.java.can.canopen.CANOpenFrame;
import org.apache.plc4x.java.can.canopen.CANOpenFrameBuilder;
import org.apache.plc4x.java.can.canopen.CANOpenFrameBuilderFactory;
import org.apache.plc4x.java.spi.ConversationContext;
import org.apache.plc4x.java.spi.ConversationContext.SendRequestContext;

import java.time.Duration;
import java.util.function.Consumer;

public class SocketCANConversation implements CANConversation<CANOpenFrame> {

    private final int nodeId;
    private final ConversationContext<CANOpenFrame> context;
    private final int timeout;
    private final CANOpenFrameBuilderFactory factory;

    public SocketCANConversation(int nodeId, ConversationContext<CANOpenFrame> context, int timeout, CANOpenFrameBuilderFactory factory) {
        this.nodeId = nodeId;
        this.context = context;
        this.timeout = timeout;
        this.factory = factory;
    }

    @Override
    public int getNodeId() {
        return nodeId;
    }

    @Override
    public CANOpenFrameBuilder createBuilder() {
        return factory.createBuilder();
    }

    public SendRequestContext<CANOpenFrame> send(CANOpenFrame frame) {
        return context.sendRequest(frame)
            .expectResponse(CANOpenFrame.class, Duration.ofMillis(timeout));
    }

}
