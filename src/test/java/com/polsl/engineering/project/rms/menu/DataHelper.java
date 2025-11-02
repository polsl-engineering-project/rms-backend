package com.polsl.engineering.project.rms.menu;

import org.instancio.Instancio;

import java.math.BigDecimal;
import java.util.ArrayList;

public class DataHelper {

    public static MenuCategory createMenuCategory() {
        return MenuCategory.builder()
                .id(null)
                .name("Pizza")
                .description("All kinds of napoletana pizzas.")
                .items(new ArrayList<>())
                .createdAt(null)
                .updatedAt(null)
                .version(null)
                .build();
    }

    public static MenuItem createMenuItem() {
        return MenuItem.builder()
                .id(null)
                .name("Test Item")
                .description("Test Description")
                .price(new BigDecimal("15.99"))
                .available(true)
                .calories(500)
                .allergens("nuts")
                .vegetarian(false)
                .vegan(false)
                .glutenFree(false)
                .spiceLevel(MenuItem.SpiceLevel.MILD)
                .createdAt(null)
                .updatedAt(null)
                .version(null)
                .build();
    }
}
