package org.example.productservice.repository;

import org.example.productservice.model.PriceEntry;
import org.example.productservice.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PriceEntryRepositoryTest {

    @Autowired
    private PriceEntryRepository priceEntryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void findByProductIdOrderByParsedAtDesc_returnsEntriesInDescendingOrder() throws InterruptedException {
        Product product = productRepository.save(new Product("Test", "Shop", "url", true));
        priceEntryRepository.save(new PriceEntry(product.getId(), 30000L));
        Thread.sleep(20);
        priceEntryRepository.save(new PriceEntry(product.getId(), 35000L));

        List<PriceEntry> result = priceEntryRepository.findByProductIdOrderByParsedAtDesc(product.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPrice()).isEqualTo(35000L);
        assertThat(result.get(1).getPrice()).isEqualTo(30000L);
    }

    @Test
    void findByProductIdOrderByParsedAtDesc_returnsOnlyEntriesForGivenProduct() {
        Product p1 = productRepository.save(new Product("Product 1", "Shop", "url1", true));
        Product p2 = productRepository.save(new Product("Product 2", "Shop", "url2", true));
        priceEntryRepository.save(new PriceEntry(p1.getId(), 10000L));
        priceEntryRepository.save(new PriceEntry(p2.getId(), 20000L));

        List<PriceEntry> result = priceEntryRepository.findByProductIdOrderByParsedAtDesc(p1.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductId()).isEqualTo(p1.getId());
    }

    @Test
    void findByProductIdOrderByParsedAtDesc_returnsEmptyForUnknownProduct() {
        List<PriceEntry> result = priceEntryRepository.findByProductIdOrderByParsedAtDesc(999999L);

        assertThat(result).isEmpty();
    }

    @Test
    void deleteByProductId_removesAllEntriesForProduct() {
        Product product = productRepository.save(new Product("Test", "Shop", "url", true));
        priceEntryRepository.save(new PriceEntry(product.getId(), 30000L));
        priceEntryRepository.save(new PriceEntry(product.getId(), 35000L));

        priceEntryRepository.deleteByProductId(product.getId());

        List<PriceEntry> result = priceEntryRepository.findByProductIdOrderByParsedAtDesc(product.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void deleteByProductId_doesNotAffectOtherProductEntries() {
        Product p1 = productRepository.save(new Product("Product 1", "Shop", "url1", true));
        Product p2 = productRepository.save(new Product("Product 2", "Shop", "url2", true));
        priceEntryRepository.save(new PriceEntry(p1.getId(), 10000L));
        priceEntryRepository.save(new PriceEntry(p2.getId(), 20000L));

        priceEntryRepository.deleteByProductId(p1.getId());

        assertThat(priceEntryRepository.findByProductIdOrderByParsedAtDesc(p1.getId())).isEmpty();
        assertThat(priceEntryRepository.findByProductIdOrderByParsedAtDesc(p2.getId())).hasSize(1);
    }

    @Test
    void save_persistsPriceEntryWithTimestamp() {
        Product product = productRepository.save(new Product("Test", "Shop", "url", true));

        PriceEntry saved = priceEntryRepository.save(new PriceEntry(product.getId(), 42000L));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getProductId()).isEqualTo(product.getId());
        assertThat(saved.getPrice()).isEqualTo(42000L);
        assertThat(saved.getParsedAt()).isNotNull();
    }
}
