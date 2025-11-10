package com.polsl.engineering.project.rms.bill;

import com.polsl.engineering.project.rms.bill.cmd.OpenBillCommand;
import com.polsl.engineering.project.rms.bill.exception.InvalidBillActionException;
import com.polsl.engineering.project.rms.bill.exception.MenuItemNotFoundException;
import com.polsl.engineering.project.rms.bill.exception.MenuItemVersionMismatchException;
import com.polsl.engineering.project.rms.bill.vo.*;
import com.polsl.engineering.project.rms.common.exception.ResourceNotFoundException;
import com.polsl.engineering.project.rms.menu.MenuApi;
import com.polsl.engineering.project.rms.menu.dto.MenuItemSnapshotForOrder;
import com.polsl.engineering.project.rms.order.vo.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillServiceTest {

    @Mock
    BillRepository billRepository;

    @Mock
    MenuApi menuApi;

    @Mock
    BillMapper mapper;

    @Mock
    Clock clock;

    @InjectMocks
    BillService underTest;

    @Test
    @DisplayName("Given valid open bill request, When openBill, Then saves bill and returns response")
    void GivenValidOpenBillRequest_WhenOpenBill_ThenSavesBillAndReturnsResponse() {
        // given
        var payloadLineId = UUID.randomUUID();
        var payloadLine = new BillPayloads.BillLine(payloadLineId, 2, 1L);
        var waiterInfo = new WaiterInfo("John", "Doe", UUID.randomUUID().toString());
        var request = new BillPayloads.OpenBillRequest(5, waiterInfo, List.of(payloadLine));

        var snapshot = new MenuItemSnapshotForOrder(payloadLineId, new BigDecimal("30.00"), "Pizza", 1L);
        when(menuApi.getSnapshotsForOrderByIds(anyList())).thenReturn(Map.of(payloadLineId, snapshot));

        when(mapper.toCommand(any(BillPayloads.OpenBillRequest.class), anyList()))
                .thenAnswer(invocation -> new OpenBillCommand(
                        TableNumber.of(invocation.getArgument(0, BillPayloads.OpenBillRequest.class).tableNumber()),
                        invocation.getArgument(0, BillPayloads.OpenBillRequest.class).waiterInfo(),
                        invocation.getArgument(1, List.class)
                ));

        var expectedUuid = UUID.randomUUID();
        when(mapper.toResponse(any(Bill.class))).thenReturn(new BillPayloads.BillOpenedResponse(expectedUuid, 5));

        when(billRepository.openBillExistsForTable(5)).thenReturn(false);

        // when
        var response = underTest.openBill(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(expectedUuid);
        assertThat(response.tableNumber()).isEqualTo(5);
        verify(billRepository).saveNewBill(any(Bill.class));
        verify(mapper).toResponse(any(Bill.class));
    }

    @Test
    @DisplayName("Given table with open bill, When openBill, Then throws InvalidBillActionException")
    void GivenTableWithOpenBill_WhenOpenBill_ThenThrowsInvalidBillActionException() {
        // given
        var payloadLineId = UUID.randomUUID();
        var payloadLine = new BillPayloads.BillLine(payloadLineId, 1, 0L);
        var waiterInfo = new WaiterInfo("Jane", "Smith", UUID.randomUUID().toString());
        var request = new BillPayloads.OpenBillRequest(7, waiterInfo, List.of(payloadLine));

        when(billRepository.openBillExistsForTable(7)).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> underTest.openBill(request))
                .isInstanceOf(InvalidBillActionException.class)
                .hasMessageContaining("Table 7 already has an open bill");

        verify(billRepository, never()).saveNewBill(any());
    }

    @Test
    @DisplayName("Given missing menu item, When openBill, Then throws MenuItemNotFoundException")
    void GivenMissingMenuItem_WhenOpenBill_ThenThrowsMenuItemNotFoundException() {
        // given
        var payloadLineId = UUID.randomUUID();
        var payloadLine = new BillPayloads.BillLine(payloadLineId, 1, 0L);
        var waiterInfo = new WaiterInfo("Alice", "Johnson", UUID.randomUUID().toString());
        var request = new BillPayloads.OpenBillRequest(10, waiterInfo, List.of(payloadLine));

        when(billRepository.openBillExistsForTable(10)).thenReturn(false);
        when(menuApi.getSnapshotsForOrderByIds(anyList())).thenReturn(Map.of());

        // when / then
        assertThatThrownBy(() -> underTest.openBill(request))
                .isInstanceOf(MenuItemNotFoundException.class)
                .hasMessageContaining(payloadLineId.toString());

        verify(billRepository, never()).saveNewBill(any());
    }

    @Test
    @DisplayName("Given version mismatch, When openBill, Then throws MenuItemVersionMismatchException")
    void GivenVersionMismatch_WhenOpenBill_ThenThrowsMenuItemVersionMismatchException() {
        // given
        var payloadLineId = UUID.randomUUID();
        var payloadLine = new BillPayloads.BillLine(payloadLineId, 1, 5L); // request says version 5
        var waiterInfo = new WaiterInfo("Bob", "Brown", UUID.randomUUID().toString());
        var request = new BillPayloads.OpenBillRequest(12, waiterInfo, List.of(payloadLine));

        when(billRepository.openBillExistsForTable(12)).thenReturn(false);
        var snapshot = new MenuItemSnapshotForOrder(payloadLineId, new BigDecimal("20.00"), "Item", 1L);
        when(menuApi.getSnapshotsForOrderByIds(anyList())).thenReturn(Map.of(payloadLineId, snapshot));

        // when / then
        assertThatThrownBy(() -> underTest.openBill(request))
                .isInstanceOf(MenuItemVersionMismatchException.class);

        verify(billRepository, never()).saveNewBill(any());
    }

    @Test
    @DisplayName("Given existing bill, When addItems, Then updates repository with lines")
    void GivenExistingBill_WhenAddItems_ThenUpdatesRepositoryWithLines() {
        // given
        var billId = UUID.randomUUID().toString();
        var billMock = mock(Bill.class);
        when(billRepository.findById(any())).thenReturn(Optional.of(billMock));
        when(billMock.addItems(any(), any(Clock.class))).thenReturn(com.polsl.engineering.project.rms.common.result.Result.ok(null));

        var menuItemId = UUID.randomUUID();
        var payloadLine = new BillPayloads.BillLine(menuItemId, 2, 1L);
        var request = new BillPayloads.AddItemsToBillRequest(List.of(payloadLine));

        var snapshot = new MenuItemSnapshotForOrder(menuItemId, new BigDecimal("15.00"), "NewItem", 1L);
        when(menuApi.getSnapshotsForOrderByIds(anyList())).thenReturn(Map.of(menuItemId, snapshot));

        // when
        underTest.addItems(billId, request);

        // then
        verify(billRepository).updateWithLines(billMock);
        verify(billMock).addItems(any(), any(Clock.class));
    }

    @Test
    @DisplayName("Given bill not found, When addItems, Then throws ResourceNotFoundException")
    void GivenBillNotFound_WhenAddItems_ThenThrowsResourceNotFoundException() {
        // given
        var billId = UUID.randomUUID().toString();
        when(billRepository.findById(any())).thenReturn(Optional.empty());

        var menuItemId = UUID.randomUUID();
        var payloadLine = new BillPayloads.BillLine(menuItemId, 1, 0L);
        var request = new BillPayloads.AddItemsToBillRequest(List.of(payloadLine));

        // when / then
        assertThatThrownBy(() -> underTest.addItems(billId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(billId);

        verify(billRepository, never()).updateWithLines(any());
    }

    @Test
    @DisplayName("Given failing aggregate, When addItems, Then throws InvalidBillActionException")
    void GivenFailingAggregate_WhenAddItems_ThenThrowsInvalidBillActionException() {
        // given
        var billId = UUID.randomUUID().toString();
        var billMock = mock(Bill.class);
        when(billRepository.findById(any())).thenReturn(Optional.of(billMock));
        when(billMock.addItems(any(), any(Clock.class))).thenReturn(com.polsl.engineering.project.rms.common.result.Result.failure("cannot add"));

        var menuItemId = UUID.randomUUID();
        var payloadLine = new BillPayloads.BillLine(menuItemId, 1, 0L);
        var request = new BillPayloads.AddItemsToBillRequest(List.of(payloadLine));

        var snapshot = new MenuItemSnapshotForOrder(menuItemId, new BigDecimal("10.00"), "Item", 0L);
        when(menuApi.getSnapshotsForOrderByIds(anyList())).thenReturn(Map.of(menuItemId, snapshot));

        // when / then
        assertThatThrownBy(() -> underTest.addItems(billId, request))
                .isInstanceOf(InvalidBillActionException.class)
                .hasMessageContaining("cannot add");

        verify(billRepository, never()).updateWithLines(billMock);
    }

    @Test
    @DisplayName("Given existing bill, When removeItems, Then updates repository with lines")
    void GivenExistingBill_WhenRemoveItems_ThenUpdatesRepositoryWithLines() {
        // given
        var billId = UUID.randomUUID().toString();
        var billMock = mock(Bill.class);
        when(billRepository.findById(any())).thenReturn(Optional.of(billMock));
        when(billMock.removeItems(any(), any(Clock.class))).thenReturn(com.polsl.engineering.project.rms.common.result.Result.ok(null));

        var menuItemId = UUID.randomUUID();
        var removeLine = new BillPayloads.RemoveLine(menuItemId, 1);
        var request = new BillPayloads.RemoveItemsFromBillRequest(List.of(removeLine));

        // when
        underTest.removeItems(billId, request);

        // then
        verify(billRepository).updateWithLines(billMock);
        verify(billMock).removeItems(any(), any(Clock.class));
    }

    @Test
    @DisplayName("Given bill not found, When removeItems, Then throws ResourceNotFoundException")
    void GivenBillNotFound_WhenRemoveItems_ThenThrowsResourceNotFoundException() {
        // given
        var billId = UUID.randomUUID().toString();
        when(billRepository.findById(any())).thenReturn(Optional.empty());

        var menuItemId = UUID.randomUUID();
        var removeLine = new BillPayloads.RemoveLine(menuItemId, 1);
        var request = new BillPayloads.RemoveItemsFromBillRequest(List.of(removeLine));

        // when / then
        assertThatThrownBy(() -> underTest.removeItems(billId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(billId);

        verify(billRepository, never()).updateWithLines(any());
    }

    @Test
    @DisplayName("Given existing bill, When closeBill, Then updates repository without lines")
    void GivenExistingBill_WhenCloseBill_ThenUpdatesRepositoryWithoutLines() {
        // given
        var billId = UUID.randomUUID().toString();
        var billMock = mock(Bill.class);
        when(billRepository.findById(any())).thenReturn(Optional.of(billMock));
        when(billMock.close(any(Clock.class))).thenReturn(com.polsl.engineering.project.rms.common.result.Result.ok(null));

        // when
        underTest.closeBill(billId);

        // then
        verify(billRepository).updateWithoutLines(billMock);
        verify(billMock).close(any(Clock.class));
    }

    @Test
    @DisplayName("Given bill not found, When closeBill, Then throws ResourceNotFoundException")
    void GivenBillNotFound_WhenCloseBill_ThenThrowsResourceNotFoundException() {
        // given
        var billId = UUID.randomUUID().toString();
        when(billRepository.findById(any())).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> underTest.closeBill(billId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(billId);

        verify(billRepository, never()).updateWithoutLines(any());
    }

    @Test
    @DisplayName("Given failing aggregate, When closeBill, Then throws InvalidBillActionException")
    void GivenFailingAggregate_WhenCloseBill_ThenThrowsInvalidBillActionException() {
        // given
        var billId = UUID.randomUUID().toString();
        var billMock = mock(Bill.class);
        when(billRepository.findById(any())).thenReturn(Optional.of(billMock));
        when(billMock.close(any(Clock.class))).thenReturn(com.polsl.engineering.project.rms.common.result.Result.failure("cannot close"));

        // when / then
        assertThatThrownBy(() -> underTest.closeBill(billId))
                .isInstanceOf(InvalidBillActionException.class)
                .hasMessageContaining("cannot close");

        verify(billRepository, never()).updateWithoutLines(billMock);
    }

    @Test
    @DisplayName("Given existing bill, When payBill, Then updates repository without lines")
    void GivenExistingBill_WhenPayBill_ThenUpdatesRepositoryWithoutLines() {
        // given
        var billId = UUID.randomUUID().toString();
        var billMock = mock(Bill.class);
        when(billRepository.findById(any())).thenReturn(Optional.of(billMock));
        when(billMock.pay(any(), any(Clock.class))).thenReturn(com.polsl.engineering.project.rms.common.result.Result.ok(null));

        var request = new BillPayloads.PayBillRequest(PaymentMethod.CARD, 50.00);

        // when
        underTest.payBill(billId, request);

        // then
        verify(billRepository).updateWithoutLines(billMock);
        verify(billMock).pay(any(), any(Clock.class));
    }

    @Test
    @DisplayName("Given bill not found, When payBill, Then throws ResourceNotFoundException")
    void GivenBillNotFound_WhenPayBill_ThenThrowsResourceNotFoundException() {
        // given
        var billId = UUID.randomUUID().toString();
        when(billRepository.findById(any())).thenReturn(Optional.empty());

        var request = new BillPayloads.PayBillRequest(PaymentMethod.CASH, 100.00);

        // when / then
        assertThatThrownBy(() -> underTest.payBill(billId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(billId);

        verify(billRepository, never()).updateWithoutLines(any());
    }

    @Test
    @DisplayName("Given failing aggregate, When payBill, Then throws InvalidBillActionException")
    void GivenFailingAggregate_WhenPayBill_ThenThrowsInvalidBillActionException() {
        // given
        var billId = UUID.randomUUID().toString();
        var billMock = mock(Bill.class);
        when(billRepository.findById(any())).thenReturn(Optional.of(billMock));
        when(billMock.pay(any(), any(Clock.class))).thenReturn(com.polsl.engineering.project.rms.common.result.Result.failure("cannot pay"));

        var request = new BillPayloads.PayBillRequest(PaymentMethod.CARD, 75.00);

        // when / then
        assertThatThrownBy(() -> underTest.payBill(billId, request))
                .isInstanceOf(InvalidBillActionException.class)
                .hasMessageContaining("cannot pay");

        verify(billRepository, never()).updateWithoutLines(billMock);
    }

    @Test
    @DisplayName("Given valid search request, When searchBills, Then returns page response")
    void GivenValidSearchRequest_WhenSearchBills_ThenReturnsPageResponse() {
        // given
        var request = BillPayloads.BillSearchRequest.builder()
                .statuses(List.of(BillStatus.OPEN))
                .page(0)
                .size(20)
                .build();

        var billSummary = new BillPayloads.BillSummaryResponse(
                UUID.randomUUID(), 5, BillStatus.OPEN, null,
                "John Doe", UUID.randomUUID().toString(), new BigDecimal("50.00"), BigDecimal.ZERO,
                2, null, null, null, null
        );
        var expectedResponse = new BillPayloads.BillPageResponse(
                List.of(billSummary), 0, 20, 1, 1, true, true, false, false
        );

        when(billRepository.searchBills(any(), eq(0), eq(20))).thenReturn(expectedResponse);

        // when
        var response = underTest.searchBills(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).hasSize(1);
        verify(billRepository).searchBills(any(), eq(0), eq(20));
    }

    @Test
    @DisplayName("Given existing bill, When searchBill, Then returns bill with lines")
    void GivenExistingBill_WhenSearchBill_ThenReturnsBillWithLines() {
        // given
        var billId = UUID.randomUUID();
        var billMock = mock(Bill.class);
        when(billRepository.findById(any())).thenReturn(Optional.of(billMock));

        var billLine = new BillLine(UUID.randomUUID().toString(), 2, new Money(new BigDecimal("30.00")), "Pizza", 1L);
        when(billMock.getLines()).thenReturn(List.of(billLine));

        var expectedResponse = new BillPayloads.BillSummaryWithLinesResponse(
                billId, 5, BillStatus.OPEN, null,
                "John Doe", UUID.randomUUID().toString(), new BigDecimal("60.00"), BigDecimal.ZERO,
                List.of(), null, null, null, null
        );
        when(mapper.toSummaryWithLinesResponse(any(), anyList())).thenReturn(expectedResponse);

        // when
        var response = underTest.searchBill(billId.toString());

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(billId);
        verify(billRepository).findById(any());
        verify(mapper).toSummaryWithLinesResponse(any(), anyList());
    }

    @Test
    @DisplayName("Given bill not found, When searchBill, Then throws ResourceNotFoundException")
    void GivenBillNotFound_WhenSearchBill_ThenThrowsResourceNotFoundException() {
        // given
        var billId = UUID.randomUUID().toString();
        when(billRepository.findById(any())).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> underTest.searchBill(billId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(billId);
    }
}