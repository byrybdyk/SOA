package org.lovesoa.secondservice.resources;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.lovesoa.secondservice.client.MoviesClient;
import org.lovesoa.secondservice.dto.movies.MovieResponseDTO;
import org.lovesoa.secondservice.dto.movies.MovieSearchRequest;


import java.util.*;
import java.util.stream.Collectors;

@Path("/genres")
@Produces(MediaType.APPLICATION_JSON)
public class GenresResource {

    @Inject
    MoviesClient moviesClient;

    @POST
    @Path("/redistribute-rewards/{fromGenre}/{toGenre}")
    public Map<String, Object> redistributeOscars(
            @PathParam("fromGenre") String fromGenre,
            @PathParam("toGenre") String toGenre,
            @HeaderParam("Authorization") String bearerToken
    ) {
        if (bearerToken == null || bearerToken.isBlank())
            throw new BadRequestException("Bearer-токен обязателен");

        String token = bearerToken.replace("Bearer ", "");

        // Получаем все фильмы
        MovieSearchRequest request = new MovieSearchRequest();
        List<MovieResponseDTO> allMovies = moviesClient.searchMovies(request, token).getContent();

        List<MovieResponseDTO> from = allMovies.stream()
                .filter(m -> fromGenre.equalsIgnoreCase(m.getGenre()))
                .collect(Collectors.toList());

        List<MovieResponseDTO> to = allMovies.stream()
                .filter(m -> toGenre.equalsIgnoreCase(m.getGenre()))
                .collect(Collectors.toList());

        int totalOscars = from.stream().mapToInt(m -> Optional.ofNullable(m.getOscarsCount()).orElse(0)).sum();
        int countTo = to.size();

        if (countTo == 0)
            throw new BadRequestException("Нет фильмов жанра " + toGenre + " для перераспределения");

        int perMovie = totalOscars / countTo;

        // Обновляем: у from жанра — 0, у to — добавляем
        for (MovieResponseDTO movie : from) {
            moviesClient.updateMovieOscars(movie.getId(), 0, token);
        }

        for (MovieResponseDTO movie : to) {
            int newCount = perMovie;
            moviesClient.updateMovieOscars(movie.getId(), newCount, token);
        }

        return Map.of(
                "redistributedFrom", fromGenre,
                "redistributedTo", toGenre,
                "oscarsMoved", totalOscars,
                "oscarsPerMovie", perMovie
        );
    }
}