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

package com.dismu.p2p.utilities;

import java.io.*;

/**
 * Created by r00tman on 2/14/14.
 */
public class Packet {
    public int type = -1;
    public byte[] data = null;

    public void read(InputStream is) throws IOException {
        DataInputStream dis = new DataInputStream(is);
        this.type = dis.readInt();
        int size = dis.readInt();
        this.data = new byte[size];
        dis.read(this.data);
        this.parse();
    }

    public void write(OutputStream os) throws IOException {
        this.serialize();
        DataOutputStream dos = new DataOutputStream(os);
        dos.writeInt(this.type);
        dos.writeInt(this.data.length);
        dos.write(this.data);

        dos.flush();
        os.flush();
    }

    public boolean isMine() {
        return false;
    }

    public void parse() {

    }

    public void serialize() {

    }
}
