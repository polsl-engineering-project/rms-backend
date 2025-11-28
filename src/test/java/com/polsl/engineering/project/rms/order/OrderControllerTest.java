package com.polsl.engineering.project.rms.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polsl.engineering.project.rms.order.vo.Address;
import com.polsl.engineering.project.rms.order.vo.CustomerInfo;
import com.polsl.engineering.project.rms.order.vo.Money;
import com.polsl.engineering.project.rms.security.jwt.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.polsl.engineering.project.rms.general.exception.ResourceNotFoundException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;

import java.util.List;
import java.util.UUID;

import static com.polsl.engineering.project.rms.order.vo.DeliveryMode.ASAP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    OrderService orderService;

    // required by context
    @MockitoBean
    JwtService jwtService;

    @Test
    @DisplayName("Given valid PlacePickUpOrder_When POST /api/v1/orders/place-pick-up-order_Then 201 and body")
    void GivenValidPlacePickUpOrder_WhenPostPlacePickUpOrder_Then201AndBody() throws Exception {
        // given
        var menuItemId = UUID.randomUUID();
        var line = new OrderPayloads.OrderLineRequest(menuItemId, 2);
        var customerInfo = new CustomerInfo("John","Doe","123456789");
        var req = new OrderPayloads.PlacePickUpOrderRequest(customerInfo, ASAP, LocalTime.now(), List.of(line));
        var expectedId = UUID.randomUUID();
        var expectedResponse = new OrderPayloads.OrderPlacedResponse(expectedId);

        when(orderService.placePickUpOrder(any())).thenReturn(expectedResponse);

        // when
        var result = mockMvc.perform(post("/api/v1/orders/place-pick-up-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        // then
        result.andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(expectedId.toString()));

        ArgumentCaptor<OrderPayloads.PlacePickUpOrderRequest> captor = ArgumentCaptor.forClass(OrderPayloads.PlacePickUpOrderRequest.class);
        verify(orderService).placePickUpOrder(captor.capture());
        var captured = captor.getValue();
        assertThat(captured.orderLines()).hasSize(1);
        assertThat(captured.customerInfo().firstName()).isEqualTo("John");
        verifyNoMoreInteractions(orderService);
    }

    @Test
    @DisplayName("Given valid PlaceDeliveryOrder_When POST /api/v1/orders/place-delivery-order_Then 201 and body")
    void GivenValidPlaceDeliveryOrder_WhenPostPlaceDeliveryOrder_Then201AndBody() throws Exception {
        // given
        var menuItemId = UUID.randomUUID();
        var line = new OrderPayloads.OrderLineRequest(menuItemId, 1);
        var customerInfo = new CustomerInfo("Jane","Roe","987654321");
        var address = new Address("Main St","1", null, "City","00-001");
        var req = new OrderPayloads.PlaceDeliveryOrderRequest(customerInfo, address, ASAP, LocalTime.now(), List.of(line));
        var expectedId = UUID.randomUUID();
        var expectedResponse = new OrderPayloads.OrderPlacedResponse(expectedId);

        when(orderService.placeDeliveryOrder(any())).thenReturn(expectedResponse);

        // when
        var result = mockMvc.perform(post("/api/v1/orders/place-delivery-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        // then
        result.andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(expectedId.toString()));

        ArgumentCaptor<OrderPayloads.PlaceDeliveryOrderRequest> captor = ArgumentCaptor.forClass(OrderPayloads.PlaceDeliveryOrderRequest.class);
        verify(orderService).placeDeliveryOrder(captor.capture());
        var captured = captor.getValue();
        assertThat(captured.orderLines()).hasSize(1);
        assertThat(captured.address().street()).isEqualTo("Main St");
        verifyNoMoreInteractions(orderService);
    }

    @Test
    @DisplayName("Given existing order_When POST /api/v1/orders/{id}/approve_Then 204 No Content")
    void GivenExistingOrder_WhenPostApprove_Then204() throws Exception {
        // given
        var id = UUID.randomUUID().toString();
        var req = new OrderPayloads.ApproveOrderRequest(15);

        // when
        var result = mockMvc.perform(post("/api/v1/orders/" + id + "/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        // then
        result.andExpect(status().isNoContent());
        ArgumentCaptor<OrderPayloads.ApproveOrderRequest> captor = ArgumentCaptor.forClass(OrderPayloads.ApproveOrderRequest.class);
        verify(orderService).approve(eq(id), captor.capture());
        assertThat(captor.getValue().estimatedPreparationMinutes()).isEqualTo(15);
    }

    @Test
    @DisplayName("Given existing order_When POST /api/v1/orders/{id}/ready_Then 204 No Content")
    void GivenExistingOrder_WhenPostMarkAsReady_Then204() throws Exception {
        // given
        var id = UUID.randomUUID().toString();

        // when
        var result = mockMvc.perform(post("/api/v1/orders/" + id + "/ready"));

        // then
        result.andExpect(status().isNoContent());
        verify(orderService).markAsReady(id);
    }

    @Test
    @DisplayName("Given existing order_When POST /api/v1/orders/{id}/start-delivery_Then 204 No Content")
    void GivenExistingOrder_WhenPostStartDelivery_Then204() throws Exception {
        // given
        var id = UUID.randomUUID().toString();

        // when
        var result = mockMvc.perform(post("/api/v1/orders/" + id + "/start-delivery"));

        // then
        result.andExpect(status().isNoContent());
        verify(orderService).startDelivery(id);
    }

    @Test
    @DisplayName("Given existing order_When POST /api/v1/orders/{id}/complete_Then 204 No Content")
    void GivenExistingOrder_WhenPostCompleteOrder_Then204() throws Exception {
        // given
        var id = UUID.randomUUID().toString();

        // when
        var result = mockMvc.perform(post("/api/v1/orders/" + id + "/complete"));

        // then
        result.andExpect(status().isNoContent());
        verify(orderService).completeOrder(eq(id), any());
    }

    @Test
    @DisplayName("Given existing order_When POST /api/v1/orders/{id}/cancel_Then 204 No Content")
    void GivenExistingOrder_WhenPostCancelOrder_Then204() throws Exception {
        // given
        var id = UUID.randomUUID().toString();
        var req = new OrderPayloads.CancelOrderRequest("Customer changed mind");

        // when
        var result = mockMvc.perform(post("/api/v1/orders/" + id + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        // then
        result.andExpect(status().isNoContent());
        ArgumentCaptor<OrderPayloads.CancelOrderRequest> captor = ArgumentCaptor.forClass(OrderPayloads.CancelOrderRequest.class);
        verify(orderService).cancelOrder(eq(id), captor.capture());
        assertThat(captor.getValue().reason()).contains("changed mind");
    }

    @Test
    @DisplayName("Given existing order_When POST /api/v1/orders/{id}/change-lines_Then 204 No Content")
    void GivenExistingOrder_WhenPostChangeOrderLines_Then204() throws Exception {
        // given
        var id = UUID.randomUUID().toString();
        var addLine = new OrderPayloads.OrderLineRequest(UUID.randomUUID(), 1);
        var removeLine = new OrderPayloads.RemoveLine(UUID.randomUUID(), 1);
        var req = new OrderPayloads.ChangeOrderLinesRequest(List.of(addLine), List.of(removeLine), 5);

        // when
        var result = mockMvc.perform(post("/api/v1/orders/" + id + "/change-lines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        // then
        result.andExpect(status().isNoContent());
        ArgumentCaptor<OrderPayloads.ChangeOrderLinesRequest> captor = ArgumentCaptor.forClass(OrderPayloads.ChangeOrderLinesRequest.class);
        verify(orderService).changeOrderLines(eq(id), captor.capture());
        assertThat(captor.getValue().updatedEstimatedPreparationTimeMinutes()).isEqualTo(5);
    }

    @Test
    @DisplayName("Given existing order_When GET /api/v1/orders/{id}_Then 200 and details body")
    void GivenExistingOrder_WhenGetOrderDetails_Then200AndBody() throws Exception {
        // given
        var id = UUID.randomUUID().toString();
        var customerInfo = new CustomerInfo("Anna","Lee","123");
        var address = new Address("St","2",null,"City","00-000");
        var lineResp = new OrderPayloads.OrderLineResponse(UUID.randomUUID(), 1, Money.zero(), "item");
        var details = new OrderPayloads.OrderDetailsResponse(
                UUID.fromString(id),
                "APPROVED",
                customerInfo,
                address,
                ASAP,
                LocalTime.of(12,0),
                List.of(lineResp),
                10,
                LocalDateTime.now().minusMinutes(5),
                null,
                Instant.now()
        );

        when(orderService.getOrderDetails(eq(id))).thenReturn(details);

        // when
        var result = mockMvc.perform(get("/api/v1/orders/" + id));

        // then
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id));

        verify(orderService).getOrderDetails(eq(id));
        verifyNoMoreInteractions(orderService);
    }

    @Test
    @DisplayName("Given missing order_When GET /api/v1/orders/{id}_Then 404")
    void GivenMissingOrder_WhenGetOrderDetails_Then404() throws Exception {
        // given
        var id = UUID.randomUUID().toString();
        when(orderService.getOrderDetails(eq(id))).thenThrow(new ResourceNotFoundException("Order not found"));

        // when
        var result = mockMvc.perform(get("/api/v1/orders/" + id));

        // then
        result.andExpect(status().isNotFound());
        verify(orderService).getOrderDetails(eq(id));
    }

}