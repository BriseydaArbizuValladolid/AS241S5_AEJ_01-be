package apis.briseyda.arbizu;

import apis.briseyda.arbizu.repository.ApiRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class ApplicationTests {
    @MockBean
    private ApiRepository apiRepository;
	@Test
	void contextLoads() {
	}

}
