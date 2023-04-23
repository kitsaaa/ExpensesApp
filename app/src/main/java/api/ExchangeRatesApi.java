package api;

import model.ExchangeRates;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ExchangeRatesApi {
    @GET("latest")
    Call<ExchangeRates> getLatestRates(@Query("base") String baseCurrency, @Query("access_key") String apiKey);
}
