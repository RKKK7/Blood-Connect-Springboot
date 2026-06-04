package com.bloodconnect.config;

import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SocketRunner {

    private final SocketIOServer server;

    public SocketRunner(SocketIOServer server) {
        this.server = server;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        server.start();
        System.out.println("\uD83D\uDD0C Socket.IO server started on port " + server.getConfiguration().getPort());
    }

    @PreDestroy
    public void stop() {
        server.stop();
    }
}
