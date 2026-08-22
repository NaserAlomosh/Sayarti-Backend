package com.sayarti.backend.fuel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sayarti.backend.AbstractIntegrationTest;
import com.sayarti.backend.auth.repository.EmailVerificationOtpRepository;
import com.sayarti.backend.auth.repository.RefreshTokenRepository;
import com.sayarti.backend.fuel.repository.FuelRecordRepository;
import com.sayarti.backend.user.repository.UserRepository;
import com.sayarti.backend.vehicle.repository.VehicleRepository;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FuelRecordIntegrationTest extends AbstractIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired FuelRecordRepository fuelRecords;
    @Autowired VehicleRepository vehicles;
    @Autowired RefreshTokenRepository tokens;
    @Autowired EmailVerificationOtpRepository otps;
    @Autowired UserRepository users;

    @BeforeEach
    @AfterEach
    void clearDatabase() {
        fuelRecords.deleteAll();
        vehicles.deleteAll();
        tokens.deleteAll();
        otps.deleteAll();
        users.deleteAll();
    }

    static Stream<String> liquidPowertrains() {
        return Stream.of("GASOLINE", "DIESEL", "HYBRID", "PLUG_IN_HYBRID");
    }

    @ParameterizedTest
    @MethodSource("liquidPowertrains")
    void supportedPowertrainsCreateRecordsAndCalculateCost(String powertrain) throws Exception {
        Session session = session("supported-" + powertrain + "@example.com");
        UUID vehicleId = vehicle(session, powertrain, 10000);
        mvc.perform(post(url(vehicleId)).header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON).content(createBody(10000, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalCost").value(37.31))
                .andExpect(jsonPath("$.data.currencyCode").value("JOD"));
    }

    @Test
    void electricAndInvalidValuesAreRejected() throws Exception {
        Session session = session("invalid@example.com");
        UUID electric = vehicle(session, "ELECTRIC", 10000);
        mvc.perform(post(url(electric)).header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON).content(createBody(10000, "USD")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("FUEL_NOT_SUPPORTED_FOR_VEHICLE"));
        UUID gasoline = vehicle(session, "GASOLINE", 10000);
        for (String body : new String[] {
                createBody(10000, "USD").replace("\"quantityLiters\":45.5",
                        "\"quantityLiters\":0"),
                createBody(10000, "USD").replace("\"quantityLiters\":45.5",
                        "\"quantityLiters\":-1"),
                createBody(10000, "USD").replace("\"pricePerLiter\":0.82",
                        "\"pricePerLiter\":0"),
                createBody(10000, "USD").replace("\"pricePerLiter\":0.82",
                        "\"pricePerLiter\":-1")}) {
            mvc.perform(post(url(gasoline)).header("Authorization", bearer(session))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post(url(gasoline)).header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON).content(createBody(10000, "ZZZ")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("CURRENCY_NOT_SUPPORTED"));
    }

    @Test
    void odometerRulesUpdateVehicleAtomically() throws Exception {
        Session session = session("mileage@example.com");
        UUID vehicleId = vehicle(session, "GASOLINE", 10000);
        create(session, vehicleId, createBody(9999, "USD")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("INVALID_VEHICLE_MILEAGE"));
        create(session, vehicleId, createBody(10000, "USD")).andExpect(status().isCreated());
        create(session, vehicleId, createBody(12000, "USD")).andExpect(status().isCreated());
        assertThat(vehicles.findById(vehicleId).orElseThrow().getCurrentMileage()).isEqualTo(12000);
    }

    @Test
    void ownershipUsesNotFoundSemanticsForEveryOperation() throws Exception {
        Session owner = session("fuel-owner@example.com");
        Session other = session("fuel-other@example.com");
        UUID vehicleId = vehicle(owner, "GASOLINE", 10000);
        UUID recordId = recordId(create(owner, vehicleId, createBody(10000, "USD"))
                .andReturn().getResponse().getContentAsString());
        create(other, vehicleId, createBody(10000, "USD")).andExpect(status().isNotFound());
        mvc.perform(get(url(vehicleId)).header("Authorization", bearer(other)))
                .andExpect(status().isNotFound());
        mvc.perform(get(url(vehicleId) + "/" + recordId).header("Authorization", bearer(other)))
                .andExpect(status().isNotFound());
        mvc.perform(patch(url(vehicleId) + "/" + recordId).header("Authorization", bearer(other))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pricePerLiter\":1}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete(url(vehicleId) + "/" + recordId)
                        .header("Authorization", bearer(other))).andExpect(status().isNotFound());
    }

    @Test
    void missingVehicleAndFuelRecordUseNotFoundSemantics() throws Exception {
        Session session = session("missing-fuel@example.com");
        UUID missingVehicle = UUID.randomUUID();
        create(session, missingVehicle, createBody(10000, "USD"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VEHICLE_NOT_FOUND"));
        mvc.perform(get(url(missingVehicle)).header("Authorization", bearer(session)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VEHICLE_NOT_FOUND"));

        UUID vehicleId = vehicle(session, "GASOLINE", 10000);
        UUID missingRecord = UUID.randomUUID();
        mvc.perform(get(url(vehicleId) + "/" + missingRecord)
                        .header("Authorization", bearer(session)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("FUEL_RECORD_NOT_FOUND"));
        mvc.perform(patch(url(vehicleId) + "/" + missingRecord)
                        .header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pricePerLiter\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("FUEL_RECORD_NOT_FOUND"));
        mvc.perform(delete(url(vehicleId) + "/" + missingRecord)
                        .header("Authorization", bearer(session)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("FUEL_RECORD_NOT_FOUND"));
    }

    @Test
    void patchRecalculatesAndCannotChangeOdometerOrVehicle() throws Exception {
        Session session = session("patch@example.com");
        UUID vehicleId = vehicle(session, "GASOLINE", 10000);
        UUID otherVehicle = vehicle(session, "GASOLINE", 0);
        UUID recordId = recordId(create(session, vehicleId, createBody(10000, "USD"))
                .andReturn().getResponse().getContentAsString());
        mvc.perform(patch(url(vehicleId) + "/" + recordId).header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"quantityLiters":10,"pricePerLiter":1.23456,"odometerKm":1,
                         "vehicleId":"%s","totalCost":1}
                        """.formatted(otherVehicle)))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(url(vehicleId) + "/" + recordId).header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantityLiters\":10,\"pricePerLiter\":1.2345}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCost").value(12.345))
                .andExpect(jsonPath("$.data.odometerKm").value(10000))
                .andExpect(jsonPath("$.data.vehicleId").value(vehicleId.toString()));
    }

    @Test
    void softDeleteExcludesReadsAndDoesNotReduceMileage() throws Exception {
        Session session = session("delete-fuel@example.com");
        UUID vehicleId = vehicle(session, "GASOLINE", 10000);
        UUID first = recordId(create(session, vehicleId, createBody(11000, "USD"))
                .andReturn().getResponse().getContentAsString());
        Thread.sleep(5);
        UUID second = recordId(create(session, vehicleId,
                createBody(12000, "USD").replace("2026-08-20", "2026-08-21"))
                .andReturn().getResponse().getContentAsString());
        mvc.perform(get(url(vehicleId)).header("Authorization", bearer(session)))
                .andExpect(jsonPath("$.data[0].id").value(second.toString()))
                .andExpect(jsonPath("$.data[1].id").value(first.toString()));
        mvc.perform(delete(url(vehicleId) + "/" + second).header("Authorization", bearer(session)))
                .andExpect(status().isOk());
        mvc.perform(get(url(vehicleId)).header("Authorization", bearer(session)))
                .andExpect(jsonPath("$.data.length()").value(1));
        mvc.perform(get(url(vehicleId) + "/" + second).header("Authorization", bearer(session)))
                .andExpect(status().isNotFound());
        assertThat(fuelRecords.findById(second).orElseThrow().getDeletedAt()).isNotNull();
        assertThat(vehicles.findById(vehicleId).orElseThrow().getCurrentMileage()).isEqualTo(12000);
    }

    @Test
    void deletedVehicleCannotReceiveRecords() throws Exception {
        Session session = session("deleted-vehicle-fuel@example.com");
        UUID vehicleId = vehicle(session, "GASOLINE", 10000);
        mvc.perform(delete("/api/v1/vehicles/" + vehicleId)
                        .header("Authorization", bearer(session))).andExpect(status().isOk());
        create(session, vehicleId, createBody(10000, "USD")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VEHICLE_NOT_FOUND"));
    }

    @Test
    void existingRecordRetainsCurrencyAfterDefaultChanges() throws Exception {
        Session session = session("currency-retention@example.com");
        UUID vehicleId = vehicle(session, "GASOLINE", 10000);
        UUID recordId = recordId(create(session, vehicleId, createBody(10000, null))
                .andReturn().getResponse().getContentAsString());
        var user = users.findById(session.userId()).orElseThrow();
        user.changeDefaultCurrency("USD");
        users.saveAndFlush(user);
        mvc.perform(get(url(vehicleId) + "/" + recordId).header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currencyCode").value("JOD"));
    }

    @Test
    void summaryCalculatesAggregateAndUtcMonthlyCosts() throws Exception {
        Session session = session("fuel-summary@example.com");
        UUID vehicleId = vehicle(session, "GASOLINE", 1000);
        create(session, vehicleId, fuelBody(1000, "30", "2", "2026-07-31T23:59:59Z"))
                .andExpect(status().isCreated());
        create(session, vehicleId, fuelBody(1300, "20", "3", "2026-08-01T00:00:00Z"))
                .andExpect(status().isCreated());
        create(session, vehicleId, fuelBody(1800, "50", "2", "2026-08-31T23:59:59Z"))
                .andExpect(status().isCreated());

        mvc.perform(get(url(vehicleId) + "/summary?month=2026-08")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalFuelQuantity").value(100.0))
                .andExpect(jsonPath("$.data.totalDistanceKm").value(800.0))
                .andExpect(jsonPath("$.data.averageFuelEfficiencyKmPerLiter").value(11.4286))
                .andExpect(jsonPath("$.data.averageLitersPer100Km").value(8.75))
                .andExpect(jsonPath("$.data.costsByCurrency[0].totalFuelCost").value(220.0))
                .andExpect(jsonPath("$.data.costsByCurrency[0].monthlyFuelCost").value(160.0))
                .andExpect(jsonPath("$.data.costsByCurrency[0].costPerKm").value(0.275));
    }

    @Test
    void summaryExcludesDeletedRecordsAndProtectsOwnershipAndMissingVehicles() throws Exception {
        Session owner = session("summary-owner@example.com");
        Session other = session("summary-other@example.com");
        UUID vehicleId = vehicle(owner, "GASOLINE", 1000);
        UUID first = recordId(create(owner, vehicleId,
                fuelBody(1000, "40", "2", "2026-08-01T10:00:00Z"))
                .andReturn().getResponse().getContentAsString());
        create(owner, vehicleId, fuelBody(1500, "40", "2", "2026-08-02T10:00:00Z"));
        mvc.perform(delete(url(vehicleId) + "/" + first).header("Authorization", bearer(owner)))
                .andExpect(status().isOk());
        mvc.perform(get(url(vehicleId) + "/summary?month=2026-08")
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalFuelQuantity").value(40.0))
                .andExpect(jsonPath("$.data.totalDistanceKm").value(0.0))
                .andExpect(jsonPath("$.data.averageFuelEfficiencyKmPerLiter").doesNotExist());
        mvc.perform(get(url(vehicleId) + "/summary").header("Authorization", bearer(other)))
                .andExpect(status().isNotFound());
        mvc.perform(get(url(UUID.randomUUID()) + "/summary")
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isNotFound());
    }

    @Test
    void emptyLiquidAndElectricSummariesDocumentUnavailableConsumption() throws Exception {
        Session session = session("empty-summary@example.com");
        UUID liquid = vehicle(session, "DIESEL", 1000);
        UUID electric = vehicle(session, "ELECTRIC", 1000);
        mvc.perform(get(url(liquid) + "/summary?month=2026-08")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liquidFuelCalculationsSupported").value(true))
                .andExpect(jsonPath("$.data.costsByCurrency.length()").value(0));
        mvc.perform(get(url(electric) + "/summary?month=2026-08")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liquidFuelCalculationsSupported").value(false))
                .andExpect(jsonPath("$.data.averageFuelEfficiencyKmPerLiter").doesNotExist());
    }

    private org.springframework.test.web.servlet.ResultActions create(
            Session session, UUID vehicleId, String body) throws Exception {
        return mvc.perform(post(url(vehicleId)).header("Authorization", bearer(session))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
    private UUID vehicle(Session session, String powertrain, long mileage) throws Exception {
        String extra = switch (powertrain) {
            case "DIESEL" -> "\"fuelType\":\"DIESEL\",\"fuelTankCapacityLiters\":50,";
            case "ELECTRIC" -> "\"batteryCapacityKwh\":70,\"estimatedRangeKm\":500,";
            case "PLUG_IN_HYBRID" -> "\"fuelType\":\"GASOLINE_95\",\"fuelTankCapacityLiters\":40,\"batteryCapacityKwh\":15,";
            default -> "\"fuelType\":\"GASOLINE_95\",\"fuelTankCapacityLiters\":50,";
        };
        String response = mvc.perform(post("/api/v1/vehicles").header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"brand":"Test","model":"Car","year":2025,"powertrainType":"%s",
                         %s "currentMileage":%d}
                        """.formatted(powertrain, extra, mileage)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(response).path("data").path("id").asText());
    }
    private Session session(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Fuel","lastName":"Owner","email":"%s",
                 "password":"StrongPass1","countryCode":"JO"}
                """.formatted(email))).andExpect(status().isCreated());
        var user = users.findByEmailIgnoreCaseAndDeletedAtIsNull(email).orElseThrow();
        user.verifyEmail();
        users.saveAndFlush(user);
        String response = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"StrongPass1\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode data = json.readTree(response).path("data");
        return new Session(user.getId(), data.path("accessToken").asText());
    }
    private String createBody(long odometer, String currency) {
        String currencyField = currency == null ? "" : "\"currencyCode\":\"" + currency + "\",";
        return """
                {"odometerKm":%d,"quantityLiters":45.5,"pricePerLiter":0.82,%s
                 "filledAt":"2026-08-20T10:00:00Z","fullTank":true,
                 "stationName":"Station","notes":"Road trip"}
                """.formatted(odometer, currencyField);
    }
    private String fuelBody(long odometer, String quantity, String price, String filledAt) {
        return """
                {"odometerKm":%d,"quantityLiters":%s,"pricePerLiter":%s,
                 "currencyCode":"USD","filledAt":"%s","fullTank":true}
                """.formatted(odometer, quantity, price, filledAt);
    }
    private UUID recordId(String response) throws Exception {
        return UUID.fromString(json.readTree(response).path("data").path("id").asText());
    }
    private String url(UUID vehicleId) { return "/api/v1/vehicles/" + vehicleId + "/fuel-records"; }
    private String bearer(Session session) { return "Bearer " + session.token(); }
    private record Session(UUID userId, String token) { }
}
