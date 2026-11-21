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
                new String[]{"photography", "Photography", "Photographie de mariage", "Φωτογραφία γάμου", "تصوير حفلات الزفاف", "Wedding photography", "Photographie pour votre mariage", "Φωτογράφιση για τον γάμο σας", "تصوير لحفل زفافكم"},
                new String[]{"man-dress", "Man Dress", "Tenue homme", "Ανδρική ενδυμασία", "ملابس رجالية", "Men's wedding attire", "Tenues de mariage pour hommes", "Ανδρική ενδυμασία γάμου", "ملابس الزفاف الرجالية"},
                new String[]{"man-hair", "Man Hair", "Coiffure homme", "Ανδρικό χτένισμα", "تصفيف شعر الرجال", "Men's hair styling", "Coiffure pour hommes", "Ανδρικό χτένισμα", "خدمات تصفيف شعر الرجال"},
                new String[]{"sweets", "Sweets", "Pâtisseries et douceurs", "Γλυκά", "حلويات", "Wedding sweets and desserts", "Pâtisseries et desserts de mariage", "Γλυκά και επιδόρπια γάμου", "حلويات ومأكولات الزفاف"},
                new String[]{"woman-nails", "Woman Nails", "Onglerie femme", "Γυναικεία περιποίηση νυχιών", "أظافر النساء", "Women's nail services", "Soins des ongles pour femmes", "Υπηρεσίες περιποίησης νυχιών για γυναίκες", "خدمات العناية بأظافر النساء"},
                new String[]{"woman-shoes", "Woman Shoes", "Chaussures femme", "Γυναικεία παπούτσια", "أحذية نسائية", "Women's wedding shoes", "Chaussures de mariage pour femmes", "Γυναικεία παπούτσια γάμου", "أحذية الزفاف النسائية"},
                new String[]{"man-shoes", "Man Shoes", "Chaussures homme", "Ανδρικά παπούτσια", "أحذية رجالية", "Men's wedding shoes", "Chaussures de mariage pour hommes", "Ανδρικά παπούτσια γάμου", "أحذية الزفاف الرجالية"},
                new String[]{"zaffeh-dancers", "Zaffeh/Dancers", "Zaffé et danseurs", "Ζαφέ και χορευτές", "زفة وراقصون", "Wedding zaffeh and dancers", "Zaffé et danseurs pour mariage", "Ζαφέ και χορευτές για γάμο", "زفة وراقصون لحفلات الزفاف"},
                new String[]{"singers", "Singers", "Chanteurs", "Τραγουδιστές", "مغنون", "Wedding singers", "Chanteurs pour mariage", "Τραγουδιστές για γάμο", "مغنون لحفلات الزفاف"},
                new String[]{"firework", "Firework", "Feux d'artifice", "Πυροτεχνήματα", "ألعاب نارية", "Wedding fireworks", "Feux d'artifice pour mariage", "Πυροτεχνήματα για γάμο", "ألعاب نارية لحفلات الزفاف"},
                new String[]{"entertainment", "Entertainment", "Animations", "Ψυχαγωγία", "ترفيه", "Wedding entertainment", "Animations pour mariage", "Ψυχαγωγία για γάμο", "ترفيه لحفلات الزفاف"},
                new String[]{"event-organizer", "Event Organizer", "Organisation d'événements", "Διοργάνωση εκδηλώσεων", "تنظيم المناسبات", "Wedding and event organizers", "Organisateurs de mariages et d'événements", "Διοργανωτές γάμων και εκδηλώσεων", "منظمو حفلات الزفاف والمناسبات"},
                new String[]{"decoration", "Decoration", "Décoration", "Διακόσμηση", "ديكور", "Wedding decoration", "Décoration de mariage", "Διακόσμηση γάμου", "ديكور حفلات الزفاف"},
                new String[]{"invitation-printing", "Invitation/Printing", "Invitations et impression", "Προσκλήσεις και εκτυπώσεις", "دعوات وطباعة", "Wedding invitations and printing", "Invitations et impression pour mariage", "Προσκλήσεις και εκτυπώσεις γάμου", "دعوات وطباعة لحفلات الزفاف"},
                new String[]{"jewelry", "Jewelry", "Bijoux", "Κοσμήματα", "مجوهرات", "Wedding jewelry", "Bijoux de mariage", "Κοσμήματα γάμου", "مجوهرات الزفاف"},
                new String[]{"souvenir-gifts", "Souvenir gifts", "Cadeaux souvenirs", "Αναμνηστικά δώρα", "هدايا تذكارية", "Wedding souvenir gifts", "Cadeaux souvenirs de mariage", "Αναμνηστικά δώρα γάμου", "هدايا تذكارية للزفاف"},
                new String[]{"legal-services", "Legal Services", "Services juridiques", "Νομικές υπηρεσίες", "خدمات قانونية", "Legal services", "Services juridiques", "Νομικές υπηρεσίες", "خدمات قانونية"},
                new String[]{"travel-honeymoon", "Travel/HoneyMoon", "Voyage et lune de miel", "Ταξίδι και μήνας του μέλιτος", "السفر وشهر العسل", "Wedding travel and honeymoons", "Voyages et lunes de miel", "Ταξίδια και μήνες του μέλιτος", "السفر ورحلات شهر العسل"},
                new String[]{"sound", "Sound", "Sonorisation", "Ήχος", "الصوت", "Wedding sound services", "Sonorisation de mariage", "Υπηρεσίες ήχου για γάμο", "خدمات الصوت لحفلات الزفاف"},
                new String[]{"lighting", "Lighting", "Éclairage", "Φωτισμός", "الإضاءة", "Wedding lighting", "Éclairage de mariage", "Φωτισμός γάμου", "إضاءة حفلات الزفاف"},
                new String[]{"woman-body-care", "Woman Body Care", "Soins du corps femme", "Γυναικεία περιποίηση σώματος", "العناية بجسم المرأة", "Women's body care", "Soins du corps pour femmes", "Περιποίηση σώματος για γυναίκες", "خدمات العناية بجسم المرأة"},
                new String[]{"man-body-care", "Man Body care", "Soins du corps homme", "Ανδρική περιποίηση σώματος", "العناية بجسم الرجل", "Men's body care", "Soins du corps pour hommes", "Περιποίηση σώματος για άνδρες", "خدمات العناية بجسم الرجل"},
                new String[]{"tables-services-rent", "Tables Services Rent", "Location de tables et services", "Ενοικίαση τραπεζιών και ειδών", "تأجير الطاولات ومستلزماتها", "Table and service rentals", "Location de tables et de matériel de service", "Ενοικίαση τραπεζιών και ειδών σερβιρίσματος", "تأجير الطاولات ومستلزمات الخدمة"},
                new String[]{"woman-accessories", "Woman Accessories", "Accessoires femme", "Γυναικεία αξεσουάρ", "إكسسوارات نسائية", "Women's wedding accessories", "Accessoires de mariage pour femmes", "Γυναικεία αξεσουάρ γάμου", "إكسسوارات الزفاف النسائية"},
                new String[]{"guesthouse", "GuestHouse", "Maison d'hôtes", "Ξενώνας", "بيت ضيافة", "Guesthouse accommodation", "Hébergement en maison d'hôtes", "Διαμονή σε ξενώνα", "الإقامة في بيوت الضيافة"}
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
