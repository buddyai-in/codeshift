package com.codeshift.gateway;

import com.codeshift.common.ModelProfile;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Per-role model configuration, bound from {@code codeshift.model.*}.
 *
 * <p>This is the heart of LLM-agnosticism: each capability profile maps to a
 * {@code provider:model} spec plus options. Swapping Claude for Gemini, Bedrock,
 * Azure, Mistral or a local Ollama model on any role is a config change — no Java
 * code changes. Example {@code application.yml}:
 *
 * <pre>
 * codeshift:
 *   model:
 *     profiles:
 *       REASONING: { model: "anthropic:claude-opus-4-8",        temperature: 0.0 }
 *       CODEGEN:   { model: "anthropic:claude-sonnet-4-6",      temperature: 0.0 }
 *       CHEAP:     { model: "anthropic:claude-haiku-4-5-20251001", temperature: 0.0 }
 *       EMBED:     { model: "openai:text-embedding-3-small" }
 * </pre>
 */
@ConfigurationProperties(prefix = "codeshift.model")
public class ModelProfilesProperties {

    /** Optional fallback specs per profile (used on provider outage / rate limit). */
    private Map<ModelProfile, ProfileSpec> profiles = new EnumMap<>(ModelProfile.class);
    private Map<ModelProfile, String> fallbacks = new EnumMap<>(ModelProfile.class);

    public Map<ModelProfile, ProfileSpec> getProfiles() {
        return profiles;
    }

    public void setProfiles(Map<ModelProfile, ProfileSpec> profiles) {
        this.profiles = profiles;
    }

    public Map<ModelProfile, String> getFallbacks() {
        return fallbacks;
    }

    public void setFallbacks(Map<ModelProfile, String> fallbacks) {
        this.fallbacks = fallbacks;
    }

    /** The concrete {@code provider:model} a profile resolves to (for logging / the ledger). */
    public String modelFor(ModelProfile profile) {
        ProfileSpec spec = profiles.get(profile);
        if (spec == null || spec.getModel() == null) {
            throw new IllegalStateException("No model configured for profile " + profile
                    + " (set codeshift.model.profiles." + profile + ".model)");
        }
        return spec.getModel();
    }

    public String fallbackFor(ModelProfile profile) {
        return fallbacks.get(profile);
    }

    /** A single profile's model + generation options. */
    public static class ProfileSpec {
        private String model;
        private Double temperature = 0.0;
        private Integer maxTokens;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }
    }
}
