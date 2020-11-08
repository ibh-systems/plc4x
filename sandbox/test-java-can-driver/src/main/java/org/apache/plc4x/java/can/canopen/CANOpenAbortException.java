package org.apache.plc4x.java.can.canopen;

import org.apache.plc4x.java.api.exceptions.PlcProtocolException;

public class CANOpenAbortException extends PlcProtocolException {

    private final long abortCode;

    public CANOpenAbortException(String message, long abortCode) {
        super(message);
        this.abortCode = abortCode;
    }

    public CANOpenAbortException(Throwable cause, long abortCode) {
        super(cause);
        this.abortCode = abortCode;
    }

    public long getAbortCode() {
        return abortCode;
    }

}
