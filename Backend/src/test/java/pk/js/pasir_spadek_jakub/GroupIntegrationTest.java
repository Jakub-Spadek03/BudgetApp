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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@ActiveProfiles("test")
class GroupIntegrationTest {

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

    private void register(String username, String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, email, password)))
                .andExpect(status().isOk());
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }

    private String graphql(String query, String token) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("query", query));
        var req = post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(req)
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private Long createGroup(String token, String name) throws Exception {
        String response = graphql(
                "mutation { createGroup(groupDTO: { name: \"%s\" }) { id name ownerId } }".formatted(name),
                token
        );
        JsonNode json = objectMapper.readTree(response);
        return json.get("data").get("createGroup").get("id").asLong();
    }

    private Long addMember(String token, String userEmail, Long groupId) throws Exception {
        String response = graphql(
                "mutation { addMember(membershipDTO: { userEmail: \"%s\", groupId: \"%d\" }) { id userId groupId userEmail } }"
                        .formatted(userEmail, groupId),
                token
        );
        JsonNode json = objectMapper.readTree(response);
        return json.get("data").get("addMember").get("id").asLong();
    }

    private Long getUserIdFromMembers(String token, Long groupId, String email) throws Exception {
        String response = graphql(
                "{ groupMembers(groupId: \"%d\") { id userId userEmail } }".formatted(groupId),
                token
        );
        JsonNode members = objectMapper.readTree(response).get("data").get("groupMembers");
        for (JsonNode m : members) {
            if (m.get("userEmail").asText().equals(email)) {
                return m.get("userId").asLong();
            }
        }
        return null;
    }

    private Long getMembershipIdFromMembers(String token, Long groupId, String email) throws Exception {
        String response = graphql(
                "{ groupMembers(groupId: \"%d\") { id userId userEmail } }".formatted(groupId),
                token
        );
        JsonNode members = objectMapper.readTree(response).get("data").get("groupMembers");
        for (JsonNode m : members) {
            if (m.get("userEmail").asText().equals(email)) {
                return m.get("id").asLong();
            }
        }
        return null;
    }

    // =========================
    // 1. Grupy
    // =========================

    @Test
    void shouldCreateGroupAndAddOwnerAsMember() throws Exception {
        String janEmail = uniqueEmail("jan-group");
        register("Jan", janEmail, "haslo123");
        String janToken = loginAndGetToken(janEmail, "haslo123");

        Long groupId = createGroup(janToken, "GrupaTest");
        assertThat(groupId).isNotNull();

        String members = graphql(
                "{ groupMembers(groupId: \"%d\") { id userEmail } }".formatted(groupId),
                janToken
        );
        assertThat(members).contains(janEmail);

        String myGroups = graphql("{ myGroups { id name } }", janToken);
        assertThat(myGroups).contains("GrupaTest");
    }

    @Test
    void shouldOnlyOwnerBeAbleToAddMember() throws Exception {
        String janEmail = uniqueEmail("jan-addmember");
        String marcinEmail = uniqueEmail("marcin-addmember");
        register("Jan", janEmail, "haslo123");
        register("Marcin", marcinEmail, "haslo123");
        String janToken = loginAndGetToken(janEmail, "haslo123");
        String marcinToken = loginAndGetToken(marcinEmail, "haslo123");

        Long groupId = createGroup(janToken, "GrupaOwner");

        // Marcin próbuje dodać Jana — błąd
        String errorResponse = graphql(
                "mutation { addMember(membershipDTO: { userEmail: \"%s\", groupId: \"%d\" }) { id } }"
                        .formatted(janEmail, groupId),
                marcinToken
        );
        assertThat(errorResponse).contains("errors");

        // Jan dodaje Marcina — sukces
        String successResponse = graphql(
                "mutation { addMember(membershipDTO: { userEmail: \"%s\", groupId: \"%d\" }) { id userEmail } }"
                        .formatted(marcinEmail, groupId),
                janToken
        );
        assertThat(successResponse).contains(marcinEmail);
    }

    @Test
    void shouldReturnGroupMembersOnlyToGroupMember() throws Exception {
        String janEmail = uniqueEmail("jan-members");
        String marcinEmail = uniqueEmail("marcin-members");
        String obcyEmail = uniqueEmail("obcy-members");
        register("Jan", janEmail, "haslo123");
        register("Marcin", marcinEmail, "haslo123");
        register("Obcy", obcyEmail, "haslo123");
        String janToken = loginAndGetToken(janEmail, "haslo123");
        String obcyToken = loginAndGetToken(obcyEmail, "haslo123");

        Long groupId = createGroup(janToken, "GrupaMembers");
        addMember(janToken, marcinEmail, groupId);

        // Marcin (członek) może zobaczyć członków
        String marcinToken = loginAndGetToken(marcinEmail, "haslo123");
        String membersResponse = graphql(
                "{ groupMembers(groupId: \"%d\") { id userEmail } }".formatted(groupId),
                marcinToken
        );
        assertThat(membersResponse).contains(janEmail);

        // Obcy nie może zobaczyć członków
        String obcyResponse = graphql(
                "{ groupMembers(groupId: \"%d\") { id userEmail } }".formatted(groupId),
                obcyToken
        );
        assertThat(obcyResponse).contains("errors");
    }

    @Test
    void shouldReturnGroupDebtsOnlyToGroupMember() throws Exception {
        String janEmail = uniqueEmail("jan-debts");
        String obcyEmail = uniqueEmail("obcy-debts");
        register("Jan", janEmail, "haslo123");
        register("Obcy", obcyEmail, "haslo123");
        String janToken = loginAndGetToken(janEmail, "haslo123");
        String obcyToken = loginAndGetToken(obcyEmail, "haslo123");

        Long groupId = createGroup(janToken, "GrupaDebts");

        // Jan (członek) może zobaczyć długi
        String janResponse = graphql(
                "{ groupDebts(groupId: \"%d\") { id amount } }".formatted(groupId),
                janToken
        );
        assertThat(janResponse).doesNotContain("errors");

        // Obcy nie może zobaczyć długów
        String obcyResponse = graphql(
                "{ groupDebts(groupId: \"%d\") { id amount } }".formatted(groupId),
                obcyToken
        );
        assertThat(obcyResponse).contains("errors");
    }

    @Test
    void shouldGroupExpenseCreateDebtsFromOthersToCurrentUser() throws Exception {
        String janEmail = uniqueEmail("jan-expense");
        String marcinEmail = uniqueEmail("marcin-expense");
        register("Jan", janEmail, "haslo123");
        register("Marcin", marcinEmail, "haslo123");
        String janToken = loginAndGetToken(janEmail, "haslo123");

        Long groupId = createGroup(janToken, "GrupaExpense");
        addMember(janToken, marcinEmail, groupId);

        graphql(
                "mutation { addGroupTransaction(groupTransactionDTO: { groupId: \"%d\", amount: 100, type: \"EXPENSE\", title: \"Pizza\" }) }"
                        .formatted(groupId),
                janToken
        );

        String debts = graphql(
                "{ groupDebts(groupId: \"%d\") { id amount title debtor { email } creditor { email } } }".formatted(groupId),
                janToken
        );
        assertThat(debts).contains("Pizza");
        assertThat(debts).contains(marcinEmail);
        assertThat(debts).contains(janEmail);
    }

    @Test
    void shouldRemovingMemberNotDeleteHistoricalDebts() throws Exception {
        String janEmail = uniqueEmail("jan-remove");
        String marcinEmail = uniqueEmail("marcin-remove");
        register("Jan", janEmail, "haslo123");
        register("Marcin", marcinEmail, "haslo123");
        String janToken = loginAndGetToken(janEmail, "haslo123");

        Long groupId = createGroup(janToken, "GrupaRemove");
        Long marcinMembershipId = addMember(janToken, marcinEmail, groupId);

        graphql(
                "mutation { addGroupTransaction(groupTransactionDTO: { groupId: \"%d\", amount: 60, type: \"EXPENSE\", title: \"Kawa\" }) }"
                        .formatted(groupId),
                janToken
        );

        // Usuń Marcina
        graphql(
                "mutation { removeMember(membershipId: \"%d\") }".formatted(marcinMembershipId),
                janToken
        );

        // Długi dalej istnieją
        String debts = graphql(
                "{ groupDebts(groupId: \"%d\") { id amount title } }".formatted(groupId),
                janToken
        );
        assertThat(debts).contains("Kawa");
    }

    @Test
    void shouldNotBeAbleToRemoveOwnerFromGroup() throws Exception {
        String janEmail = uniqueEmail("jan-noremove");
        register("Jan", janEmail, "haslo123");
        String janToken = loginAndGetToken(janEmail, "haslo123");

        Long groupId = createGroup(janToken, "GrupaNoRemove");
        Long janMembershipId = getMembershipIdFromMembers(janToken, groupId, janEmail);

        String response = graphql(
                "mutation { removeMember(membershipId: \"%d\") }".formatted(janMembershipId),
                janToken
        );
        assertThat(response).contains("errors");
    }

    @Test
    void shouldNonOwnerNotBeAbleToDeleteGroup() throws Exception {
        String janEmail = uniqueEmail("jan-nodelete");
        String marcinEmail = uniqueEmail("marcin-nodelete");
        register("Jan", janEmail, "haslo123");
        register("Marcin", marcinEmail, "haslo123");
        String janToken = loginAndGetToken(janEmail, "haslo123");
        String marcinToken = loginAndGetToken(marcinEmail, "haslo123");

        Long groupId = createGroup(janToken, "GrupaNoDelete");
        addMember(janToken, marcinEmail, groupId);

        String response = graphql(
                "mutation { deleteGroup(id: \"%d\") }".formatted(groupId),
                marcinToken
        );
        assertThat(response).contains("errors");
    }

    @Test
    void shouldCreateDebtOnlyBetweenGroupMembers() throws Exception {
        String janEmail = uniqueEmail("jan-debt");
        String marcinEmail = uniqueEmail("marcin-debt");
        register("Jan", janEmail, "haslo123");
        register("Marcin", marcinEmail, "haslo123");
        String janToken = loginAndGetToken(janEmail, "haslo123");

        Long groupId = createGroup(janToken, "GrupaDebt");
        addMember(janToken, marcinEmail, groupId);

        Long janId = getUserIdFromMembers(janToken, groupId, janEmail);
        Long marcinId = getUserIdFromMembers(janToken, groupId, marcinEmail);

        String response = graphql(
                "mutation { createDebt(debtDTO: { debtorId: \"%d\", creditorId: \"%d\", groupId: \"%d\", amount: 35, title: \"Pizza\" }) { id amount title } }"
                        .formatted(marcinId, janId, groupId),
                janToken
        );
        assertThat(response).contains("Pizza");
        assertThat(response).doesNotContain("errors");
    }

    @Test
    void shouldRejectDebtToSelfAndOutsideGroupMember() throws Exception {
        String janEmail = uniqueEmail("jan-debtreject");
        String obcyEmail = uniqueEmail("obcy-debtreject");
        register("Jan", janEmail, "haslo123");
        register("Obcy", obcyEmail, "haslo123");
        String janToken = loginAndGetToken(janEmail, "haslo123");

        Long groupId = createGroup(janToken, "GrupaDebtReject");
        Long janId = getUserIdFromMembers(janToken, groupId, janEmail);

        // Dług do samego siebie
        String selfDebt = graphql(
                "mutation { createDebt(debtDTO: { debtorId: \"%d\", creditorId: \"%d\", groupId: \"%d\", amount: 10, title: \"Test\" }) { id } }"
                        .formatted(janId, janId, groupId),
                janToken
        );
        assertThat(selfDebt).contains("errors");

        // Dług z osobą spoza grupy — najpierw pobierz id obcego
        String obcyToken = loginAndGetToken(obcyEmail, "haslo123");
        String obcyGroupResponse = graphql(
                "mutation { createGroup(groupDTO: { name: \"TempGroup\" }) { id } }",
                obcyToken
        );
        Long tempGroupId = objectMapper.readTree(obcyGroupResponse).get("data").get("createGroup").get("id").asLong();
        Long obcyId = getUserIdFromMembers(obcyToken, tempGroupId, obcyEmail);

        String outsideDebt = graphql(
                "mutation { createDebt(debtDTO: { debtorId: \"%d\", creditorId: \"%d\", groupId: \"%d\", amount: 10, title: \"Test\" }) { id } }"
                        .formatted(obcyId, janId, groupId),
                janToken
        );
        assertThat(outsideDebt).contains("errors");
    }

    @Test
    void shouldOwnerCreateDebtBetweenOtherMembers() throws Exception {
        String janEmail = uniqueEmail("jan-ownerdebt");
        String marcinEmail = uniqueEmail("marcin-ownerdebt");
        register("Jan", janEmail, "haslo123");
        register("Marcin", marcinEmail, "haslo123");
        String janToken = loginAndGetToken(janEmail, "haslo123");

        Long groupId = createGroup(janToken, "GrupaOwnerDebt");
        addMember(janToken, marcinEmail, groupId);

        Long janId = getUserIdFromMembers(janToken, groupId, janEmail);
        Long marcinId = getUserIdFromMembers(janToken, groupId, marcinEmail);

        String response = graphql(
                "mutation { createDebt(debtDTO: { debtorId: \"%d\", creditorId: \"%d\", groupId: \"%d\", amount: 17.50, title: \"Zupa\" }) { id amount title } }"
                        .formatted(marcinId, janId, groupId),
                janToken
        );
        assertThat(response).contains("Zupa");
        assertThat(response).doesNotContain("errors");
    }

    @Test
    void shouldMemberCreateDebtOnlyAsParticipant() throws Exception {
        String janEmail = uniqueEmail("jan-memberdebt");
        String marcinEmail = uniqueEmail("marcin-memberdebt");
        register("Jan", janEmail, "haslo123");
        register("Marcin", marcinEmail, "haslo123");
        String janToken = loginAndGetToken(janEmail, "haslo123");
        String marcinToken = loginAndGetToken(marcinEmail, "haslo123");

        Long groupId = createGroup(janToken, "GrupaMemberDebt");
        addMember(janToken, marcinEmail, groupId);

        Long janId = getUserIdFromMembers(janToken, groupId, janEmail);
        Long marcinId = getUserIdFromMembers(janToken, groupId, marcinEmail);

        // Marcin tworzy dług gdzie jest uczestnikiem — sukces
        String response = graphql(
                "mutation { createDebt(debtDTO: { debtorId: \"%d\", creditorId: \"%d\", groupId: \"%d\", amount: 55.50, title: \"Kawa\" }) { id amount } }"
                        .formatted(marcinId, janId, groupId),
                marcinToken
        );
        assertThat(response).doesNotContain("errors");
    }

    @Test
    void shouldParticipantDeleteDebt() throws Exception {
        String janEmail = uniqueEmail("jan-deletedebt");
        String marcinEmail = uniqueEmail("marcin-deletedebt");
        register("Jan", janEmail, "haslo123");
        register("Marcin", marcinEmail, "haslo123");
        String janToken = loginAndGetToken(janEmail, "haslo123");

        Long groupId = createGroup(janToken, "GrupaDeleteDebt");
        addMember(janToken, marcinEmail, groupId);

        Long janId = getUserIdFromMembers(janToken, groupId, janEmail);
        Long marcinId = getUserIdFromMembers(janToken, groupId, marcinEmail);

        String createResponse = graphql(
                "mutation { createDebt(debtDTO: { debtorId: \"%d\", creditorId: \"%d\", groupId: \"%d\", amount: 35, title: \"Pizza\" }) { id } }"
                        .formatted(marcinId, janId, groupId),
                janToken
        );
        Long debtId = objectMapper.readTree(createResponse).get("data").get("createDebt").get("id").asLong();

        String deleteResponse = graphql(
                "mutation { deleteDebt(debtId: \"%d\") }".formatted(debtId),
                janToken
        );
        assertThat(deleteResponse).contains("true");
    }

    @Test
    void shouldRejectDeleteDebtByNonParticipantNonOwner() throws Exception {
        String janEmail = uniqueEmail("jan-rejectdelete");
        String marcinEmail = uniqueEmail("marcin-rejectdelete");
        String nowyEmail = uniqueEmail("nowy-rejectdelete");
        register("Jan", janEmail, "haslo123");
        register("Marcin", marcinEmail, "haslo123");
        register("Nowy", nowyEmail, "haslo123");
        String janToken = loginAndGetToken(janEmail, "haslo123");
        String nowyToken = loginAndGetToken(nowyEmail, "haslo123");

        Long groupId = createGroup(janToken, "GrupaRejectDelete");
        addMember(janToken, marcinEmail, groupId);
        addMember(janToken, nowyEmail, groupId);

        Long janId = getUserIdFromMembers(janToken, groupId, janEmail);
        Long marcinId = getUserIdFromMembers(janToken, groupId, marcinEmail);

        String createResponse = graphql(
                "mutation { createDebt(debtDTO: { debtorId: \"%d\", creditorId: \"%d\", groupId: \"%d\", amount: 35, title: \"Pizza\" }) { id } }"
                        .formatted(marcinId, janId, groupId),
                janToken
        );
        Long debtId = objectMapper.readTree(createResponse).get("data").get("createDebt").get("id").asLong();

        // Nowy nie jest uczestnikiem długu ani właścicielem grupy
        String response = graphql(
                "mutation { deleteDebt(debtId: \"%d\") }".formatted(debtId),
                nowyToken
        );
        assertThat(response).contains("errors");
    }

    @Test
    void shouldOwnerDeleteDebtAsNonParticipant() throws Exception {
        String janEmail = uniqueEmail("jan-ownerdelete");
        String marcinEmail = uniqueEmail("marcin-ownerdelete");
        String nowyEmail = uniqueEmail("nowy-ownerdelete");
        register("Jan", janEmail, "haslo123");
        register("Marcin", marcinEmail, "haslo123");
        register("Nowy", nowyEmail, "haslo123");
        String janToken = loginAndGetToken(janEmail, "haslo123");

        Long groupId = createGroup(janToken, "GrupaOwnerDelete");
        addMember(janToken, marcinEmail, groupId);
        addMember(janToken, nowyEmail, groupId);

        Long marcinId = getUserIdFromMembers(janToken, groupId, marcinEmail);
        Long nowyId = getUserIdFromMembers(janToken, groupId, nowyEmail);

        // Dług między Marcinem a Nowym (Jan nie jest uczestnikiem)
        String createResponse = graphql(
                "mutation { createDebt(debtDTO: { debtorId: \"%d\", creditorId: \"%d\", groupId: \"%d\", amount: 20, title: \"Obiad\" }) { id } }"
                        .formatted(marcinId, nowyId, groupId),
                janToken
        );
        Long debtId = objectMapper.readTree(createResponse).get("data").get("createDebt").get("id").asLong();

        // Jan (właściciel) usuwa dług
        String response = graphql(
                "mutation { deleteDebt(debtId: \"%d\") }".formatted(debtId),
                janToken
        );
        assertThat(response).contains("true");
    }

    @Test
    void shouldRejectInvalidGraphQLInputs() throws Exception {
        String janEmail = uniqueEmail("jan-validation");
        register("Jan", janEmail, "haslo123");
        String janToken = loginAndGetToken(janEmail, "haslo123");

        Long groupId = createGroup(janToken, "GrupaValidation");

        // Pusta nazwa grupy
        String emptyName = graphql(
                "mutation { createGroup(groupDTO: { name: \"\" }) { id } }",
                janToken
        );
        assertThat(emptyName).contains("errors");

        Long janId = getUserIdFromMembers(janToken, groupId, janEmail);

        // Ujemna kwota długu
        String negativeAmount = graphql(
                "mutation { createDebt(debtDTO: { debtorId: \"%d\", creditorId: \"%d\", groupId: \"%d\", amount: -10, title: \"Test\" }) { id } }"
                        .formatted(janId, janId, groupId),
                janToken
        );
        assertThat(negativeAmount).contains("errors");

        // Zły typ transakcji
        String badType = graphql(
                "mutation { addGroupTransaction(groupTransactionDTO: { groupId: \"%d\", amount: 50, type: \"ZLY_TYP\", title: \"Test\" }) }"
                        .formatted(groupId),
                janToken
        );
        assertThat(badType).contains("errors");
    }

    @Test
    void shouldOwnerDeleteGroupWithDebtsAndMembers() throws Exception {
        String janEmail = uniqueEmail("jan-fulldelete");
        String marcinEmail = uniqueEmail("marcin-fulldelete");
        register("Jan", janEmail, "haslo123");
        register("Marcin", marcinEmail, "haslo123");
        String janToken = loginAndGetToken(janEmail, "haslo123");

        Long groupId = createGroup(janToken, "GrupaFullDelete");
        addMember(janToken, marcinEmail, groupId);

        graphql(
                "mutation { addGroupTransaction(groupTransactionDTO: { groupId: \"%d\", amount: 100, type: \"EXPENSE\", title: \"Pizza\" }) }"
                        .formatted(groupId),
                janToken
        );

        String deleteResponse = graphql(
                "mutation { deleteGroup(id: \"%d\") }".formatted(groupId),
                janToken
        );
        assertThat(deleteResponse).contains("true");

        String myGroups = graphql("{ myGroups { id name } }", janToken);
        assertThat(myGroups).doesNotContain("GrupaFullDelete");
    }
}