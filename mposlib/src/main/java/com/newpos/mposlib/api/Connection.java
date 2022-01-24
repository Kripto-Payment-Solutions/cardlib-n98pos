package com.newpos.mposlib.api;

import com.newpos.mposlib.model.Packet;

public interface Connection {

    void send(Packet packet);
}
