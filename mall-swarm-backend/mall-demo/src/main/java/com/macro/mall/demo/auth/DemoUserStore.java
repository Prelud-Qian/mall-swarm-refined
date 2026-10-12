package com.macro.mall.demo.auth;

import java.util.Map;

/**
 * Demo 专用内存用户表。
 * 只服务于登录机制演示，密码明文存储 —— 真实项目必须存 BCrypt 哈希。
 */
public class DemoUserStore {

    /** 用户名 -> 密码 */
    private static final Map<String, String> USERS = Map.of(
            "admin", "123456",
            "user", "123456"
    );

    private DemoUserStore() {
    }

    /**
     * 校验账号密码，通过返回用户名，否则返回 null。
     */
    public static String authenticate(String username, String password) {
        String stored = USERS.get(username);
        return stored != null && stored.equals(password) ? username : null;
    }
}
