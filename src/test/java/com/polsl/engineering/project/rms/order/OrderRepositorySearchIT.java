package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.ContainersEnvironment;
import com.polsl.engineering.project.rms.order.vo.DeliveryMode;
import com.polsl.engineering.project.rms.order.vo.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({OrderRepository.class, OrderDbMapper.class, OrderQueryParamsBuilder.class})
@DisplayName("Integration tests for OrderRepository.searchOrders")
class OrderRepositorySearchIT extends ContainersEnvironment {

    @Autowired
    OrderRepository underTest;

    @Autowired
    DataSource dataSource;

    @BeforeEach
    void setUp() {
        var populator = new ResourceDatabasePopulator(new ClassPathResource("init/sql/order.sql"));
        populator.execute(dataSource);
    }

    @Test
    @DisplayName("Given criteria_When searchOrders_Then returns matching orders paged")
    void GivenCriteria_WhenSearchOrders_ThenReturnsMatchingOrders() {
        // given
        var criteria = OrderPayloads.OrderSearchRequest.builder()
                .statuses(List.of(OrderStatus.APPROVED))
                .customerFirstName("John")
                .page(0)
                .size(10)
                .build();

        // when
        var page = underTest.searchOrders(criteria, 0, 10);

        // then
        assertThat(page).isNotNull();
        assertThat(page.content()).isNotEmpty();
        // one of entries should have customerFirstName John -> verify at least one summary exists
        assertThat(page.content()).anySatisfy(s -> assertThat(s.customerFirstName()).isEqualTo("John"));
    }
}

