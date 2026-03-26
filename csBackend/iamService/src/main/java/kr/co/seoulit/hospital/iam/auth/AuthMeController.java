package kr.co.seoulit.hospital.iam.auth;

import kr.co.seoulit.hospital.iam.rbac.RbacService;
import kr.co.seoulit.hospital.iam.user.UserAccount;
import kr.co.seoulit.hospital.iam.user.UserAccountRepository;
import kr.co.seoulit.hospital.iam.user.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthMeController {

    private final UserAccountRepository userRepo;
    private final UserRoleRepository userRoleRepo;
    private final RbacService rbacService;

    @GetMapping("/me")
    public MeResponse me(@RequestHeader(value = "X-Login-Id", required = false) String loginIdHeader) {
        String loginId = resolveLoginId(loginIdHeader);
        UserAccount user = userRepo.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다."));
        List<String> roles = userRoleRepo.findByUserId(user.getUserId()).stream()
                .map(ur -> ur.getRoleCode()).distinct().toList();
        List<String> perms = rbacService.getPermissionsByRoles(roles);
        String primaryRole = roles.isEmpty() ? "USER" : roles.get(0);
        return new MeResponse(user.getUserId(), user.getLoginId(), user.getStaffId(), roles, perms,
                new AuthService.UserView(user.getLoginId(), user.getLoginId(), primaryRole, user.getStaffId()));
    }

    private String resolveLoginId(String loginIdHeader) {
        if (loginIdHeader != null && !loginIdHeader.isBlank()) return loginIdHeader;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null && !auth.getName().isBlank()) return auth.getName();
        throw new IllegalArgumentException("로그인 정보가 없습니다.");
    }

    public record MeResponse(Long userId, String loginId, String staffId, List<String> roles,
                             List<String> permissions, AuthService.UserView user) {}
}
