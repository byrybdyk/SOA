package org.lovesoa.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "movies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private String name;

    @NotNull
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "coordinates_id", referencedColumnName = "id")
    private Coordinates coordinates;

    @NotNull
    @Column(nullable = false)
    private LocalDate creationDate;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer oscarsCount;

    @Enumerated(EnumType.STRING)
    private MovieGenre genre;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MpaaRating mpaaRating;

    @NotNull
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "operator_id", referencedColumnName = "id")
    private Person operator;
}
