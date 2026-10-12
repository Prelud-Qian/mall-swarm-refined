package com.macro.mall.demo.auth;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 手写 JWT：签发 + 验签。
 * 格式：base64url(header) . base64url(payload) . base64url(HMAC-SHA256(前两段, 密钥))
 */
public class JwtUtil {

    /**
     * 签名密钥。演示用硬编码；真实项目放配置中心/环境变量
     */
    private static final String SECRET = "mall-demo-jwt-secret-change-me-in-production";

    /**
     * 默认有效期：30 分钟
     */
    private static final long EXPIRE_SECONDS = 30 * 60;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private JwtUtil() {
    }

    /**
     * 签发 token，默认 30 分钟有效
     */
    public static String issue(String username) {
        return issue(username, EXPIRE_SECONDS);
    }

    /**
     * 签发 token，指定有效期（秒）。演示过期用
     */
    public static String issue(String username, long expireSeconds) {
        long now = System.currentTimeMillis() / 1000;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", username);
        payload.put("iat", now);
        payload.put("exp", now + expireSeconds);

        String content = encode(toJsonBytes(Map.of("alg", "HS256", "typ", "JWT")))
                + "." + encode(toJsonBytes(payload));
        return content + "." + sign(content);
    }

    /**
     * 验签并返回 payload。失败抛 JwtException，消息区分「格式错误 / 签名不匹配 / 已过期」。
     */
    public static Map<String, Object> verify(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtException("token 格式错误（应为 header.payload.signature 三段）");
        }
        // 固定时间比较，避免通过响应耗时逐字节猜签名
        /**
         * 这段代码是 JWT 验签的核心：重新算一遍签名，和 token 里带的签名比对，不一致就说明 token 被篡改或密钥不对。
         *
         * JWT 长这样：
         * Header.Payload.Signature
         * parts[0] parts[1]  parts[2]
         *
         * 验签逻辑：
         * ① 拿 parts[0] + "." + parts[1]（Header.Payload）
         * ② 用 secret 重新算一遍签名  →  sign(...)
         * ③ 把新签名 和 token 里的 parts[2] 比对
         * ④ 一致 → 通过；不一致 → 抛异常
         */
        if (!MessageDigest.isEqual(sign(parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new JwtException("签名不匹配：token 被篡改或密钥不对");
        }
        Map<String, Object> payload;
        try {
            payload = MAPPER.readValue(DECODER.decode(parts[1]), Map.class);
        } catch (Exception e) {
            throw new JwtException("payload 不是合法 JSON");
        }
        long exp = ((Number) payload.get("exp")).longValue();
        if (exp < System.currentTimeMillis() / 1000) {
            throw new JwtException("token 已过期");
        }
        return payload;
    }

    /**
     * HMAC-SHA256 签名，返回 base64url
     */
    private static String sign(String content) {
        try {
            // 获取一个用于计算 HMAC-SHA256 的“加密机器”实例  这一步只选算法，还没装密钥
            Mac mac = Mac.getInstance("HmacSHA256");
            /**
             * SECRET	你的密钥字符串（就是前面说的 secret）
             * .getBytes(StandardCharsets.UTF_8)	把字符串按 UTF-8 转成字节
             * new SecretKeySpec(字节, "HmacSHA256")	把字节包装成“密钥对象”
             * mac.init(...)	把密钥装进机器
             *
             * 告诉机器“用哪把钥匙来签名”。
             * 为什么必须指定 UTF-8？
             * 因为字符串转字节有多种编码，签和验必须用同一种，否则结果不一致。UTF-8 是标准选择。
             */
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            /**
             * content	要签名的数据（JWT 里通常是 Base64(Header) + "." + Base64(Payload)）
             * .getBytes(StandardCharsets.UTF_8)	数据转字节
             * mac.doFinal(...)	喂入数据，输出签名字节数组
             * encode(...)	把签名字节做 Base64 URL 编码（就是你前面看的 ENCODER.encodeToString）
             * return	返回最终的签名字符串
             */
            return encode(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("签名失败", e);
        }
    }

    private static byte[] toJsonBytes(Object value) {
        try {
            // 用 Jackson 的 ObjectMapper 把一个 Java 对象转成 JSON 字节并返回
            return MAPPER.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String encode(byte[] bytes) {
        return ENCODER.encodeToString(bytes);
    }

    /**
     * 验签失败的统一异常
     */
    public static class JwtException extends RuntimeException {
        public JwtException(String message) {
            super(message);
        }
    }
}
