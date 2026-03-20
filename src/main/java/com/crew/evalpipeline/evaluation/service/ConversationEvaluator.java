package com.crew.evalpipeline.evaluation.service;

import com.crew.evalpipeline.evaluation.model.EvaluationComponentResult;
import com.crew.evalpipeline.evaluation.model.EvaluationContext;

public interface ConversationEvaluator {

    EvaluationComponentResult evaluate(EvaluationContext context);
}
