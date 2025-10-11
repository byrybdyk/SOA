package org.lovesoa.secondservice.dto.movies;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class MovieSearchRequest {
    private Map<String, Object> filters;
    private List<String> sort;
    private Integer page = 0;
    private Integer size = 100;
}
