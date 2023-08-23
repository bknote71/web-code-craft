package com.bknote71.codecraft.robocode.loader;

import com.bknote71.codecraft.robocode.api.Robot;
import com.bknote71.codecraft.robocode.proxy.RobotProxy;

import java.util.HashSet;
import java.util.Set;

public class RobotClassLoader extends ClassLoader {

    // aws s3 class loader
    private AwsS3ClassLoader classLoader;

    private String author;
    private int specIndex;
    private String fullClassName;

    private RobotProxy robotProxy;
    private Class<?> robotClass;

    private Set<String> referencedClasses = new HashSet<String>();

    public RobotClassLoader(String author, int specIndex, String fullClassName) {
        this.author = author;
        this.fullClassName = fullClassName;
        this.specIndex = specIndex;
        classLoader = new AwsS3ClassLoader("robot-class");
    }

    public void setRobotProxy(RobotProxy robotProxy) {
        this.robotProxy = robotProxy;
    }

    public synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> result = classLoader.findClass(name);
        if (result != null)
            return result;

        // 클래스를 로드할 수 없습니다.
        return null;
    }

//    private Class<?> loadRobotClassLocaly(String name, boolean resolve) throws ClassNotFoundException {
//        Class<?> result = findLoadedClass(name);
//        if (result == null) {
//            ByteBuffer resource = findLocalResource(name);
//            if (resource != null) {
//                result = defineClass(name, resource, codeSource);
//                if (resolve) {
//                    resolveClass(result);
//                }
//                ClassAnalyzer.getReferencedClasses(resource, referencedClasses);
//            }
//        }
//        return result;
//    }
//
//    private ByteBuffer findLocalResource(final String name) {
//        return AccessController.doPrivileged(new PrivilegedAction<ByteBuffer>() {
//            public ByteBuffer run() {
//                // try to find it in robot's class path
//                // this is URL, don't change to File.pathSeparator
//                String path = name.replace('.', '/').concat(".class");
//                URL url = findResource(path);
//
//                return ClassFileReader.readClassFileFromURL(url);
//            }
//        });
//    }

    public synchronized Class<?> loadRobotMainClass(boolean resolve) {
        try {
            if (robotClass == null) {
                robotClass = loadClass(author + "/" + specIndex + "/" + fullClassName, resolve);
                if (robotClass == null || !Robot.class.isAssignableFrom(robotClass)) {
                    System.out.println("robot class is null? " + robotClass);
                    return null;
                }
                if (resolve) {
                    // 의존성 확인
                    HashSet<String> clone;

                    do {
                        clone = new HashSet<>(referencedClasses);
                        for (String reference : clone) {
                            loadClass(reference);
                        }
                    } while (referencedClasses.size() != clone.size());
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return robotClass;
    }

    public Robot createRobotInstance() throws InstantiationException, IllegalAccessException {
        loadRobotMainClass(true);
        return (Robot) robotClass.newInstance();
    }

    public void cleanup() {
        classLoader = null;
        robotProxy = null;
        robotClass = null;
    }
}
