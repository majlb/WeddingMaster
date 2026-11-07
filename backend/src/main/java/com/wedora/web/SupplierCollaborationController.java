package com.wedora.web;

import com.wedora.service.SupplierCollaborationService;
import com.wedora.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collaborations")
public class SupplierCollaborationController {
    private final SupplierCollaborationService service;

    public SupplierCollaborationController(SupplierCollaborationService service) {
        this.service = service;
    }

    @GetMapping("/public")
    public List<CollaborationResponse> listPublic(@AuthenticationPrincipal String email) {
        return service.listPublic(email);
    }

    @GetMapping
    public List<CollaborationResponse> listForSupplier(@AuthenticationPrincipal String email) {
        return service.listForSupplier(email);
    }

    @PostMapping
    public CollaborationResponse create(@AuthenticationPrincipal String email,
                                        @Valid @RequestBody CollaborationCreateRequest request) {
        return service.create(email, request);
    }

    @PostMapping("/{id}/invitations")
    public CollaborationResponse invite(@PathVariable Long id, @AuthenticationPrincipal String email,
                                        @Valid @RequestBody CollaborationInviteRequest request) {
        return service.invite(id, email, request);
    }

    @PostMapping("/{id}/activate")
    public CollaborationResponse activate(@PathVariable Long id, @AuthenticationPrincipal String email) {
        return service.activate(id, email);
    }

    @DeleteMapping("/{id}/members/{supplierId}")
    public void removeInvite(@PathVariable Long id, @PathVariable Long supplierId,
                             @AuthenticationPrincipal String email) {
        service.removeInvite(id, supplierId, email);
    }

    @PostMapping("/{id}/response")
    public CollaborationResponse respond(@PathVariable Long id, @AuthenticationPrincipal String email,
                                         @RequestBody CollaborationResponseRequest request) {
        return service.respond(id, email, request.accepted());
    }

    @PostMapping("/{id}/items")
    public CollaborationResponse addItem(@PathVariable Long id, @AuthenticationPrincipal String email,
                                         @Valid @RequestBody CollaborationItemRequest request) {
        return service.addItem(id, email, request);
    }

    @PostMapping("/{id}/items/batch")
    public CollaborationResponse addItems(@PathVariable Long id, @AuthenticationPrincipal String email,
                                          @Valid @RequestBody CollaborationItemsRequest request) {
        return service.addItems(id, email, request);
    }

    @PostMapping("/{id}/select")
    public CollaborationResponse select(@PathVariable Long id, @AuthenticationPrincipal String email) {
        return service.select(id, email);
    }

    @DeleteMapping("/{id}/select")
    public void unselect(@PathVariable Long id, @AuthenticationPrincipal String email) {
        service.unselect(id, email);
    }
}
