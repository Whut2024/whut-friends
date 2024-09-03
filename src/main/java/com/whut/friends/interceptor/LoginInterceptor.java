package com.whut.friends.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTUtil;
import com.whut.friends.common.ErrorCode;
import com.whut.friends.constant.UserConstant;
import com.whut.friends.exception.ThrowUtils;
import com.whut.friends.model.entity.User;
import com.whut.friends.utils.UserHolder;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * @author whut2024
 * @since 2024-09-01
 */
@AllArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {


    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {
        final String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token))
            return true;

        final JWTPayload payload = JWTUtil.parseToken(token).getPayload();

        final Object object = payload.getClaim(UserConstant.TTL);
        ThrowUtils.throwIf(object == null, ErrorCode.PARAMS_ERROR);
        final long expireTime = (Long) object;
        if (expireTime < System.currentTimeMillis())
            return true;

        final User user = JSONUtil.toBean((JSONObject) payload.getClaim(UserConstant.USER_KEY), User.class);

        final String version = (String) payload.getClaim(UserConstant.VERSION_KEY);
        final String cacheKey = UserConstant.USER_LOGIN_VERSION + user.getId();

        final String cacheVersion = redisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cacheVersion))
            redisTemplate.expire(cacheKey, UserConstant.USER_LOGIN_VERSION_TTL, TimeUnit.MINUTES);

        if (!StrUtil.isAllNotBlank(version, cacheVersion) || !version.equals(cacheVersion))
            return true;

        UserHolder.set(user);
        return true;
    }

    @Override
    public void postHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, ModelAndView modelAndView) throws Exception {
        UserHolder.remove();
    }
}
