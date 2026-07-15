package com.codeshift.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeshift.common.ModelProfile;
import com.codeshift.gateway.ModelProfilesProperties.ProfileSpec;
import org.junit.jupiter.api.Test;

/** The gateway is config-driven and LLM-agnostic. */
class ModelProfilesTest {

    private ModelProfilesProperties props() {
        ModelProfilesProperties p = new ModelProfilesProperties();
        ProfileSpec reasoning = new ProfileSpec();
        reasoning.setModel("anthropic:claude-opus-4-8");
        ProfileSpec codegen = new ProfileSpec();
        codegen.setModel("openai:gpt-4.1-mini");
        p.getProfiles().put(ModelProfile.REASONING, reasoning);
        p.getProfiles().put(ModelProfile.CODEGEN, codegen);
        return p;
    }

    @Test
    void resolvesModelPerProfileFromConfig() {
        ModelProfilesProperties p = props();
        // Swapping providers is a config change, not a code change:
        assertThat(p.modelFor(ModelProfile.REASONING)).isEqualTo("anthropic:claude-opus-4-8");
        assertThat(p.modelFor(ModelProfile.CODEGEN)).startsWith("openai:");
    }

    @Test
    void modelIdStripsProviderPrefix() {
        assertThat(ModelGateway.modelId("anthropic:claude-opus-4-8")).isEqualTo("claude-opus-4-8");
        assertThat(ModelGateway.modelId("llama3.1")).isEqualTo("llama3.1");
    }

    @Test
    void missingProfileFailsLoudly() {
        assertThatThrownBy(() -> new ModelProfilesProperties().modelFor(ModelProfile.EMBED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No model configured");
    }
}
