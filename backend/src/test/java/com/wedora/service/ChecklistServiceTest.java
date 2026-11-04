package com.wedora.service;

import com.wedora.domain.AppUser;
import com.wedora.domain.ChecklistItem;
import com.wedora.domain.Role;
import com.wedora.repository.ChecklistRepository;
import com.wedora.repository.UserRepository;
import com.wedora.web.ApiException;
import com.wedora.web.dto.ChecklistRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChecklistServiceTest {
    @Mock private ChecklistRepository checklist;
    @Mock private UserRepository users;

    private ChecklistService service;
    private ChecklistItem item;

    @BeforeEach
    void setUp() {
        service = new ChecklistService(checklist, users);
        AppUser customer = new AppUser();
        customer.setEmail("couple@example.test");
        customer.setRole(Role.CUSTOMER);
        item = new ChecklistItem();
        item.setCustomer(customer);
        item.setTitle("Old task");
        item.setCompleted(true);
        item.setBudgetAmount(new BigDecimal("25.00"));
        when(checklist.findById(1L)).thenReturn(Optional.of(item));
    }

    @Test
    void coupleCanUpdateTaskDetailsWithoutChangingCompletion() {
        var response = service.update(1L, "couple@example.test",
                new ChecklistRequest("  New task  ", "venues", LocalDate.of(2027, 5, 1),
                        "Details", new BigDecimal("150.50")));

        assertThat(response.title()).isEqualTo("New task");
        assertThat(response.categorySlug()).isEqualTo("venues");
        assertThat(response.budgetAmount()).isEqualByComparingTo("150.50");
        assertThat(response.dueDate()).isEqualTo(LocalDate.of(2027, 5, 1));
        assertThat(response.notes()).isEqualTo("Details");
        assertThat(response.completed()).isTrue();
    }

    @Test
    void coupleCannotUpdateAnotherAccountsTask() {
        assertThatThrownBy(() -> service.update(1L, "another@example.test",
                new ChecklistRequest("New task", "venues", null, null, null)))
                .isInstanceOf(ApiException.Forbidden.class);
    }
}
