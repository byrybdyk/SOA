package org.lovesoa.controller;

import org.lovesoa.dtos.MovieSearchRequest;
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
        return movieSearchService.searchMovies(
                request.getFilters() != null ? request.getFilters() : Map.of(),
                request.getSort(),
                request.getPage() != null ? request.getPage() : 0,
                request.getSize() != null ? request.getSize() : 20
        );
    }
}
