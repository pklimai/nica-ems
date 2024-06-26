package ru.mipt.npm.nica.ems

val periodConfig = ParameterConfig("period_number", "int", true, "Period Number")
val runConfig = ParameterConfig("run_number", "int", true, "Run Number")

val softwareConfig = ParameterConfig("software_version", "string", false, "Software Version")

val beamParticleConfig = ParameterConfig("beam_particle", "string", false, "Beam Particle")
val targetParticleConfig = ParameterConfig("target_particle", "string", false, "Target Particle")
val energyConfig = ParameterConfig("energy", "float", true, "Energy, GeV")

val limitConfig = ParameterConfig("limit", "int", false, "Limit [$DEFAULT_LIMIT_FOR_WEB]")
val offsetConfig = ParameterConfig("offset", "int", false, "Offset")
