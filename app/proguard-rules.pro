# FloatMaster intentionally has no broad application keep rule.
# WHY: Keeping com.floatmaster.** defeats R8 shrinking/obfuscation and increases release attack surface.
# Hilt, Room, Compose and kotlinx.serialization publish their required consumer rules.
# Add a narrow rule here only when a release-only R8 test demonstrates a concrete reflection requirement.
