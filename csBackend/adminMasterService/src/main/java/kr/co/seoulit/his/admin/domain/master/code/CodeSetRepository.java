package kr.co.seoulit.his.admin.domain.master.code;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CodeSetRepository extends JpaRepository<CodeSet, Long>, JpaSpecificationExecutor<CodeSet> {
    Optional<CodeSet> findByCodeSetKey(String codeSetKey);
}
