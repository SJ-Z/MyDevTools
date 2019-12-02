package com.cose.start;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author sjzhang
 * @date 2019/9/3 11:22 AM
 * @description
 */
public class MyClassLoader extends ClassLoader {

    private String baseClassPath;

    public static Map<String, Class<?>> classMap = new HashMap<>();

    public MyClassLoader(ClassLoader parent, String baseClassPath) {
        super(parent);
        this.baseClassPath = baseClassPath;
    }

    /**
     * 重写 findClass方法，加载指定文件
     * @param name
     * @return
     * @throws ClassNotFoundException
     */
    @Override
    protected Class<?> findClass(String name) {
        String classPath = baseClassPath.concat(name);
        File classFile = new File(classPath);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            InputStream stream = new FileInputStream(classFile);
            int b;
            while((b = stream.read())!=-1){
                outputStream.write(b);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] bytes = outputStream.toByteArray();
        int pos = name.lastIndexOf(".class");
        String defineClassName = name.substring(0, pos).replace("/", ".");
        Class<?> defineClass = super.defineClass(defineClassName, bytes, 0, bytes.length);
        classMap.put(defineClassName, defineClass);
        return defineClass;
    }

    /**
     * 清除该ClassLoader加载的所有类
     */
    public void deleteClasses() {
        classMap.forEach((name, clazz) -> clazz = null);
    }
}
