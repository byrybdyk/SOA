package org.lovesoa.secondservice.dto.movies;

import lombok.Data;

@Data
public class LocationDTO {
    private Long id;
    private Integer x;
    private Integer y;
    private Long z;
}
