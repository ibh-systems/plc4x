package org.apache.plc4x.java.can.api.conversation.canopen;

import org.apache.plc4x.java.api.exceptions.PlcException;
import org.apache.plc4x.java.api.exceptions.PlcRuntimeException;
import org.apache.plc4x.java.api.types.PlcResponseCode;
import org.apache.plc4x.java.api.value.PlcValue;
import org.apache.plc4x.java.can.canopen.CANOpenFrame;
import org.apache.plc4x.java.canopen.readwrite.*;
import org.apache.plc4x.java.canopen.readwrite.io.DataItemIO;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenDataType;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenService;
import org.apache.plc4x.java.canopen.readwrite.types.SDOResponseCommand;
import org.apache.plc4x.java.spi.generation.ParseException;

import java.util.concurrent.CompletableFuture;

public class SDODownloadConversation extends CANOpenConversationBase {

    private final CANConversation<CANOpenFrame> delegate;
    private final IndexAddress indexAddress;
    private final byte[] data;

    public SDODownloadConversation(CANConversation<CANOpenFrame> delegate, int nodeId, int answerNodeId, IndexAddress indexAddress, PlcValue value, CANOpenDataType type) {
        super(delegate, nodeId, answerNodeId);
        this.delegate = delegate;
        this.indexAddress = indexAddress;

        try {
            data = DataItemIO.staticSerialize(value, type,  null,true).getData();
        } catch (ParseException e) {
            throw new PlcRuntimeException("Could not serialize data", e);
        }
    }

    public void execute(CompletableFuture<PlcResponseCode> receiver) {
        if (data.length > 4) {
            // segmented
            SDOInitiateSegmentedUploadResponse size = new SDOInitiateSegmentedUploadResponse(data.length);
            delegate.send(createFrame(new SDOInitiateDownloadRequest(false, true, indexAddress, size)))
                .check(this::isTransmitSDOFromReceiver)
                .onTimeout(receiver::completeExceptionally)
                .onError((response, error) -> receiver.completeExceptionally(error))
                .unwrap(CANOpenFrame::getPayload)
                .only(CANOpenSDOResponse.class)
                .unwrap(CANOpenSDOResponse::getResponse)
                .check(p -> p.getCommand() == SDOResponseCommand.INITIATE_DOWNLOAD)
                .only(SDOInitiateDownloadResponse.class)
                .check(p -> indexAddress.equals(p.getAddress()))
                .handle(x -> {
                    put(data, receiver, false, 0);
                });

            return;
        }

        // expedited
        SDOInitiateDownloadRequest rq = new SDOInitiateDownloadRequest(
            true, true,
            indexAddress,
            new SDOInitiateExpeditedUploadResponse(data)
        );

        delegate.send(createFrame(rq))
            .check(this::isTransmitSDOFromReceiver)
            .onTimeout(receiver::completeExceptionally)
            .onError((response, error) -> {
                if (error != null) {
                    receiver.completeExceptionally(error);
                } else {
                    receiver.completeExceptionally(new PlcException("Transaction terminated"));
                }
            })
            .unwrap(CANOpenFrame::getPayload)
            .only(CANOpenSDOResponse.class)
            .unwrap(CANOpenSDOResponse::getResponse)
            .only(SDOInitiateDownloadResponse.class)
            .check(r -> r.getCommand() == SDOResponseCommand.INITIATE_DOWNLOAD)
            .handle(r -> {
                receiver.complete(PlcResponseCode.OK);
            });
    }

    private void put(byte[] data, CompletableFuture<PlcResponseCode> receiver, boolean toggle, int offset) {
        int remaining = data.length - offset;
        byte[] segment = new byte[Math.min(remaining, 7)];
        System.arraycopy(data, offset, segment, 0, segment.length);

        delegate.send(createFrame(new SDOSegmentDownloadRequest(toggle, remaining <= 7, segment)))
            .check(this::isTransmitSDOFromReceiver)
            .onTimeout(receiver::completeExceptionally)
            .unwrap(CANOpenFrame::getPayload)
            .only(CANOpenSDOResponse.class)
            .unwrap(CANOpenSDOResponse::getResponse)
            .only(SDOSegmentDownloadResponse.class)
            .onError((response, error) -> {
                if (error != null) {
                    receiver.completeExceptionally(error);
                } else {
                    receiver.completeExceptionally(new PlcException("Transaction terminated"));
                }
            })
            .check(response -> response.getToggle() == toggle)
            .handle(reply -> {
                if (offset + segment.length == data.length) {
                    receiver.complete(PlcResponseCode.OK);
                } else {
                    put(data, receiver, !toggle, offset + segment.length);
                }
            });
    }

}
