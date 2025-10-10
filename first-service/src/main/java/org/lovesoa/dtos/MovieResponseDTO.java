package org.lovesoa.dtos;


import lombok.Builder;
import lombok.Data;
import org.lovesoa.models.*;

@Data
@Builder
public class MovieResponseDTO {
    private Long id;
    private String name;
    private Coordinates coordinates;
    private Integer oscarsCount;
    private String genre;
    private String mpaaRating;
    private Person operator;
    private java.time.LocalDate creationDate;
}
