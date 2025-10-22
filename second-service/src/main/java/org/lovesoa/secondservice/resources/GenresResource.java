package org.lovesoa.secondservice.resources;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.lovesoa.secondservice.client.MoviesClient;
import org.lovesoa.secondservice.dto.RedistributeRewardsResponseDTO;
import org.lovesoa.secondservice.dto.movies.MovieResponseDTO;
import org.lovesoa.secondservice.dto.movies.MovieSearchRequest;

import java.util.*;

@Path("/genres")
@Produces(MediaType.APPLICATION_JSON)
public class GenresResource {

    @Inject
    MoviesClient moviesClient;

    @POST
    @Path("/redistribute-rewards/{fromGenre}/{toGenre}")
    public RedistributeRewardsResponseDTO redistributeRewards(
            @HeaderParam("Authorization") String bearerToken,
            @PathParam("fromGenre") String fromGenre,
            @PathParam("toGenre") String toGenre
    ) {
        if (bearerToken == null || bearerToken.isBlank())
            throw new BadRequestException("Bearer-токен обязателен");

        String token = bearerToken.replace("Bearer ", "");

        MovieSearchRequest fromRequest = new MovieSearchRequest();
        fromRequest.setFilters(Map.of(
                "genre[eq]", fromGenre,
                "oscarsCount[gt]", 0
        ));
        List<MovieResponseDTO> fromMovies = moviesClient.searchMovies(fromRequest, token).getContent();

        MovieSearchRequest toRequest = new MovieSearchRequest();
        toRequest.setFilters(Map.of("genre[eq]", toGenre));
        List<MovieResponseDTO> toMovies = moviesClient.searchMovies(toRequest, token).getContent();

        int totalOscars = fromMovies.stream().mapToInt(MovieResponseDTO::getOscarsCount).sum();

        fromMovies.forEach(m -> m.setOscarsCount(0));
        moviesClient.updateMoviesBatch(fromMovies, token);

        Random random = new Random();
        for (int i = 0; i < totalOscars; i++) {
            if (toMovies.isEmpty()) break;
            MovieResponseDTO randomMovie = toMovies.get(random.nextInt(toMovies.size()));
            randomMovie.setOscarsCount(randomMovie.getOscarsCount() + 1);
        }
        moviesClient.updateMoviesBatch(toMovies, token);

        RedistributeRewardsResponseDTO response = new RedistributeRewardsResponseDTO();
        response.setTransferredCount(totalOscars);
        return response;
    }
}
