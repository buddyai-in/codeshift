package com.codeshift.evals;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeshift.agents.AnalysisAgent;
import com.codeshift.bsg.BsgProducer;
import com.codeshift.gateway.ModelGateway;
import com.codeshift.gateway.ModelProfilesProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;

/**
 * The BSG-extraction release gate. It certifies the production Analysis Agent on
 * the golden corpus: if a prompt/model/producer change regresses coverage below
 * the threshold, this test fails and the change does not ship.
 *
 * <p>Runs offline — the gateway reports no provider, so the agent takes its
 * deterministic path — which is exactly the floor every provider must clear.
 */
class BsgExtractionEvalTest {

    /** Minimum mean-F1 a producer must clear on the golden corpus to be certifiable. */
    private static final double GATE_THRESHOLD = 0.95;

    /** An ObjectProvider with no bean — models "no model provider configured". */
    private static <T> ObjectProvider<T> none() {
        return new ObjectProvider<>() {
            @Override public T getObject() { throw new UnsupportedOperationException(); }
            @Override public T getObject(Object... args) { throw new UnsupportedOperationException(); }
            @Override public T getIfAvailable() { return null; }
            @Override public T getIfUnique() { return null; }
        };
    }

    private BsgProducer producerUnderTest() {
        ObjectProvider<ChatModel> noModel = none();
        ModelGateway gateway = new ModelGateway(noModel, new ModelProfilesProperties());
        return new AnalysisAgent(gateway);
    }

    @Test
    void analysisAgentClearsTheGoldenCorpusGate() {
        BsgProducer producer = producerUnderTest();
        List<GoldenCase> corpus = GoldenCorpus.defaultCases();

        double meanF1 = BsgEvaluator.meanF1(producer, corpus);

        assertThat(meanF1)
                .as("mean F1 over the golden corpus must clear the release gate")
                .isGreaterThanOrEqualTo(GATE_THRESHOLD);
    }

    @Test
    void everyCaseIsScoredAndReported() {
        BsgProducer producer = producerUnderTest();
        for (GoldenCase c : GoldenCorpus.defaultCases()) {
            EvalScore score = BsgEvaluator.evaluate(producer, c);
            assertThat(score.recall())
                    .as("recall for case " + c.name())
                    .isGreaterThanOrEqualTo(GATE_THRESHOLD);
            assertThat(score.matched()).isEqualTo(score.expected());
        }
    }

    @Test
    void gateCatchesARegressedProducer() {
        // A producer that drops half the units must fail the gate — proves the gate bites.
        BsgProducer regressed = (projectId, moduleIds, projectPath) -> {
            List<String> half = moduleIds.subList(0, Math.max(1, moduleIds.size() / 2));
            return new AnalysisAgent(new ModelGateway(none(), new ModelProfilesProperties()))
                    .produce(projectId, half, null);
        };
        double meanF1 = BsgEvaluator.meanF1(regressed, GoldenCorpus.defaultCases());
        assertThat(meanF1).isLessThan(GATE_THRESHOLD);
    }
}
