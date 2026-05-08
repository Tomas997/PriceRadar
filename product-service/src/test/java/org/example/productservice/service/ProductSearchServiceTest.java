package org.example.productservice.service;

import org.example.productservice.dto.TrackProductRequest;
import org.example.productservice.exception.ProductNotFoundException;
import org.example.productservice.model.PriceEntry;
import org.example.productservice.model.Product;
import org.example.productservice.repository.PriceEntryRepository;
import org.example.productservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PriceEntryRepository priceEntryRepository;

    @InjectMocks
    private ProductSearchService service;

    @Test
    void trackProduct_savesProductAndPriceEntry() {
        TrackProductRequest request = new TrackProductRequest("Rozetka", "iPhone 15", 40000L, "https://rozetka.com/1", true);
        Product saved = new Product("iPhone 15", "Rozetka", "https://rozetka.com/1", true);
        saved.setId(1L);
        when(productRepository.save(any(Product.class))).thenReturn(saved);
        when(priceEntryRepository.save(any(PriceEntry.class))).thenReturn(new PriceEntry(1L, 40000L));

        Product result = service.trackProduct(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("iPhone 15");
        verify(productRepository).save(any(Product.class));
        verify(priceEntryRepository).save(any(PriceEntry.class));
    }

    @Test
    void getAllTrackedProducts_returnsList() {
        List<Product> products = List.of(new Product("Test", "Shop", "url", true));
        when(productRepository.findAll()).thenReturn(products);

        List<Product> result = service.getAllTrackedProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test");
    }

    @Test
    void getAllTrackedProducts_returnsEmptyList() {
        when(productRepository.findAll()).thenReturn(List.of());

        List<Product> result = service.getAllTrackedProducts();

        assertThat(result).isEmpty();
    }

    @Test
    void getProductById_returnsProduct_whenExists() {
        Product product = new Product("Test", "Shop", "url", true);
        product.setId(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Product result = service.getProductById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test");
    }

    @Test
    void getProductById_throwsNotFoundException_whenNotExists() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProductById(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getPriceHistory_returnsPrices_whenProductExists() {
        Product product = new Product("Test", "Shop", "url", true);
        product.setId(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        List<PriceEntry> entries = List.of(new PriceEntry(1L, 30000L), new PriceEntry(1L, 35000L));
        when(priceEntryRepository.findByProductIdOrderByParsedAtDesc(1L)).thenReturn(entries);

        List<PriceEntry> result = service.getPriceHistory(1L);

        assertThat(result).hasSize(2);
    }

    @Test
    void getPriceHistory_throwsNotFoundException_whenProductNotExists() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPriceHistory(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void deleteProduct_deletesProductAndPrices_whenExists() {
        when(productRepository.existsById(1L)).thenReturn(true);

        service.deleteProduct(1L);

        verify(priceEntryRepository).deleteByProductId(1L);
        verify(productRepository).deleteById(1L);
    }

    @Test
    void deleteProduct_throwsNotFoundException_whenNotExists() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteProduct(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");

        verify(productRepository, never()).deleteById(any());
        verify(priceEntryRepository, never()).deleteByProductId(any());
    }

    @Test
    void searchByTitle_returnsMatchingProducts() {
        List<Product> products = List.of(new Product("Samsung Galaxy S24", "Citrus", "url", true));
        when(productRepository.findByTitleContainingIgnoreCase("samsung")).thenReturn(products);

        List<Product> result = service.searchByTitle("samsung");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).contains("Samsung");
    }

    @Test
    void searchByTitle_returnsEmptyList_whenNoMatch() {
        when(productRepository.findByTitleContainingIgnoreCase("xyz")).thenReturn(List.of());

        List<Product> result = service.searchByTitle("xyz");

        assertThat(result).isEmpty();
    }
}
