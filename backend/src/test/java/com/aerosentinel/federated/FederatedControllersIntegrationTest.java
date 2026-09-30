package com.aerosentinel.federated;

import com.aerosentinel.dto.federated.*;
import com.aerosentinel.federated.dto.AggregationResponse;
import com.aerosentinel.federated.dto.RoundDetailResponse;
import com.aerosentinel.federated.dto.RoundResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FederatedControllersIntegrationTest {

    private MockMvc roundMockMvc;
    private MockMvc nodeMockMvc;
    private MockMvc modelMockMvc;
    private MockMvc updateMockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private FederatedRoundService roundService;

    @Mock
    private FederatedNodeService nodeService;

    @Mock
    private GlobalModelService globalModelService;

    @Mock
    private ModelUpdateService modelUpdateService;

    @InjectMocks
    private FederatedRoundController roundController;

    @InjectMocks
    private FederatedNodeController nodeController;

    @InjectMocks
    private GlobalModelController globalModelController;

    @InjectMocks
    private ModelUpdateController modelUpdateController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        roundMockMvc = MockMvcBuilders.standaloneSetup(roundController).build();
        nodeMockMvc = MockMvcBuilders.standaloneSetup(nodeController).build();
        modelMockMvc = MockMvcBuilders.standaloneSetup(globalModelController).build();
        updateMockMvc = MockMvcBuilders.standaloneSetup(modelUpdateController).build();
    }

    @Test
    @DisplayName("REST: POST /api/v1/federated/rounds creates new round with 201 Created")
    void testCreateRoundEndpoint() throws Exception {
        RoundResponse response = new RoundResponse(
                "ROUND-001", "global-v1", "CREATED", 2, List.of("PUNE", "MUMBAI", "DELHI"), Instant.now()
        );
        when(roundService.startRound(any(), any(), any(), any())).thenReturn(response);

        CreateRoundRequest req = new CreateRoundRequest("ROUND-001", "global-v1", List.of("PUNE", "MUMBAI", "DELHI"), 2);

        roundMockMvc.perform(post("/api/v1/federated/rounds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roundId").value("ROUND-001"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.minQuorum").value(2));
    }

    @Test
    @DisplayName("REST: GET /api/v1/federated/rounds/{roundId} returns detailed round state")
    void testGetRoundStatusEndpoint() throws Exception {
        RoundDetailResponse detail = new RoundDetailResponse(
                "ROUND-001", "global-v1", null, "UPDATES_COLLECTING",
                2, 1, List.of("PUNE", "MUMBAI", "DELHI"),
                1200, null, null, Instant.now(), null, Collections.emptyList()
        );
        when(roundService.getRoundStatus("ROUND-001")).thenReturn(detail);

        roundMockMvc.perform(get("/api/v1/federated/rounds/ROUND-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundId").value("ROUND-001"))
                .andExpect(jsonPath("$.status").value("UPDATES_COLLECTING"))
                .andExpect(jsonPath("$.receivedUpdatesCount").value(1));
    }

    @Test
    @DisplayName("REST: POST /api/v1/federated/rounds/{roundId}/updates submits model update")
    void testSubmitModelUpdateEndpoint() throws Exception {
        ModelUpdateResponse updateRes = new ModelUpdateResponse("ROUND-001", "PUNE", "VALIDATED", 1200, Instant.now());
        when(modelUpdateService.submitUpdate(eq("ROUND-001"), any())).thenReturn(updateRes);

        SubmitModelUpdateRequest req = new SubmitModelUpdateRequest(
                "PUNE", "global-v1", "pune-v1", 1200,
                Map.of("mae", 0.43, "rmse", 0.51, "rocAuc", 0.72, "brierScore", 0.18),
                Collections.emptyList(), "storage/models/updates/pune.json"
        );

        updateMockMvc.perform(post("/api/v1/federated/rounds/ROUND-001/updates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundId").value("ROUND-001"))
                .andExpect(jsonPath("$.nodeId").value("PUNE"))
                .andExpect(jsonPath("$.status").value("VALIDATED"))
                .andExpect(jsonPath("$.sampleCount").value(1200));
    }

    @Test
    @DisplayName("REST: GET /api/v1/federated/models/active returns active consensus model")
    void testGetActiveModelEndpoint() throws Exception {
        GlobalModelResponse modelRes = new GlobalModelResponse(
                "global-v1", null, null, true, List.of("PUNE", "MUMBAI", "DELHI"),
                0, Map.of("rocAuc", 0.5, "brierScore", 0.25), "storage/models/global/global-v1.joblib", Instant.now()
        );
        when(globalModelService.getActiveModel()).thenReturn(modelRes);

        modelMockMvc.perform(get("/api/v1/federated/models/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelVersion").value("global-v1"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.artifactPath").value("storage/models/global/global-v1.joblib"));
    }

    @Test
    @DisplayName("REST: GET /api/v1/federated/models/catalog returns global model catalog")
    void testGetModelCatalogEndpoint() throws Exception {
        ModelCatalogItemResponse item1 = new ModelCatalogItemResponse(
                "global-v2", "global-v1", "ROUND-001", true, List.of("PUNE", "MUMBAI", "DELHI"),
                3000, Map.of("rocAuc", 0.72), "storage/models/global/global-v2.joblib", Instant.now()
        );
        when(globalModelService.getCatalog()).thenReturn(List.of(item1));

        modelMockMvc.perform(get("/api/v1/federated/models/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].modelVersion").value("global-v2"));
    }

    @Test
    @DisplayName("REST: Node registration & heartbeat endpoints")
    void testNodeEndpoints() throws Exception {
        FederatedNodeResponse nodeRes = new FederatedNodeResponse(
                "PUNE", "Pune Municipal Node", UUID.randomUUID(), "ONLINE", "global-v1", Instant.now(), "http://pune:8081"
        );
        when(nodeService.registerNode(any())).thenReturn(nodeRes);
        when(nodeService.recordHeartbeat(eq("PUNE"), any())).thenReturn(nodeRes);

        RegisterNodeRequest reg = new RegisterNodeRequest("PUNE", UUID.randomUUID(), "Pune Municipal Node", "http://pune:8081");

        nodeMockMvc.perform(post("/api/v1/federated/nodes/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nodeId").value("PUNE"))
                .andExpect(jsonPath("$.status").value("ONLINE"));

        nodeMockMvc.perform(post("/api/v1/federated/nodes/PUNE/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NodeHeartbeatRequest("ONLINE", "global-v1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodeId").value("PUNE"));
    }
}
