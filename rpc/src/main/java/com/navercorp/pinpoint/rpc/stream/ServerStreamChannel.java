/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.rpc.stream;

import com.navercorp.pinpoint.common.util.Assert;
import com.navercorp.pinpoint.rpc.packet.stream.StreamClosePacket;
import com.navercorp.pinpoint.rpc.packet.stream.StreamCode;
import com.navercorp.pinpoint.rpc.packet.stream.StreamCreatePacket;
import com.navercorp.pinpoint.rpc.packet.stream.StreamCreateSuccessPacket;
import com.navercorp.pinpoint.rpc.packet.stream.StreamPacket;
import com.navercorp.pinpoint.rpc.packet.stream.StreamResponsePacket;

import org.jboss.netty.channel.Channel;

import java.net.SocketAddress;

/**
 * @author koo.taejin
 */
public class ServerStreamChannel extends AbstractStreamChannel {

    private final Channel channel;
    private final ServerStreamChannelMessageHandler streamChannelMessageHandler;
    private StreamChannelStateChangeEventHandler stateChangeEventHandler = new LoggingStreamChannelStateChangeEventHandler();
    
    public ServerStreamChannel(Channel channel, int streamId, StreamChannelRepository streamChannelRepository, ServerStreamChannelMessageHandler streamChannelMessageHandler) {
        super(streamId, streamChannelRepository);
        this.channel = Assert.requireNonNull(channel, "channel");
        this.streamChannelMessageHandler = Assert.requireNonNull(streamChannelMessageHandler, "streamChannelMessageHandler must not be null");
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return channel.getRemoteAddress();
    }

    @Override
    void write(StreamPacket packet) {
        write(null, packet);
    }

    @Override
    void write(StreamChannelStateCode expectedCode, StreamPacket packet) {
        state.assertState(expectedCode);
        channel.write(packet);
    }

    public void setStateChangeEventHandler(StreamChannelStateChangeEventHandler stateChangeEventHandler) {
        this.stateChangeEventHandler = Assert.requireNonNull(stateChangeEventHandler, "stateChangeEventHandler must not be null");
    }

    public void sendData(byte[] payload) {
        StreamResponsePacket packet = new StreamResponsePacket(getStreamId(), payload);
        write(StreamChannelStateCode.CONNECTED, packet);
    }

    public void sendCreateSuccess() {
        StreamCreateSuccessPacket packet = new StreamCreateSuccessPacket(getStreamId());
        write(StreamChannelStateCode.CONNECTED, packet);
    }

    public void handleStreamCreatePacket(StreamCreatePacket packet) throws StreamException {
        changeStateTo(StreamChannelStateCode.CONNECT_ARRIVED, true);
        StreamCode result = streamChannelMessageHandler.handleStreamCreatePacket(this, packet);
        if (result != StreamCode.OK) {
            throw new StreamException(result);
        }
        changeStateConnected();
        sendCreateSuccess();
    }

    @Override
    public void handleStreamClosePacket(StreamClosePacket packet) {
        streamChannelMessageHandler.handleStreamClosePacket(this, packet);
        disconnect(packet.getCode());
    }

    @Override
    public StreamChannelStateChangeEventHandler getStateChangeEventHandler() {
        return stateChangeEventHandler;
    }

}
