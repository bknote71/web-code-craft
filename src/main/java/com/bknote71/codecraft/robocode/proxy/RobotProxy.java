package com.bknote71.codecraft.robocode.proxy;

import com.bknote71.codecraft.robocode.api.Robot;
import com.bknote71.codecraft.robocode.loader.RobotClassLoader;
import com.bknote71.codecraft.robocode.event.EventManager;
import com.bknote71.codecraft.robocode.robointerface.IBasicRobotPeer;
import com.bknote71.codecraft.robocode.robointerface.IRobotPeer;
import com.bknote71.codecraft.robocode.core.*;

/**
 * 로봇 프록시
 * (커스텀) 로봇을 로드하고, 그 로봇을 실행시키는 주체
 */
public abstract class RobotProxy implements Runnable, IRobotPeer { // 로봇 피어와 1:1 대응

    private final RobotSpecification robotSpecification;

    // 이벤트를 관리하는 이벤트 매니저
    protected EventManager eventManager;

    // 호스트를 관리? 호스트 PC 관리 용 매니저라는 소리인가봄
    // private final IHostManager hostManager;

    // 로봇 프록시(?)를 실행시킬 스레드 관리하는 매니저
    protected RobotThreadManager robotThreadManager;
    // 배틀 스레드, 로봇 스레드를 관리하는 스레드 매니저 (?)
    private ThreadManager threadManager;

    // 팔요
    private Robot robot;
    protected final RobotPeer peer;

    // "커스텀 로봇" 정보를 로드할 로더
    protected RobotClassLoader robotClassLoader;

    protected final RobotStatics statics;

    public RobotProxy(RobotSpecification robotSpecification, RobotPeer robotPeer, RobotStatics statics) {
        this.peer = robotPeer;
        this.statics = statics;
        this.robotSpecification = robotSpecification;

        robotClassLoader = createLoader(robotSpecification);
        robotClassLoader.setRobotProxy(this);

        robotThreadManager = new RobotThreadManager(this);

        // 로봇 클래스 로드
        loadClassBattle();
    }

    public Robot getRobot() {
        return robot;
    }

    public RobotPeer getRobotPeer() {
        return peer;
    }

    public ClassLoader getRobotClassloader() {
        return robotClassLoader;
    }

    public RobotStatics getStatics() {
        return statics;
    }

    private RobotClassLoader createLoader(RobotSpecification robotSpecification) { // 유저 네임 붙이기.
        System.out.println("create loader spec index: "  + robotSpecification.getSpecIndex());
        return new RobotClassLoader(robotSpecification.getUsername(), robotSpecification.getSpecIndex(), robotSpecification.getFullClassName());
    }

    protected abstract void initializeBattle(ExecCommands commands, RobotStatus stat);

    public void startBattle(ExecCommands commands, RobotStatus stat) {
        initializeBattle(commands, stat);
        threadManager = null;
        robotThreadManager.start(threadManager);
    }

    private void loadClassBattle() {
        robotClassLoader.loadRobotMainClass(true);
    }

    // 매번 죽고 태어날 때마다 로봇 로드
    private boolean loadRobot() {
        robot = null;
        try {
             robot = robotClassLoader.createRobotInstance();

            if (robot == null) {
                System.out.println("로봇 생성 실패");
                return false;
            }

            // robot 의 액션 -> peer 호출
            // 그럼 이 프록시의 액션을 호출한다는 것이다.
            // 그러면 이 프록시는 실제의 로봇 피어를 호출하면 된다.
            robot.setPeer((IBasicRobotPeer) this);
            eventManager.setRobot(robot);
        } catch (Exception e) {
            robot = null;
            return false;
        }
        return true;
    }

    // 로봇 로직 수행
    protected abstract void executeImpl();

    @Override
    public void run() {
        try {
            if (loadRobot() && robot != null) {
                // 로봇 피어 실행 시작
                peer.setRunning(true);

                // 처음 시작할 때의 이벤트 처리
                eventManager.processEvents();

                // 유저 코드 실행
                callUserCode();
            }

            // 일반 로봇 로직 수행 (이벤트에 대응만 하는 건가?)
            while (peer.isRunning())
                executeImpl();

            peer.setRunning(false);
        } catch (Exception e) {
            System.out.println("thread interrupted? " + Thread.interrupted());
        }
    }

    private void callUserCode() {
        Runnable runnable = robot.getRobotRunnable();
        if (runnable != null)
            runnable.run();
    }

    public void cleanup() {
        robot = null;
        System.out.println("robot proxy cleanup");

        if (robotThreadManager != null) {
            robotThreadManager.cleanup();
            robotThreadManager = null;
        }

        if (robotClassLoader != null) {
            robotClassLoader.cleanup();
            robotClassLoader = null;
        }
    }
}
