package com.crew.evalpipeline.evaluation.service;

import com.crew.evalpipeline.conversation.entity.ConversationEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.judge", name = "provider", havingValue = "openai")
public class OpenAiCompatibleJudgeProvider implements JudgeProvider {

    @Override
    public JudgeResult judge(ConversationEntity conversation) {
        return new JudgeResult(0.5, 0.4);
    }
}
