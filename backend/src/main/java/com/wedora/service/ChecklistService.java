package com.wedora.service;

import com.wedora.domain.AppUser;
import com.wedora.domain.ChecklistItem;
import com.wedora.domain.Role;
import com.wedora.repository.ChecklistRepository;
import com.wedora.repository.UserRepository;
import com.wedora.web.ApiException;
import com.wedora.web.dto.ChecklistRequest;
import com.wedora.web.dto.ChecklistResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ChecklistService {
    private final ChecklistRepository checklist;
    private final UserRepository users;

    public ChecklistService(ChecklistRepository checklist, UserRepository users) {
        this.checklist = checklist;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<ChecklistResponse> list(String email) {
        AppUser customer = customer(email);
        return checklist.findByCustomerIdOrderByCreatedAtAsc(customer.getId())
                .stream().map(ChecklistResponse::from).toList();
    }

    @Transactional
    public ChecklistResponse add(String email, ChecklistRequest request) {
        ChecklistItem item = new ChecklistItem();
        item.setCustomer(customer(email));
        applyRequest(item, request);
        return ChecklistResponse.from(checklist.save(item));
    }

    @Transactional
    public ChecklistResponse update(Long id, String email, ChecklistRequest request) {
        ChecklistItem item = checklist.findById(id)
                .orElseThrow(() -> new ApiException.NotFound("Checklist item not found"));
        if (!item.getCustomer().getEmail().equalsIgnoreCase(email)) {
            throw new ApiException.Forbidden("You can only manage your own checklist");
        }
        applyRequest(item, request);
        return ChecklistResponse.from(item);
    }

    @Transactional
    public ChecklistResponse setCompleted(Long id, String email, boolean completed) {
        ChecklistItem item = checklist.findById(id).orElseThrow(() -> new ApiException.NotFound("Checklist item not found"));
        if (!item.getCustomer().getEmail().equalsIgnoreCase(email)) {
            throw new ApiException.Forbidden("You can only manage your own checklist");
        }
        item.setCompleted(completed);
        return ChecklistResponse.from(item);
    }

    @Transactional
    public void delete(Long id, String email) {
        AppUser customer = customer(email);
        if (checklist.deleteByIdAndCustomerId(id, customer.getId()) == 0) {
            throw new ApiException.NotFound("Checklist item not found");
        }
    }

    private AppUser customer(String email) {
        AppUser user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException.Unauthorized("Account not found"));
        if (user.getRole() != Role.CUSTOMER) throw new ApiException.Forbidden("Checklist is available to customer accounts");
        return user;
    }

    private void applyRequest(ChecklistItem item, ChecklistRequest request) {
        item.setTitle(request.title().trim());
        item.setCategorySlug(request.categorySlug());
        item.setDueDate(request.dueDate());
        item.setNotes(request.notes());
        item.setBudgetAmount(request.budgetAmount());
    }
}
