package com.example.parserservice.marketplace;

import com.example.parserservice.exception.ProductParseException;
import com.example.parserservice.model.ProductCandidate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenShopParser implements MarketplaceSearchParser {

    private final ObjectMapper mapper;
    private final CloseableHttpClient httpClient;

    @Override
    public String marketplaceName() {
        return "OpenShop";
    }

    @Override
    public List<ProductCandidate> parseProducts(String json) {
        try {
            JsonNode root = mapper.readTree(json);

            List<ProductCandidate> products = new ArrayList<>();
            for (JsonNode item : root) {
                products.add(new ProductCandidate(
                        marketplaceName(),
                        item.path("name").asText(),
                        parsePrice(item.path("special").asText()),
                        item.path("href").asText(),
                        true
                ));
            }
            return products;
        } catch (Exception e) {
            throw new ProductParseException("Failed to parse OpenShop response", e);
        }
    }

    @Override
    public List<ProductCandidate> searchProducts(String query) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://openshop.ua/index.php"
                + "?route=extension/module/cyber_autosearch/ajaxLiveSearch"
                + "&filter_name=" + encoded;

        HttpGet request = new HttpGet(url);
        request.addHeader("Accept", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getCode() != 200) {
                throw new ProductParseException("OpenShop HTTP " + response.getCode(), null);
            }
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            return parseProducts(body);
        } catch (IOException | ParseException e) {
            throw new ProductParseException("OpenShop request failed", e);
        }
    }

    private long parsePrice(String raw) {
        try {
            String cleaned = raw
                    .replaceAll("<[^>]*>", "")   // strip HTML tags
                    .replace("грн.", "")
                    .trim();
            return (long) Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Cannot parse OpenShop price: '{}'", raw);
            return 0L;
        }
    }
}
