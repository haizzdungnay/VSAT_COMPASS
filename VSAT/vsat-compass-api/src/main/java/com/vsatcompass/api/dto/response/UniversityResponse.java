package com.vsatcompass.api.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UniversityResponse {
    private Long id;
    private String code;
    private String name;
    private String region;
    private String website;
}
