package com.whut.friends.interceptor;

import com.whut.friends.model.entity.User;
import com.whut.friends.model.enums.UserRoleEnum;
import com.whut.friends.utils.NetUtils;
import com.whut.friends.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 校验用户是否为管理员
 * @author whut2024
 * @since 2024-09-02
 */
@Slf4j
public class RoleInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {

        final User user = UserHolder.get();

        if (UserRoleEnum.isAdmin(user))
            return true;

        log.warn("no authorization ip: {}", NetUtils.getIpAddress(request));
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return false;
    }
}
