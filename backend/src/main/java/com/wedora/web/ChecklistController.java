package com.wedora.web;

import com.wedora.service.ChecklistService;
import com.wedora.web.dto.ChecklistRequest;
import com.wedora.web.dto.ChecklistResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/checklist")
public class ChecklistController {
    private final ChecklistService checklistService;

    public ChecklistController(ChecklistService checklistService) {
        this.checklistService = checklistService;
    }

    @GetMapping
    public List<ChecklistResponse> list(@AuthenticationPrincipal String email) {
        return checklistService.list(email);
    }

    @PostMapping
    public ChecklistResponse add(@AuthenticationPrincipal String email, @Valid @RequestBody ChecklistRequest request) {
        return checklistService.add(email, request);
    }

    @PutMapping("/{id}")
    public ChecklistResponse update(@PathVariable Long id, @AuthenticationPrincipal String email,
                                   @Valid @RequestBody ChecklistRequest request) {
        return checklistService.update(id, email, request);
    }

    @PatchMapping("/{id}")
    public ChecklistResponse complete(@PathVariable Long id, @AuthenticationPrincipal String email,
                                      @RequestParam boolean completed) {
        return checklistService.setCompleted(id, email, completed);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal String email) {
        checklistService.delete(id, email);
    }
}
