package com.bknote71.codecraft.session;

import com.bknote71.codecraft.robocode.core.RobotManager;
import com.bknote71.codecraft.robocode.core.battle.Battle;
import com.bknote71.codecraft.robocode.core.RobotPeer;
import com.bknote71.codecraft.session.packet.ServerPacketManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientSessionHandler extends TextWebSocketHandler {

    private final ServerPacketManager serverPacketManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // session 이 지정한 방에 플레이어 추가!!
        log.info("connected established");

        log.info("session: {}", session.getAttributes());

        // 클라이언트 세션 생성 및 추가
        ClientSession generatedSession = ClientSessionManager.Instance.generate(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // ClientSession 이게 굳이 필요한가?
        // 일단 ClientSession == WebSocketSession wrapper
        log.info("handle text message");
        ClientSession clientSession = ClientSessionManager.Instance.find(session.getId());
        serverPacketManager.handlePacket(clientSession, message);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        super.handleTransportError(session, exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("connection closed status {} and closed={}", status, !session.isOpen());
        // leave room
        ClientSession clientSession = ClientSessionManager.Instance.find(session.getId());
        RobotPeer robot = clientSession.getMyRobot();
        if (robot == null)
            return;

        Battle battle = robot.getBattle();
        if (battle == null)
            return;

        battle.push(battle::leaveBattle, robot.getId());
        RobotManager.Instance.remove(robot.getId()); // 임시 <<
        ClientSessionManager.Instance.remove(clientSession);
    }
}
