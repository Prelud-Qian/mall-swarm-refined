package com.macro.mall.demo.auth;

import com.macro.mall.common.api.CommonResult;
import com.macro.mall.common.constant.AuthConstant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT 登录 demo：服务端不保存会话，每次请求靠验签认出用户。
 */
@Tag(name = "JwtAuthController", description = "JWT 登录 demo（无状态）")
@Controller
@RequestMapping("/jwt")
public class JwtAuthController {

    /** refreshToken -> username。演示用内存，重启即失效 */
    private static final Map<String, String> REFRESH_TOKENS = new ConcurrentHashMap<>();

    @Operation(summary = "登录，签发 accessToken + refreshToken")
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<Object> login(@RequestParam String username,
                                      @RequestParam String password,
                                      @RequestParam(defaultValue = "1800") long expireSeconds) {
        String user = DemoUserStore.authenticate(username, password);
        if (user == null){
            return CommonResult.failed("用户名或密码错误");
        }
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        REFRESH_TOKENS.put(refreshToken, user);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accessToken", JwtUtil.issue(user, expireSeconds));
        data.put("refreshToken", refreshToken);
        data.put("expireSeconds", expireSeconds);
        return CommonResult.success(data);
    }

    @Operation(summary = "查看当前用户（请求头带 Authorization: Bearer <token>）")
    @RequestMapping(value = "/me", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<Object> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractToken(authorization);
        if (token == null){
            return CommonResult.unauthorized("缺少 Authorization 请求头");
        }
        try{
            // 验签
            return CommonResult.success(JwtUtil.verify(token));
        } catch (JwtUtil.JwtException e){
            return CommonResult.unauthorized(e.getMessage());
        }
    }

    @Operation(summary = "退出登录（演示：服务端无法作废已签发的 accessToken）")
    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<Object> logout() {
        return CommonResult.success("服务端什么都没做 —— 无状态，没有东西可以作废。"
                + "你现在再调一次 /jwt/me，旧 token 依然有效", "已「退出」");
    }

    @Operation(summary = "用 refreshToken 换新的 accessToken（旧的立即作废）")
    @RequestMapping(value = "/refresh", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<Object> refresh(@RequestParam String refreshToken) {
        // remove 而不是 get：取走即作废，防止同一个 refreshToken 被重放
        String user = REFRESH_TOKENS.remove(refreshToken);
        if (user == null){
            return CommonResult.failed("refreshToken 无效或已被使用");
        }
        String newRefreshToken = UUID.randomUUID().toString().replace("-", "");
        REFRESH_TOKENS.put(newRefreshToken, user);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accessToken", JwtUtil.issue(user));
        data.put("refreshToken", newRefreshToken);
        return CommonResult.success(data);
    }

    /** 从 "Bearer xxx" 里取出 token */
    private static String extractToken(String authorization) {
        if (authorization == null || !authorization.startsWith(AuthConstant.JWT_TOKEN_PREFIX)) {
            return null;
        }
        return authorization.substring(AuthConstant.JWT_TOKEN_PREFIX.length()).trim();
    }
}
