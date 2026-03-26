package kr.co.seoulit.hospital.iam.rbac;

import kr.co.seoulit.hospital.iam.permission.Permission;
import kr.co.seoulit.hospital.iam.permission.PermissionRepository;
import kr.co.seoulit.hospital.iam.role.Role;
import kr.co.seoulit.hospital.iam.role.RolePermission;
import kr.co.seoulit.hospital.iam.role.RolePermissionRepository;
import kr.co.seoulit.hospital.iam.role.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RbacService {

    private final RoleRepository roleRepo;
    private final PermissionRepository permRepo;
    private final RolePermissionRepository rolePermRepo;

    public List<Role> listRoles() { return roleRepo.findAll(); }

    public List<Permission> listPermissions() { return permRepo.findAll(); }

    public Role upsertRole(Role r) { return roleRepo.save(r); }

    public Permission upsertPermission(Permission p) { return permRepo.save(p); }

    @Transactional
    public void setRolePermissions(String roleCode, List<String> permCodes) {
        roleRepo.findById(roleCode).orElseThrow(() -> new IllegalArgumentException("roleCode not found: " + roleCode));
        // validate perms exist
        for (String pc : permCodes) {
            permRepo.findById(pc).orElseThrow(() -> new IllegalArgumentException("permCode not found: " + pc));
        }
        rolePermRepo.deleteByRoleCode(roleCode);
        for (String pc : permCodes) {
            rolePermRepo.save(RolePermission.builder().roleCode(roleCode).permCode(pc).build());
        }
    }

    public List<String> getPermissionsByRoles(List<String> roleCodes) {
        return rolePermRepo.findPermCodesByRoleCodes(roleCodes).stream().distinct().toList();
    }
}
