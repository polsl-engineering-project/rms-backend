# Order aggregate — Summary

This document summarizes the Order aggregate implementation and behavior in the RMS project. It focuses on the domain logic (state, commands, invariants), how events are produced by the aggregate, and how persistence and outbox integration are handled by the repository and services.

Files reviewed to produce this summary (selected):
- `Order` (domain aggregate implementation)
- `OrderService` (application service that delegates commands to the aggregate)
- `OrderRepository` and `OrderDbMapper` (persistence layer; JDBC implementation)
- `OrderMapper`, `OrderPayloads` (DTOs / commands / API payloads)
- `OrderLinesRemover` (algorithm to remove quantities from order lines)
- `OrderOutboxService`, `OrderOutboxEvent`, `OrderOutboxEventRepository` (outbox persistence)
- Command records in `order.cmd` and event types in `order.event`

Note: I read source files to extract behavior and implementation details; references to specific methods, constants or files below come from those sources.

---

High-level responsibilities of the aggregate

- Represent a single customer order (pickup or delivery) and own the business logic for transitions and invariants.
- Validate requests to place an order (ASAP vs SCHEDULED), change lines, cancel, approve and progress through kitchen/delivery states.
- Emit domain events for significant state transitions. The aggregate stores emitted events in-memory until the aggregate's changes are persisted; the application service then persists events to the outbox.

Core value objects and state

- Identity and versioning
  - `OrderId` (UUID wrapper) as identity; aggregate stores `version` (long) for optimistic concurrency control.

- Important fields on aggregate `Order`:
  - `type` (OrderType.PICKUP or DELIVERY)
  - `deliveryMode` (enum DeliveryMode: ASAP or SCHEDULED)
  - `status` (OrderStatus enum covering lifecycle states)
  - `lines` (List<OrderLine>) — order lines contain menu item id, quantity, unit price (Money) and menu item version snapshot
  - `deliveryAddress` (for delivery orders)
  - `customerInfo` (name/phone)
  - `scheduledFor` (LocalTime) for scheduled deliveries/pickups
  - `estimatedPreparationMinutes` (Integer) — adjusted by kitchen or lines changes
  - `cancellationReason` (String) when canceled
  - `placedAt` and `updatedAt` timestamps
  - `events` (List<OrderEvent>) — transient list of domain events emitted by aggregate

Aggregate construction and invariants

- Creation flows are implemented as static factory methods:
  - `placePickUpOrder(PlacePickUpOrderCommand, Clock)`
  - `placeDeliveryOrder(PlaceDeliveryOrderCommand, Clock)`

- Both creation methods call `validateOrderPlacement(...)` which enforces:
  - For `DeliveryMode.SCHEDULED`, `scheduledFor` must be provided and be in the future (compared with injected `Clock`).
  - For `DeliveryMode.ASAP`, `scheduledFor` must be null.
  - There must be at least one order line.

- On successful creation the aggregate sets:
  - `status = PENDING_APPROVAL`
  - `placedAt` and `updatedAt` = now (Clock)
  - `version = 0`
  - Emits either `PickUpOrderPlacedEvent` or `DeliveryOrderPlacedEvent` with a snapshot of lines and relevant metadata.

State transitions / behavior methods

Each domain action is exposed as a method that returns a `Result<Void>` (success or failure string). The application-layer `OrderService` translates failures into `InvalidOrderActionException`.

- `approveByFrontDesk(Clock)`
  - Allowed from `PENDING_APPROVAL` only.
  - Sets status -> `APPROVED_BY_FRONT_DESK`, updates `updatedAt`, emits `OrderApprovedByFrontDeskEvent`.

- `approveByKitchen(ApproveOrderByKitchenCommand, Clock)`
  - Allowed from `APPROVED_BY_FRONT_DESK` only.
  - For ASAP orders requires `estimatedPreparationMinutes` > 0.
  - Sets status -> `CONFIRMED`, sets `estimatedPreparationMinutes`, updates `updatedAt`, emits `OrderApprovedByKitchenEvent`.

- `markAsReady(Clock)`
  - Allowed from `CONFIRMED` only.
  - For PICKUP orders sets status -> `READY_FOR_PICKUP`; for DELIVERY -> `READY_FOR_DRIVER`.
  - Updates `updatedAt`, emits `OrderMarkedAsReadyEvent` (with new status).

