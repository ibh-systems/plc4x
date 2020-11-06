package org.apache.plc4x.java.can.api.conversation.canopen;

import org.apache.plc4x.java.api.value.PlcValue;
import org.apache.plc4x.java.can.canopen.CANOpenFrame;
import org.apache.plc4x.java.canopen.readwrite.CANOpenSDORequest;
import org.apache.plc4x.java.canopen.readwrite.SDORequest;
import org.apache.plc4x.java.canopen.readwrite.io.DataItemIO;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenDataType;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenService;
import org.apache.plc4x.java.spi.generation.ParseException;
import org.apache.plc4x.java.spi.generation.ReadBuffer;

public abstract class CANOpenConversationBase {

    protected final CANConversation<CANOpenFrame> delegate;
    protected final int nodeId;
    private final int answerNodeId;

    public CANOpenConversationBase(CANConversation<CANOpenFrame> delegate, int nodeId, int answerNodeId) {
        this.delegate = delegate;
        this.nodeId = nodeId;
        this.answerNodeId = answerNodeId;
    }

    protected PlcValue decodeFrom(byte[] data, CANOpenDataType type, int length) throws ParseException {
        return DataItemIO.staticParse(new ReadBuffer(data, true), type, length);
    }

    protected boolean isTransmitSDOFromReceiver(CANOpenFrame frame) {
        return frame.getNodeId() == answerNodeId && frame.getService() == CANOpenService.TRANSMIT_SDO;
    }

    protected CANOpenFrame createFrame(SDORequest rq) {
        return delegate.createBuilder()
            .withNodeId(nodeId)
            .withService(CANOpenService.RECEIVE_SDO)
            .withPayload(new CANOpenSDORequest(rq.getCommand(), rq))
            .build();
    }

}
