package com.wedora.service;

import com.wedora.domain.Category;
import com.wedora.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CategorySeeder implements CommandLineRunner {
    private final CategoryRepository categories;

    public CategorySeeder(CategoryRepository categories) {
        this.categories = categories;
    }

    @Override
    public void run(String... args) {
        List<String[]> defaults = List.of(
                new String[]{"venues", "Venues", "Lieux de réception", "Χώροι δεξιώσεων", "أماكن حفلات الزفاف", "Wedding venues", "Lieux pour célébrer votre mariage", "Χώροι για τη δεξίωσή σας", "أماكن لإقامة حفل زفافكم"},
                new String[]{"catering", "Catering", "Traiteurs", "Υπηρεσίες catering", "خدمات تقديم الطعام", "Wedding catering", "Traiteurs pour votre mariage", "Υπηρεσίες catering για τον γάμο σας", "خدمات الطعام لحفل زفافكم"},
                new String[]{"flowers", "Flowers", "Fleurs", "Λουλούδια γάμου", "زهور الزفاف", "Wedding flowers", "Fleurs pour votre mariage", "Λουλούδια για τον γάμο σας", "زهور لحفل زفافكم"},
                new String[]{"dresses", "Dresses", "Robes et tenues de mariage", "Νυφικά και ενδύματα", "فساتين وملابس الزفاف", "Wedding dresses and attire", "Robes et tenues pour votre mariage", "Νυφικά και ενδύματα για τον γάμο σας", "فساتين وأزياء لحفل زفافكم"},
                new String[]{"hair", "Hair", "Coiffure de mariage", "Χτένισμα γάμου", "تصفيف شعر الزفاف", "Wedding hair styling", "Coiffure pour votre mariage", "Χτένισμα για τον γάμο σας", "تصفيف الشعر لحفل زفافكم"},
                new String[]{"makeup", "Makeup", "Maquillage de mariage", "Μακιγιάζ γάμου", "مكياج الزفاف", "Wedding makeup", "Maquillage pour votre mariage", "Μακιγιάζ για τον γάμο σας", "مكياج لحفل زفافكم"},
                new String[]{"photography", "Photography", "Photographie de mariage", "Φωτογραφία γάμου", "تصوير حفلات الزفاف", "Wedding photography", "Photographie pour votre mariage", "Φωτογράφιση για τον γάμο σας", "تصوير لحفل زفافكم"}
        );
        for (String[] item : defaults) {
            Category category = categories.findBySlug(item[0]).orElse(null);
            if (category == null) {
                category = new Category();
                category.setName(item[1]);
                category.setSlug(item[0]);
                category.setDescription(item[5]);
                category.setTranslations(translations(item));
                categories.save(category);
            } else if (category.getTranslations() == null || category.getTranslations().isEmpty()) {
                category.setTranslations(translations(item));
                categories.save(category);
            }
        }
    }

    private Map<String, Map<String, String>> translations(String[] item) {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        String[] languages = {"en", "fr", "el", "ar"};
        for (int i = 0; i < languages.length; i++) {
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("name", item[1 + i]);
            fields.put("description", item[5 + i]);
            result.put(languages[i], fields);
        }
        return result;
    }
}
