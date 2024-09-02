package com.whut.friends.interceptor;

import com.whut.friends.model.entity.User;
import com.whut.friends.model.enums.UserRoleEnum;
import com.whut.friends.utils.UserHolder;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 校验用户是否为管理员
 * @author whut2024
 * @since 2024-09-02
 */
public class RoleInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {

        final User user = UserHolder.get();

        if (UserRoleEnum.isAdmin(user))
            return true;

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return false;
    }
}
