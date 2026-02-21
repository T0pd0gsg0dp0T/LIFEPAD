package com.lifepad.app.domain.cbt

enum class ThinkingTrap(
    val displayName: String,
    val description: String,
    val example: String
) {
    ALL_OR_NOTHING(
        "All-or-Nothing Thinking",
        "Seeing things in black or white categories with no middle ground",
        "\"I failed one test, so I'm a total failure\""
    ),
    OVERGENERALIZATION(
        "Overgeneralization",
        "Seeing a single negative event as a never-ending pattern of defeat",
        "\"This always happens to me\""
    ),
    MENTAL_FILTER(
        "Mental Filter",
        "Dwelling exclusively on negatives while ignoring positives",
        "\"The whole day was ruined because of one comment\""
    ),
    DISQUALIFYING_POSITIVE(
        "Disqualifying the Positive",
        "Rejecting positive experiences by insisting they don't count",
        "\"That compliment doesn't count, they were just being nice\""
    ),
    JUMPING_TO_CONCLUSIONS(
        "Jumping to Conclusions",
        "Making negative interpretations without definite facts",
        "\"They must think I'm stupid\""
    ),
    MAGNIFICATION(
        "Magnification / Minimization",
        "Exaggerating the importance of negatives or shrinking the importance of positives",
        "\"This small mistake is a catastrophe\""
    ),
    EMOTIONAL_REASONING(
        "Emotional Reasoning",
        "Assuming that negative feelings reflect the way things really are",
        "\"I feel anxious, so something bad must be about to happen\""
    ),
    SHOULD_STATEMENTS(
        "Should Statements",
        "Using 'should', 'must', or 'ought to' statements to motivate yourself or others",
        "\"I should be able to handle this without help\""
    ),
    LABELING(
        "Labeling",
        "Attaching a fixed, global label to yourself or others instead of describing the specific behavior",
        "\"I'm a loser\" instead of \"I made a mistake\""
    ),
    PERSONALIZATION(
        "Personalization",
        "Seeing yourself as the cause of negative external events you were not primarily responsible for",
        "\"It's my fault they're unhappy\""
    ),
    CATASTROPHIZING(
        "Catastrophizing",
        "Expecting the worst possible outcome in any situation",
        "\"What if everything goes completely wrong?\""
    ),
    FORTUNE_TELLING(
        "Fortune Telling",
        "Predicting that things will turn out badly without adequate evidence",
        "\"I just know I'm going to mess this up\""
    )
}
