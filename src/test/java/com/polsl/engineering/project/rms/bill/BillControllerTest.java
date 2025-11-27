package com.polsl.engineering.project.rms.bill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polsl.engineering.project.rms.bill.vo.BillStatus;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BillController.class)
@AutoConfigureMockMvc(addFilters = false)
class BillControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    BillService billService;

    //context
    @MockitoBean
    JwtService jwtService;

    @Test
    @DisplayName("Given valid search request, When POST /api/v1/bills, Then 200 and response")
    void GivenValidSearchRequest_WhenPostSearchBills_Then200AndResponse() throws Exception {
        // given
        var billSummary = new BillPayloads.BillSummaryResponse(
                UUID.randomUUID(), 5, BillStatus.OPEN, UUID.randomUUID().toString(), new BigDecimal("50.00"),
                2, null, null, null
        );
        var expectedResponse = new BillPayloads.BillPageResponse(
                List.of(billSummary), 0, 20, 1, 1, true, true, false, false
        );

        when(billService.searchBills(any())).thenReturn(expectedResponse);

        // when
        var result = mockMvc.perform(get("/api/v1/bills")
                        .queryParam("statuses", "OPEN")
                        .queryParam("tableNumbers", "5")
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].tableNumber").value(5));

        verify(billService).searchBills(any(BillPayloads.BillSearchRequest.class));
    }

    @Test
    @DisplayName("Given existing bill id, When GET /api/v1/bills/{id}, Then 200 and bill details")
    void GivenExistingBillId_WhenGetBillById_Then200AndBillDetails() throws Exception {
        // given
        var billId = UUID.randomUUID();
        var billLineResponse = new BillPayloads.BillLineResponse(UUID.randomUUID(), 2, "Pizza", new BigDecimal("60.00"));
        var expectedResponse = new BillPayloads.BillSummaryWithLinesResponse(
                billId, 7, BillStatus.OPEN,UUID.randomUUID().toString(), new BigDecimal("60.00"),
                List.of(billLineResponse), null, null, null
        );

        when(billService.searchBill(billId.toString())).thenReturn(expectedResponse);

        // when
        var result = mockMvc.perform(get("/api/v1/bills/" + billId));

        // then
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(billId.toString()))
                .andExpect(jsonPath("$.tableNumber").value(7))
                .andExpect(jsonPath("$.billLines[0].quantity").value(2))
                .andExpect(jsonPath("$.billLines[0].unitPrice").value(60.00));

        verify(billService).searchBill(billId.toString());
    }

    @Test
    @DisplayName("Given valid open bill request, When POST /api/v1/bills/open, Then 201 and response")
    void GivenValidOpenBillRequest_WhenPostOpenBill_Then201AndResponse() throws Exception {
        // given
        var menuItemId = UUID.randomUUID();
        var billLine = new BillPayloads.BillLine(menuItemId, 2);
        var waiterInfo = UUID.randomUUID().toString();
        var request = new BillPayloads.OpenBillRequest(
                10,
                waiterInfo,
                List.of(billLine)
        );
        var expectedId = UUID.randomUUID();
        var expectedResponse = new BillPayloads.BillOpenedResponse(expectedId, 10);

        when(billService.openBill(any())).thenReturn(expectedResponse);

        // when
        var result = mockMvc.perform(post("/api/v1/bills/open")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(expectedId.toString()))
                .andExpect(jsonPath("$.tableNumber").value(10));

        ArgumentCaptor<BillPayloads.OpenBillRequest> captor = ArgumentCaptor.forClass(BillPayloads.OpenBillRequest.class);
        verify(billService).openBill(captor.capture());
        var captured = captor.getValue();
        assertThat(captured.tableNumber()).isEqualTo(10);
        assertThat(captured.userId()).isEqualTo(waiterInfo);
        assertThat(captured.initialLines()).hasSize(1);
        verifyNoMoreInteractions(billService);
    }

    @Test
    @DisplayName("Given invalid open bill request - null table number, When POST /api/v1/bills/open, Then 400")
    void GivenInvalidOpenBillRequest_NullTableNumber_WhenPostOpenBill_Then400() throws Exception {
        // given
        var menuItemId = UUID.randomUUID();
        var billLine = new BillPayloads.BillLine(menuItemId, 1);
        var waiterInfo = UUID.randomUUID().toString();
        var request = new BillPayloads.OpenBillRequest(
                null, // invalid
                waiterInfo,
                List.of(billLine)
        );

        // when
        var result = mockMvc.perform(post("/api/v1/bills/open")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
        verifyNoInteractions(billService);
    }

    @Test
    @DisplayName("Given invalid open bill request - empty lines, When POST /api/v1/bills/open, Then 400")
    void GivenInvalidOpenBillRequest_EmptyLines_WhenPostOpenBill_Then400() throws Exception {
        // given
        var request = new BillPayloads.OpenBillRequest(
                5,
                UUID.randomUUID().toString(),
                List.of() // invalid
        );

        // when
        var result = mockMvc.perform(post("/api/v1/bills/open")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
        verifyNoInteractions(billService);
    }

    @Test
    @DisplayName("Given valid add items request, When POST /api/v1/bills/{id}/add-items, Then 204")
    void GivenValidAddItemsRequest_WhenPostAddItems_Then204() throws Exception {
        // given
        var billId = UUID.randomUUID().toString();
        var menuItemId = UUID.randomUUID();
        var billLine = new BillPayloads.BillLine(menuItemId, 3);
        var request = new BillPayloads.AddItemsToBillRequest(List.of(billLine));

        // when
        var result = mockMvc.perform(post("/api/v1/bills/" + billId + "/add-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isNoContent());

        ArgumentCaptor<BillPayloads.AddItemsToBillRequest> captor = ArgumentCaptor.forClass(BillPayloads.AddItemsToBillRequest.class);
        verify(billService).addItems(eq(billId), captor.capture());
        assertThat(captor.getValue().newLines()).hasSize(1);
        assertThat(captor.getValue().newLines().get(0).quantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("Given invalid add items request - empty lines, When POST /api/v1/bills/{id}/add-items, Then 400")
    void GivenInvalidAddItemsRequest_EmptyLines_WhenPostAddItems_Then400() throws Exception {
        // given
        var billId = UUID.randomUUID().toString();
        var request = new BillPayloads.AddItemsToBillRequest(List.of());

        // when
        var result = mockMvc.perform(post("/api/v1/bills/" + billId + "/add-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
        verifyNoInteractions(billService);
    }

    @Test
    @DisplayName("Given valid remove items request, When POST /api/v1/bills/{id}/remove-items, Then 204")
    void GivenValidRemoveItemsRequest_WhenPostRemoveItems_Then204() throws Exception {
        // given
        var billId = UUID.randomUUID().toString();
        var menuItemId = UUID.randomUUID();
        var removeLine = new BillPayloads.RemoveLine(menuItemId, 2);
        var request = new BillPayloads.RemoveItemsFromBillRequest(List.of(removeLine));

        // when
        var result = mockMvc.perform(post("/api/v1/bills/" + billId + "/remove-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isNoContent());

        ArgumentCaptor<BillPayloads.RemoveItemsFromBillRequest> captor = ArgumentCaptor.forClass(BillPayloads.RemoveItemsFromBillRequest.class);
        verify(billService).removeItems(eq(billId), captor.capture());
        assertThat(captor.getValue().removedLines()).hasSize(1);
        assertThat(captor.getValue().removedLines().get(0).quantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("Given invalid remove items request - empty lines, When POST /api/v1/bills/{id}/remove-items, Then 400")
    void GivenInvalidRemoveItemsRequest_EmptyLines_WhenPostRemoveItems_Then400() throws Exception {
        // given
        var billId = UUID.randomUUID().toString();
        var request = new BillPayloads.RemoveItemsFromBillRequest(List.of());

        // when
        var result = mockMvc.perform(post("/api/v1/bills/" + billId + "/remove-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
        verifyNoInteractions(billService);
    }

    @Test
    @DisplayName("Given existing bill, When POST /api/v1/bills/{id}/close, Then 204")
    void GivenExistingBill_WhenPostCloseBill_Then204() throws Exception {
        // given
        var billId = UUID.randomUUID().toString();

        // when
        var result = mockMvc.perform(post("/api/v1/bills/" + billId + "/close"));

        // then
        result.andExpect(status().isNoContent());
        verify(billService).closeBill(billId);
    }
}