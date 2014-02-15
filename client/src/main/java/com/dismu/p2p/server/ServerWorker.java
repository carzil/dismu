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

import com.dismu.p2p.packets.Packet;
import com.dismu.p2p.utilities.PacketSerialize;
import com.dismu.p2p.packets.RequestSeedsPacket;
import com.dismu.p2p.packets.RequestSeedsResponsePacket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by r00tman on 2/15/14.
 */
public class ServerWorker implements Runnable {
    private Socket socket;

    public ServerWorker(Socket s) {
        this.socket = s;
    }

    @Override
    public void run() {
        try {
            OutputStream os = this.socket.getOutputStream();
            InputStream is = this.socket.getInputStream();

            Packet packet = null;
            packet = PacketSerialize.readPacket(is);
            if (packet instanceof RequestSeedsPacket) {
                RequestSeedsResponsePacket rp = new RequestSeedsResponsePacket();
                rp.addresses = new InetAddress[2];
                rp.addresses[0] = InetAddress.getLocalHost();
                rp.addresses[1] = InetAddress.getByName("8.8.8.8");
                rp.write(os);
            }

            os.close();
            is.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
