package com.wedora.web.dto;

import com.wedora.domain.Guest;

import java.time.Instant;

public record GuestResponse(
        Long id,
        String name,
        Integer personCount,
        String phone,
        boolean approved,
        Instant createdAt
) {
    public static GuestResponse from(Guest guest) {
        return new GuestResponse(
                guest.getId(),
                guest.getName(),
                guest.getPersonCount(),
                guest.getPhone(),
                guest.isApproved(),
                guest.getCreatedAt()
        );
    }
}
