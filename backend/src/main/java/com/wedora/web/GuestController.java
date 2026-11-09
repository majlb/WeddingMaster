package com.wedora.web;

import com.wedora.domain.AppUser;
import com.wedora.domain.Guest;
import com.wedora.repository.GuestRepository;
import com.wedora.repository.UserRepository;
import com.wedora.web.dto.GuestRequest;
import com.wedora.web.dto.GuestResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/api/guests")
@Transactional
public class GuestController {
    private final GuestRepository guestRepository;
    private final UserRepository userRepository;

    public GuestController(GuestRepository guestRepository, UserRepository userRepository) {
        this.guestRepository = guestRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<GuestResponse> list(@AuthenticationPrincipal String email) {
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException.NotFound("User not found"));
        return guestRepository.findByWeddingCoupleIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(GuestResponse::from)
                .toList();
    }

    @PostMapping
    public GuestResponse create(@AuthenticationPrincipal String email, @Valid @RequestBody GuestRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException.NotFound("User not found"));

        Guest guest = new Guest();
        guest.setWeddingCouple(user);
        guest.setName(request.name().trim());
        guest.setPersonCount(request.personCount());
        guest.setPhone(request.phone() != null ? request.phone().trim() : null);
        guest.setApproved(request.approved());

        return GuestResponse.from(guestRepository.save(guest));
    }

    @PutMapping("/{id}")
    public GuestResponse update(@PathVariable Long id, @AuthenticationPrincipal String email,
                                @Valid @RequestBody GuestRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException.NotFound("User not found"));

        Guest guest = guestRepository.findByIdAndWeddingCoupleId(id, user.getId())
                .orElseThrow(() -> new ApiException.NotFound("Guest not found"));

        guest.setName(request.name().trim());
        guest.setPersonCount(request.personCount());
        guest.setPhone(request.phone() != null ? request.phone().trim() : null);
        guest.setApproved(request.approved());

        return GuestResponse.from(guestRepository.save(guest));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal String email) {
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException.NotFound("User not found"));

        Guest guest = guestRepository.findByIdAndWeddingCoupleId(id, user.getId())
                .orElseThrow(() -> new ApiException.NotFound("Guest not found"));

        guestRepository.delete(guest);
    }
}
