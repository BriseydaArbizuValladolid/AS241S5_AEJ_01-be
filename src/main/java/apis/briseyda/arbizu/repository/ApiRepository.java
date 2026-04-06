package apis.briseyda.arbizu.repository;

import apis.briseyda.arbizu.model.ApiModel;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface ApiRepository extends ReactiveMongoRepository<ApiModel, String> {
    
}
