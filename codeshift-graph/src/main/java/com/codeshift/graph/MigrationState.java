package com.codeshift.graph;

import com.codeshift.bsg.model.ArchitecturePlan;
import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.TransformationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

/**
 * Shared graph state (langgraph4j).
 *
 * <p>Kept small and serialisable: it holds <em>references</em> (project id, BSG
 * version id, topological order, cursors, budget) — never heavy payloads. Large
 * artifacts (BSG nodes, code, test results) live in Postgres/S3 and are fetched
 * on demand. That is what lets a run scale to 100k+ LOC without bloating the
 * checkpoint.
 *
 * <p>The SCHEMA declares reducer channels; {@code log} appends across steps,
 * everything else overwrites with the latest value.
 */
public class MigrationState extends AgentState {

    public static final Map<String, Channel<?>> SCHEMA =
            Map.of("log", Channels.appender(ArrayList::new));

    public MigrationState(Map<String, Object> initData) {
        super(initData);
    }

    public Optional<String> phase() {
        return value("phase");
    }

    public Optional<String> projectId() {
        return value("project_id");
    }

    public List<String> topoOrder() {
        return this.<List<String>>value("topo_order").orElseGet(List::of);
    }

    public Optional<String> reviewDecision() {
        return value("review_decision");
    }

    /** The BSG produced by the Analysis Agent, awaiting review. */
    public Optional<BsgGraph> bsg() {
        return value("bsg");
    }

    /** The target architecture proposed by the Architecture Agent, awaiting review. */
    public Optional<ArchitecturePlan> architecture() {
        return value("architecture");
    }

    /** Generated code + tests from the Transformation / Test Generation agents. */
    public Optional<TransformationResult> transformation() {
        return value("transformation");
    }

    public List<String> log() {
        return this.<List<String>>value("log").orElseGet(List::of);
    }
}
