package com.polsl.engineering.project.rms.menu;

import com.polsl.engineering.project.rms.ContainersEnvironment;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MenuCategoryRepositoryTest extends ContainersEnvironment {
}
