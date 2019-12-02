package com.cose.start;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author sjzhang
 * @date 2019/9/1 10:29 PM
 * @description 模拟devtools实现热部署
 *
 * JVM中的Class只有满足以下三个条件，才能被GC回收，也就是该Class被卸载（unload）：
 *    - 该类所有的实例都已经被GC，也就是JVM中不存在该Class的任何实例。
 *    - 加载该类的ClassLoader已经被GC。
 *    - 该类的java.lang.Class 对象没有在任何地方被引用，如不能在任何地方通过反射访问该类的方法.
 */
public class MyDevTools {

    public static void main(String[] args) throws Exception {
        //死循环模拟生产环境
        while (true) {
            //项目路径
            String projectPath = System.getProperty("user.dir");
            //生成.class文件的路径
            String classesPath = projectPath + "/out/production/MyDevTools/";
            //定义一个类加载器，指定类的父加载器为空，绕过双亲委派机制
            MyClassLoader loader = new MyClassLoader(null, classesPath);
            //类路径下所有.java格式的文件
            List<String> allClassPath = getAllClassPath(projectPath.concat("/src"), new LinkedList<>());
            //由于本项目需要，排除掉src目录下属于com/cose/start目录下的类
            allClassPath = allClassPath.stream()
                    .filter(path -> !path.contains("com/cose/start"))
                    .collect(Collectors.toList());
            //将List写入到一个txt文件中
            String sourceFilePath = "./source.txt";
            writeFileContext(allClassPath, sourceFilePath);
            //同时编译所有的.java文件为.class
            Process process = Runtime.getRuntime().exec("javac -d " + classesPath + " @" + sourceFilePath);
            process.waitFor();
            //加载类
            for (String path : allClassPath) {
                int pos1 = path.lastIndexOf("src/") + 4;
                String className = path.substring(pos1).replace("java", "class");
                loader.loadClass(className);
            }
            //项目启动类名称
            String startClassName = "com.jvm.plugin.TestTools";
            Class<?> clazzTestTools = MyClassLoader.classMap.get(startClassName);
            Method method = clazzTestTools.getMethod("main", new Class[]{String[].class});
            method.invoke(null, new String[]{null});

            //类卸载
            loader.deleteClasses();
            System.out.println("gc1...类卸载");
            System.gc();
            Thread.sleep(3000);
            loader = null;
            System.out.println("gc2...类加载器卸载");
            System.gc();

            //休眠5秒后重启
            Thread.sleep(5000);
        }
    }

    /**
     * 递归获取某路径下的所有.java格式的文件
     * @param path
     * @return 所有文件地址
     */
    public static List<String> getAllClassPath(String path, List<String> pathList) {
        File file = new File(path);
        //如果这个路径是文件夹
        if (file.isDirectory()) {
            //获取路径下的所有文件
            File[] files = file.listFiles();
            if (files != null) {
                for (File curFile : files) {
                    //如果还是文件夹，递归获取里面的文件，文件夹
                    if (curFile.isDirectory()) {
                        getAllClassPath(curFile.getPath(), pathList);
                    } else {
                        String filePath = curFile.getPath();
                        if (filePath.endsWith(".java")) {
                            pathList.add(filePath);
                        }
                    }
                }
            }
        } else {
            String filePath = file.getPath();
            if (filePath.endsWith(".java")) {
                pathList.add(filePath);
            }
        }
        return pathList;
    }

    /**
     * 将list按行写入到txt文件中
     * @param strings
     * @param path
     * @throws Exception
     */
    public static void writeFileContext(List<String> strings, String path) throws Exception {
        File file = new File(path);
        //如果没有文件就创建
        if (!file.isFile()) {
            file.createNewFile();
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(path));
        for (String str : strings){
            writer.write(str + "\r\n");
        }
        writer.close();
    }

}
