package moa.category.internal;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// 기본 분류 템플릿을 그 매장에 적용했다는 기록. 애그리거트가 아니라 리스너의 처리 기록 (04 설계 2.2, D2-8)
// store_id가 PK라서 같은 매장에 두 번 적용하면 두 번째는 PK 충돌로 통째로 되돌아간다
@Entity
@Table(name = "template_applications", catalog = "category")
class TemplateApplication {

    @Id
    private Long storeId;

    private LocalDateTime appliedAt;

    protected TemplateApplication() {
    }

    TemplateApplication(Long storeId, LocalDateTime appliedAt) {
        this.storeId = storeId;
        this.appliedAt = appliedAt;
    }
}
