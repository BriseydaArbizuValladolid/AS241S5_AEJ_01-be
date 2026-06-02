package apis.briseyda.arbizu;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=" +
    "org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration," +
    "org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration," +
    "org.springframework.boot.autoconfigure.data.mongo.MongoReactiveRepositoriesAutoConfiguration"
})
class ApplicationTests {
	
	@Test
	void contextLoads() {
	}

}
