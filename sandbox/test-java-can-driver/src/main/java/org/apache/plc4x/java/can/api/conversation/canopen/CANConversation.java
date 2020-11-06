package org.apache.plc4x.java.can.api.conversation.canopen;

import org.apache.plc4x.java.can.canopen.CANOpenFrame;
import org.apache.plc4x.java.can.canopen.CANOpenFrameBuilder;
import org.apache.plc4x.java.spi.ConversationContext.SendRequestContext;

import java.util.function.Consumer;

public interface CANConversation<W extends CANOpenFrame> {

    int getNodeId();

    CANOpenFrameBuilder createBuilder();

    SendRequestContext<W> send(W frame);

}

