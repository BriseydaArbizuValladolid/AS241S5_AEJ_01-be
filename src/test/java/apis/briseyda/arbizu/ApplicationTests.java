package apis.briseyda.arbizu;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnableAutoConfiguration(exclude = {MongoReactiveAutoConfiguration.class})
class ApplicationTests {

	@Test
	void contextLoads() {
	}

}
