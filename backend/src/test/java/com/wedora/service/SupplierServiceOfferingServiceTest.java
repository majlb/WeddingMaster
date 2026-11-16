package com.wedora.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedora.domain.Category;
import com.wedora.domain.AppUser;
import com.wedora.domain.Booking;
import com.wedora.domain.BookingStatus;
import com.wedora.domain.PricingType;
import com.wedora.domain.Supplier;
import com.wedora.domain.SupplierAvailabilityBlock;
import com.wedora.domain.SupplierServiceOffering;
import com.wedora.repository.BookingRepository;
import com.wedora.repository.SupplierAvailabilityBlockRepository;
import com.wedora.repository.SupplierServiceOfferingRepository;
import com.wedora.web.ApiException;
import com.wedora.web.dto.SupplierServiceRequest;
import com.wedora.web.dto.SupplierServiceTranslationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.math.BigDecimal;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierServiceOfferingServiceTest {
    @Mock private SupplierServiceOfferingRepository offerings;
    @Mock private SupplierAvailabilityBlockRepository blocks;
    @Mock private BookingRepository bookings;
    @Mock private SupplierService suppliers;
    @Mock private MediaService media;

    private SupplierServiceOfferingService service;
    private Supplier supplier;
    private Category category;

    @BeforeEach
    void setUp() {
        service = new SupplierServiceOfferingService(offerings, blocks, bookings, suppliers, media);
        category = new Category();
        category.setId(2L);
        category.setName("Venue");
        category.setSlug("venue");
        supplier = new Supplier();
        supplier.setId(1L);
        supplier.setCategories(List.of(category));
    }

    @ParameterizedTest
    @EnumSource(PricingType.class)
    void createsAndReturnsEveryPricingType(PricingType type) throws Exception {
        when(suppliers.requireOwned(1L, "owner@example.test")).thenReturn(supplier);
        when(offerings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.create(1L, "owner@example.test", request(type, 2, 20), null);

        assertThat(result.pricingType()).isEqualTo(type);
        assertThat(result.minimumAmount()).isEqualTo(2);
        assertThat(result.maximumAmount()).isEqualTo(20);
        String json = new ObjectMapper().writeValueAsString(result);
        assertThat(json).contains("\"pricingType\":\"" + type + "\"", "\"minimumAmount\":2", "\"maximumAmount\":20");
        verify(offerings).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getPricingType() == type && saved.getMinimumAmount() == 2 && saved.getMaximumAmount() == 20));
    }

    @Test
    void omittedAmountsDefaultToOne() {
        when(suppliers.requireOwned(1L, "owner@example.test")).thenReturn(supplier);
        when(offerings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.create(1L, "owner@example.test", request(null, null, null), null);
        assertThat(result.minimumAmount()).isEqualTo(1);
        assertThat(result.maximumAmount()).isEqualTo(1);
        assertThat(result.pricingType()).isNull();
    }

    @Test
    void updatesAndListsPricingAndQuantities() {
        when(suppliers.requireOwned(1L, "owner@example.test")).thenReturn(supplier);
        SupplierServiceOffering offering = new SupplierServiceOffering();
        offering.setSupplier(supplier);
        offering.setCategory(category);
        when(offerings.findByIdAndSupplierId(3L, 1L)).thenReturn(Optional.of(offering));
        when(offerings.findBySupplierIdOrderByIdAsc(1L)).thenReturn(List.of(offering));

        var updated = service.update(1L, 3L, "owner@example.test", request(PricingType.TABLE, 3, 10), null);
        var listed = service.listOwned(1L, "owner@example.test").get(0);
        assertThat(updated.pricingType()).isEqualTo(PricingType.TABLE);
        assertThat(listed.pricingType()).isEqualTo(PricingType.TABLE);
        assertThat(listed.minimumAmount()).isEqualTo(3);
        assertThat(listed.maximumAmount()).isEqualTo(10);

        service.update(1L, 3L, "owner@example.test", request(PricingType.WEDDING, null, null), null);
        assertThat(offering.getPricingType()).isEqualTo(PricingType.WEDDING);
        assertThat(offering.getMinimumAmount()).isEqualTo(3);
        assertThat(offering.getMaximumAmount()).isEqualTo(10);
    }

    @Test
    void rejectsInvalidQuantitiesWithoutSaving() {
        when(suppliers.requireOwned(1L, "owner@example.test")).thenReturn(supplier);
        assertThatThrownBy(() -> service.create(1L, "owner@example.test", request(PricingType.PERSON, 0, 1), null))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.create(1L, "owner@example.test", request(PricingType.PERSON, 5, 4), null))
                .isInstanceOf(ApiException.class);
        verify(offerings, never()).save(any());
    }

    @Test
    void rejectsUpdateWhenOmittedMaximumWouldMakeRangeInvalid() {
        when(suppliers.requireOwned(1L, "owner@example.test")).thenReturn(supplier);
        SupplierServiceOffering offering = new SupplierServiceOffering();
        offering.setSupplier(supplier);
        offering.setCategory(category);
        offering.setMaximumAmount(5);
        when(offerings.findByIdAndSupplierId(3L, 1L)).thenReturn(Optional.of(offering));
        assertThatThrownBy(() -> service.update(1L, 3L, "owner@example.test",
                request(PricingType.PERSON, 6, null), null)).isInstanceOf(ApiException.class);
        assertThat(offering.getMinimumAmount()).isEqualTo(1);
        assertThat(offering.getMaximumAmount()).isEqualTo(5);
    }

    @Test
    void requestJsonUsesThePricingEnumAndAmounts() throws Exception {
        var request = new ObjectMapper().readValue("""
                {"categoryId":2,"pricingType":"PERSON","minimumAmount":2,"maximumAmount":30}
                """, SupplierServiceRequest.class);
        assertThat(request.pricingType()).isEqualTo(PricingType.PERSON);
        assertThat(request.minimumAmount()).isEqualTo(2);
        assertThat(request.maximumAmount()).isEqualTo(30);
    }

    @Test
    void migrationDefaultsExistingRowsAndEnforcesRange() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:service_quantity_limits");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE supplier_services (id BIGINT PRIMARY KEY)");
            statement.execute("INSERT INTO supplier_services (id) VALUES (1)");
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V21__service_quantity_limits.sql"));
            try (var rows = statement.executeQuery("SELECT minimum_amount, maximum_amount FROM supplier_services WHERE id = 1")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getInt("minimum_amount")).isEqualTo(1);
                assertThat(rows.getInt("maximum_amount")).isEqualTo(1);
            }
            assertThatThrownBy(() -> statement.execute("UPDATE supplier_services SET minimum_amount = 2 WHERE id = 1"))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void ownedBookingsIncludeAllStatusesAndMatchBlocksByServiceAndDate() {
        when(suppliers.requireOwned(1L, "owner@example.test")).thenReturn(supplier);
        var date = LocalDate.now().plusDays(30);
        var first = offering(3L);
        var second = offering(4L);
        var blocked = new SupplierAvailabilityBlock();
        blocked.setServiceOffering(first);
        blocked.setBlockedDate(date);
        when(blocks.findByServiceOfferingSupplierId(1L)).thenReturn(List.of(blocked));
        when(bookings.findBySupplierId(1L)).thenReturn(List.of(
                booking(11L, first, date, BookingStatus.REQUESTED),
                booking(12L, first, date, BookingStatus.CONFIRMED),
                booking(13L, second, date, BookingStatus.QUOTED),
                booking(14L, first, date.plusDays(1), BookingStatus.DECLINED),
                booking(15L, null, date, BookingStatus.REQUESTED)));

        var result = service.listOwnedBookings(1L, "owner@example.test");
        assertThat(result).hasSize(4);
        assertThat(result).extracting("dateBlocked").containsExactly(true, true, false, false);
        assertThat(result.get(0).customerName()).isEqualTo("A Couple");
        assertThat(result.get(3).status()).isEqualTo(BookingStatus.DECLINED);
    }

    @Test
    void bookingListRequiresOwnershipBeforeReadingData() {
        when(suppliers.requireOwned(1L, "other@example.test")).thenThrow(new ApiException.Forbidden("Not yours"));
        assertThatThrownBy(() -> service.listOwnedBookings(1L, "other@example.test"))
                .isInstanceOf(ApiException.Forbidden.class);
        verify(bookings, never()).findBySupplierId(any());
        verify(blocks, never()).findByServiceOfferingSupplierId(any());
    }

    @Test
    void supplierCanBlockAndUnblockADateWithBookings() {
        when(suppliers.requireOwned(1L, "owner@example.test")).thenReturn(supplier);
        var offering = offering(3L);
        var date = LocalDate.now().plusDays(1);
        when(offerings.findByIdForUpdate(3L)).thenReturn(Optional.of(offering));
        var saved = service.blockDate(1L, 3L, "owner@example.test", date);
        assertThat(saved.status()).isEqualTo("BLOCKED");
        var block = new SupplierAvailabilityBlock();
        block.setServiceOffering(offering);
        block.setBlockedDate(date);
        when(blocks.findByServiceOfferingIdAndBlockedDate(3L, date)).thenReturn(Optional.of(block));
        service.unblockDate(1L, 3L, "owner@example.test", date);
        verify(blocks).delete(block);
    }

    @Test
    void migrationRemovesOnlyBookingCreatedBlocks() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:manual_availability");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE supplier_availability_blocks (id BIGINT, booking_id BIGINT)");
            statement.execute("INSERT INTO supplier_availability_blocks VALUES (1, 42), (2, NULL)");
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V22__manual_service_availability.sql"));
            try (var rows = statement.executeQuery("SELECT id FROM supplier_availability_blocks")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getLong(1)).isEqualTo(2);
                assertThat(rows.next()).isFalse();
            }
        }
    }

    private SupplierServiceOffering offering(Long id) {
        var offering = new SupplierServiceOffering();
        offering.setId(id);
        offering.setSupplier(supplier);
        offering.setCategory(category);
        return offering;
    }

    private Booking booking(Long id, SupplierServiceOffering offering, LocalDate date, BookingStatus status) {
        var customer = new AppUser();
        customer.setName("A Couple");
        var booking = new Booking();
        booking.setId(id);
        booking.setSupplier(supplier);
        booking.setCustomer(customer);
        booking.setServiceOffering(offering);
        booking.setEventDate(date);
        booking.setStatus(status);
        return booking;
    }

    private SupplierServiceRequest request(PricingType type, Integer minimum, Integer maximum) {
        var translation = new SupplierServiceTranslationRequest("Service", "Description");
        return new SupplierServiceRequest(Map.of("en", translation, "fr", translation, "el", translation, "ar", translation),
                BigDecimal.TEN, 2L, type, minimum, maximum);
    }
}
