package org.lovesoa.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.lovesoa.dtos.MovieCreateRequest;
import org.lovesoa.dtos.MovieResponseDTO;
import org.lovesoa.models.Movie;
import org.lovesoa.service.MovieService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @PostMapping
    public ResponseEntity<Movie> createMovie(@RequestBody @Valid MovieCreateRequest request) {
        Movie created = movieService.createMovie(request);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponseDTO> getMovieById(@PathVariable Long id) {
        return movieService.getMovieById(id)
                .map(movie -> ResponseEntity.ok(
                        MovieResponseDTO.builder()
                                .id(movie.getId())
                                .name(movie.getName())
                                .coordinates(movie.getCoordinates())
                                .oscarsCount(movie.getOscarsCount())
                                .genre(movie.getGenre() != null ? movie.getGenre().name() : null)
                                .mpaaRating(movie.getMpaaRating().name())
                                .operator(movie.getOperator())
                                .creationDate(movie.getCreationDate())
                                .build()
                ))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
