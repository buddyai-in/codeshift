package com.codeshift.gateway;

import com.codeshift.common.ModelProfile;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * The model gateway. Every model call in CodeShift goes through here.
 *
 * <p>No agent hard-codes a provider — it asks for a capability {@link ModelProfile}
 * (reasoning / codegen / cheap / embed) and the gateway hands back a Spring AI
 * {@link ChatClient} configured with that profile's model + options. Because
 * Spring AI abstracts every provider behind one {@link ChatModel} interface,
 * switching Anthropic → OpenAI → Bedrock → Gemini → Azure → Mistral → local
 * Ollama is a matter of the active provider starter + {@code application.yml},
 * never a code change.
 *
 * <p>Phase 0 wires a single active provider. Provider-prefix routing across
 * multiple {@link ChatModel} beans (e.g. "anthropic:" vs "openai:") and fallback
 * chains are a Phase 1 enhancement — the config already carries both fields.
 */
@Service
public class ModelGateway {

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final ModelProfilesProperties profiles;

    public ModelGateway(ObjectProvider<ChatModel> chatModelProvider, ModelProfilesProperties profiles) {
        this.chatModelProvider = chatModelProvider;
        this.profiles = profiles;
    }

    /** A ready-to-use chat client for a capability tier. */
    public ChatClient chat(ModelProfile profile) {
        ChatModel model = chatModelProvider.getIfAvailable();
        if (model == null) {
            throw new IllegalStateException(
                    "No ChatModel bean available. Add a Spring AI provider starter "
                    + "(e.g. spring-ai-starter-model-anthropic) and set its API key "
                    + "to enable " + profile + " calls.");
        }
        ModelProfilesProperties.ProfileSpec spec = profiles.getProfiles().get(profile);
        ChatOptions.Builder options = ChatOptions.builder().model(modelId(profiles.modelFor(profile)));
        if (spec.getTemperature() != null) {
            options.temperature(spec.getTemperature());
        }
        if (spec.getMaxTokens() != null) {
            options.maxTokens(spec.getMaxTokens());
        }
        return ChatClient.builder(model).defaultOptions(options.build()).build();
    }

    /** The concrete {@code provider:model} a profile currently maps to. */
    public String resolvedModel(ModelProfile profile) {
        return profiles.modelFor(profile);
    }

    /** Strip the "provider:" prefix, leaving the model id Spring AI's options expect. */
    static String modelId(String spec) {
        int i = spec.indexOf(':');
        return i >= 0 ? spec.substring(i + 1) : spec;
    }
}
