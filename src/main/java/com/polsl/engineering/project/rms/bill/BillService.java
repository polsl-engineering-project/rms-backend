package com.polsl.engineering.project.rms.bill;

import com.polsl.engineering.project.rms.bill.cmd.AddItemsToBillCommand;
import com.polsl.engineering.project.rms.bill.cmd.RemoveItemsFromBillCommand;
import com.polsl.engineering.project.rms.bill.exception.InvalidBillActionException;
import com.polsl.engineering.project.rms.bill.exception.MenuItemNotFoundException;
import com.polsl.engineering.project.rms.bill.vo.*;
import com.polsl.engineering.project.rms.general.exception.ResourceNotFoundException;
import com.polsl.engineering.project.rms.general.result.Result;
import com.polsl.engineering.project.rms.menu.MenuApi;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class BillService {

    private static final Integer DEFAULT_PAGE_SIZE = 20;
    private static final Integer DEFAULT_PAGE_NUMBER = 0;

    private final BillRepository billRepository;
    private final MenuApi menuApi;
    private final BillMapper mapper;
    private final Clock clock;
    private final BillOutboxService billOutboxService;

    @Transactional
    BillPayloads.BillOpenedResponse openBill(BillPayloads.OpenBillRequest request) {
        validateTableNumer(request.tableNumber());

        var billLines = getBillLines(request.initialLines());
        var cmd = mapper.toCommand(request, billLines);
        var result = Bill.open(cmd, clock);
        validateActionResult(result);

        var bill = result.getValue();
        billRepository.saveNewBill(bill);

        saveEvents(bill);
        return mapper.toResponse(bill);
    }

    @Transactional
    void addItems(String billId, BillPayloads.AddItemsToBillRequest request) {
        var bill = findByIdOrThrow(billId);
        var billLines = getBillLines(request.newLines());
        var cmd = new AddItemsToBillCommand(billLines);
        var result = bill.addItems(cmd, clock);
        validateActionResult(result);
        saveEvents(bill);
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
        saveEvents(bill);
        billRepository.updateWithLines(bill);
    }

    @Transactional
    void closeBill(String billId) {
        var bill = findByIdOrThrow(billId);
        var result = bill.close(clock);
        validateActionResult(result);
        saveEvents(bill);
        billRepository.updateWithoutLines(bill);
    }

    List<BillPayloads.BillSummaryWithLinesResponse> getOpenBills(){
        return billRepository.findOpenBills().stream().map(bill -> {
            var lines = bill.getLines().stream()
                    .map(line -> {
                        var menuItemId = UUID.fromString(line.menuItemId());
                        return new BillPayloads.BillLineResponse(
                                menuItemId,
                                line.quantity(),
                                line.menuItemName(),
                                line.unitPrice().amount()
                        );
                    })
                    .toList();

            return mapper.toSummaryWithLinesResponse(bill, lines);
        }).toList();
    }


    BillPayloads.BillPageResponse searchBills(BillPayloads.BillSearchRequest request) {
        var page = validatePage(request.page());
        var size = validateSize(request.size());
        return billRepository.searchBills(request, page, size);
    }

    BillPayloads.BillSummaryWithLinesResponse searchBill(String id){
        var bill =  findByIdOrThrow(id);

        var billLines = bill.getLines().stream()
                .map(line -> {
                    var menuItemId = UUID.fromString(line.menuItemId());
                    return new BillPayloads.BillLineResponse(
                            menuItemId,
                            line.quantity(),
                            line.menuItemName(),
                            line.unitPrice().amount()
                    );
                })
                .toList();

        return mapper.toSummaryWithLinesResponse(bill, billLines);
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

            var billLine = new BillLine(
                    lineFromRequest.menuItemId().toString(),
                    lineFromRequest.quantity(),
                    new Money(snapshot.price()),
                    snapshot.name()
            );
            billLines.add(billLine);
        }

        return billLines;
    }

    private int validatePage(Integer pageNumber) {
        return (pageNumber == null || pageNumber < 0)
                ? DEFAULT_PAGE_NUMBER
                : pageNumber;
    }

    private int validateSize(Integer size) {
        return (size == null || size < 1)
                ? DEFAULT_PAGE_SIZE
                : size;
    }

    private void validateTableNumer(Integer tableNumber){
        if(billRepository.openBillExistsForTable(tableNumber)){
            throw new InvalidBillActionException(
                    "Table %d already has an open bill.".formatted(tableNumber)
            );
        }
    }

    private void saveEvents(Bill bill) {
        var events = bill.pullEvents();
        for (var event : events) {
            billOutboxService.persistEvent(bill.getId(), event);
        }
    }


}