package top.orosirian.orodisk.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import top.orosirian.orodisk.model.Result;
import top.orosirian.orodisk.model.request.CreateShareRequest;
import top.orosirian.orodisk.model.request.VerifyShareRequest;
import top.orosirian.orodisk.model.response.ShareInfoResponse;
import top.orosirian.orodisk.model.response.ShareResponse;
import top.orosirian.orodisk.service.ShareService;

import java.util.List;

@RestController
@RequestMapping("/share")
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @SaCheckLogin
    @PostMapping("/create")
    public Result<ShareResponse> createShare(@RequestBody CreateShareRequest request) {
        return Result.success(shareService.createShare(request));
    }

    @GetMapping("/info/{shareCode}")
    public Result<ShareInfoResponse> getShareInfo(@PathVariable String shareCode) {
        return Result.success(shareService.getShareInfo(shareCode));
    }

    @PostMapping("/verify")
    public Result<Boolean> verifyPassword(@RequestBody VerifyShareRequest request) {
        return Result.success(shareService.verifyPassword(request));
    }

    @GetMapping("/download/{shareCode}")
    public void downloadShare(
            @PathVariable String shareCode,
            @RequestParam(required = false) String password,
            HttpServletResponse response) throws Exception {
        shareService.downloadShare(shareCode, password, response);
    }

    @GetMapping("/preview/{shareCode}")
    public void previewShare(
            @PathVariable String shareCode,
            @RequestParam(required = false) String password,
            HttpServletResponse response) throws Exception {
        shareService.previewShare(shareCode, password, response);
    }

    @SaCheckLogin
    @GetMapping("/list")
    public Result<List<ShareResponse>> listMyShares() {
        return Result.success(shareService.listMyShares());
    }

    @SaCheckLogin
    @DeleteMapping("/cancel/{shareId}")
    public Result<Void> cancelShare(@PathVariable Long shareId) {
        shareService.cancelShare(shareId);
        return Result.success();
    }

}
