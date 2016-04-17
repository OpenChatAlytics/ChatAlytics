package com.chatalytics.core.realtime;

import com.google.common.base.Strings;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

/**
 * {@link Encoder} and {@link Decoder} for sending {@link ConnectionType} params through the web
 * socket
 *
 * @author giannis
 *
 */
public class ConnectionTypeEncoderDecoder implements Encoder.Text<ConnectionType>,
                                                     Decoder.Text<ConnectionType> {

    @Override
    public void init(EndpointConfig config) {}

    @Override
    public void destroy() {}

    @Override
    public ConnectionType decode(String connectionTypeStr) throws DecodeException {
        return ConnectionType.valueOf(connectionTypeStr);
    }

    @Override
    public boolean willDecode(String str) {
        return !Strings.isNullOrEmpty(str);
    }

    @Override
    public String encode(ConnectionType connectionType) throws EncodeException {
        return connectionType.name();
    }

}
