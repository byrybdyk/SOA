# DefaultApi

All URIs are relative to *http://localhost:8080/api/oscar*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**directorsGetLoosersGet**](DefaultApi.md#directorsGetLoosersGet) | **GET** /directors/get-loosers | Получить режиссёров без оскаров |
| [**genresRedistributeRewardsFromGenreToGenrePost**](DefaultApi.md#genresRedistributeRewardsFromGenreToGenrePost) | **POST** /genres/redistribute-rewards/{from-genre}/{to-genre} | Перераспределить Оскары между жанрами |


<a id="directorsGetLoosersGet"></a>
# **directorsGetLoosersGet**
> List&lt;String&gt; directorsGetLoosersGet()

Получить режиссёров без оскаров

Возвращает список режиссёров, ни один фильм которых не получил \&quot;Оскара\&quot;.

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.DefaultApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/oscar");

    DefaultApi apiInstance = new DefaultApi(defaultClient);
    try {
      List<String> result = apiInstance.directorsGetLoosersGet();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DefaultApi#directorsGetLoosersGet");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

**List&lt;String&gt;**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Список режиссёров |  -  |
| **400** | Неверный запрос |  -  |
| **500** | Внутренняя ошибка сервера |  -  |

<a id="genresRedistributeRewardsFromGenreToGenrePost"></a>
# **genresRedistributeRewardsFromGenreToGenrePost**
> GenresRedistributeRewardsFromGenreToGenrePost200Response genresRedistributeRewardsFromGenreToGenrePost(fromGenre, toGenre)

Перераспределить Оскары между жанрами

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.DefaultApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/oscar");

    DefaultApi apiInstance = new DefaultApi(defaultClient);
    MovieGenre fromGenre = MovieGenre.fromValue("DRAMA"); // MovieGenre | Жанр, откуда забираются награды
    MovieGenre toGenre = MovieGenre.fromValue("DRAMA"); // MovieGenre | Жанр, куда передаются награды
    try {
      GenresRedistributeRewardsFromGenreToGenrePost200Response result = apiInstance.genresRedistributeRewardsFromGenreToGenrePost(fromGenre, toGenre);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DefaultApi#genresRedistributeRewardsFromGenreToGenrePost");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **fromGenre** | [**MovieGenre**](.md)| Жанр, откуда забираются награды | [enum: DRAMA, MUSICAL, THRILLER, FANTASY] |
| **toGenre** | [**MovieGenre**](.md)| Жанр, куда передаются награды | [enum: DRAMA, MUSICAL, THRILLER, FANTASY] |

### Return type

[**GenresRedistributeRewardsFromGenreToGenrePost200Response**](GenresRedistributeRewardsFromGenreToGenrePost200Response.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Операция завершена успешно |  -  |
| **400** | Неверный запрос |  -  |
| **401** | Требуется аутентификация |  -  |
| **403** | Доступ запрещен |  -  |
| **422** | Нарушены бизнес-правила |  -  |
| **500** | Внутренняя ошибка сервера |  -  |

