package com.wedora.web.dto;

import com.wedora.domain.CollaborationItem;
import com.wedora.domain.CollaborationMember;
import com.wedora.domain.CollaborationMemberStatus;
import com.wedora.domain.SupplierCollaboration;
import com.wedora.domain.TranslationSupport;
import org.springframework.context.i18n.LocaleContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CollaborationResponse(
        Long id,
        String name,
        Map<String, String> names,
        Instant createdAt,
        boolean active,
        boolean readyToActivate,
        boolean selected,
        Long creatorSupplierId,
        List<Member> members,
        List<Item> items
) {
    public static CollaborationResponse from(SupplierCollaboration collaboration,
                                             List<CollaborationMember> members,
                                             List<CollaborationItem> items,
                                             boolean publicView,
                                             boolean selected) {
        String language = LocaleContextHolder.getLocale().getLanguage();
        List<Member> visibleMembers = members.stream()
                .filter(member -> !publicView || member.getStatus() == CollaborationMemberStatus.ACCEPTED)
                .map(member -> new Member(member.getSupplier().getId(),
                        member.getSupplier().getBusinessName(), member.getStatus().name()))
                .toList();
        List<Item> visibleItems = items.stream().map(item -> {
            var service = item.getServiceOffering();
            return new Item(item.getId(), service.getSupplier().getId(),
                    service.getSupplier().getBusinessName(), service.getId(),
                    TranslationSupport.value(service.getLocalizedContent(), language, "name", service.getName()),
                    TranslationSupport.value(service.getLocalizedContent(), language, "description", service.getDescription()),
                    service.getPrice(), service.getSupplier().getCurrency(), service.getImageUrl(),
                    service.getCategory() == null ? null : TranslationSupport.value(
                            service.getCategory().getTranslations(), language, "name", service.getCategory().getName()),
                    service.getCategory() == null ? null : service.getCategory().getSlug());
        }).toList();
        Map<String, String> names = collaboration.getLocalizedNames();
        String localizedName = names == null ? null : names.get(language);
        return new CollaborationResponse(collaboration.getId(),
                localizedName == null || localizedName.isBlank() ? collaboration.getName() : localizedName,
                names, collaboration.getCreatedAt(), collaboration.isActive(),
                !collaboration.isActive() && members.stream()
                        .allMatch(member -> member.getStatus() == CollaborationMemberStatus.ACCEPTED)
                        && members.size() > 1
                        && members.stream().allMatch(member -> items.stream().anyMatch(item ->
                                item.getServiceOffering().getSupplier().getId().equals(member.getSupplier().getId()))),
                selected, collaboration.getCreatedBySupplier().getId(), visibleMembers, visibleItems);
    }

    public record Member(Long supplierId, String supplierName, String status) {}

    public record Item(Long id, Long supplierId, String supplierName, Long serviceId,
                       String serviceName, String description, BigDecimal price,
                       String currency, String imageUrl, String categoryName, String categorySlug) {}
}
