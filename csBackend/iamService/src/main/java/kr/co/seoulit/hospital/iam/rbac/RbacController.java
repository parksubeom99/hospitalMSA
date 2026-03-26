package kr.co.seoulit.hospital.iam.rbac;

import kr.co.seoulit.hospital.iam.permission.Permission;
import kr.co.seoulit.hospital.iam.role.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rbac")
public class RbacController {

    private final RbacService rbacService;

    @GetMapping("/roles")
    public List<Role> roles() { return rbacService.listRoles(); }

    @PostMapping("/roles")
    public Role upsertRole(@RequestBody Role role) { return rbacService.upsertRole(role); }

    @GetMapping("/permissions")
    public List<Permission> permissions() { return rbacService.listPermissions(); }

    @PostMapping("/permissions")
    public Permission upsertPermission(@RequestBody Permission perm) { return rbacService.upsertPermission(perm); }

    public record SetPermsRequest(List<String> permCodes) {}

    @PutMapping("/roles/{roleCode}/permissions")
    public void setRolePerms(@PathVariable String roleCode, @RequestBody SetPermsRequest req) {
        rbacService.setRolePermissions(roleCode, req.permCodes() == null ? List.of() : req.permCodes());
    }
}
