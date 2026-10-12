package com.macro.mall.demo.auth;

import com.macro.mall.common.api.CommonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cookie + Session 登录 demo：状态存在服务端内存，浏览器只带一串 sessionId。
 * 与 JwtAuthController 对照着看 —— 这里每一行都在跟服务端存储打交道。
 */
@Tag(name = "SessionAuthController", description = "Cookie+Session 登录 demo（有状态）")
@Controller
@RequestMapping("/session")
public class SessionAuthController {

    /** session 里存用户名的 key */
    private static final String USER_KEY = "user";

    @Operation(summary = "登录：服务端创建 session，响应头下发 JSESSIONID Cookie")
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<Object> login(@RequestParam String username,
                                      @RequestParam String password,
                                      HttpServletRequest request) {
        String user = DemoUserStore.authenticate(username, password);
        if (user == null){
            return CommonResult.failed("用户名或密码错误");
        }
        // 核心动作①：服务端凭空开辟一块内存，把用户塞进去
        HttpSession session = request.getSession();
        session.setAttribute(USER_KEY, user);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", session.getId());
        data.put("提示", "sessionId 已经通过响应头 Set-Cookie 下发，之后浏览器每次请求自动带上");
        return CommonResult.success(data);
    }

    @Operation(summary = "查看当前用户（浏览器自动带 Cookie，不需要任何请求头）")
    @RequestMapping(value = "/me", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<Object> me(HttpServletRequest request) {
        // 核心动作②：false = 只查不建。写成无参的话，未登录的请求也会被送一个新 session
        HttpSession session = request.getSession(false);
        if (session == null){
            return CommonResult.unauthorized("服务端找不到这个会话，请先登录");
        }
        // 核心动作③：从服务端内存里把用户取出来 —— 请求里除了 sessionId 什么凭证都没有
        return CommonResult.success(describe(session, (String) session.getAttribute(USER_KEY)));
    }

    @Operation(summary = "退出登录：服务端销毁 session，立即失效")
    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<Object> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            // 核心动作④：服务端把这块内存删掉。这是 JWT 做不到的事
            session.invalidate();
        }
        return CommonResult.success("会话已在服务端销毁，同一个 Cookie 再请求会被当成未登录");
    }

    @Operation(summary = "查看服务端保存的会话状态（未登录也返回，用来看服务端记住了什么）")
    @RequestMapping(value = "/stat", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<Object> stat(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return CommonResult.success("服务端没有这个会话（还没登录，或者已退出/已过期）");
        }
        return CommonResult.success(describe(session, (String) session.getAttribute(USER_KEY)));
    }

    /** 把服务端内存里这块会话状态读出来给前端看 */
    private static Map<String, Object> describe(HttpSession session, String user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", user);
        data.put("sessionId", session.getId());
        data.put("session创建时间", new Date(session.getCreationTime()));
        data.put("最后访问时间", new Date(session.getLastAccessedTime()));
        data.put("空闲超时(秒)", session.getMaxInactiveInterval());
        return data;
    }
}
