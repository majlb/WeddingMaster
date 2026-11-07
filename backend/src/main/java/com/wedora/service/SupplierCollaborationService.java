package com.wedora.service;

import com.wedora.domain.*;
import com.wedora.repository.*;
import com.wedora.web.ApiException;
import com.wedora.web.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SupplierCollaborationService {
    private final SupplierCollaborationRepository collaborations;
    private final CollaborationMemberRepository members;
    private final CollaborationItemRepository items;
    private final SupplierRepository suppliers;
    private final SupplierServiceOfferingRepository services;
    private final UserRepository users;
    private final CollaborationSelectionRepository selections;
    private final UserNotificationService userNotifications;

    public SupplierCollaborationService(SupplierCollaborationRepository collaborations,
                                        CollaborationMemberRepository members,
                                        CollaborationItemRepository items,
                                        SupplierRepository suppliers,
                                        SupplierServiceOfferingRepository services,
                                        UserRepository users,
                                        CollaborationSelectionRepository selections,
                                        UserNotificationService userNotifications) {
        this.collaborations = collaborations;
        this.members = members;
        this.items = items;
        this.suppliers = suppliers;
        this.services = services;
        this.users = users;
        this.selections = selections;
        this.userNotifications = userNotifications;
    }

    @Transactional
    public CollaborationResponse create(String email, CollaborationCreateRequest request) {
        Supplier creator = requireOwnedApprovedSupplier(request.supplierId(), email);
        Map<String, String> names = validateNames(request.names());
        Set<Long> selectedServiceIds = new HashSet<>();
        for (Long serviceId : request.serviceIds()) {
            if (!selectedServiceIds.add(serviceId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "A service can only be selected once");
            }
            SupplierServiceOffering service = services.findById(serviceId)
                    .orElseThrow(() -> new ApiException.NotFound("Supplier service not found"));
            if (!service.getSupplier().getId().equals(creator.getId())) {
                throw new ApiException.Forbidden("Creator services must belong to the selected supplier profile");
            }
        }
        SupplierCollaboration collaboration = new SupplierCollaboration();
        collaboration.setName(names.get("en"));
        collaboration.setLocalizedNames(names);
        collaboration.setCreatedBySupplier(creator);
        collaboration = collaborations.save(collaboration);
        addMember(collaboration, creator, CollaborationMemberStatus.ACCEPTED);
        for (Long serviceId : selectedServiceIds) {
            CollaborationItem item = new CollaborationItem();
            item.setCollaboration(collaboration);
            item.setServiceOffering(services.findById(serviceId).orElseThrow());
            items.save(item);
        }

        Set<Long> invitedIds = new HashSet<>();
        for (Long supplierId : request.invitedSupplierIds()) {
            if (!invitedIds.add(supplierId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "A supplier can only be invited once");
            }
            addInvitation(collaboration, creator, supplierId);
        }
        return response(collaboration, false, false);
    }

    @Transactional
    public CollaborationResponse invite(Long collaborationId, String email, CollaborationInviteRequest request) {
        SupplierCollaboration collaboration = requireCollaboration(collaborationId);
        requireCreator(collaboration, email);
        ensureInactive(collaboration);
        addInvitation(collaboration, collaboration.getCreatedBySupplier(), request.supplierId());
        return response(collaboration, false, false);
    }

    @Transactional
    public CollaborationResponse activate(Long collaborationId, String email) {
        SupplierCollaboration collaboration = requireCollaboration(collaborationId);
        requireCreator(collaboration, email);
        if (collaboration.isActive()) return response(collaboration, false, false);

        List<CollaborationMember> collaborationMembers =
                members.findByCollaborationIdOrderByCreatedAtAsc(collaborationId);
        List<CollaborationItem> collaborationItems =
                items.findByCollaborationIdOrderByCreatedAtAsc(collaborationId);
        boolean complete = collaborationMembers.size() > 1
                && collaborationMembers.stream().allMatch(member ->
                    member.getStatus() == CollaborationMemberStatus.ACCEPTED)
                && collaborationMembers.stream().allMatch(member -> collaborationItems.stream().anyMatch(item ->
                    item.getServiceOffering().getSupplier().getId().equals(member.getSupplier().getId())));
        if (!complete) {
            throw new ApiException.Conflict(
                    "Every invited supplier must accept and add at least one service before activation");
        }
        collaboration.setActive(true);
        collaborationMembers.stream()
                .filter(member -> !member.getSupplier().getOwner().getId()
                        .equals(collaboration.getCreatedBySupplier().getOwner().getId()))
                .forEach(member -> userNotifications.notifyCollaboration(
                        member.getSupplier().getOwner(), collaborationId, "COLLABORATION_ACTIVATED",
                        collaboration.getCreatedBySupplier().getBusinessName()));
        return response(collaboration, false, false);
    }

    @Transactional
    public void removeInvite(Long collaborationId, Long supplierId, String email) {
        SupplierCollaboration collaboration = requireCollaboration(collaborationId);
        requireCreator(collaboration, email);
        ensureInactive(collaboration);
        CollaborationMember member = members.findByCollaborationIdAndSupplierId(collaborationId, supplierId)
                .orElseThrow(() -> new ApiException.NotFound("Collaboration invitation not found"));
        if (member.getStatus() == CollaborationMemberStatus.ACCEPTED) {
            throw new ApiException.Conflict("An accepted supplier cannot be removed");
        }
        members.delete(member);
    }

    @Transactional
    public CollaborationResponse respond(Long collaborationId, String email, boolean accepted) {
        SupplierCollaboration collaboration = requireCollaboration(collaborationId);
        ensureInactive(collaboration);
        CollaborationMember member = requireUserSupplierIds(email).stream()
                .map(supplierId -> members.findByCollaborationIdAndSupplierId(collaborationId, supplierId).orElse(null))
                .filter(candidate -> candidate != null && candidate.getStatus() == CollaborationMemberStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new ApiException.NotFound("Pending collaboration invitation not found"));
        member.setStatus(accepted ? CollaborationMemberStatus.ACCEPTED : CollaborationMemberStatus.DECLINED);
        userNotifications.notifyCollaboration(collaboration.getCreatedBySupplier().getOwner(),
                collaborationId, accepted ? "COLLABORATION_ACCEPTED" : "COLLABORATION_DECLINED",
                member.getSupplier().getBusinessName());
        return response(collaboration, false, false);
    }

    @Transactional
    public CollaborationResponse addItem(Long collaborationId, String email, CollaborationItemRequest request) {
        SupplierCollaboration collaboration = requireCollaboration(collaborationId);
        ensureInactive(collaboration);
        SupplierServiceOffering service = services.findById(request.serviceId())
                .orElseThrow(() -> new ApiException.NotFound("Supplier service not found"));
        if (!requireUserSupplierIds(email).contains(service.getSupplier().getId())) {
            throw new ApiException.Forbidden("You can only add services from your own accepted supplier profile");
        }
        CollaborationMember member = members.findByCollaborationIdAndSupplierId(collaborationId,
                        service.getSupplier().getId())
                .orElseThrow(() -> new ApiException.Forbidden("An accepted supplier member is required"));
        if (member.getStatus() != CollaborationMemberStatus.ACCEPTED) {
            throw new ApiException.Forbidden("An accepted supplier member is required");
        }
        if (items.existsByCollaborationIdAndServiceOfferingId(collaborationId, service.getId())) {
            throw new ApiException.Conflict("This service is already included in the collaboration");
        }
        CollaborationItem item = new CollaborationItem();
        item.setCollaboration(collaboration);
        item.setServiceOffering(service);
        items.save(item);
        notifyCreatorServiceAdded(collaboration, service);
        return response(collaboration, false, false);
    }

    @Transactional
    public CollaborationResponse addItems(Long collaborationId, String email, CollaborationItemsRequest request) {
        SupplierCollaboration collaboration = requireCollaboration(collaborationId);
        ensureInactive(collaboration);
        if (request == null || request.serviceIds() == null || request.serviceIds().isEmpty()
                || request.serviceIds().stream().distinct().count() != request.serviceIds().size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose one or more different services");
        }
        Set<Long> ownedSupplierIds = new HashSet<>(requireUserSupplierIds(email));
        Set<Long> existingServiceIds = items.findByCollaborationIdOrderByCreatedAtAsc(collaborationId).stream()
                .map(item -> item.getServiceOffering().getId())
                .collect(java.util.stream.Collectors.toSet());
        List<SupplierServiceOffering> requestedServices = services.findAllById(request.serviceIds());
        if (requestedServices.size() != request.serviceIds().size()) {
            throw new ApiException.NotFound("One or more supplier services were not found");
        }
        for (SupplierServiceOffering service : requestedServices) {
            Long supplierId = service.getSupplier().getId();
            if (!ownedSupplierIds.contains(supplierId)) {
                throw new ApiException.Forbidden("You can only add services from your own accepted supplier profile");
            }
            CollaborationMember member = members.findByCollaborationIdAndSupplierId(collaborationId, supplierId)
                    .orElseThrow(() -> new ApiException.Forbidden("An accepted supplier member is required"));
            if (member.getStatus() != CollaborationMemberStatus.ACCEPTED) {
                throw new ApiException.Forbidden("An accepted supplier member is required");
            }
            if (existingServiceIds.contains(service.getId())) {
                throw new ApiException.Conflict("A service can only be added to a collaboration once");
            }
        }
        for (SupplierServiceOffering service : requestedServices) {
            CollaborationItem item = new CollaborationItem();
            item.setCollaboration(collaboration);
            item.setServiceOffering(service);
            items.save(item);
            notifyCreatorServiceAdded(collaboration, service);
        }
        return response(collaboration, false, false);
    }

    @Transactional(readOnly = true)
    public List<CollaborationResponse> listForSupplier(String email) {
        Map<Long, SupplierCollaboration> unique = new LinkedHashMap<>();
        for (Long supplierId : requireUserSupplierIds(email)) {
            collaborations.findForSupplier(supplierId)
                    .forEach(collaboration -> unique.putIfAbsent(collaboration.getId(), collaboration));
        }
        return unique.values().stream()
                .map(collaboration -> response(collaboration, false, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CollaborationResponse> listPublic(String email) {
        AppUser customer = requireCustomer(email);
        Set<Long> selectedIds = selections.findByCustomerId(customer.getId()).stream()
                .map(selection -> selection.getCollaboration().getId())
                .collect(java.util.stream.Collectors.toSet());
        return collaborations.findPublicCollaborations().stream()
                .map(collaboration -> response(collaboration, true, selectedIds.contains(collaboration.getId())))
                .toList();
    }

    @Transactional
    public CollaborationResponse select(Long collaborationId, String email) {
        AppUser customer = requireCustomer(email);
        SupplierCollaboration collaboration = collaborations.findByIdAndActiveTrue(collaborationId)
                .orElseThrow(() -> new ApiException.NotFound("Collaboration not found"));
        if (!selections.existsByCollaborationIdAndCustomerId(collaborationId, customer.getId())) {
            CollaborationSelection selection = new CollaborationSelection();
            selection.setCollaboration(collaboration);
            selection.setCustomer(customer);
            selections.save(selection);
            notifyCollaborationSuppliers(collaborationId, collaboration, customer.getName(),
                    "COLLABORATION_SELECTED");
        }
        return response(collaboration, true, true);
    }

    @Transactional
    public void unselect(Long collaborationId, String email) {
        AppUser customer = requireCustomer(email);
        SupplierCollaboration collaboration = collaborations.findByIdAndActiveTrue(collaborationId)
                .orElseThrow(() -> new ApiException.NotFound("Collaboration not found"));
        if (selections.existsByCollaborationIdAndCustomerId(collaborationId, customer.getId())) {
            selections.deleteByCollaborationIdAndCustomerId(collaborationId, customer.getId());
            notifyCollaborationSuppliers(collaborationId, collaboration, customer.getName(),
                    "COLLABORATION_UNSELECTED");
        }
    }

    private Map<String, String> validateNames(Map<String, String> requestedNames) {
        List<String> supportedLanguages = List.of("en", "fr", "el", "ar");
        if (requestedNames == null || requestedNames.size() != supportedLanguages.size()
                || !requestedNames.keySet().containsAll(supportedLanguages)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Collaboration names are required in English, French, Greek, and Arabic");
        }
        Map<String, String> names = new LinkedHashMap<>();
        for (String language : supportedLanguages) {
            String name = requestedNames.get(language);
            if (name == null || name.isBlank() || name.trim().length() > 150) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Each translated collaboration name must contain 1 to 150 characters");
            }
            names.put(language, name.trim());
        }
        return names;
    }

    private CollaborationResponse response(SupplierCollaboration collaboration, boolean publicView,
                                            boolean selected) {
        return CollaborationResponse.from(collaboration,
                members.findByCollaborationIdOrderByCreatedAtAsc(collaboration.getId()),
                items.findByCollaborationIdOrderByCreatedAtAsc(collaboration.getId()),
                publicView, selected);
    }

    private void addInvitation(SupplierCollaboration collaboration, Supplier creator, Long supplierId) {
        Supplier invitee = suppliers.findByIdAndApprovedTrueAndActiveTrue(supplierId)
                .orElseThrow(() -> new ApiException.NotFound("Supplier not found"));
        if (invitee.getOwner().getId().equals(creator.getOwner().getId())) {
            throw new ApiException.Conflict("You cannot invite your own supplier profile");
        }
        boolean alreadyRepresented = members.findByCollaborationIdOrderByCreatedAtAsc(collaboration.getId()).stream()
                .anyMatch(member -> member.getSupplier().getOwner().getId().equals(invitee.getOwner().getId()));
        if (alreadyRepresented) throw new ApiException.Conflict("This supplier has already been invited");
        addMember(collaboration, invitee, CollaborationMemberStatus.PENDING);
        userNotifications.notifyCollaboration(invitee.getOwner(), collaboration.getId(),
                "COLLABORATION_INVITATION", creator.getBusinessName());
    }

    private void notifyCreatorServiceAdded(SupplierCollaboration collaboration,
                                           SupplierServiceOffering service) {
        if (!service.getSupplier().getOwner().getId()
                .equals(collaboration.getCreatedBySupplier().getOwner().getId())) {
            userNotifications.notifyCollaboration(collaboration.getCreatedBySupplier().getOwner(),
                    collaboration.getId(), "COLLABORATION_SERVICE_ADDED",
                    service.getSupplier().getBusinessName());
        }
    }

    private void notifyCollaborationSuppliers(Long collaborationId, SupplierCollaboration collaboration,
                                              String actorName, String type) {
        items.findByCollaborationIdOrderByCreatedAtAsc(collaborationId).stream()
                .map(item -> item.getServiceOffering().getSupplier().getOwner())
                .collect(java.util.stream.Collectors.toMap(AppUser::getId, user -> user, (first, ignored) -> first))
                .values()
                .forEach(recipient -> userNotifications.notifyCollaboration(
                        recipient, collaborationId, type, actorName));
    }

    private void requireCreator(SupplierCollaboration collaboration, String email) {
        Supplier creator = collaboration.getCreatedBySupplier();
        if (!creator.isApproved() || !creator.isActive()) {
            throw new ApiException.Forbidden("An approved active supplier profile is required for collaborations");
        }
        if (!creator.getOwner().getEmail().equalsIgnoreCase(email)) {
            throw new ApiException.Forbidden("Only the collaboration creator can manage it");
        }
    }

    private void ensureInactive(SupplierCollaboration collaboration) {
        if (collaboration.isActive()) throw new ApiException.Conflict("An active collaboration cannot be changed");
    }

    private AppUser requireCustomer(String email) {
        AppUser user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException.Unauthorized("Account not found"));
        if (user.getRole() != Role.CUSTOMER) {
            throw new ApiException.Forbidden("Collaborations are available to couple accounts");
        }
        return user;
    }

    private SupplierCollaboration requireCollaboration(Long id) {
        return collaborations.findById(id)
                .orElseThrow(() -> new ApiException.NotFound("Collaboration not found"));
    }

    private Supplier requireOwnedApprovedSupplier(Long supplierId, String email) {
        Supplier supplier = suppliers.findByIdAndApprovedTrueAndActiveTrue(supplierId)
                .orElseThrow(() -> new ApiException.NotFound("Supplier not found"));
        if (!supplier.getOwner().getEmail().equalsIgnoreCase(email)) {
            throw new ApiException.Forbidden("You can only manage your own supplier profile");
        }
        return supplier;
    }

    private List<Long> requireUserSupplierIds(String email) {
        AppUser user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException.Unauthorized("Account not found"));
        if (user.getRole() != Role.SUPPLIER) {
            throw new ApiException.Forbidden("Collaborations are available to supplier accounts");
        }
        List<Long> supplierIds = suppliers.findByOwnerId(user.getId()).stream()
                .filter(Supplier::isApproved)
                .filter(Supplier::isActive)
                .map(Supplier::getId)
                .toList();
        if (supplierIds.isEmpty()) {
            throw new ApiException.Forbidden("An approved active supplier profile is required for collaborations");
        }
        return supplierIds;
    }

    private void addMember(SupplierCollaboration collaboration, Supplier supplier,
                           CollaborationMemberStatus status) {
        CollaborationMember member = new CollaborationMember();
        member.setCollaboration(collaboration);
        member.setSupplier(supplier);
        member.setStatus(status);
        members.save(member);
    }
}
