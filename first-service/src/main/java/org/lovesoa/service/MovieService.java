package org.lovesoa.service;

import lombok.RequiredArgsConstructor;
import org.lovesoa.dtos.MovieCreateRequest;
import org.lovesoa.exception.exceptions.MovieNotFoundException;
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
    @Transactional
    public Movie updateMovie(Long id, MovieCreateRequest request) {
        // Находим существующий фильм
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        // Обновляем Location оператора
        if (request.getOperator() != null && request.getOperator().getLocation() != null) {
            Location location = movie.getOperator().getLocation();
            if (location == null) {
                location = Location.builder().build();
                movie.getOperator().setLocation(location);
            }
            location.setX(request.getOperator().getLocation().getX());
            location.setY(request.getOperator().getLocation().getY());
            location.setZ(request.getOperator().getLocation().getZ());
            locationRepository.save(location);
        }

        // Обновляем Person (оператора)
        if (request.getOperator() != null) {
            Person operator = movie.getOperator();
            if (operator == null) {
                operator = Person.builder().build();
                movie.setOperator(operator);
            }
            operator.setName(request.getOperator().getName());
            operator.setHeight(request.getOperator().getHeight());
            operator.setWeight(request.getOperator().getWeight());
            operator.setLocation(movie.getOperator().getLocation());
            personRepository.save(operator);
        }

        // Обновляем Coordinates
        if (request.getCoordinates() != null) {
            Coordinates coordinates = movie.getCoordinates();
            if (coordinates == null) {
                coordinates = Coordinates.builder().build();
                movie.setCoordinates(coordinates);
            }
            coordinates.setX(request.getCoordinates().getX());
            coordinates.setY(request.getCoordinates().getY());
            coordinatesRepository.save(coordinates);
        }

        // Обновляем Movie
        movie.setName(request.getName());
        movie.setOscarsCount(request.getOscarsCount());
        movie.setGenre(request.getGenre() != null ? MovieGenre.valueOf(request.getGenre()) : null);
        movie.setMpaaRating(MpaaRating.valueOf(request.getMpaaRating()));

        return movieRepository.save(movie);
    }

    @Transactional
    public void deleteMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException(id));

        movieRepository.delete(movie);
    }

}

