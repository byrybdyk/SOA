package org.lovesoa.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovieSearchRequest {
    private Map<String, Object> filters; // ключи вида "operator.height[gt]", значения String/Number/Array
    private List<String> sort;           // список "field:asc|desc"
    private Integer page = 0;            // default 0
    private Integer size = 20;           // default 20
}
