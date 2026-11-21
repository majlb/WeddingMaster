package com.wedora.service;

import com.wedora.domain.Category;
import com.wedora.repository.CategoryRepository;
import com.wedora.web.dto.CategoryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CategorySeederTest {
    private static final List<String> ADDED_SLUGS = List.of(
            "man-dress", "man-hair", "sweets", "woman-nails", "woman-shoes", "man-shoes",
            "zaffeh-dancers", "singers", "firework", "entertainment", "event-organizer",
            "decoration", "invitation-printing", "jewelry", "souvenir-gifts", "legal-services",
            "travel-honeymoon", "sound", "lighting", "woman-body-care", "man-body-care",
            "tables-services-rent", "woman-accessories", "guesthouse");

    @Test
    void createsAllRequestedCategoriesWithLocalizedNamesAndDescriptions() {
        CategoryRepository repository = mock(CategoryRepository.class);
        org.mockito.ArgumentCaptor<Category> saved = org.mockito.ArgumentCaptor.forClass(Category.class);
        when(repository.findBySlug(any())).thenReturn(Optional.empty());

        new CategorySeeder(repository).run();

        verify(repository, times(31)).save(saved.capture());
        List<Category> added = saved.getAllValues().stream()
                .filter(category -> ADDED_SLUGS.contains(category.getSlug())).toList();
        assertThat(added).extracting(Category::getSlug).containsExactlyInAnyOrderElementsOf(ADDED_SLUGS);
        assertThat(added).extracting(Category::getName).containsExactlyInAnyOrder(
                "Man Dress", "Man Hair", "Sweets", "Woman Nails", "Woman Shoes", "Man Shoes",
                "Zaffeh/Dancers", "Singers", "Firework", "Entertainment", "Event Organizer",
                "Decoration", "Invitation/Printing", "Jewelry", "Souvenir gifts", "Legal Services",
                "Travel/HoneyMoon", "Sound", "Lighting", "Woman Body Care", "Man Body care",
                "Tables Services Rent", "Woman Accessories", "GuestHouse");
        for (Category category : added) {
            assertThat(category.isActive()).isTrue();
            assertThat(category.getTranslations()).containsOnlyKeys("en", "fr", "el", "ar");
            for (String language : List.of("en", "fr", "el", "ar")) {
                assertThat(category.getTranslations().get(language)).containsKeys("name", "description");
                assertThat(category.getTranslations().get(language).get("name")).isNotBlank();
                assertThat(category.getTranslations().get(language).get("description")).isNotBlank();
            }
        }
        try {
            LocaleContextHolder.setLocale(Locale.FRENCH);
            Category jewelry = added.stream().filter(category -> category.getSlug().equals("jewelry")).findFirst().orElseThrow();
            assertThat(CategoryResponse.from(jewelry).name()).isEqualTo("Bijoux");
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    @Test
    void preservesExistingCustomizedAndArchivedCategories() {
        CategoryRepository repository = mock(CategoryRepository.class);
        Category existing = new Category();
        existing.setSlug("man-dress");
        existing.setName("Customized");
        existing.setActive(false);
        existing.setTranslations(Map.of("en", Map.of("name", "Customized")));
        when(repository.findBySlug("man-dress")).thenReturn(Optional.of(existing));

        new CategorySeeder(repository).run();

        verify(repository, never()).save(existing);
        assertThat(existing.isActive()).isFalse();
        assertThat(existing.getName()).isEqualTo("Customized");
    }
}
