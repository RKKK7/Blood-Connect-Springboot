package com.bloodconnect.service;

import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.stereotype.Service;

@Service
public class SocketService {

    private final SocketIOServer server;

    public SocketService(SocketIOServer server) {
        this.server = server;
    }

    /** io.to(userId).emit(event, payload) */
    public void toUser(String userId, String event, Object payload) {
        if (userId != null) server.getRoomOperations(userId).sendEvent(event, payload);
    }

    /** io.to(`chat_${donationId}`).emit(event, payload) */
    public void toChat(String donationId, String event, Object payload) {
        server.getRoomOperations("chat_" + donationId).sendEvent(event, payload);
    }

    /** io.emit(event, payload) */
    public void broadcast(String event, Object payload) {
        server.getBroadcastOperations().sendEvent(event, payload);
    }
}
