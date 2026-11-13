package com.vmataj.travel;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.*;

@Embeddable
record Destination(@Positive long placeId, @NotBlank @Size(max = 160) String name,
                   @Size(max = 160) String region, @NotBlank @Size(max = 160) String country,
                   @Pattern(regexp = "[A-Z]{2}") String countryCode,
                   @DecimalMin("-90") @DecimalMax("90") double latitude,
                   @DecimalMin("-180") @DecimalMax("180") double longitude,
                   @Size(max = 100) String timezone, @PositiveOrZero Long population) {}
