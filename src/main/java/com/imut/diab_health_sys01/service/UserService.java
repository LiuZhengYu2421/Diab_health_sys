package com.imut.diab_health_sys01.service;

import com.imut.diab_health_sys01.dto.ChangePasswordRequest;
import com.imut.diab_health_sys01.dto.LoginRequest;
import com.imut.diab_health_sys01.vo.LoginResult;
import com.imut.diab_health_sys01.dto.RegisterRequest;
import com.imut.diab_health_sys01.dto.UpdateUserRequest;
import com.imut.diab_health_sys01.vo.UserInfoVO;

/**
 * 用户认证与信息管理服务
 */
public interface UserService {

    /** 登录 */
    LoginResult login(LoginRequest request);

    /** 注册 */
    LoginResult register(RegisterRequest request);

    /** 获取当前用户信息 */
    UserInfoVO getUserInfo(Integer userId);

    /** 更新个人信息 */
    UserInfoVO updateUserInfo(Integer userId, UpdateUserRequest request);

    /** 修改密码 */
    void changePassword(Integer userId, ChangePasswordRequest request);

    /** 注销账户（硬删除，物理删除记录，不可恢复） */
    void deleteAccount(Integer userId);
}
