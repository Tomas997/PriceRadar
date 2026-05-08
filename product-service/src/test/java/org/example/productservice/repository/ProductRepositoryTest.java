package org.example.productservice.repository;

import org.example.productservice.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void findByTitleContainingIgnoreCase_returnsMatchingProducts() {
        productRepository.save(new Product("Samsung Galaxy S24", "Citrus", "https://citrus.ua/1", true));
        productRepository.save(new Product("iPhone 15 Pro", "Rozetka", "https://rozetka.com/1", false));

        List<Product> results = productRepository.findByTitleContainingIgnoreCase("samsung");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).contains("Samsung");
    }

    @Test
    void findByTitleContainingIgnoreCase_isCaseInsensitive() {
        productRepository.save(new Product("iPhone 15 Pro", "Rozetka", "https://rozetka.com/1", true));

        List<Product> resultsUpper = productRepository.findByTitleContainingIgnoreCase("IPHONE");
        List<Product> resultsLower = productRepository.findByTitleContainingIgnoreCase("iphone");

        assertThat(resultsUpper).hasSize(1);
        assertThat(resultsLower).hasSize(1);
    }

    @Test
    void findByTitleContainingIgnoreCase_returnsMultipleMatches() {
        productRepository.save(new Product("Samsung Galaxy S24", "Citrus", "https://citrus.ua/1", true));
        productRepository.save(new Product("Samsung Galaxy A55", "Rozetka", "https://rozetka.com/2", true));
        productRepository.save(new Product("iPhone 15 Pro", "Rozetka", "https://rozetka.com/1", false));

        List<Product> results = productRepository.findByTitleContainingIgnoreCase("Samsung");

        assertThat(results).hasSize(2);
    }

    @Test
    void findByTitleContainingIgnoreCase_returnsEmptyWhenNoMatch() {
        productRepository.save(new Product("Samsung Galaxy", "Citrus", "https://citrus.ua/1", true));

        List<Product> results = productRepository.findByTitleContainingIgnoreCase("sony");

        assertThat(results).isEmpty();
    }

    @Test
    void save_persistsProduct() {
        Product product = new Product("Test Product", "TestShop", "https://test.com/1", true);

        Product saved = productRepository.save(product);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Test Product");
        assertThat(saved.getMarketplace()).isEqualTo("TestShop");
        assertThat(saved.getInStock()).isTrue();
    }

    @Test
    void existsById_returnsTrueForExisting() {
        Product saved = productRepository.save(new Product("Test", "Shop", "url", true));

        assertThat(productRepository.existsById(saved.getId())).isTrue();
    }

    @Test
    void existsById_returnsFalseForMissing() {
        assertThat(productRepository.existsById(999999L)).isFalse();
    }

    @Test
    void deleteById_removesProduct() {
        Product saved = productRepository.save(new Product("Test", "Shop", "url", true));
        Long id = saved.getId();

        productRepository.deleteById(id);

        assertThat(productRepository.findById(id)).isEmpty();
    }
}
