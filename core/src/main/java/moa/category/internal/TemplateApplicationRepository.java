package moa.category.internal;

import org.springframework.data.repository.Repository;

interface TemplateApplicationRepository extends Repository<TemplateApplication, Long> {

    TemplateApplication save(TemplateApplication application);

    boolean existsById(Long storeId);
}
