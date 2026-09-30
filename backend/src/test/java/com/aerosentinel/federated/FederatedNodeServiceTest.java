package com.aerosentinel.federated;

import com.aerosentinel.dto.federated.FederatedNodeResponse;
import com.aerosentinel.dto.federated.NodeHeartbeatRequest;
import com.aerosentinel.dto.federated.RegisterNodeRequest;
import com.aerosentinel.exception.ResourceNotFoundException;
import com.aerosentinel.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FederatedNodeServiceTest {

    @Mock
    private FederatedNodeRepository nodeRepository;

    @InjectMocks
    private FederatedNodeService nodeService;

    private FederatedNode puneNode;

    @BeforeEach
    void setUp() {
        puneNode = new FederatedNode("PUNE", "Pune Municipal Node", UUID.randomUUID(), NodeStatus.ONLINE, "global-v1");
        puneNode.setEndpointUrl("http://pune.internal:8081");
        puneNode.setLastSeenAt(Instant.now());
    }

    @Test
    @DisplayName("Node Service: Registers new node and records heartbeat")
    void testNodeRegistrationAndHeartbeat() {
        when(nodeRepository.findByNodeId("PUNE")).thenReturn(Optional.of(puneNode));
        when(nodeRepository.save(any(FederatedNode.class))).thenAnswer(i -> i.getArgument(0));

        RegisterNodeRequest regReq = new RegisterNodeRequest("PUNE", UUID.randomUUID(), "Pune Updated Node", "http://pune.internal:8082");
        FederatedNodeResponse regRes = nodeService.registerNode(regReq);

        assertThat(regRes.nodeId()).isEqualTo("PUNE");
        assertThat(regRes.nodeName()).isEqualTo("Pune Updated Node");

        // Now record heartbeat
        NodeHeartbeatRequest hbReq = new NodeHeartbeatRequest("TRAINING", "global-v2");
        FederatedNodeResponse hbRes = nodeService.recordHeartbeat("PUNE", hbReq);

        assertThat(hbRes.nodeId()).isEqualTo("PUNE");
        assertThat(hbRes.status()).isEqualTo("TRAINING");
        assertThat(hbRes.modelVersion()).isEqualTo("global-v2");
        verify(nodeRepository, atLeast(2)).save(any(FederatedNode.class));
    }

    @Test
    @DisplayName("Node Service: Verifies node status transitions")
    void testNodeStatusTransitions() {
        when(nodeRepository.findByNodeId("PUNE")).thenReturn(Optional.of(puneNode));
        when(nodeRepository.save(any(FederatedNode.class))).thenAnswer(i -> i.getArgument(0));

        FederatedNodeResponse resTraining = nodeService.updateNodeStatus("PUNE", NodeStatus.TRAINING);
        assertThat(resTraining.status()).isEqualTo("TRAINING");

        FederatedNodeResponse resReady = nodeService.updateNodeStatus("PUNE", NodeStatus.UPDATE_READY);
        assertThat(resReady.status()).isEqualTo("UPDATE_READY");

        FederatedNodeResponse resError = nodeService.updateNodeStatus("PUNE", NodeStatus.ERROR);
        assertThat(resError.status()).isEqualTo("ERROR");
    }

    @Test
    @DisplayName("Node Service: Detects stale nodes and marks OFFLINE")
    void testCheckOfflineNodes() {
        FederatedNode staleNode = new FederatedNode("DELHI", "Delhi DPCC Node", null, NodeStatus.ONLINE, "global-v1");
        staleNode.setLastSeenAt(Instant.now().minus(Duration.ofMinutes(15)));

        when(nodeRepository.findAll()).thenReturn(List.of(puneNode, staleNode));
        when(nodeRepository.findAllByOrderByNodeIdAsc()).thenReturn(List.of(puneNode, staleNode));

        nodeService.checkOfflineNodes(Duration.ofMinutes(10));

        assertThat(staleNode.getStatus()).isEqualTo(NodeStatus.OFFLINE);
        verify(nodeRepository).save(staleNode);
    }

    @Test
    @DisplayName("Node Service: Throws ValidationException for empty nodeId")
    void testRegisterNode_ValidationFailure() {
        assertThatThrownBy(() -> nodeService.registerNode(new RegisterNodeRequest("", null, "", null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Node Service: Throws ResourceNotFoundException for unknown node heartbeat")
    void testHeartbeat_NotFound() {
        when(nodeRepository.findByNodeId("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> nodeService.recordHeartbeat("UNKNOWN", new NodeHeartbeatRequest("ONLINE", "v1")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
