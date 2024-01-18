package main;

import com.google.gson.Gson;
import lombok.Builder;
import lombok.Data;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;

public class CrptApi {
    private final Semaphore semaphore;
    private final int requestsLimit;
    private final long interval;

    public CrptApi(int requestsLimit, long interval) {
        this.requestsLimit = requestsLimit;
        this.interval = interval;
        this.semaphore = new Semaphore(requestsLimit, true);
    }

    public static void main(String[] args) {
        CrptApi api = new CrptApi(5, 1000);

        for (int i = 0; i < 100; i++) {
            final int documentNumber = i + 1;
            Thread thread = new Thread(() ->
                    api.createDocument("Document " + documentNumber, "Signature " + documentNumber)
            );
            thread.start();
        }
    }

    public void createDocument(Object document, String signature) {
        try {
            semaphore.acquire();
            createAndSendDocument();
            System.out.println("Document: " + document.toString());
            System.out.println("Signature: " + signature);
            Thread.sleep(2000);
            semaphore.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

    private void createAndSendDocument() {
        try {
            CrptDocument crptDocument = CrptDocument.createCrptDocument();
            String jsonInputString = convertObjectToJson(crptDocument);
            sendHttpRequest(jsonInputString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String convertObjectToJson(Object object) {
        Gson gson = new Gson();
        return gson.toJson(object);
    }

    private void sendHttpRequest(String jsonInputString) throws IOException {
        URL url = new URL("https://ismp.crpt.ru/api/v3/lk/documents/create");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        setConnectionProperties(connection);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        System.out.println("Response Code: " + responseCode);

        connection.disconnect();
    }

    private void setConnectionProperties(HttpURLConnection connection) {
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Created POJO classes here based on requirements.It could be better to create each class in its own file for better readability and clean coding :)
    @Data
    @Builder
    public static class CrptDocument {
        private Description description;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private Date productionDate;
        private String productionType;
        private List<Product> products;
        private Date regDate;
        private String regNumber;

        @Data
        @Builder
        public static class Description {
            private String participantInn;
        }

        @Data
        @Builder
        public static class Product {
            private String certificateDocument;
            private Date certificateDocumentDate;
            private String certificateDocumentNumber;
            private String ownerInn;
            private String producerInn;
            private Date productionDate;
            private String tnvedCode;
            private String uitCode;
            private String uituCode;
        }

        static CrptDocument createCrptDocument() {
            return CrptDocument.builder()
                    .description(createDescription())
                    .docId("123456")
                    .docStatus("Draft")
                    .docType("LP_INTRODUCE_GOODS")
                    .importRequest(true)
                    .ownerInn("7890123456")
                    .participantInn("1234567890")
                    .producerInn("9876543210")
                    .productionDate(new Date())
                    .productionType("SampleType")
                    .products(createProducts())
                    .regDate(new Date())
                    .regNumber("REG123")
                    .build();
        }

        private static Description createDescription() {
            return Description.builder()
                    .participantInn("1234567890")
                    .build();
        }

        private static List<Product> createProducts() {
            List<Product> products = new ArrayList<>();
            Product product = Product.builder()
                    .certificateDocument("Cert123")
                    .certificateDocumentDate(new Date())
                    .certificateDocumentNumber("Cert456")
                    .ownerInn("7890123456")
                    .producerInn("9876543210")
                    .productionDate(new Date())
                    .tnvedCode("123456")
                    .uitCode("789012")
                    .uituCode("345678")
                    .build();
            products.add(product);
            return products;
        }
    }

}