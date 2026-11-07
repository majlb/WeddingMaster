package com.wedora.service;

import com.wedora.domain.*;
import com.wedora.repository.*;
import com.wedora.web.ApiException;
import com.wedora.web.dto.CollaborationCreateRequest;
import com.wedora.web.dto.CollaborationInviteRequest;
import com.wedora.web.dto.CollaborationItemRequest;
import com.wedora.web.dto.CollaborationItemsRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SupplierCollaborationServiceTest {
    @Mock private SupplierCollaborationRepository collaborations;
    @Mock private CollaborationMemberRepository members;
    @Mock private CollaborationItemRepository items;
    @Mock private SupplierRepository suppliers;
    @Mock private SupplierServiceOfferingRepository services;
    @Mock private UserRepository users;
    @Mock private CollaborationSelectionRepository selections;
    @Mock private UserNotificationService userNotifications;

    private SupplierCollaborationService service;
    private AppUser supplierUser;
    private Supplier ownSupplier;
    private SupplierCollaboration collaboration;

    @BeforeEach
    void setUp() {
        service = new SupplierCollaborationService(
                collaborations, members, items, suppliers, services, users, selections, userNotifications);
        supplierUser = new AppUser();
        supplierUser.setId(5L);
        supplierUser.setEmail("supplier@example.test");
        supplierUser.setRole(Role.SUPPLIER);
        ownSupplier = supplier(10L, supplierUser, "My studio");
        collaboration = new SupplierCollaboration();
        collaboration.setId(50L);
        collaboration.setName("Hair and makeup offer");
        collaboration.setCreatedBySupplier(ownSupplier);
    }

    @Test
    void creatorJoinsAsAcceptedMember() {
        when(suppliers.findByIdAndApprovedTrueAndActiveTrue(10L)).thenReturn(Optional.of(ownSupplier));
        when(collaborations.save(any(SupplierCollaboration.class))).thenAnswer(invocation -> {
            SupplierCollaboration saved = invocation.getArgument(0);
            saved.setId(50L);
            return saved;
        });
        when(members.save(any(CollaborationMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AppUser inviteeUser = user(6L, "invitee@example.test");
        Supplier invitee = supplier(20L, inviteeUser, "Beauty studio");
        when(suppliers.findByIdAndApprovedTrueAndActiveTrue(20L)).thenReturn(Optional.of(invitee));
        SupplierServiceOffering creatorService = offering(70L, ownSupplier);
        when(services.findById(70L)).thenReturn(Optional.of(creatorService));
        when(items.save(any(CollaborationItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CollaborationMember creatorMember =
                member(collaboration, ownSupplier, CollaborationMemberStatus.ACCEPTED);
        CollaborationMember inviteeMember =
                member(collaboration, invitee, CollaborationMemberStatus.PENDING);
        when(members.findByCollaborationIdOrderByCreatedAtAsc(50L))
                .thenReturn(List.of(creatorMember), List.of(creatorMember, inviteeMember));
        when(items.findByCollaborationIdOrderByCreatedAtAsc(50L)).thenReturn(List.of());

        var result = service.create("supplier@example.test",
                new CollaborationCreateRequest(10L,
                        java.util.Map.of("en", "  Hair and makeup offer  ", "fr", "Offre coiffure",
                                "el", "Προσφορά μαλλιών", "ar", "عرض الشعر"),
                        List.of(20L), List.of(70L)));

        assertThat(result.name()).isEqualTo("Hair and makeup offer");
        assertThat(result.members()).hasSize(2);
        assertThat(result.members().get(0).status()).isEqualTo("ACCEPTED");
        assertThat(result.members().get(1).status()).isEqualTo("PENDING");
        verify(items).save(any(CollaborationItem.class));
        verify(members, times(2)).save(any(CollaborationMember.class));
        verify(userNotifications).notifyCollaboration(
                inviteeUser, 50L, "COLLABORATION_INVITATION", ownSupplier.getBusinessName());
    }

    @Test
    void acceptedSupplierCanInviteAnotherApprovedSupplier() {
        Supplier invitee = supplier(20L, user(6L, "invitee@example.test"), "Beauty studio");
        CollaborationMember existing = member(collaboration, ownSupplier, CollaborationMemberStatus.ACCEPTED);
        when(collaborations.findById(50L)).thenReturn(Optional.of(collaboration));
        when(suppliers.findByIdAndApprovedTrueAndActiveTrue(20L)).thenReturn(Optional.of(invitee));
        when(members.findByCollaborationIdOrderByCreatedAtAsc(50L)).thenReturn(List.of(existing));
        when(members.save(any(CollaborationMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(items.findByCollaborationIdOrderByCreatedAtAsc(50L)).thenReturn(List.of());

        service.invite(50L, "supplier@example.test", new CollaborationInviteRequest(20L));

        verify(members).save(org.mockito.ArgumentMatchers.argThat(member ->
                member.getSupplier().equals(invitee) && member.getStatus() == CollaborationMemberStatus.PENDING));
    }

    @Test
    void inviteeCanAcceptPendingInvitation() {
        AppUser inviteeUser = user(6L, "invitee@example.test");
        Supplier invitee = supplier(20L, inviteeUser, "Beauty studio");
        CollaborationMember pending = member(collaboration, invitee, CollaborationMemberStatus.PENDING);
        when(collaborations.findById(50L)).thenReturn(Optional.of(collaboration));
        when(users.findByEmailIgnoreCase("invitee@example.test")).thenReturn(Optional.of(inviteeUser));
        when(suppliers.findByOwnerId(6L)).thenReturn(List.of(invitee));
        when(members.findByCollaborationIdAndSupplierId(50L, 20L)).thenReturn(Optional.of(pending));
        when(members.findByCollaborationIdOrderByCreatedAtAsc(50L)).thenReturn(List.of(pending));
        when(items.findByCollaborationIdOrderByCreatedAtAsc(50L)).thenReturn(List.of());

        service.respond(50L, "invitee@example.test", true);

        assertThat(pending.getStatus()).isEqualTo(CollaborationMemberStatus.ACCEPTED);
        verify(userNotifications).notifyCollaboration(
                supplierUser, 50L, "COLLABORATION_ACCEPTED", invitee.getBusinessName());
    }

    @Test
    void acceptedSupplierCanAddMultipleServicesAtOnce() {
        SupplierServiceOffering first = offering(80L, ownSupplier);
        SupplierServiceOffering second = offering(81L, ownSupplier);
        CollaborationMember accepted = member(collaboration, ownSupplier, CollaborationMemberStatus.ACCEPTED);
        when(collaborations.findById(50L)).thenReturn(Optional.of(collaboration));
        when(users.findByEmailIgnoreCase("supplier@example.test")).thenReturn(Optional.of(supplierUser));
        when(suppliers.findByOwnerId(5L)).thenReturn(List.of(ownSupplier));
        when(members.findByCollaborationIdAndSupplierId(50L, 10L)).thenReturn(Optional.of(accepted));
        when(members.findByCollaborationIdOrderByCreatedAtAsc(50L)).thenReturn(List.of(accepted));
        when(items.findByCollaborationIdOrderByCreatedAtAsc(50L)).thenReturn(List.of());
        when(services.findAllById(List.of(80L, 81L))).thenReturn(List.of(first, second));
        when(items.save(any(CollaborationItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.addItems(50L, "supplier@example.test", new CollaborationItemsRequest(List.of(80L, 81L)));

        verify(items, times(2)).save(any(CollaborationItem.class));
    }

    @Test
    void creatorCanActivateAfterEveryMemberAcceptsAndAddsAService() {
        Supplier invitee = supplier(20L, user(6L, "invitee@example.test"), "Beauty studio");
        SupplierServiceOffering creatorService = offering(71L, ownSupplier);
        SupplierServiceOffering inviteeService = offering(72L, invitee);
        when(collaborations.findById(50L)).thenReturn(Optional.of(collaboration));
        when(members.findByCollaborationIdOrderByCreatedAtAsc(50L)).thenReturn(List.of(
                member(collaboration, ownSupplier, CollaborationMemberStatus.ACCEPTED),
                member(collaboration, invitee, CollaborationMemberStatus.ACCEPTED)));
        when(items.findByCollaborationIdOrderByCreatedAtAsc(50L)).thenReturn(List.of(
                item(collaboration, creatorService), item(collaboration, inviteeService)));

        var result = service.activate(50L, "supplier@example.test");

        assertThat(result.active()).isTrue();
        assertThat(collaboration.isActive()).isTrue();
    }

    @Test
    void creatorCannotActivateWhileAnInvitationIsPending() {
        Supplier invitee = supplier(20L, user(6L, "invitee@example.test"), "Beauty studio");
        when(collaborations.findById(50L)).thenReturn(Optional.of(collaboration));
        when(members.findByCollaborationIdOrderByCreatedAtAsc(50L)).thenReturn(List.of(
                member(collaboration, ownSupplier, CollaborationMemberStatus.ACCEPTED),
                member(collaboration, invitee, CollaborationMemberStatus.PENDING)));
        when(items.findByCollaborationIdOrderByCreatedAtAsc(50L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.activate(50L, "supplier@example.test"))
                .isInstanceOf(ApiException.Conflict.class);
        assertThat(collaboration.isActive()).isFalse();
    }

    @Test
    void inactiveSupplierCannotCreateCollaboration() {
        when(suppliers.findByIdAndApprovedTrueAndActiveTrue(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("supplier@example.test",
                new CollaborationCreateRequest(10L,
                        java.util.Map.of("en", "Hair and makeup offer", "fr", "Offre coiffure",
                                "el", "Προσφορά μαλλιών", "ar", "عرض الشعر"),
                        List.of(), List.of())))
                .isInstanceOf(ApiException.NotFound.class);
        verify(collaborations, never()).save(any(SupplierCollaboration.class));
    }

    @Test
    void supplierCannotAddAnotherSuppliersService() {
        Supplier otherSupplier = supplier(20L, user(6L, "other@example.test"), "Other studio");
        SupplierServiceOffering offering = new SupplierServiceOffering();
        offering.setId(70L);
        offering.setSupplier(otherSupplier);
        when(collaborations.findById(50L)).thenReturn(Optional.of(collaboration));
        when(services.findById(70L)).thenReturn(Optional.of(offering));
        when(users.findByEmailIgnoreCase("supplier@example.test")).thenReturn(Optional.of(supplierUser));
        when(suppliers.findByOwnerId(5L)).thenReturn(List.of(ownSupplier));

        assertThatThrownBy(() -> service.addItem(50L, "supplier@example.test",
                new CollaborationItemRequest(70L)))
                .isInstanceOf(ApiException.Forbidden.class);
    }

    private static CollaborationMember member(SupplierCollaboration collaboration, Supplier supplier,
                                              CollaborationMemberStatus status) {
        CollaborationMember member = new CollaborationMember();
        member.setCollaboration(collaboration);
        member.setSupplier(supplier);
        member.setStatus(status);
        return member;
    }

    private static CollaborationItem item(SupplierCollaboration collaboration,
                                          SupplierServiceOffering service) {
        CollaborationItem item = new CollaborationItem();
        item.setCollaboration(collaboration);
        item.setServiceOffering(service);
        return item;
    }

    private static SupplierServiceOffering offering(Long id, Supplier supplier) {
        SupplierServiceOffering offering = new SupplierServiceOffering();
        offering.setId(id);
        offering.setSupplier(supplier);
        return offering;
    }

    private static Supplier supplier(Long id, AppUser owner, String name) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setOwner(owner);
        supplier.setBusinessName(name);
        supplier.setCurrency("USD");
        supplier.setApproved(true);
        return supplier;
    }

    private static AppUser user(Long id, String email) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setEmail(email);
        user.setRole(Role.SUPPLIER);
        return user;
    }
}
