package org.lovesoa.secondservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.lovesoa.secondservice.dto.MovieCreateRequest;
import org.lovesoa.secondservice.dto.movies.*;
import org.lovesoa.secondservice.dto.pagable.PageableDTO;
import org.lovesoa.secondservice.dto.pagable.SortDTO;

import java.util.*;

@ApplicationScoped
public class MoviesClient {

    private static final String BASE_URL = "http://first-service:8080/first-service/movies";
    private final Client client = ClientBuilder.newClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // --- поиск фильмов ---
    public MoviePageDTO searchMovies(MovieSearchRequest request, String bearerToken) {
        if (request.getFilters() == null) request.setFilters(new HashMap<>());
        if (request.getSort() == null) request.setSort(new ArrayList<>());
        if (request.getPage() == null) request.setPage(0);
        if (request.getSize() == null) request.setSize(20);

        WebTarget target = client.target(BASE_URL + "/search");
        System.out.println("Request URL: '" + target.getUri() + "'");
        Invocation.Builder builder = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", "Bearer " + bearerToken);

        Response response = builder.post(Entity.json(request));
        String rawResponse = response.readEntity(String.class);

        if (response.getStatus() != 200) {
            throw new RuntimeException("Ошибка при запросе к первому сервису: " + response.getStatus());
        }

        try {
            Map<String, Object> pageMap = objectMapper.readValue(rawResponse, Map.class);
            List<Map<String, Object>> contentList = pageMap.get("content") != null
                    ? objectMapper.convertValue(pageMap.get("content"), List.class)
                    : Collections.emptyList();

            List<MovieResponseDTO> movies = new ArrayList<>();
            for (Map<String, Object> item : contentList) {
                MovieResponseDTO dto = new MovieResponseDTO();
                dto.setId(item.get("id") != null ? ((Number) item.get("id")).longValue() : null);
                dto.setName(item.getOrDefault("name", "").toString());
                dto.setGenre(item.getOrDefault("genre", "").toString());
                dto.setOscarsCount(item.get("oscarsCount") != null ? ((Number) item.get("oscarsCount")).intValue() : 0);
                dto.setCreationDate(item.getOrDefault("creationDate", null) != null ? item.get("creationDate").toString() : null);
                dto.setMpaaRating(item.getOrDefault("mpaaRating", null) != null ? item.get("mpaaRating").toString() : null);

                Map<String, Object> coordinatesMap = item.get("coordinates") != null
                        ? objectMapper.convertValue(item.get("coordinates"), Map.class)
                        : null;
                if (coordinatesMap != null) {
                    CoordinatesDTO coords = new CoordinatesDTO();
                    coords.setId(coordinatesMap.get("id") != null ? ((Number) coordinatesMap.get("id")).longValue() : null);
                    coords.setX(coordinatesMap.get("x") != null ? ((Number) coordinatesMap.get("x")).doubleValue() : 0);
                    coords.setY(coordinatesMap.get("y") != null ? ((Number) coordinatesMap.get("y")).doubleValue() : 0);
                    dto.setCoordinates(coords);
                }

                Map<String, Object> operatorMap = item.get("operator") != null
                        ? objectMapper.convertValue(item.get("operator"), Map.class)
                        : null;
                if (operatorMap != null) {
                    OperatorDTO operator = new OperatorDTO();
                    operator.setId(operatorMap.get("id") != null ? ((Number) operatorMap.get("id")).longValue() : null);
                    operator.setName(operatorMap.getOrDefault("name", "").toString());
                    operator.setHeight(operatorMap.get("height") != null ? ((Number) operatorMap.get("height")).doubleValue() : null);
                    operator.setWeight(operatorMap.get("weight") != null ? ((Number) operatorMap.get("weight")).doubleValue() : null);

                    Map<String, Object> locationMap = operatorMap.get("location") != null
                            ? objectMapper.convertValue(operatorMap.get("location"), Map.class)
                            : null;
                    if (locationMap != null) {
                        LocationDTO loc = new LocationDTO();
                        loc.setId(locationMap.get("id") != null ? ((Number) locationMap.get("id")).longValue() : null);
                        loc.setX(locationMap.get("x") != null ? ((Number) locationMap.get("x")).intValue() : null);
                        loc.setY(locationMap.get("y") != null ? ((Number) locationMap.get("y")).intValue() : null);
                        loc.setZ(locationMap.get("z") != null ? ((Number) locationMap.get("z")).longValue() : null);
                        operator.setLocation(loc);
                    }

                    dto.setOperator(operator);
                }

                movies.add(dto);
            }

            int pageNumber = pageMap.get("number") != null ? ((Number) pageMap.get("number")).intValue() : 0;
            int pageSize = pageMap.get("size") != null ? ((Number) pageMap.get("size")).intValue() : movies.size();
            long totalElements = pageMap.get("totalElements") != null ? ((Number) pageMap.get("totalElements")).longValue() : movies.size();
            int totalPages = (int) Math.ceil((double) totalElements / pageSize);

            SortDTO sortDTO = new SortDTO();
            sortDTO.setSorted(false);
            sortDTO.setUnsorted(true);
            sortDTO.setEmpty(true);

            PageableDTO pageableDTO = new PageableDTO();
            pageableDTO.setPageNumber(pageNumber);
            pageableDTO.setPageSize(pageSize);
            pageableDTO.setOffset((long) (pageNumber * pageSize));
            pageableDTO.setPaged(true);
            pageableDTO.setUnpaged(false);
            pageableDTO.setSort(sortDTO);

            MoviePageDTO pageDTO = new MoviePageDTO();
            pageDTO.setContent(movies);
            pageDTO.setPageable(pageableDTO);
            pageDTO.setTotalElements(totalElements);
            pageDTO.setTotalPages(totalPages);
            pageDTO.setNumberOfElements(movies.size());
            pageDTO.setSize(pageSize);
            pageDTO.setNumber(pageNumber);
            pageDTO.setSort(sortDTO);
            pageDTO.setFirst(pageNumber == 0);
            pageDTO.setLast(pageNumber == totalPages - 1);
            pageDTO.setEmpty(movies.isEmpty());

            return pageDTO;

        } catch (Exception e) {
            throw new RuntimeException("Ошибка обработки JSON-ответа: " + e.getMessage(), e);
        }
    }

