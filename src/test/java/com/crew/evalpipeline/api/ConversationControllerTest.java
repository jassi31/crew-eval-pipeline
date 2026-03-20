package com.crew.evalpipeline.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.crew.evalpipeline.api.dto.ConversationDtos.ConversationAcceptedResponse;
import com.crew.evalpipeline.conversation.service.ConversationService;
import com.crew.evalpipeline.shared.DomainEnums.ConversationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ConversationController.class)
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConversationService conversationService;

    @Test
    void shouldAcceptConversationIngestionRequest() throws Exception {
        when(conversationService.ingestConversation(any()))
                .thenReturn(new ConversationAcceptedResponse("conv-123", "job-123", ConversationStatus.QUEUED));

        mockMvc.perform(post("/api/v1/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversation_id": "conv-123",
                                  "agent_version": "v1",
                                  "turns": [
                                    {
                                      "turn_id": 1,
                                      "role": "USER",
                                      "content": "I need to book a flight to NYC next week"
                                    }
                                  ],
                                  "metadata": {
                                    "total_latency_ms": 1000,
                                    "mission_completed": false
                                  }
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.conversation_id").value("conv-123"))
                .andExpect(jsonPath("$.job_id").value("job-123"))
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }
}
