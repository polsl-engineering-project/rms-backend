package com.polsl.engineering.project.rms.bill;

import com.polsl.engineering.project.rms.bill.cmd.AddItemsToBillCommand;
import com.polsl.engineering.project.rms.bill.cmd.OpenBillCommand;
import com.polsl.engineering.project.rms.bill.cmd.RemoveItemsFromBillCommand;
import com.polsl.engineering.project.rms.bill.exception.InvalidBillActionException;
import com.polsl.engineering.project.rms.bill.exception.MenuItemNotFoundException;
import com.polsl.engineering.project.rms.bill.exception.MenuItemVersionMismatchException;
import com.polsl.engineering.project.rms.bill.vo.*;
import com.polsl.engineering.project.rms.common.exception.ResourceNotFoundException;
import com.polsl.engineering.project.rms.common.result.Result;
import com.polsl.engineering.project.rms.menu.MenuApi;
import jakarta.transaction.Transactional;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
class BillService {

    private final BillRepository billRepository;
    private final MenuApi menuApi;
    private final BillMapper mapper;
    private final Clock clock;

    @Transactional
    BillPayloads.BillOpenedResponse openBill(BillPayloads.OpenBillRequest request) {
        var billLines = getBillLines(request.initialLines());
        var cmd = mapper.toCommand(request, billLines);
        var result = Bill.open(cmd, clock);
        validateActionResult(result);

        var bill = result.getValue();
        billRepository.saveNewBill(bill);

        return mapper.toResponse(bill);
    }

    @Transactional
    void addItems(String billId, BillPayloads.AddItemsToBillRequest request) {
        var bill = findByIdOrThrow(billId);
        var billLines = getBillLines(request.newLines());
        var cmd = new AddItemsToBillCommand(billLines);
        var result = bill.addItems(cmd, clock);
        validateActionResult(result);
        billRepository.updateWithLines(bill);
    }

    @Transactional
    void removeItems(String billId, BillPayloads.RemoveItemsFromBillRequest request) {
        var bill = findByIdOrThrow(billId);

        var removedLinesDomain = new ArrayList<BillLineRemoval>();
        for (var rl : request.removedLines()) {
            removedLinesDomain.add(new BillLineRemoval(rl.menuItemId().toString(), rl.quantity()));
        }

        var cmd = new RemoveItemsFromBillCommand(removedLinesDomain);
        var result = bill.removeItems(cmd, clock);
        validateActionResult(result);
        billRepository.updateWithLines(bill);
    }

    @Transactional
    void closeBill(String billId) {
        var bill = findByIdOrThrow(billId);
        var result = bill.close(clock);
        validateActionResult(result);
        billRepository.updateWithoutLines(bill);
    }

    BillPayloads.BillPageResponse searchBills(BillPayloads.BillSearchRequest request) {
        var criteria = request.toCriteria();
        var page = billRepository.searchBills(criteria, request.page(), request.size());
        return BillPayloads.BillPageResponse.from(page);
    }

    private Bill findByIdOrThrow(String id) {
        return billRepository.findById(BillId.from(id))
                .orElseThrow(() -> new ResourceNotFoundException("Bill with id %s not found".formatted(id)));
    }

    private static <T> void validateActionResult(Result<T> result) {
        if (result.isFailure()) {
            throw new InvalidBillActionException(result.getError());
        }
    }

    private List<BillLine> getBillLines(List<BillPayloads.BillLine> linesFromRequest) {
        var billLines = new ArrayList<BillLine>();

        var ids = linesFromRequest.stream()
                .map(BillPayloads.BillLine::menuItemId)
                .toList();

        var snapshotsMap = menuApi.getSnapshotsForOrderByIds(ids);

        for (var lineFromRequest : linesFromRequest) {
            var snapshot = snapshotsMap.get(lineFromRequest.menuItemId());
            if (snapshot == null) {
                throw new MenuItemNotFoundException(lineFromRequest);
            }
            if (snapshot.version() != lineFromRequest.version()) {
                throw new MenuItemVersionMismatchException(snapshot.version(), lineFromRequest);
            }

            var billLine = new BillLine(
                    lineFromRequest.menuItemId().toString(),
                    lineFromRequest.quantity(),
                    new Money(snapshot.price()),
                    snapshot.version()
            );
            billLines.add(billLine);
        }

        return billLines;
    }
}