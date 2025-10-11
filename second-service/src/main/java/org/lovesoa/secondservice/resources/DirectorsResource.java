package org.lovesoa.secondservice.resources;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.lovesoa.secondservice.client.MoviesClient;
import org.lovesoa.secondservice.dto.movies.MoviePageDTO;
import org.lovesoa.secondservice.dto.movies.MovieSearchRequest;

import java.util.*;

@Path("/directors")
@Produces(MediaType.APPLICATION_JSON)
public class DirectorsResource {

    @Inject
    MoviesClient moviesClient;

    @POST
    @Path("/loosers/search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public MoviePageDTO getLooserDirectors(
            @HeaderParam("Authorization") String bearerToken,
            MovieSearchRequest request
    ) {
        if (bearerToken == null || bearerToken.isBlank())
            throw new BadRequestException("Bearer-токен обязателен");

        if (request == null)
            request = new MovieSearchRequest();

        Map<String, Object> filters = new HashMap<>();
        if(request.getFilters() != null && !request.getFilters().containsKey("oscarsCount[eq]")){
            filters.put("oscarsCount[eq]", 0);
        }

        if (request.getFilters() != null)
            filters.putAll(request.getFilters());

        request.setFilters(filters);

        return moviesClient.searchMovies(
                request,
                bearerToken.replace("Bearer ", "")
        );
    }

}
