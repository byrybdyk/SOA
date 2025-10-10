package org.lovesoa.service;

import lombok.RequiredArgsConstructor;
import org.lovesoa.dtos.MovieCreateRequest;
import org.lovesoa.models.*;
import org.lovesoa.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final CoordinatesRepository coordinatesRepository;
    private final PersonRepository personRepository;
    private final LocationRepository locationRepository;

    public Optional<Movie> getMovieById(Long id) {
        return movieRepository.findById(id);
    }

    @Transactional
    public Movie createMovie(MovieCreateRequest request) {
        // Сначала Location
        Location location = Location.builder()
                .x(request.getOperator().getLocation().getX())
                .y(request.getOperator().getLocation().getY())
                .z(request.getOperator().getLocation().getZ())
                .build();
        locationRepository.save(location);

        // Потом Person
        Person operator = Person.builder()
                .name(request.getOperator().getName())
                .height(request.getOperator().getHeight())
                .weight(request.getOperator().getWeight())
                .location(location)
                .build();
        personRepository.save(operator);

        // Потом Coordinates
        Coordinates coordinates = Coordinates.builder()
                .x(request.getCoordinates().getX())
                .y(request.getCoordinates().getY())
                .build();
        coordinatesRepository.save(coordinates);

        // Потом Movie
        Movie movie = Movie.builder()
                .name(request.getName())
                .coordinates(coordinates)
                .oscarsCount(request.getOscarsCount())
                .genre(request.getGenre() != null ? MovieGenre.valueOf(request.getGenre()) : null)
                .mpaaRating(MpaaRating.valueOf(request.getMpaaRating()))
                .operator(operator)
                .creationDate(LocalDate.now())
                .build();

        return movieRepository.save(movie);
    }
}

