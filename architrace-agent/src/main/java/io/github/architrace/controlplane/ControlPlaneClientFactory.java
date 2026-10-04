/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.controlplane;

import com.google.inject.Singleton;
import io.github.architrace.grpc.ControlPlaneClient;
import io.github.architrace.grpc.GrpcAddressParser;
import io.github.architrace.grpc.TransportClient;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;

@Singleton
public class ControlPlaneClientFactory {

    public TransportClient create(String server) {
        return new ControlPlaneClient(NettyChannelBuilder.forAddress(GrpcAddressParser.parseHostPort(server))
                .usePlaintext()
                .build());
    }
}
