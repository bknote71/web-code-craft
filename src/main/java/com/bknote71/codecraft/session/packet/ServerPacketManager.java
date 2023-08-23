package com.bknote71.codecraft.session.packet;

import com.bknote71.codecraft.proto.*;
import com.bknote71.codecraft.session.ClientSession;
import com.bknote71.codecraft.util.PacketTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

@Component
public class ServerPacketManager {
    private final PacketHandler packetHandler;

    @Autowired
    public ServerPacketManager(PacketHandler packetHandler) {
        this.packetHandler = packetHandler;
        register();
    }

    private Map<ProtocolType, TriConsumer<ClientSession, TextMessage, ProtocolType>> onRecvs = new HashMap<>();
    private Map<ProtocolType, BiConsumer<ClientSession, Protocol>> handlers = new HashMap<>();

    public void register() {
        onRecvs.put(ProtocolType.C_EnterBattle, (session, message, protocol) -> makePacket(session, message, protocol, CEnterBattle.class));
        handlers.put(ProtocolType.C_EnterBattle, packetHandler::CEnterBattleHandler);
        onRecvs.put(ProtocolType.C_ChangeRobot, (session, message, protocol) -> makePacket(session, message, protocol, CChangeRobot.class));
        handlers.put(ProtocolType.C_ChangeRobot, packetHandler::CChangeRobotHandler);
        onRecvs.put(ProtocolType.C_Chat, (session, message, protocol) -> makePacket(session, message, protocol, CChat.class));
        handlers.put(ProtocolType.C_Chat, packetHandler::CChatHandler);
    }

    public void handlePacket(ClientSession session, TextMessage message) {
        // tri consumer 에서 makePacket 을 찾은 이후, 패킷을 만들고 핸들러를 실행하도록 한다.
        // json
        String json = message.getPayload();
        ProtocolType protocol = PacketTranslator.protocol(json);

        TriConsumer<ClientSession, TextMessage, ProtocolType> action;
        if (onRecvs.containsKey(protocol)) {
            action = onRecvs.get(protocol);
            action.accept(session, message, protocol);
        }
    }

    public <T extends Protocol> void makePacket(ClientSession session, TextMessage message, ProtocolType protocol, Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            T pkt = PacketTranslator.object(message.getPayload());

            BiConsumer<ClientSession, Protocol> action;
            if (handlers.containsKey(protocol)) {
                action = handlers.get(protocol);
                action.accept(session, pkt);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
