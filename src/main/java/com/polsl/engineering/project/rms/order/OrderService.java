package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.general.exception.ResourceNotFoundException;
import com.polsl.engineering.project.rms.general.result.Result;
import com.polsl.engineering.project.rms.menu.MenuApi;
import com.polsl.engineering.project.rms.order.exception.InvalidOrderActionException;
import com.polsl.engineering.project.rms.order.exception.MenuItemNotFoundException;
import com.polsl.engineering.project.rms.order.exception.MenuItemVersionMismatchException;
import com.polsl.engineering.project.rms.order.vo.Money;
import com.polsl.engineering.project.rms.order.vo.OrderId;
import com.polsl.engineering.project.rms.order.vo.OrderLine;
import com.polsl.engineering.project.rms.order.vo.OrderLineRemoval;
import com.polsl.engineering.project.rms.order.cmd.ChangeOrderLinesCommand;
import com.polsl.engineering.project.rms.security.UserPrincipal;
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

    private static final Integer DEFAULT_PAGE_SIZE = 20;
    private static final Integer DEFAULT_PAGE_NUMBER = 0;

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
    void approve(String id, OrderPayloads.ApproveOrderRequest request) {
        var order = findByIdOrThrow(id);
        var cmd = mapper.toCommand(request);
        var result = order.approve(cmd, clock);
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
    void completeOrder(String orderId, UserPrincipal userPrincipal) {
        var order = findByIdOrThrow(orderId);
        var result = order.complete(userPrincipal, clock);
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

    List<OrderPayloads.OrderDetailsResponse> getActiveOrders() {
        return jdbcRepository.findActiveOrders()
                .stream()
                .map(mapper::toDetailsResponse)
                .toList();
    }

    OrderPayloads.OrderPageResponse searchOrders(OrderPayloads.OrderSearchRequest request) {
        var page = validatePage(request.page());
        var size = validateSize(request.size());
        return jdbcRepository.searchOrders(request, page, size);
    }

    OrderPayloads.OrderCustomerViewResponse getOrderForCustomer(String orderId) {
        return mapper.toCustomerViewResponse(findByIdOrThrow(orderId));
    }

    private Order findByIdOrThrow(String id) {
        return jdbcRepository.findById(OrderId.from(id))
                .orElseThrow(() -> new ResourceNotFoundException("Order with id %s not found".formatted(id)));
    }

    private <T> void validateActionResult(Result<T> result) {
        if (result.isFailure()) {
            throw new InvalidOrderActionException(result.getError());
        }
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
