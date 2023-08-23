package com.bknote71.codecraft.robocode.proxy;


import com.bknote71.codecraft.robocode.api.Bullet;
import com.bknote71.codecraft.robocode.event.Event;
import com.bknote71.codecraft.robocode.robointerface.IBasicRobotPeer;
import com.bknote71.codecraft.util.Utils;
import com.bknote71.codecraft.robocode.core.*;
import com.bknote71.codecraft.robocode.event.*;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class BasicRobotProxy extends RobotProxy implements IBasicRobotPeer {
    private static final long
            MAX_SET_CALL_COUNT = 10000,
            MAX_GET_CALL_COUNT = 10000;

    private RobotStatus status;
    private boolean isDisabled;
    protected ExecCommands commands;
    private ExecResults execResults;

    // 로봇 개인이 관리하는 불릿
    private final Map<Integer, Bullet> bullets = new ConcurrentHashMap<Integer, Bullet>();
    // private int nextBulletId; // 0 is used for bullet explosions

    private final AtomicInteger setCallCount = new AtomicInteger(0);
    private final AtomicInteger getCallCount = new AtomicInteger(0);

    // protected Condition waitCondition;
    private boolean testingCondition;
    private double firedEnergy;
    private double firedHeat;

    public BasicRobotProxy(RobotSpecification robotSpecification, RobotPeer robotPeer, RobotStatics statics) {
        super(robotSpecification, robotPeer, statics);

        // 로봇의 이벤트를 처리할 이벤트 매니저
        System.out.println("robotpeer? " + robotPeer + " name? " + robotPeer.getName());
        eventManager = new EventManager(this);
    }

    @Override
    protected void initializeBattle(ExecCommands commands, RobotStatus stat) {
        updateStatus(commands, stat);
        eventManager.reset();
        eventManager.add(new StatusEvent(status)); // start event
    }
    // 프록시 역할: 부가 기능

    // blocking actions
    public void execute() {
        executeImpl();
    }

    // asynchronous actions
    public Bullet setFire(double power) {
        setCall();
        return setFireImpl(power); // 총알 업데이트 커맨드
    }

    public void move(double distance) {
        setMoveImpl(distance); // set move 커맨드
        do {
            execute(); // Always tick at least once
        } while (getDistanceRemaining() != 0);
    }

    public void turnBody(double radians) {
        setTurnBodyImpl(radians);
        do {
            execute(); // Always tick at least once
        } while (getBodyTurnRemaining() != 0);
    }

    public void turnGun(double radians) {
        setTurnGunImpl(radians);
        do {
            execute(); // Always tick at least once
        } while (getGunTurnRemaining() != 0);
    }

    public Bullet fire(double power) {
        Bullet bullet = setFire(power); // 총알 업데이트 커맨드 추가 (총알 생성)
        execute();
        return bullet;
    }

    // set robot image
    public void setRobotImage(String robotImageName) {
        peer.setRobotImage(robotImageName);
    }

    // fast setters
    public void setBodyColor(Color color) {
        commands.setBodyColor(color != null ? color.getRGB() : ExecCommands.defaultBodyColor);
    }

    public void setGunColor(Color color) {
        commands.setGunColor(color != null ? color.getRGB() : ExecCommands.defaultGunColor);
    }

    public void setRadarColor(Color color) {
        commands.setRadarColor(color != null ? color.getRGB() : ExecCommands.defaultRadarColor);
    }

    public void setBulletColor(Color color) {
        commands.setBulletColor(color != null ? color.getRGB() : ExecCommands.defaultBulletColor);
    }

    public void setScanColor(Color color) {
        commands.setScanColor(color != null ? color.getRGB() : ExecCommands.defaultScanColor);
    }

    // counters
    public void setCall() {
//        if (!isDisabled) {
//            final int res = setCallCount.incrementAndGet();
//
//            if (res >= MAX_SET_CALL_COUNT) {
//                isDisabled = true;
//            }
//        }
    }

    public void getCall() {
//        if (!isDisabled) {
//            final int res = getCallCount.incrementAndGet();
//
//            if (res >= MAX_GET_CALL_COUNT) {
//                isDisabled = true;
//            }
//        }
    }

    public double getDistanceRemaining() {
        getCall();
        return commands.getDistanceRemaining();
    }

    public double getRadarTurnRemaining() {
        getCall();
        return commands.getRadarTurnRemaining();
    }

    public double getBodyTurnRemaining() {
        getCall();
        return commands.getBodyTurnRemaining();
    }

    public double getGunTurnRemaining() {
        getCall();
        return commands.getGunTurnRemaining();
    }

    public double getVelocity() {
        getCall();
        return status.getVelocity();
    }

    public double getGunCoolingRate() {
        getCall();
        return statics.getBattleRules().getGunCoolingRate();
    }

    public String getName() {
        getCall();
        return statics.getName();
    }

    public long getTime() {
        getCall();
        return getTimeImpl();
    }

    private long getTimeImpl() {
        return status.getTime();
    }

    public double getBodyHeading() {
        getCall();
        return status.getHeadingRadians();
    }

    public double getGunHeading() {
        getCall();
        return status.getGunHeadingRadians();
    }

    public double getRadarHeading() {
        getCall();
        return status.getRadarHeadingRadians();
    }

    public double getEnergy() {
        getCall();
        return getEnergyImpl();
    }

    public double getGunHeat() {
        getCall();
        return getGunHeatImpl();
    }

    public double getX() {
        getCall();
        return status.getX();
    }

    public double getY() {
        getCall();
        return status.getY();
    }

    public int getOthers() {
        getCall();
        return status.getOthers();
    }

    public double getBattleFieldHeight() {
        getCall();
        return statics.getBattleRules().getBattlefieldHeight();
    }

    public double getBattleFieldWidth() {
        getCall();
        return statics.getBattleRules().getBattlefieldWidth();
    }

    public void rescan() {
        boolean reset = false;
        boolean origInterruptableValue = false;

        // 스캔이 우선순위가 가장 높다면 스캔 (?? 뭔지 모르겠으니까 패스)
        if (eventManager.getCurrentTopEventPriority() == eventManager.getScannedRobotEventPriority()) {
            reset = true;
            origInterruptableValue = eventManager.isInterruptible(eventManager.getScannedRobotEventPriority());
            eventManager.setInterruptible(eventManager.getScannedRobotEventPriority(), true);
        }

        commands.setScan(true);
        executeImpl();

        if (reset) {
            eventManager.setInterruptible(eventManager.getScannedRobotEventPriority(), origInterruptableValue);
        }
    }

    // 로봇 로직(커맨드) 수행
    @Override
    protected final void executeImpl() {
        // interrupt 확인
        if (Thread.currentThread().isInterrupted()) {
            System.out.println("thread is interrupted so return");
            return;
        }

        if (execResults == null) {
            // this is to slow down undead robot after cleanup, from fast exception-loop
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
        }

        // Entering tick
        if (robotThreadManager == null)
            return;
        robotThreadManager.checkRunThread();

        // setSetCallCount(0);
        // setGetCallCount(0);

        // TODO: This stops autoscan from scanning...
//        if (waitCondition != null && waitCondition.test()) {
//            waitCondition = null;
//            commands.setScan(true);
//        }

        // Call server(peer), 피어의 실행 작업, 이벤트, .. 래퍼 가져오기
        // execResult: 실행해야 할 커맨드, 이벤트 래퍼
        execResults = peer.executeImpl(commands);

        // 상태 업데이트
        updateStatus(execResults.getCommands(), execResults.getStatus());

        firedEnergy = 0;
        firedHeat = 0;

        // add new events
        eventManager.add(new StatusEvent(execResults.getStatus()));
        if (execResults.isPaintEnabled()) {
            // Add paint event, if robot is a paint robot and its painting is enabled
            eventManager.add(new PaintEvent());
        }

        // add other events, 기존의 이벤트
        if (execResults.getEvents() != null) {
            for (Event event : execResults.getEvents()) {
                eventManager.add(event);
            }
        }

        // 총알 업데이트 to bullet
        if (execResults.getBulletUpdates() != null) {
            for (BulletStatus bulletStatus : execResults.getBulletUpdates()) {
                final Bullet bullet = bullets.get(bulletStatus.bulletId);
                if (bullet != null) {
                    bullet.update(bulletStatus.x, bulletStatus.y, bulletStatus.victimName,
                            bulletStatus.isActive);

                    if (!bulletStatus.isActive) {
                        bullets.remove(bulletStatus.bulletId);
                    }
                }
            }
        }

        // 이벤트 처리토록 한다.
        try {
            eventManager.processEvents();
        } catch (EventInterruptedException e) {
            // 기존 스캔 이벤트
        }
    }

    private void updateStatus(ExecCommands commands, RobotStatus status) {
        this.commands = commands; // update commands
        this.status = status;
        // System.out.println("update status and energy: " + status.getEnergy() + " and gunHeat: " + status.getGunHeat());
    }

    // 에너지는 없음 <<
    private final double getEnergyImpl() {
//        return status.getEnergy() - firedEnergy;
        return status.getEnergy();
    }

    private final double getGunHeatImpl() {
        return status.getGunHeat() + firedHeat;
    }

    protected final void setMoveImpl(double distance) {
        if (getEnergyImpl() == 0) {
            return;
        }
        commands.setDistanceRemaining(distance);
        commands.setMoved(true);
    }

    private final Bullet setFireImpl(double power) {
        if (getGunHeatImpl() > 0 || getEnergyImpl() == 0) {
            System.out.println("총알 못쏨: " + getGunHeatImpl() + ", " + getEnergyImpl());
            return null;
        }

        power = min(getEnergyImpl(), min(max(power, Rules.MIN_BULLET_POWER), Rules.MAX_BULLET_POWER));

        Bullet bullet;
        BulletCommand wrapper;
        Event currentTopEvent = eventManager.getCurrentTopEvent();

        int nextBulletId = BulletIdGenerator.Instance.getId();

        if (currentTopEvent != null && currentTopEvent.getTime() == status.getTime()
                && status.getGunHeadingRadians() == status.getRadarHeadingRadians()
                && ScannedRobotEvent.class.isAssignableFrom(currentTopEvent.getClass())) {
            // this is angle assisted bullet
            System.out.println("scan 하여 쏘는 총알: angle assisted bullet");
            ScannedRobotEvent e = (ScannedRobotEvent) currentTopEvent;
            double fireAssistAngle = Utils.normalAbsoluteAngle(status.getHeadingRadians() + e.getBearingRadians());

            bullet = new Bullet(fireAssistAngle, getX(), getY(), power, statics.getName(), null, true, nextBulletId);
            wrapper = new BulletCommand(power, true, fireAssistAngle, nextBulletId);
        } else {
            // this is normal bullet
            System.out.println("그냥 쏘는 총알: normal bullet");
            bullet = new Bullet(status.getGunHeadingRadians(), getX(), getY(), power, statics.getName(), null, true,
                    nextBulletId);
            wrapper = new BulletCommand(power, false, 0, nextBulletId);
        }

        firedEnergy += power;
        firedHeat += Rules.getGunHeat(power);

        // 불릿 커맨드 추가 (이게 핵심)
        System.out.println("command bullet 추가");
        commands.getBullets().add(wrapper);

        // 이거는 일단 신경 ㄴㄴ
        // bullets.put(nextBulletId, bullet);

        return bullet;
    }

    // turn command
    protected final void setTurnGunImpl(double radians) {
        commands.setGunTurnRemaining(radians);
    }

    protected final void setTurnBodyImpl(double radians) {
        if (getEnergyImpl() > 0) {
            commands.setBodyTurnRemaining(radians);
        }
    }

    protected final void setTurnRadarImpl(double radians) {
        commands.setRadarTurnRemaining(radians);
    }

    public void cleanup() {
        super.cleanup();

        if (eventManager != null) {
            eventManager.cleanup();
            eventManager = null;
        }

        execResults = null;
        status = null;
        commands = null;
    }
}
