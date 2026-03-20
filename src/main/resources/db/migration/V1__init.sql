CREATE TABLE conversations (
    conversation_id VARCHAR(255) PRIMARY KEY,
    agent_version VARCHAR(255) NOT NULL,
    raw_payload JSONB NOT NULL,
    expected_outcome JSONB,
    source VARCHAR(255),
    status VARCHAR(64) NOT NULL,
    total_latency_ms BIGINT NOT NULL DEFAULT 0,
    mission_completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE conversation_tags (
    conversation_id VARCHAR(255) NOT NULL REFERENCES conversations (conversation_id) ON DELETE CASCADE,
    tag VARCHAR(255) NOT NULL
);

CREATE TABLE turns (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL REFERENCES conversations (conversation_id) ON DELETE CASCADE,
    turn_index INTEGER NOT NULL,
    turn_id VARCHAR(255) NOT NULL,
    role VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    timestamp TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE tool_calls (
    id BIGSERIAL PRIMARY KEY,
    turn_id BIGINT NOT NULL REFERENCES turns (id) ON DELETE CASCADE,
    tool_name VARCHAR(255) NOT NULL,
    parameters JSONB NOT NULL,
    result JSONB,
    latency_ms BIGINT,
    execution_status VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE feedback (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL UNIQUE REFERENCES conversations (conversation_id) ON DELETE CASCADE,
    user_rating INTEGER,
    ops_quality VARCHAR(64),
    ops_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE annotations (
    id BIGSERIAL PRIMARY KEY,
    feedback_id BIGINT NOT NULL REFERENCES feedback (id) ON DELETE CASCADE,
    type VARCHAR(64) NOT NULL,
    label VARCHAR(255) NOT NULL,
    annotator_id VARCHAR(255) NOT NULL,
    confidence DOUBLE PRECISION,
    ground_truth JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE annotator_profiles (
    annotator_id VARCHAR(255) PRIMARY KEY,
    weight DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE evaluation_jobs (
    job_id VARCHAR(255) PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL REFERENCES conversations (conversation_id) ON DELETE CASCADE,
    status VARCHAR(64) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    locked_at TIMESTAMPTZ,
    lock_owner VARCHAR(255),
    failure_reason TEXT,
    trigger_source VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE evaluations (
    evaluation_id VARCHAR(255) PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL REFERENCES conversations (conversation_id) ON DELETE CASCADE,
    evaluator_version VARCHAR(64) NOT NULL,
    overall_score DOUBLE PRECISION NOT NULL,
    response_quality_score DOUBLE PRECISION NOT NULL,
    tool_accuracy_score DOUBLE PRECISION NOT NULL,
    coherence_score DOUBLE PRECISION NOT NULL,
    heuristic_score DOUBLE PRECISION NOT NULL,
    overall_confidence DOUBLE PRECISION NOT NULL,
    response_quality_confidence DOUBLE PRECISION NOT NULL,
    tool_accuracy_confidence DOUBLE PRECISION NOT NULL,
    coherence_confidence DOUBLE PRECISION NOT NULL,
    heuristic_confidence DOUBLE PRECISION NOT NULL,
    evaluator_human_divergence DOUBLE PRECISION,
    review_decision VARCHAR(64) NOT NULL,
    needs_human_review BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE evaluation_issues (
    id BIGSERIAL PRIMARY KEY,
    evaluation_id VARCHAR(255) NOT NULL REFERENCES evaluations (evaluation_id) ON DELETE CASCADE,
    issue_type VARCHAR(64) NOT NULL,
    severity VARCHAR(64) NOT NULL,
    description TEXT NOT NULL,
    details JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE improvement_suggestions (
    suggestion_id VARCHAR(255) PRIMARY KEY,
    scope VARCHAR(64) NOT NULL,
    target_type VARCHAR(128) NOT NULL,
    target_key VARCHAR(255) NOT NULL,
    agent_version VARCHAR(255) NOT NULL,
    suggestion TEXT NOT NULL,
    rationale TEXT NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    frequency INTEGER NOT NULL,
    expected_impact VARCHAR(255) NOT NULL,
    evidence_conversation_ids JSONB NOT NULL,
    status VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE calibration_snapshots (
    id BIGSERIAL PRIMARY KEY,
    score_correlation DOUBLE PRECISION NOT NULL,
    issue_precision DOUBLE PRECISION NOT NULL,
    issue_recall DOUBLE PRECISION NOT NULL,
    coverage DOUBLE PRECISION NOT NULL,
    blind_spots JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_conversations_agent_version ON conversations (agent_version);
CREATE INDEX idx_conversations_status ON conversations (status);
CREATE INDEX idx_conversations_created_at ON conversations (created_at);
CREATE INDEX idx_turns_conversation_turn_index ON turns (conversation_id, turn_index);
CREATE INDEX idx_tool_calls_tool_name ON tool_calls (tool_name);
CREATE INDEX idx_annotations_type ON annotations (type);
CREATE INDEX idx_evaluation_jobs_status_retry ON evaluation_jobs (status, next_retry_at);
CREATE INDEX idx_evaluation_jobs_conversation ON evaluation_jobs (conversation_id);
CREATE INDEX idx_evaluations_conversation_created_at ON evaluations (conversation_id, created_at);
CREATE INDEX idx_evaluation_issues_type ON evaluation_issues (issue_type);
CREATE INDEX idx_suggestions_agent_version_status ON improvement_suggestions (agent_version, status);