    public void updateMoviesBatch(List<MovieResponseDTO> movies, String bearerToken) {
        WebTarget target = client.target(BASE_URL);
        Invocation.Builder builder = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", "Bearer " + bearerToken);

        List<MovieCreateRequest> requests = new ArrayList<>();
        for (MovieResponseDTO movie : movies) {
            requests.add(buildMovieCreateRequest(movie, movie.getOscarsCount()));
        }

        Response response = builder.put(Entity.json(requests));

        if (response.getStatus() != 200) {
            throw new RuntimeException("Ошибка пакетного обновления фильмов: " + response.getStatus());
        }
    }

    private MovieCreateRequest buildMovieCreateRequest(MovieResponseDTO movie, int oscarsCount) {
        MovieCreateRequest request = new MovieCreateRequest();
        request.setId(movie.getId());
        request.setName(movie.getName());
        request.setGenre(movie.getGenre());
        request.setOscarsCount(oscarsCount);
        request.setMpaaRating(movie.getMpaaRating());

        MovieCreateRequest.CoordinatesDto coords = new MovieCreateRequest.CoordinatesDto();
        if (movie.getCoordinates() != null) {
            coords.setX((int) movie.getCoordinates().getX());
            coords.setY((float) movie.getCoordinates().getY());
        }
        request.setCoordinates(coords);

        MovieCreateRequest.PersonDto operator = new MovieCreateRequest.PersonDto();
        if (movie.getOperator() != null) {
            operator.setName(movie.getOperator().getName());
            operator.setHeight(movie.getOperator().getHeight().floatValue());
            operator.setWeight(movie.getOperator().getWeight().intValue());

            MovieCreateRequest.PersonDto.LocationDto location = new MovieCreateRequest.PersonDto.LocationDto();
            if (movie.getOperator().getLocation() != null) {
                location.setX(movie.getOperator().getLocation().getX().intValue());
                location.setY(movie.getOperator().getLocation().getY().intValue());
                location.setZ(movie.getOperator().getLocation().getZ().longValue());
            }
            operator.setLocation(location);
        }
        request.setOperator(operator);
        return request;
    }
}
