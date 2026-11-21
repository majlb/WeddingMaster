package com.wedora.repository;

import com.wedora.domain.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:booking-history;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookingHistoryPersistenceTest {
    @Autowired EntityManager entityManager;
    @Autowired BookingRepository bookings;

    @Test
    void historyPersistsOnCreationAndSubsequentLockedTransition() {
        AppUser customer = user("customer", Role.CUSTOMER);
        AppUser owner = user("supplier", Role.SUPPLIER);
        entityManager.persist(customer);
        entityManager.persist(owner);
        Supplier supplier = new Supplier();
        supplier.setOwner(owner);
        supplier.setBusinessName("Supplier");
        supplier.setSlug("supplier");
        supplier.setLocation("Paris");
        entityManager.persist(supplier);
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setSupplier(supplier);
        booking.setEventDate(LocalDate.now().plusDays(1));
        booking.addHistory(BookingStatus.NEW_REQUEST, "CUSTOMER", "Hello");
        bookings.saveAndFlush(booking);
        Long id = booking.getId();
        entityManager.clear();

        Booking loaded = bookings.findByIdForUpdate(id).orElseThrow();
        loaded.addHistory(BookingStatus.UNDER_REVIEW, "SUPPLIER", null);
        entityManager.flush();
        entityManager.clear();

        Booking persisted = bookings.findById(id).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(BookingStatus.UNDER_REVIEW);
        assertThat(persisted.getHistory()).extracting(BookingHistory::getStatus)
                .containsExactly(BookingStatus.NEW_REQUEST, BookingStatus.UNDER_REVIEW);
        assertThat(persisted.getHistory().get(0).getComment()).isEqualTo("Hello");
    }

    private AppUser user(String name, Role role) {
        AppUser user = new AppUser();
        user.setName(name);
        user.setUsername(name);
        user.setEmail(name + "@example.test");
        user.setPasswordHash("hash");
        user.setRole(role);
        return user;
    }
}
