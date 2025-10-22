package org.lovesoa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.lovesoa.dtos.MovieCreateRequest;
import org.lovesoa.dtos.MovieResponseDTO;
import org.lovesoa.models.Movie;
import org.lovesoa.repository.MovieRepository;
import org.lovesoa.service.MovieService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final MovieRepository movieRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    /**
     * Универсальный PUT — поддерживает одиночное и пакетное обновление фильмов
     *
     * Примеры:
     *  PUT /movies/5  — { ... }       → обновит один фильм
     *  PUT /movies    — [ {...}, {...} ] → обновит несколько фильмов
     */
    @PutMapping(value = {"/{id}", ""})
    public ResponseEntity<?> updateMovieFlexible(
            @PathVariable(required = false) Long id,
            @RequestBody Object body
    ) {
        try {
            if (body instanceof List<?> list) {
                // Пакетное обновление
                List<MovieCreateRequest> requests = ((List<?>) list).stream()
                        .map(item -> objectMapper.convertValue(item, MovieCreateRequest.class))
                        .toList();

                List<Movie> updated = requests.stream()
                        .map(req -> movieService.updateMovie(req.getId(), req))
                        .toList();

                return ResponseEntity.ok(updated);

            } else if (body instanceof Map<?, ?> map) {
                // Одиночное обновление
                MovieCreateRequest request = objectMapper.convertValue(map, MovieCreateRequest.class);
                Long movieId = id != null ? id : request.getId();

                if (movieId == null)
                    return ResponseEntity.badRequest().body("Movie id is required");

                Movie updated = movieService.updateMovie(movieId, request);
                return ResponseEntity.ok(updated);

            } else {
                return ResponseEntity.badRequest().body("Invalid request body format");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing request: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count/by-operator-weight")
    public Map<String, Long> countByOperatorWeight(@RequestParam int weight) {
        long count = movieRepository.countByOperatorWeightGreaterThan(weight);
        return Map.of("count", count);
    }
}
