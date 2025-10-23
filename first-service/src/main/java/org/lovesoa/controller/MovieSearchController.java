package org.lovesoa.controller;

import org.lovesoa.dtos.MovieSearchRequest;
import org.lovesoa.exception.exceptions.BadRequestException;
import org.lovesoa.models.Movie;
import org.lovesoa.service.MovieSearchService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/movies")
public class MovieSearchController {

    private final MovieSearchService movieSearchService;

    public MovieSearchController(MovieSearchService movieSearchService) {
        this.movieSearchService = movieSearchService;
    }

    @PostMapping("/search")
    public Page<Movie> searchMovies(@RequestBody(required = false) MovieSearchRequest request) {
        if (request == null) request = new MovieSearchRequest();
        if (request.getSize() < 1) {
            throw new BadRequestException("size must be >= 1");
        }
        return movieSearchService.searchMovies(
                request.getFilters() != null ? request.getFilters() : Map.of(),
                request.getSort(),
                request.getPage() != null ? request.getPage() : 0,
                request.getSize() != null ? request.getSize() : 20
        );
    }
}
