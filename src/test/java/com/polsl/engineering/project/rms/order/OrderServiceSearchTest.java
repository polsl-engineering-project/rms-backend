package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.menu.MenuApi;
import com.polsl.engineering.project.rms.order.vo.DeliveryMode;
import com.polsl.engineering.project.rms.order.vo.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceSearchTest {

    @Mock
    OrderRepository jdbcRepository;

    @Mock
    MenuApi menuApi;

    @Mock
    OrderMapper mapper;

    @Mock
    java.time.Clock clock;

    @Mock
    OrderOutboxService outboxService;

    @InjectMocks
    OrderService underTest;

    @Test
    @DisplayName("Given criteria_When searchOrders_Then delegates to repository and returns page")
    void GivenCriteria_WhenSearchOrders_ThenDelegatesToRepository() {
        // given
        var summary = new OrderPayloads.OrderSummaryResponse(UUID.randomUUID(), OrderStatus.APPROVED, DeliveryMode.ASAP, "John", Instant.now(), Instant.now());
        var page = new OrderPayloads.OrderPageResponse(List.of(summary), 0, 10, 1L, 1, true, true, false, false);

        when(jdbcRepository.searchOrders(any(OrderPayloads.OrderSearchRequest.class), anyInt(), anyInt()))
                .thenReturn(page);

        var request = OrderPayloads.OrderSearchRequest.builder()
                .page(0)
                .size(10)
                .build();

        // when
        var result = underTest.searchOrders(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        verify(jdbcRepository).searchOrders(eq(request), eq(0), eq(10));
    }

    @Test
    @DisplayName("Given null paging_When searchOrders_Then uses defaults")
    void GivenNullPaging_WhenSearchOrders_ThenUsesDefaults() {
        // given
        var summary = new OrderPayloads.OrderSummaryResponse(UUID.randomUUID(), OrderStatus.APPROVED, DeliveryMode.ASAP, "John", Instant.now(), Instant.now());
        var page = new OrderPayloads.OrderPageResponse(List.of(summary), 0, 20, 1L, 1, true, true, false, false);

        when(jdbcRepository.searchOrders(any(OrderPayloads.OrderSearchRequest.class), anyInt(), anyInt()))
                .thenReturn(page);

        var request = OrderPayloads.OrderSearchRequest.builder().build(); // page/size null

        // when
        underTest.searchOrders(request);

        // then
        var captorPage = ArgumentCaptor.forClass(Integer.class);
        verify(jdbcRepository).searchOrders(eq(request), captorPage.capture(), anyInt());
        assertThat(captorPage.getValue()).isEqualTo(0); // DEFAULT_PAGE_NUMBER

        // verify size captured as 20
        var captorSize = ArgumentCaptor.forClass(Integer.class);
        verify(jdbcRepository).searchOrders(eq(request), anyInt(), captorSize.capture());
        assertThat(captorSize.getValue()).isEqualTo(20);
    }
}

