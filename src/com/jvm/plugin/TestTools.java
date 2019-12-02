package com.jvm.plugin;

import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

/**
 * @author sjzhang
 * @date 2019/9/1 10:41 PM
 * @description
 */
public class TestTools {

    public static void main(String[] args) {
        System.out.println("[TestTools] Constant.k:" + Constant.k);
        List<String> list = new ArrayList<>();
        list.add("1");
        System.out.println(list);
        DriverManager.getLoginTimeout();
    }

}
