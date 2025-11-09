package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.common.exception.ResourceNotFoundException;
import com.polsl.engineering.project.rms.common.result.Result;
import com.polsl.engineering.project.rms.menu.MenuApi;
import com.polsl.engineering.project.rms.order.exception.InvalidOrderActionException;
import com.polsl.engineering.project.rms.order.exception.MenuItemNotFoundException;
import com.polsl.engineering.project.rms.order.exception.MenuItemVersionMismatchException;
import com.polsl.engineering.project.rms.order.vo.Money;
import com.polsl.engineering.project.rms.order.vo.OrderId;
import com.polsl.engineering.project.rms.order.vo.OrderLine;
import com.polsl.engineering.project.rms.order.vo.OrderLineRemoval;
import com.polsl.engineering.project.rms.order.cmd.ChangeOrderLinesCommand;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
class OrderService {

    private final OrderRepository jdbcRepository;
    private final MenuApi menuApi;
    private final OrderMapper mapper;
    private final Clock clock;
    private final OrderOutboxService outboxService;

    @Transactional
    OrderPayloads.OrderPlacedResponse placePickUpOrder(OrderPayloads.PlacePickUpOrderRequest request) {
        var orderLines = getOrderLines(request.orderLines());
        var cmd = mapper.toCommand(request, orderLines);
        var result = Order.placePickUpOrder(cmd, clock);
        validateActionResult(result);

        var order = result.getValue();
        jdbcRepository.saveNewOrder(order);

        saveEvents(order);

        return mapper.toResponse(order);
    }
    
    @Transactional
    OrderPayloads.OrderPlacedResponse placeDeliveryOrder(OrderPayloads.PlaceDeliveryOrderRequest request) {
        var orderLines = getOrderLines(request.orderLines());
        var cmd = mapper.toCommand(request, orderLines);
        var result = Order.placeDeliveryOrder(cmd, clock);
        validateActionResult(result);

        var order = result.getValue();
        jdbcRepository.saveNewOrder(order);

        saveEvents(order);

        return mapper.toResponse(order);
    }

    @Transactional
    void approveByFrontDesk(String orderId) {
        var order = findByIdOrThrow(orderId);
        var result = order.approveByFrontDesk(clock);
        validateActionResult(result);
        jdbcRepository.updateWithoutLines(order);
        saveEvents(order);
    }

    @Transactional
    void approveByKitchen(String orderId, OrderPayloads.ApproveOrderByKitchenRequest request) {
        var order = findByIdOrThrow(orderId);
        var cmd = mapper.toCommand(request);
        var result = order.approveByKitchen(cmd, clock);
        validateActionResult(result);
        jdbcRepository.updateWithoutLines(order);
        saveEvents(order);
    }

    @Transactional
    void markAsReady(String orderId) {
        var order = findByIdOrThrow(orderId);
        var result = order.markAsReady(clock);
        validateActionResult(result);
        jdbcRepository.updateWithoutLines(order);
        saveEvents(order);
    }

    @Transactional
    void startDelivery(String orderId) {
        var order = findByIdOrThrow(orderId);
        var result = order.startDelivery(clock);
        validateActionResult(result);
        jdbcRepository.updateWithoutLines(order);
        saveEvents(order);
    }

    @Transactional
    void completeOrder(String orderId) {
        var order = findByIdOrThrow(orderId);
        var result = order.complete(clock);
        validateActionResult(result);
        jdbcRepository.updateWithoutLines(order);
        saveEvents(order);
    }

    @Transactional
    void cancelOrder(String orderId, OrderPayloads.CancelOrderRequest request) {
        var order = findByIdOrThrow(orderId);
        var cmd = mapper.toCommand(request);
        var result = order.cancel(cmd, clock);
        validateActionResult(result);
        jdbcRepository.updateWithoutLines(order);
        saveEvents(order);
    }

    @Transactional
    void changeOrderLines(String orderId, OrderPayloads.ChangeOrderLinesRequest request) {
        var order = findByIdOrThrow(orderId);

        List<OrderLine> newLinesDomain = List.of();
        if (request.newLines() != null && !request.newLines().isEmpty()) {
            newLinesDomain = getOrderLines(request.newLines());
        }

        List<OrderLineRemoval> removedLinesDomain = List.of();
        if (request.removedLines() != null && !request.removedLines().isEmpty()) {
            var tmp = new ArrayList<OrderLineRemoval>();
            for (var rl : request.removedLines()) {
                tmp.add(new OrderLineRemoval(rl.menuItemId().toString(), rl.quantity()));
            }
            removedLinesDomain = tmp;
        }

        int updatedMinutes = request.updatedEstimatedPreparationTimeMinutes() != null ? request.updatedEstimatedPreparationTimeMinutes() : 0;

        var cmd = new ChangeOrderLinesCommand(newLinesDomain, removedLinesDomain, updatedMinutes);
        var result = order.changeOrderLines(cmd, clock);
        validateActionResult(result);

        jdbcRepository.updateWithLines(order);
        saveEvents(order);
    }

    private Order findByIdOrThrow(String id) {
        return jdbcRepository.findById(OrderId.from(id))
                .orElseThrow(() -> new ResourceNotFoundException("Order with id %s not found".formatted(id)));
    }

    private static <T> void validateActionResult(Result<T> result) {
        if (result.isFailure()) {
            throw new InvalidOrderActionException(result.getError());
        }
    }

    private List<OrderLine> getOrderLines(List<OrderPayloads.OrderLine> linesFromRequest) {
        var orderLines = new ArrayList<OrderLine>();

        var ids = linesFromRequest.stream()
                .map(OrderPayloads.OrderLine::menuItemId)
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

            var orderLine = new OrderLine(
                    lineFromRequest.menuItemId().toString(),
                    lineFromRequest.quantity(),
                    new Money(snapshot.price()),
                    snapshot.version()
            );
            orderLines.add(orderLine);
        }

        return orderLines;
    }

    private void saveEvents(Order order) {
        var events = order.pullEvents();
        for (var event : events) {
            outboxService.persistEvent(order.getId(), event);
        }
    }

}
