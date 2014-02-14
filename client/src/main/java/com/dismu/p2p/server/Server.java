/*
 * Copyright (c) 2014, Victor Rudnev.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of the xmlunit.sourceforge.net nor the names
 *       of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written
 *       permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.dismu.p2p.server;

import com.dismu.p2p.utilities.Packet;
import com.dismu.p2p.utilities.PacketSerialize;
import com.dismu.p2p.utilities.RequestSeedsPacket;
import com.dismu.p2p.utilities.RequestSeedsResponsePacket;
import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by r00tman on 2/14/14.
 */
public class Server {
    private InetAddress address;
    private ServerSocket ss;
    private int port;

    public static void main(String[] args) {
        Server server = new Server();
        try {
            server.address = InetAddress.getLocalHost();
            server.port = 1775;
            server.ss = new ServerSocket(server.port);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() throws IOException {
        Socket socket = ss.accept();
        OutputStream os = socket.getOutputStream();
        InputStream in = socket.getInputStream();

        Packet packet = PacketSerialize.readPacket(in);

        if (packet instanceof RequestSeedsPacket) {
            RequestSeedsResponsePacket rp = new RequestSeedsResponsePacket();
            rp.addresses = new InetAddress[2];
            rp.addresses[0] = InetAddress.getLocalHost();
            rp.addresses[1] = InetAddress.getByName("8.8.8.8");
            rp.write(os);
        }

        os.close();
        in.close();
        socket.close();
    }
}
