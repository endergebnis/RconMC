package gg.endergebnis.rconmc.whitelist;

/** Ergebnis einer Sammelaktion: {@code succeeded} von {@code total} Spielern wurden verarbeitet. */
public record BatchResult(int succeeded, int total) {}
