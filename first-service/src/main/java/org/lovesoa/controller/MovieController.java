package org.lovesoa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.lovesoa.dtos.MovieCreateRequest;
import org.lovesoa.dtos.MovieResponseDTO;
import org.lovesoa.exception.exceptions.BadRequestException;
import org.lovesoa.exception.exceptions.MovieNotFoundException;
import org.lovesoa.models.Movie;
import org.lovesoa.repository.MovieRepository;
import org.lovesoa.service.MovieService;
import org.springframework.http.HttpStatus;
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
        if (request.getId() < 1) {
            throw new BadRequestException("id must be >= 1");
        }
        Movie created = movieService.createMovie(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponseDTO> getMovieById(@PathVariable Long id) {
        if (id < 1) {
            throw new BadRequestException("id must be >= 1");
        }
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
                .orElseThrow(() -> new MovieNotFoundException(id));
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
        if(id != null){
            if (id < 1) {
                throw new BadRequestException("id must be >= 1");
            }
        }

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
                    throw new BadRequestException("Movie id is required");

                Movie updated = movieService.updateMovie(movieId, request);
                return ResponseEntity.ok(updated);

            } else {
                throw new BadRequestException("Invalid request body format");
            }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        if (id < 1) {
            throw new BadRequestException("id must be >= 1");
        }
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count/by-operator-weight")
    public Map<String, Long> countByOperatorWeight(@RequestParam int weight) {
        if (weight < 1) {
            throw new BadRequestException("weight must be >= 1");
        }
        long count = movieRepository.countByOperatorWeightGreaterThan(weight);
        return Map.of("count", count);
    }
}
