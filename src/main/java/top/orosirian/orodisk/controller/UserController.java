package top.orosirian.orodisk.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import org.springframework.web.bind.annotation.*;
import top.orosirian.orodisk.model.Result;
import top.orosirian.orodisk.model.request.LoginRequest;
import top.orosirian.orodisk.model.request.RegisterRequest;
import top.orosirian.orodisk.model.request.UpdatePasswordRequest;
import top.orosirian.orodisk.model.response.LoginResponse;
import top.orosirian.orodisk.model.response.UserInfoResponse;
import top.orosirian.orodisk.service.UserService;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

    @SaCheckLogin
    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.success();
    }

    @SaCheckLogin
    @GetMapping("/info")
    public Result<UserInfoResponse> getCurrentUser() {
        return Result.success(userService.getCurrentUser());
    }

    @SaCheckLogin
    @GetMapping("/info/{userId}")
    public Result<UserInfoResponse> getUserById(@PathVariable Long userId) {
        return Result.success(userService.getUserById(userId));
    }

    @SaCheckLogin
    @PutMapping("/password")
    public Result<Void> updatePassword(@RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(request.getOldPassword(), request.getNewPassword());
        return Result.success();
    }

}
