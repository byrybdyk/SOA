package org.lovesoa.secondservice.dto.movies;

import lombok.Data;

@Data
public class LocationDTO {
    private Long id;
    private Double x;
    private Double y;
    private Double z;
}
