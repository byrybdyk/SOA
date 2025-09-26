package org.lovesoa.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.lovesoa.models.Movie;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MovieSearchService {

    @PersistenceContext
    private EntityManager em;

    public Page<Movie> searchMovies(Map<String, Object> filters, List<String> sort, int page, int size) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Movie> cq = cb.createQuery(Movie.class);
        Root<Movie> root = cq.from(Movie.class);

        // Подготовка join для вложенных сущностей
        Map<String, From<?, ?>> joins = new HashMap<>();
        joins.put("root", root);

        // Создание фильтров
        List<Predicate> predicates = new ArrayList<>();
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String key = entry.getKey(); // e.g. "operator.height[gt]"
            Object value = entry.getValue();

            int opStart = key.indexOf("[");
            int opEnd = key.indexOf("]");
            if (opStart < 0 || opEnd < 0) continue;

            String fieldPath = key.substring(0, opStart);
            String operator = key.substring(opStart + 1, opEnd);

            Path<?> path = getPath(root, joins, fieldPath);
            Predicate pred = buildPredicate(cb, path, operator, value);
            if (pred != null) predicates.add(pred);
        }
        cq.where(predicates.toArray(new Predicate[0]));

        // Сортировка
        if (sort != null && !sort.isEmpty()) {
            List<Order> orders = new ArrayList<>();
            for (String s : sort) {
                String[] parts = s.split(":");
                if (parts.length != 2) continue;
                Path<?> path = getPath(root, joins, parts[0]);
                orders.add("desc".equalsIgnoreCase(parts[1]) ? cb.desc(path) : cb.asc(path));
            }
            cq.orderBy(orders);
        }

        // Пагинация
        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), 100);

        List<Movie> result = em.createQuery(cq)
                .setFirstResult(pageNumber * pageSize)
                .setMaxResults(pageSize)
                .getResultList();

        // Подсчет общего числа элементов
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Movie> countRoot = countQuery.from(Movie.class);
        countQuery.select(cb.count(countRoot));

        Map<String, From<?, ?>> countJoins = new HashMap<>();
        countJoins.put("root", countRoot);
        List<Predicate> countPredicates = new ArrayList<>();
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            int opStart = key.indexOf("[");
            int opEnd = key.indexOf("]");
            if (opStart < 0 || opEnd < 0) continue;

            String fieldPath = key.substring(0, opStart);
            String operator = key.substring(opStart + 1, opEnd);

            Path<?> path = getPath(countRoot, countJoins, fieldPath);
            Predicate pred = buildPredicate(cb, path, operator, value);
            if (pred != null) countPredicates.add(pred);
        }
        countQuery.where(countPredicates.toArray(new Predicate[0]));
        Long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(result, PageRequest.of(pageNumber, pageSize), total);
    }

    // Получение пути с join для вложенных объектов
    private Path<?> getPath(From<?, ?> root, Map<String, From<?, ?>> joins, String fieldPath) {
        String[] parts = fieldPath.split("\\.");
        From<?, ?> current = root;
        StringBuilder joinKey = new StringBuilder();

        for (int i = 0; i < parts.length - 1; i++) {
            joinKey.append(parts[i]);
            String key = joinKey.toString();
            if (!joins.containsKey(key)) {
                current = current.join(parts[i], JoinType.LEFT);
                joins.put(key, current);
            } else {
                current = joins.get(key);
            }
            joinKey.append(".");
        }
        return current.get(parts[parts.length - 1]);
    }

    // Создание Predicate по оператору
    private Predicate buildPredicate(CriteriaBuilder cb, Path<?> path, String operator, Object value) {
        Class<?> javaType = path.getJavaType();

        switch (operator.toLowerCase()) {
            case "eq": return cb.equal(path, value);
            case "ne": return cb.notEqual(path, value);
            case "gt":
                if (Number.class.isAssignableFrom(javaType)) {
                    return cb.gt((Path<? extends Number>) path, (Number) value);
                }
                break;
            case "lt":
                if (Number.class.isAssignableFrom(javaType)) {
                    return cb.lt((Path<? extends Number>) path, (Number) value);
                }
                break;
            case "gte":
                if (Number.class.isAssignableFrom(javaType)) {
                    return cb.ge((Path<? extends Number>) path, (Number) value);
                }
                break;
            case "lte":
                if (Number.class.isAssignableFrom(javaType)) {
                    return cb.le((Path<? extends Number>) path, (Number) value);
                }
                break;
            case "in":
                if (value instanceof Collection<?>) return path.in((Collection<?>) value);
                if (value.getClass().isArray()) return path.in(Arrays.asList((Object[]) value));
                break;
        }
        throw new IllegalArgumentException("Operator " + operator + " is not supported for field type " + javaType);
    }

}
