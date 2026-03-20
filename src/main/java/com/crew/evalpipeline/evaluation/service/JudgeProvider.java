package com.crew.evalpipeline.evaluation.service;

import com.crew.evalpipeline.conversation.entity.ConversationEntity;

public interface JudgeProvider {

    JudgeResult judge(ConversationEntity conversation);

    record JudgeResult(double score, double confidence) {
    }
}
