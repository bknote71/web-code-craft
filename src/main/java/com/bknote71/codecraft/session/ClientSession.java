package com.bknote71.codecraft.session;

import com.bknote71.codecraft.proto.Protocol;
import com.bknote71.codecraft.robocode.core.RobotPeer;
import com.bknote71.codecraft.util.PacketTranslator;
import lombok.Getter;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.io.IOException;
import java.security.Principal;

public class ClientSession {

    private String sessionId;
    private WebSocketSession webSocketSession;

    @Getter
    private RobotPeer myRobot;


    public ClientSession(String sessionId, WebSocketSession session) {
        this.sessionId = sessionId;
        this.webSocketSession = session;
    }

    public WebSocketSession getWebSocketSession() {
        return webSocketSession;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUsername() {
        Principal principal = webSocketSession.getPrincipal();
        System.out.println(webSocketSession.getAttributes());
        if (principal == null)
            return "anon";
        return principal.getName();
    }

    public void setMyRobot(RobotPeer robotPeer) {
        this.myRobot = robotPeer;
    }

    public void send(Protocol packet) {
        try {
            // packet to json
            String json = PacketTranslator.json(packet);
            if (webSocketSession.isOpen())
                webSocketSession.sendMessage(new TextMessage(json));
        } catch (IOException e) {
//            log.error("Error sending message to client: " + e.getMessage());
            try {
                webSocketSession.close();
            } catch (IOException ex) {
//                log.error("Error closing session: " + ex.getMessage());
            }
        }
    }
}
