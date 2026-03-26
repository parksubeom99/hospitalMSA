package kr.co.seoulit.hospital.iam.role;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByRoleCode(String roleCode);

    void deleteByRoleCode(String roleCode);

    @Query("select rp.permCode from RolePermission rp where rp.roleCode in :roleCodes")
    List<String> findPermCodesByRoleCodes(Collection<String> roleCodes);
}
