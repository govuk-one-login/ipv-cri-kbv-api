package uk.gov.di.cri.experian.kbv.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import spark.Spark;
import uk.gov.di.cri.experian.kbv.api.resource.KBVResource;
import uk.gov.di.cri.experian.kbv.api.service.StorageService;

public class KBVApi {
    private final KBVResource kbvResource;

    public KBVApi() {
        try {
            Spark.port(8080);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            StorageService storageService = new StorageService();
            this.kbvResource = new KBVResource(storageService, objectMapper);

            mapRoutes();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not initialise API", e); // TODO: create a dedicated Exception class
        }
    }

    private void mapRoutes() {
        Spark.post("/session", this.kbvResource.createSession); // provide the user attributes
        // provide the session id
        // get a question back
        Spark.get("/question", this.kbvResource.question);
        Spark.post("/answer", this.kbvResource.answer);
    }
}
