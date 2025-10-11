package org.lovesoa.secondservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.lovesoa.secondservice.dto.movies.*;
import org.lovesoa.secondservice.dto.pagable.PageableDTO;
import org.lovesoa.secondservice.dto.pagable.SortDTO;

import java.util.*;

@ApplicationScoped
public class MoviesClient {

    private static final String BASE_URL = "http://first-service:8080/movies";
    private final Client client = ClientBuilder.newClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MoviePageDTO searchMovies(MovieSearchRequest request, String bearerToken) {
        if (request.getFilters() == null) request.setFilters(new HashMap<>());
        if (request.getSort() == null) request.setSort(new ArrayList<>());
        if (request.getPage() == null) request.setPage(0);
        if (request.getSize() == null) request.setSize(20);

        WebTarget target = client.target(BASE_URL + "/search");
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

            List<Map<String, Object>> contentList = (List<Map<String, Object>>) pageMap.get("content");
            List<MovieResponseDTO> movies = new ArrayList<>();
            if (contentList != null) {
                for (Map<String, Object> item : contentList) {
                    MovieResponseDTO dto = new MovieResponseDTO();
                    dto.setId(item.get("id") != null ? ((Number) item.get("id")).longValue() : null);
                    dto.setName((String) item.get("name"));
                    dto.setGenre((String) item.get("genre"));
                    dto.setOscarsCount(item.get("oscarsCount") != null ? ((Number) item.get("oscarsCount")).intValue() : 0);

                    Map<String, Object> coordinatesMap = (Map<String, Object>) item.get("coordinates");
                    if (coordinatesMap != null) {
                        CoordinatesDTO coords = new CoordinatesDTO();
                        coords.setId(coordinatesMap.get("id") != null ? ((Number) coordinatesMap.get("id")).longValue() : null);
                        coords.setX(coordinatesMap.get("x") != null ? ((Number) coordinatesMap.get("x")).doubleValue() : 0);
                        coords.setY(coordinatesMap.get("y") != null ? ((Number) coordinatesMap.get("y")).doubleValue() : 0);
                        dto.setCoordinates(coords);
                    }

                    dto.setCreationDate((String) item.get("creationDate"));
                    dto.setMpaaRating((String) item.get("mpaaRating"));

                    Map<String, Object> operatorMap = (Map<String, Object>) item.get("operator");
                    if (operatorMap != null) {
                        OperatorDTO operator = new OperatorDTO();
                        operator.setId(operatorMap.get("id") != null ? ((Number) operatorMap.get("id")).longValue() : null);
                        operator.setName((String) operatorMap.get("name"));
                        operator.setHeight(operatorMap.get("height") != null ? ((Number) operatorMap.get("height")).doubleValue() : null);
                        operator.setWeight(operatorMap.get("weight") != null ? ((Number) operatorMap.get("weight")).doubleValue() : null);

                        Map<String, Object> locationMap = (Map<String, Object>) operatorMap.get("location");
                        if (locationMap != null) {
                            LocationDTO loc = new LocationDTO();
                            loc.setId(locationMap.get("id") != null ? ((Number) locationMap.get("id")).longValue() : null);
                            loc.setX(locationMap.get("x") != null ? ((Number) locationMap.get("x")).doubleValue() : null);
                            loc.setY(locationMap.get("y") != null ? ((Number) locationMap.get("y")).doubleValue() : null);
                            loc.setZ(locationMap.get("z") != null ? ((Number) locationMap.get("z")).doubleValue() : null);
                            operator.setLocation(loc);
                        }

                        dto.setOperator(operator);
                    }

                    movies.add(dto);
                }
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
            throw new RuntimeException("Ошибка обработки JSON-ответа", e);
        }
    }

    public void updateMovieOscars(Long movieId, int newCount, String bearerToken) {
        WebTarget target = client.target(BASE_URL).path(String.valueOf(movieId));

        System.out.println("=== MoviesClient.updateMovieOscars ===");
        System.out.println("URL: " + target.getUri());
        System.out.println("Bearer token: " + bearerToken);
        System.out.println("New oscarsCount: " + newCount);

        Invocation.Builder builder = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", "Bearer " + bearerToken);

        Map<String, Object> update = Map.of("oscarsCount", newCount);
        Response response = builder.put(Entity.json(update));

        String rawResponse = response.readEntity(String.class);
        System.out.println("HTTP status: " + response.getStatus());
        System.out.println("Raw response: " + rawResponse);

        if (response.getStatus() != 200) {
            throw new RuntimeException("Ошибка обновления фильма " + movieId + ": " + response.getStatus());
        }
    }
}
