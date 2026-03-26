package kr.co.seoulit.his.admin.domain.master.code;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CodeItemRepository extends JpaRepository<CodeItem, Long>, JpaSpecificationExecutor<CodeItem> {
    List<CodeItem> findByCodeSet_CodeSetKeyOrderBySortOrderAsc(String codeSetKey);

    Page<CodeItem> findByCodeSet_CodeSetKey(String codeSetKey, Pageable pageable);
}
