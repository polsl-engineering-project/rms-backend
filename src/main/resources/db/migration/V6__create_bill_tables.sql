CREATE TABLE bills (
                       id UUID PRIMARY KEY,
                       table_number INTEGER NOT NULL,
                       bill_status VARCHAR(20) NOT NULL,
                       payment_method VARCHAR(20),
                       waiter_first_name VARCHAR(100) NOT NULL,
                       waiter_last_name VARCHAR(100) NOT NULL,
                       waiter_employee_id VARCHAR(50) NOT NULL,
                       total_amount DECIMAL(10, 2) NOT NULL,
                       paid_amount DECIMAL(10,2),
                       opened_at TIMESTAMP NOT NULL,
                       closed_at TIMESTAMP,
                       paid_at TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL,
                       version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_bills_table_number ON bills(table_number);
CREATE INDEX idx_bills_status ON bills(bill_status);
CREATE INDEX idx_bills_opened_at ON bills(opened_at);

CREATE TABLE bill_lines (
                            id UUID PRIMARY KEY,
                            bill_id UUID NOT NULL,
                            menu_item_id VARCHAR(255) NOT NULL,
                            quantity INTEGER NOT NULL,
                            unit_price DECIMAL(10, 2) NOT NULL,
                            CONSTRAINT fk_bill_lines_bill FOREIGN KEY (bill_id) REFERENCES bills(id) ON DELETE CASCADE
);

CREATE INDEX idx_bill_lines_bill_id ON bill_lines(bill_id);
CREATE INDEX idx_bill_lines_menu_item_id ON bill_lines(menu_item_id);
