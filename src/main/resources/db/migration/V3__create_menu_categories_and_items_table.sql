CREATE TABLE menu_categories (
                                 id UUID NOT NULL,
                                 name VARCHAR(200) NOT NULL,
                                 description VARCHAR(500),
                                 active BOOLEAN NOT NULL DEFAULT TRUE,
                                 created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                                 updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                                 version BIGINT,
                                 CONSTRAINT pk_menu_categories PRIMARY KEY (id)
);

ALTER TABLE menu_categories
    ADD CONSTRAINT uk_menu_categories_name UNIQUE (name);


CREATE TABLE menu_items (
                            id UUID NOT NULL,
                            name VARCHAR(200) NOT NULL,
                            description VARCHAR(500),
                            price NUMERIC(10, 2) NOT NULL,
                            available BOOLEAN NOT NULL DEFAULT TRUE,
                            calories INTEGER,
                            allergens VARCHAR(500),
                            vegetarian BOOLEAN NOT NULL DEFAULT FALSE,
                            vegan BOOLEAN NOT NULL DEFAULT FALSE,
                            gluten_free BOOLEAN NOT NULL DEFAULT FALSE,
                            spice_level VARCHAR(20),
                            category_id UUID NOT NULL,
                            created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                            updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                            version BIGINT,
                            CONSTRAINT pk_menu_items PRIMARY KEY (id),
                            CONSTRAINT fk_menu_items_category
                                FOREIGN KEY (category_id)
                                    REFERENCES menu_categories (id)
                                    ON DELETE CASCADE
);