- `startDelivery(Clock)`
  - Only valid for DELIVERY orders in `READY_FOR_DRIVER`.
  - Sets status -> `IN_DELIVERY`, updates `updatedAt`, emits `OrderDeliveryStartedEvent`.

- `complete(Clock)`
  - For PICKUP: only when status `READY_FOR_PICKUP`.
  - For DELIVERY: only when status `IN_DELIVERY`.
  - Sets status -> `COMPLETED`, updates `updatedAt`, emits `OrderCompletedEvent`.

- `cancel(CancelOrderCommand, Clock)`
  - Disallowed if already COMPLETED or CANCELLED.
  - Sets status -> `CANCELLED`, records reason, updates `updatedAt`, emits `OrderCancelledEvent`.

- `changeOrderLines(ChangeOrderLinesCommand, Clock)`
  - Disallowed when order is COMPLETED, CANCELLED, or IN_DELIVERY.
  - New lines and removals are validated (cannot both add and remove same menu item in same command, must provide at least one change).
  - Removal is performed by `OrderLinesRemover.remove(...)` which implements ordered removal by highest menu item `version` first (LIFO by version):
    - It produces a snapshot copy of current lines, applies removals across sorted lines (descending version), removes or replaces line quantities accordingly.
    - Validates that requested removals are possible (item exists and sufficient quantity) and returns failure message otherwise.
  - After removals and additions, if status was READY_FOR_PICKUP / READY_FOR_DRIVER / CONFIRMED the `estimatedPreparationMinutes` may be set to provided value.
  - If status was READY_FOR_DRIVER or READY_FOR_PICKUP it moves back to CONFIRMED (because contents changed).
  - Emits `OrderLinesChangedEvent` containing snapshot of added/removed lines and updated estimated preparation time.

Event handling and emission

- The aggregate accumulates events in-memory in `events` list and exposes `pullEvents()` which returns and clears the list (so the application service can persist them to outbox).

- Emitted domain events include concrete classes in `order.event` such as:
  - `DeliveryOrderPlacedEvent`, `PickUpOrderPlacedEvent`
  - `OrderApprovedByFrontDeskEvent`, `OrderApprovedByKitchenEvent`
  - `OrderMarkedAsReadyEvent`, `OrderDeliveryStartedEvent`, `OrderCompletedEvent`, `OrderCancelledEvent`, `OrderLinesChangedEvent`

Snapshot and menu item validation at order placement

- When creating order lines from client payloads, `OrderService` calls `menuApi.getSnapshotsForOrderByIds` (Menu module integration) to obtain `MenuItemSnapshotForOrder` containing id, price, name and `version`.
- `OrderService` checks snapshot presence for each requested menu item; if missing -> throws `MenuItemNotFoundException`.
- `OrderService` compares the snapshot `version` against client-provided `version`; mismatch -> throws `MenuItemVersionMismatchException`.
- The aggregate stores in each `OrderLine` the `menuItemId` (string), `quantity`, `unitPrice` (Money created from snapshot price) and `menuItemVersion` (snapshot version). This creates an immutable price/version snapshot for the order.

Persistence and concurrency

- Persistence responsibilities are implemented in `OrderRepository` (JDBC, `JdbcTemplate`) and `OrderQueryParamsBuilder`:
  - `saveNewOrder(Order)` inserts the order row and then inserts order lines in batch.
  - `updateWithoutLines(Order)` updates the order row using `WHERE id = ? AND version = ?` and increments `version` (optimistic locking). If update affects 0 rows it throws an `OptimisticLockingFailureException`.
  - `updateWithLines(Order)` similarly updates the order row (with optimistic locking), deletes all existing order_lines for that order, and inserts fresh lines (this is safe since the method is transactional).
  - `insertLines(...)` uses `batchUpdate` for performance and logs sample params for diagnostics.

- The repository maps result set to domain using `OrderDbMapper` which reconstructs the `Order` aggregate via `Order.reconstruct(...)` including loading `order_lines` and mapping them to `OrderLine` VOs.

- Concurrency model: optimistic locking via `version` column; the repository checks update count and throws an exception to indicate stale/update conflict.

