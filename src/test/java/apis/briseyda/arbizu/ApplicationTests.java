package apis.briseyda.arbizu;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.data.mongodb.uri=", 
    "spring.data.mongodb.host=", 
    "spring.data.mongodb.port="
})
class ApplicationTests {

	@Test
	void contextLoads() {
	}

}
