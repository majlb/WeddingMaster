package com.wedora.repository;

import com.wedora.service.CategorySeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:category-seeder;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CategorySeederPersistenceTest {
    @Autowired CategoryRepository categories;

    @Test
    void seedsExistingDatabaseIdempotentlyWithoutReactivatingArchivedCategories() {
        CategorySeeder seeder = new CategorySeeder(categories);
        seeder.run();
        var existing = categories.findBySlug("sweets").orElseThrow();
        existing.setActive(false);
        existing.setName("Customized sweets");
        categories.saveAndFlush(existing);

        seeder.run();
        categories.flush();

        assertThat(categories.count()).isEqualTo(31);
        assertThat(categories.findBySlugAndActiveTrue("guesthouse")).isPresent();
        assertThat(categories.findBySlug("sweets").orElseThrow().isActive()).isFalse();
        assertThat(categories.findBySlug("sweets").orElseThrow().getName()).isEqualTo("Customized sweets");
    }
}