Outbox and eventual delivery of events

- The application-layer `OrderOutboxService` handles persistence of domain events to the outbox table by serializing event objects to JSON (using injected `ObjectMapper`) and saving `OrderOutboxEvent` entities via `OrderOutboxEventRepository`.
- `OrderOutboxEvent` entity stores: `id` (UUID), `orderId`, `type` (enum `OrderEventType`), `payload` (TEXT), `createdAt`, and `processed` flag.
- `OrderService.saveEvents(order)` grabs aggregate events (`pullEvents()`), and calls `outboxService.persistEvent(order.getId(), event)` for each event inside the same transaction that modified the order — this is the transactional outbox pattern (atomic write of state + outbox row).
- There are repository/processor components in the `order` package (e.g. poller, processor, cleaner) that operate on the outbox table to publish events to downstream systems and mark them processed; these were present in the codebase and coordinate eventual delivery (not described in full here).

Error handling and exceptions

- Domain action failures return `Result.failure(...)` with an error string; `OrderService` converts such results to `InvalidOrderActionException` (runtime) which results in HTTP 400 responses via common error handling.

- Notable exceptions thrown by the service layer are:
  - `MenuItemNotFoundException` — when menu snapshot missing from `menuApi`.
  - `MenuItemVersionMismatchException` — when item version mismatches client-provided version.
  - `InvalidOrderActionException` — thrown when aggregate rejects a command because invariants are violated.
  - `OrderOutboxPersistenceException` — if outbox event serialization fails.

Testing considerations and edge cases

- Critical scenarios to unit/integration test:
  - Placing orders with ASAP vs SCHEDULED delivery modes (validation of scheduled time in future).
  - Snapshot-based version checks for menu items: place order with outdated menu item version should fail.
  - Change order lines: ensure removal algorithm (`OrderLinesRemover`) removes correct quantities when multiple versions of the same menu item exist (it removes from newest `menuItemVersion` first).
  - State transition rules: ensure illegal transitions are rejected (e.g. approving by kitchen before front desk approval, marking ready when not confirmed, etc.).
  - Optimistic locking behavior: simulate concurrent updates to ensure `OptimisticLockingFailureException` is raised and surfaced.
  - Outbox persistence: ensure events are serialized and saved within the same DB transaction as order state changes.

Implementation notes / best practices observed

- The aggregate is implemented as a pure domain object with no direct DB access — persistence is isolated in `OrderRepository` (good DDD practice).
- Domain methods return `Result<T>` allowing the aggregate to represent validation failures without throwing exceptions; the service layer translates errors for the application layer.
- Use of snapshot version numbers on order lines prevents race conditions when client holds stale menu data and ensures orders preserve price/versions at placement time.
- `OrderLinesRemover` removes quantities across multiple matching lines ordered by descending `menuItemVersion` — this preserves the semantics that newer menu-item versions are consumed first when removing quantities (useful if an item was re-added in different versions).
- Transactional boundaries in `OrderService` ensure the state change and outbox rows are persisted atomically.

Appendix: quick mapping of commands → aggregate methods → emitted events

- `PlacePickUpOrderCommand` → `Order.placePickUpOrder(...)` → emits `PickUpOrderPlacedEvent`.
- `PlaceDeliveryOrderCommand` → `Order.placeDeliveryOrder(...)` → emits `DeliveryOrderPlacedEvent`.
- `Approve by front desk` → `Order.approveByFrontDesk(...)` → emits `OrderApprovedByFrontDeskEvent`.
- `Approve by kitchen` (`ApproveOrderByKitchenCommand`) → `Order.approveByKitchen(...)` → emits `OrderApprovedByKitchenEvent`.
- `Mark as ready` → `Order.markAsReady(...)` → emits `OrderMarkedAsReadyEvent`.
- `Start delivery` → `Order.startDelivery(...)` → emits `OrderDeliveryStartedEvent`.
- `Complete order` → `Order.complete(...)` → emits `OrderCompletedEvent`.
- `Cancel order` (`CancelOrderCommand`) → `Order.cancel(...)` → emits `OrderCancelledEvent`.
- `Change order lines` (`ChangeOrderLinesCommand`) → `Order.changeOrderLines(...)` → emits `OrderLinesChangedEvent`.
