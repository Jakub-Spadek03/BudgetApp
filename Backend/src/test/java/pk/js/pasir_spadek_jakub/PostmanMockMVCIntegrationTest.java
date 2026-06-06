package pk.js.pasir_spadek_jakub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@ActiveProfiles("test")
class PostmanMockMVCIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        this.mockMvc = webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // =========================
    // Helpery
    // =========================

    private String uniqueEmail(String prefix) {
        return prefix + "_" + UUID.randomUUID() + "@test.com";
    }

    private String registerBody(String username, String email, String password) {
        return """
                {
                  "username": "%s",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(username, email, password);
    }

    private String loginBody(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }

    private String transactionBody(Double amount, String type, String tags, String notes) {
        return """
                {
                  "amount": %s,
                  "type": "%s",
                  "tags": "%s",
                  "notes": "%s"
                }
                """.formatted(amount, type, tags, notes);
    }

    private void register(String username, String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(username, email, password)))
                .andExpect(status().isOk());
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }

    private Long createTransaction(String token, Double amount, String type, String tags, String notes) throws Exception {
        String response = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(amount, type, tags, notes)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("id").asLong();
    }

    // =========================
    // 0. Setup - Rejestracja
    // =========================

    @Test
    void shouldRegisterJanKowalski() throws Exception {
        String email = uniqueEmail("jan");
        String body = registerBody("Jan Kowalski", email, "123456");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("Jan Kowalski"))
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void shouldRegisterMarcinKowalski() throws Exception {
        String email = uniqueEmail("marcin");
        String body = registerBody("Marcin Kowalski", email, "123456");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("Marcin Kowalski"))
                .andExpect(jsonPath("$.email").value(email));
    }

    // =========================
    // 1. Authentication
    // =========================

    @Test
    void shouldLoginJanKowalski() throws Exception {
        String email = uniqueEmail("jan-login");
        register("Jan Kowalski", email, "123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void shouldLoginMarcinKowalski() throws Exception {
        String email = uniqueEmail("marcin-login");
        register("Marcin Kowalski", email, "123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    // =========================
    // 2. POST - Dodawanie transakcji
    // =========================

    @Test
    void shouldAddIncomeTransactionForJan() throws Exception {
        String email = uniqueEmail("jan-post-income");
        register("Jan Kowalski", email, "123456");
        String janToken = loginAndGetToken(email, "123456");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + janToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(1000.0, "INCOME", "Pensja", "Pensja Jana")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.amount").value(1000.0))
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.tags").value("Pensja"))
                .andExpect(jsonPath("$.notes").value("Pensja Jana"));
    }

    @Test
    void shouldAddExpenseTransactionForJan() throws Exception {
        String email = uniqueEmail("jan-post-expense");
        register("Jan Kowalski", email, "123456");
        String janToken = loginAndGetToken(email, "123456");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + janToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(250.0, "EXPENSE", "Zakupy", "Zakupy Jana")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.amount").value(250.0))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.tags").value("Zakupy"))
                .andExpect(jsonPath("$.notes").value("Zakupy Jana"));
    }

    @Test
    void shouldAddIncomeTransactionForMarcin() throws Exception {
        String email = uniqueEmail("marcin-post-income");
        register("Marcin Kowalski", email, "123456");
        String marcinToken = loginAndGetToken(email, "123456");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + marcinToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(500.0, "INCOME", "Premia", "Premia Marcina")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.amount").value(500.0))
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.tags").value("Premia"))
                .andExpect(jsonPath("$.notes").value("Premia Marcina"));
    }

    @Test
    void shouldReturn4xxWhenCreatingTransactionWithoutLogin() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(111.0, "INCOME", "Test", "Bez logowania")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldReturn400ForInvalidTransactionData() throws Exception {
        String email = uniqueEmail("jan-invalid");
        register("Jan Kowalski", email, "123456");
        String janToken = loginAndGetToken(email, "123456");

        String invalidBody = """
                {
                  "amount": -10,
                  "type": "INCOME",
                  "tags": "Test",
                  "notes": "Bledne dane"
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + janToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    // =========================
    // 3. GET - Wyświetlanie transakcji
    // =========================

    @Test
    void shouldGetAllTransactionsForJan() throws Exception {
        String janEmail = uniqueEmail("jan-get-all");
        String marcinEmail = uniqueEmail("marcin-get-all");

        register("Jan Kowalski", janEmail, "123456");
        register("Marcin Kowalski", marcinEmail, "123456");

        String janToken = loginAndGetToken(janEmail, "123456");
        String marcinToken = loginAndGetToken(marcinEmail, "123456");

        createTransaction(janToken, 1000.0, "INCOME", "Pensja", "Pensja Jana");
        createTransaction(janToken, 200.0, "EXPENSE", "Zakupy", "Zakupy Jana");
        createTransaction(marcinToken, 500.0, "INCOME", "Premia", "Premia Marcina");

        String response = mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + janToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json).hasSize(2);
        assertThat(response).contains("Pensja Jana");
        assertThat(response).contains("Zakupy Jana");
        assertThat(response).doesNotContain("Premia Marcina");
    }

    @Test
    void shouldGetAllTransactionsForMarcin() throws Exception {
        String janEmail = uniqueEmail("jan-get-all-2");
        String marcinEmail = uniqueEmail("marcin-get-all-2");

        register("Jan Kowalski", janEmail, "123456");
        register("Marcin Kowalski", marcinEmail, "123456");

        String janToken = loginAndGetToken(janEmail, "123456");
        String marcinToken = loginAndGetToken(marcinEmail, "123456");

        createTransaction(janToken, 1000.0, "INCOME", "Pensja", "Pensja Jana");
        createTransaction(marcinToken, 500.0, "INCOME", "Premia", "Premia Marcina");

        String response = mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + marcinToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json).hasSize(1);
        assertThat(response).contains("Premia Marcina");
        assertThat(response).doesNotContain("Pensja Jana");
    }

    @Test
    void shouldReturn4xxWhenGettingTransactionsWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldGetTransactionByIdForOwner() throws Exception {
        String email = uniqueEmail("jan-get-id");
        register("Jan Kowalski", email, "123456");
        String janToken = loginAndGetToken(email, "123456");

        Long transactionId = createTransaction(janToken, 1000.0, "INCOME", "Pensja", "Pensja Jana");

        mockMvc.perform(get("/api/transactions/" + transactionId)
                        .header("Authorization", "Bearer " + janToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transactionId))
                .andExpect(jsonPath("$.notes").value("Pensja Jana"));
    }

    @Test
    void shouldReturn403WhenJanGetsMarcinsTransactionById() throws Exception {
        String janEmail = uniqueEmail("jan-get-other");
        String marcinEmail = uniqueEmail("marcin-get-other");

        register("Jan Kowalski", janEmail, "123456");
        register("Marcin Kowalski", marcinEmail, "123456");

        String janToken = loginAndGetToken(janEmail, "123456");
        String marcinToken = loginAndGetToken(marcinEmail, "123456");

        Long marcinTransactionId = createTransaction(marcinToken, 500.0, "INCOME", "Premia", "Premia Marcina");

        mockMvc.perform(get("/api/transactions/" + marcinTransactionId)
                        .header("Authorization", "Bearer " + janToken))
                .andExpect(status().isForbidden());
    }

    // =========================
    // 4. PUT - Modyfikacja transakcji
    // =========================

    @Test
    void shouldUpdateOwnTransactionAsJan() throws Exception {
        String email = uniqueEmail("jan-put-own");
        register("Jan Kowalski", email, "123456");
        String janToken = loginAndGetToken(email, "123456");

        Long transactionId = createTransaction(janToken, 1000.0, "INCOME", "Pensja", "Pensja Jana");

        String updateBody = """
                {
                  "amount": 1500.0,
                  "type": "INCOME",
                  "tags": "Pensja",
                  "notes": "Pensja Jana po edycji"
                }
                """;

        mockMvc.perform(put("/api/transactions/" + transactionId)
                        .header("Authorization", "Bearer " + janToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1500.0))
                .andExpect(jsonPath("$.notes").value("Pensja Jana po edycji"));
    }

    @Test
    void shouldReturn403WhenJanUpdatesMarcinsTransaction() throws Exception {
        String janEmail = uniqueEmail("jan-put-other");
        String marcinEmail = uniqueEmail("marcin-put-other");

        register("Jan Kowalski", janEmail, "123456");
        register("Marcin Kowalski", marcinEmail, "123456");

        String janToken = loginAndGetToken(janEmail, "123456");
        String marcinToken = loginAndGetToken(marcinEmail, "123456");

        Long marcinTransactionId = createTransaction(marcinToken, 500.0, "INCOME", "Premia", "Premia Marcina");

        String updateBody = """
                {
                  "amount": 999.0,
                  "type": "EXPENSE",
                  "tags": "Hack",
                  "notes": "To nie powinno przejsc"
                }
                """;

        mockMvc.perform(put("/api/transactions/" + marcinTransactionId)
                        .header("Authorization", "Bearer " + janToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn4xxWhenUpdatingWithoutLogin() throws Exception {
        String updateBody = """
                {
                  "amount": 999.0,
                  "type": "EXPENSE",
                  "tags": "Hack",
                  "notes": "Bez logowania"
                }
                """;

        mockMvc.perform(put("/api/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().is4xxClientError());
    }

    // =========================
    // 5. DELETE - Usuwanie transakcji
    // =========================

    @Test
    void shouldDeleteOwnTransactionAsJan() throws Exception {
        String email = uniqueEmail("jan-delete-own");
        register("Jan Kowalski", email, "123456");
        String janToken = loginAndGetToken(email, "123456");

        Long transactionId = createTransaction(janToken, 300.0, "EXPENSE", "Zakupy", "Do usuniecia");

        mockMvc.perform(delete("/api/transactions/" + transactionId)
                        .header("Authorization", "Bearer " + janToken))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(get("/api/transactions/" + transactionId)
                        .header("Authorization", "Bearer " + janToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenJanDeletesMarcinsTransaction() throws Exception {
        String janEmail = uniqueEmail("jan-delete-other");
        String marcinEmail = uniqueEmail("marcin-delete-other");

        // Rejestracja obu użytkowników
        register("Jan Kowalski", janEmail, "123456");
        register("Marcin Kowalski", marcinEmail, "123456");

        // Logowanie
        String janToken = loginAndGetToken(janEmail, "123456");
        String marcinToken = loginAndGetToken(marcinEmail, "123456");

        // Marcin tworzy transakcję
        Long marcinTransactionId = createTransaction(
                marcinToken,
                500.0,
                "INCOME",
                "Premia",
                "Premia Marcina"
        );

        // Jan próbuje usunąć transakcję Marcina → 403
        mockMvc.perform(delete("/api/transactions/" + marcinTransactionId)
                        .header("Authorization", "Bearer " + janToken))
                .andExpect(status().isForbidden());
    }
}